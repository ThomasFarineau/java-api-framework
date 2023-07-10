package fr.thomasfar.jaf.core.reflections;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class Reflection {
    private final String packageName;
    private Iterable<Class<?>> classes;

    public Reflection(String packageName) {
        this.packageName = packageName;
        this.setClasses();
    }

    private static List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) return classes;
        File[] files = directory.listFiles();
        for (File file : files != null ? files : new File[0]) {
            if (file.isDirectory()) {
                assert !file.getName().contains(".");
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
            }
        }
        return classes;
    }

    private void setClasses() {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String path = packageName.replace('.', '/');
            Enumeration<URL> resources = classLoader.getResources(path);
            ArrayList<File> dirs = new ArrayList<>();
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                dirs.add(new File(resource.getFile()));
            }
            ArrayList<Class<?>> classes = new ArrayList<>();
            for (File directory : dirs) classes.addAll(findClasses(directory, packageName));
            this.classes = classes;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Iterable<Class<?>> getAnnotedClasses(Class<? extends Annotation> annotation) {
        List<Class<?>> classes = new ArrayList<>();
        for (Class<?> clazz : this.classes) {
            if (clazz.isAnnotationPresent(annotation)) classes.add(clazz);
        }
        return classes;
    }

    public String getPackageName() {
        return packageName;
    }
}
