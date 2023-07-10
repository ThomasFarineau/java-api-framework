package fr.thomasfar.jaf.core;

import fr.thomasfar.jaf.annotations.*;
import fr.thomasfar.jaf.core.reflections.EntitiesHandler;
import fr.thomasfar.jaf.core.reflections.Reflection;
import fr.thomasfar.jaf.utils.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.IntStream;

public class ApplicationContext {

    private final Map<Class<?>, Object> repositoryRegistry = new HashMap<>();
    private final Map<Class<?>, Object> serviceRegistry = new HashMap<>();
    private final Map<Class<?>, Object> controllerRegistry = new HashMap<>();
    private final List<Route> routes = new ArrayList<>();
    private final String defaultPackage;
    private final String defaultRoute;
    Reflection reflection;

    public ApplicationContext(String defaultRoute, String defaultPackage) {
        this.defaultRoute = defaultRoute;
        this.defaultPackage = defaultPackage;
        initialize();
    }

    public void initialize() {
        this.reflection = new Reflection(defaultPackage);
        new EntitiesHandler(reflection);

        scanRepositories();
        scanServices();
        scanControllers();

        new WebServer(this);
    }

    private void scanControllers() {
        reflection.getAnnotedClasses(Controller.class).forEach(clazz -> {
            try {
                Object controllerInstance = clazz.getDeclaredConstructor().newInstance();
                controllerRegistry.put(clazz, controllerInstance);
                Controller controller = (Controller) clazz.getAnnotation(Annotations.Controller.clazz());
                for (Field declaredField : clazz.getDeclaredFields()) {
                    if (declaredField.isAnnotationPresent(Annotations.Inject.clazz())) {
                        Object serviceInstance = serviceRegistry.get(declaredField.getType());
                        if (serviceInstance == null) {
                            throw new RuntimeException("No service found for " + declaredField.getType());
                        }
                        declaredField.setAccessible(true);
                        declaredField.set(controllerInstance, serviceInstance);
                    }
                }
                for (Method declaredMethod : clazz.getDeclaredMethods()) {
                    if (declaredMethod.isAnnotationPresent(Annotations.Route.clazz())) {
                        fr.thomasfar.jaf.annotations.Route route = (fr.thomasfar.jaf.annotations.Route) declaredMethod.getAnnotation(Annotations.Route.clazz());
                        String realPath = defaultRoute + controller.value() + route.value();
                        realPath = realPath.replaceAll("/+", "/");
                        addRoute(route.method(), realPath, clazz, declaredMethod);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void addRoute(HttpMethod httpMethod, String path, Class<?> controllerClass, Method routeMethod) {
        if (!routeMethod.getReturnType().equals(Response.class)) {
            throw new IllegalArgumentException("Route methods must return Response, " + routeMethod.getName() + " in controller " + controllerClass.getName() + " does not return Response");
        }
        routes.add(new Route(path, httpMethod, controllerClass, routeMethod));
    }

    private void scanServices() {
        reflection.getAnnotedClasses(Service.class).forEach(clazz -> {
            try {
                Object serviceInstance = clazz.getDeclaredConstructor().newInstance();
                serviceRegistry.put(clazz, serviceInstance);
                for (Field declaredField : clazz.getDeclaredFields()) {
                    if (declaredField.isAnnotationPresent(Annotations.Inject.clazz())) {
                        Object repositoryInstance = repositoryRegistry.get(declaredField.getType());
                        if (repositoryInstance == null) {
                            throw new RuntimeException("No repository found for " + declaredField.getType());
                        }
                        declaredField.setAccessible(true);
                        declaredField.set(serviceInstance, repositoryInstance);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void scanRepositories() {
        reflection.getAnnotedClasses(Repository.class).forEach(clazz -> {
            try {
                repositoryRegistry.put(clazz, clazz.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public Response executeRoute(String method, String path) {
        for (Route route : routes) {
            if (!route.getHttpMethod().name().equals(method)) continue;
            if (route.getPattern().matcher(path).matches()) {
                Matcher matcher = route.getPattern().matcher(path);
                if (matcher.matches()) {
                    Object controller = controllerRegistry.get(route.getController());
                    List<String> pathParams = IntStream.rangeClosed(1, matcher.groupCount()).mapToObj(matcher::group).toList();
                    Class<?>[] paramTypes = route.getMethod().getParameterTypes();
                    Object[] finalArgs = new Object[paramTypes.length];
                    for (int i = 0; i < paramTypes.length; i++) {
                        if (paramTypes[i] == String.class) finalArgs[i] = pathParams.get(i);
                        else if (paramTypes[i] == int.class || paramTypes[i] == Integer.class)
                            finalArgs[i] = Integer.parseInt(pathParams.get(i));
                        else if (paramTypes[i] == long.class || paramTypes[i] == Long.class)
                            finalArgs[i] = Long.parseLong(pathParams.get(i));
                    }
                    try {
                        return (Response) route.getMethod().invoke(controller, finalArgs);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        System.out.println(e.getMessage());
                        return Response.badRequest("Error while invoking route's method");
                    }
                }
            }
        }
        return Response.notFound();
    }

}