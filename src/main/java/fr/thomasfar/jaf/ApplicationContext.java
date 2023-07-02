package fr.thomasfar.jaf;

import fr.thomasfar.jaf.annotations.Annotations;
import fr.thomasfar.jaf.annotations.Controller;
import fr.thomasfar.jaf.utils.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.*;
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
        scanEntities();
        scanRepositories();
        scanServices();
        scanControllers();
    }

    private void scanEntities() {
        reflection.getClasses().forEach(clazz -> {
            if(clazz.isAnnotationPresent(Annotations.Entity.clazz())) {
                String path = clazz.getPackageName().replace(ApplicationContext.class.getPackageName() + ".", "").replace(".", "/");
                path = path + "/" + clazz.getSimpleName() + ".class";
                try {
                    String classFilePath = Objects.requireNonNull(ApplicationContext.class.getResource(path)).toURI().getPath();
                    System.out.println(classFilePath);
                    InputStream inputStream = ApplicationContext.class.getResourceAsStream(path);
                    if(inputStream == null) {
                        throw new RuntimeException("No class found for " + clazz.getName());
                    }
                    byte[] originalBytecode = inputStream.readAllBytes();
                    inputStream.close();

                    ClassWriter cw = new ClassWriter( 0);
                    ClassVisitor cv = new EntityClassVisitor(cw);
                    ClassReader cr = new ClassReader(originalBytecode);
                    cr.accept(cv, ClassReader.EXPAND_FRAMES);

                    byte[] newBytecode = cw.toByteArray();
                    EntityClassLoader classLoader = new EntityClassLoader();
                    classLoader.defineClass(clazz.getName(), newBytecode);

                    try (FileOutputStream fos = new FileOutputStream(classFilePath)) {
                        fos.write(newBytecode);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (IOException | URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void scanControllers() {
        reflection.getClasses().forEach(clazz -> {
            if (clazz.isAnnotationPresent(Annotations.Controller.clazz())) {
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
            }
        });
    }

    public void addRoute(fr.thomasfar.jaf.annotations.Route.HttpMethod httpMethod, String path, Class<?> controllerClass, Method routeMethod) {
        if (!routeMethod.getReturnType().equals(Response.class)) {
            throw new IllegalArgumentException("Route methods must return Response, " + routeMethod.getName() + " in controller " + controllerClass.getName() + " does not return Response");
        }
        routes.add(new Route(path, httpMethod, controllerClass, routeMethod));
    }

    private void scanServices() {
        reflection.getClasses().forEach(clazz -> {
            if (clazz.isAnnotationPresent(Annotations.Service.clazz())) {
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

            }

        });
    }

    private void scanRepositories() {
        reflection.getClasses().forEach(clazz -> {
            if (clazz.isAnnotationPresent(Annotations.Repository.clazz())) {
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
                        return (Response) route.getMethod().invoke(controller, finalArgs);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        return Response.badRequest("Error while invoking route's method");
                    }
                }
            }
        }
        return Response.notFound();
    }

}