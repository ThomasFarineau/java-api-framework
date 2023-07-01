package fr.thomasfar.jaf;

import fr.thomasfar.jaf.annotations.Annotations;
import fr.thomasfar.jaf.annotations.Controller;
import fr.thomasfar.jaf.annotations.Route;
import fr.thomasfar.jaf.utils.Pair;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class ApplicationContext {

    private final Map<Class<?>, Object> repositoryRegistry = new HashMap<>();
    private final Map<Class<?>, Object> serviceRegistry = new HashMap<>();
    private final Map<Class<?>, Object> controllerRegistry = new HashMap<>();
    private final Map<Pair<String, Route.HttpMethod>, Method> routesRegistry = new HashMap<>();
    private String defaultRoute = "/";

    Reflection reflection;

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
            if (clazz.isAnnotationPresent(Annotations.Controller.getAnnotation())) {
                try {
                    Object controllerInstance = clazz.getDeclaredConstructor().newInstance();
                    controllerRegistry.put(clazz, controllerInstance);
                    Controller controller = (Controller) clazz.getAnnotation(Annotations.Controller.getAnnotation());
                    for (Field declaredField : clazz.getDeclaredFields()) {
                        if (declaredField.isAnnotationPresent(Annotations.Inject.getAnnotation())) {
                            Object serviceInstance = serviceRegistry.get(declaredField.getType());
                            if (serviceInstance == null) {
                                throw new RuntimeException("No service found for " + declaredField.getType());
                            }
                            declaredField.setAccessible(true);
                            declaredField.set(controllerInstance, serviceInstance);
                        }
                    }
                    for (Method declaredMethod : clazz.getDeclaredMethods()) {
                        if (declaredMethod.isAnnotationPresent(Annotations.Route.getAnnotation())) {
                            Route route = (Route) declaredMethod.getAnnotation(Annotations.Route.getAnnotation());
                            String realPath = defaultRoute + controller.value() + route.value();
                            realPath = realPath.replaceAll("/+", "/");
                            routesRegistry.put(new Pair<>(realPath, route.method()), declaredMethod);
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void scanServices() {
        reflection.getClasses().forEach(clazz -> {
            if (clazz.isAnnotationPresent(Annotations.Service.getAnnotation())) {
                try {
                    Object serviceInstance = clazz.getDeclaredConstructor().newInstance();
                    serviceRegistry.put(clazz, serviceInstance);
                    for (Field declaredField : clazz.getDeclaredFields()) {
                        if (declaredField.isAnnotationPresent(Annotations.Inject.getAnnotation())) {
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
            if (clazz.isAnnotationPresent(Annotations.Repository.getAnnotation())) {
                try {
                    repositoryRegistry.put(clazz, clazz.getDeclaredConstructor().newInstance());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public Method getRoute(String method, String path) {
        return routesRegistry.get(new Pair<>(path, Route.HttpMethod.valueOf(method)));
    }

    public String executeRoute(String method, String path, Object... args) {
        Method routeMethod = getRoute(method, path);
        if (routeMethod == null) return "404 Not Found";
        Object controller = controllerRegistry.get(routeMethod.getDeclaringClass());
        try {
            return (String) routeMethod.invoke(controller, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return "500 Internal Server Error";
    }

}