package fr.thomasfar.jaf;

import fr.thomasfar.jaf.annotations.Annotations;
import fr.thomasfar.jaf.annotations.Controller;
import fr.thomasfar.jaf.utils.Response;
import fr.thomasfar.jaf.utils.Route;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.IntStream;

public class ApplicationContext {

    private final Map<Class<?>, Object> repositoryRegistry = new HashMap<>();
    private final Map<Class<?>, Object> serviceRegistry = new HashMap<>();
    private final Map<Class<?>, Object> controllerRegistry = new HashMap<>();
    private final List<Route> routes = new ArrayList<>();
    Reflection reflection;
    private String defaultRoute = "/";

    public ApplicationContext(String defaultRoute) {
        this.defaultRoute = defaultRoute;
    }

    public void initialize(String packageName) {
        this.reflection = new Reflection(packageName);
        scanRepositories();
        scanServices();
        scanControllers();
    }

    private void scanControllers() {
        reflection.getClasses().forEach(clazz -> {
            if (clazz.isAnnotationPresent(Annotations.Controller.getName())) {
                try {
                    Object controllerInstance = clazz.getDeclaredConstructor().newInstance();
                    controllerRegistry.put(clazz, controllerInstance);
                    Controller controller = (Controller) clazz.getAnnotation(Annotations.Controller.getName());
                    for (Field declaredField : clazz.getDeclaredFields()) {
                        if (declaredField.isAnnotationPresent(Annotations.Inject.getName())) {
                            Object serviceInstance = serviceRegistry.get(declaredField.getType());
                            if (serviceInstance == null) {
                                throw new RuntimeException("No service found for " + declaredField.getType());
                            }
                            declaredField.setAccessible(true);
                            declaredField.set(controllerInstance, serviceInstance);
                        }
                    }
                    for (Method declaredMethod : clazz.getDeclaredMethods()) {
                        if (declaredMethod.isAnnotationPresent(Annotations.Route.getName())) {
                            fr.thomasfar.jaf.annotations.Route route = (fr.thomasfar.jaf.annotations.Route) declaredMethod.getAnnotation(Annotations.Route.getName());
                            String realPath = defaultRoute + controller.value() + route.value();
                            realPath = realPath.replaceAll("/+", "/");
                            addRoute(route.method(), realPath, clazz, declaredMethod);
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public void addRoute(fr.thomasfar.jaf.annotations.Route.HttpMethod httpMethod, String path, Class<?> controllerClass, Method routeMethod) {
        routes.add(new Route(path, httpMethod, controllerClass, routeMethod));
    }

    private void scanServices() {
        reflection.getClasses().forEach(clazz -> {
            if (clazz.isAnnotationPresent(Annotations.Service.getName())) {
                try {
                    Object serviceInstance = clazz.getDeclaredConstructor().newInstance();
                    serviceRegistry.put(clazz, serviceInstance);
                    for (Field declaredField : clazz.getDeclaredFields()) {
                        if (declaredField.isAnnotationPresent(Annotations.Inject.getName())) {
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

            }

        });
    }

    private void scanRepositories() {
        reflection.getClasses().forEach(clazz -> {
            if (clazz.isAnnotationPresent(Annotations.Repository.getName())) {
                try {
                    repositoryRegistry.put(clazz, clazz.getDeclaredConstructor().newInstance());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public Response executeRoute(String method, String path, Object... args) {
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
                        Object result = route.getMethod().invoke(controller, finalArgs);
                        if (result != null) return Response.ok((String) result);
                        else return Response.noContent("Method executed successfully but returned null");
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        return Response.badRequest("Error while invoking route's method");
                    }
                }
            }
        }
        return Response.notFound();
    }

}