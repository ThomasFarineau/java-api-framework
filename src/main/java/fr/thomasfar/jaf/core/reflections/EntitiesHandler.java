package fr.thomasfar.jaf.core.reflections;

import fr.thomasfar.jaf.Main;
import fr.thomasfar.jaf.annotations.Entity;
import fr.thomasfar.jaf.annotations.entities.Id;
import fr.thomasfar.jaf.core.reflections.entity.EntityClassVisitor;
import fr.thomasfar.jaf.core.reflections.entity.FieldRegister;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class EntitiesHandler {

    /**
     * @param reflection Used to get all classes with the Entity annotation
     */
    public EntitiesHandler(Reflection reflection) {

        for (Class<?> entity : reflection.getAnnotedClasses(Entity.class)) {
            String path = entity.getPackageName().replace(reflection.getPackageName() + ".", "").replace(".", "/");
            path = path + "/" + entity.getSimpleName() + ".class";
            URL resource = Main.class.getResource(path);
            if (resource == null) {
                throw new RuntimeException("No class found for " + entity.getName());
            }

            try {
                String classFilePath = resource.toURI().getPath();
                InputStream inputStream = Main.class.getResourceAsStream(path);
                if (inputStream == null) {
                    throw new RuntimeException("No class found for " + entity.getName());
                }
                byte[] originalBytecode = inputStream.readAllBytes();
                inputStream.close();
                ClassWriter cw = new ClassWriter(0);
                ClassVisitor cv = new EntityClassVisitor(cw, getFields(entity));
                ClassReader cr = new ClassReader(originalBytecode);
                cr.accept(cv, ClassReader.EXPAND_FRAMES);
                byte[] newBytecode = cw.toByteArray();

                EntityClassLoader rcl = new EntityClassLoader();
                rcl.reloadClass(entity.getName(), newBytecode);

                try (FileOutputStream fos = new FileOutputStream(classFilePath)) {
                    fos.write(newBytecode);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }


        }

    }

    private static List<FieldRegister> getFields(Class<?> entity) {
        List<FieldRegister> fields = new ArrayList<>();
        for (Field field : entity.getDeclaredFields()) {
            FieldRegister fieldRegister = new FieldRegister(field.getName(), field.getType(), false, Modifier.isFinal(field.getModifiers()), field.getAnnotations());
            fields.add(fieldRegister);
        }
        Class<?> parent = entity.getSuperclass();
        while (parent != null) {
            for (Field field : parent.getDeclaredFields()) {
                FieldRegister fieldRegister = new FieldRegister(field.getName(), field.getType(), true, Modifier.isFinal(field.getModifiers()), field.getAnnotations());
                fields.add(fieldRegister);
            }
            parent = parent.getSuperclass();
        }
        for (int i = 0; i < fields.size(); i++) {
            FieldRegister field = fields.get(i);
            if (field.hasAnnotation(Id.class)) {
                fields.remove(i);
                fields.add(0, field);
                break;
            }
        }
        return fields;
    }


    private static class EntityClassLoader extends ClassLoader {
        public void reloadClass(String className, byte[] classData) throws ClassFormatError {
            defineClass(className, classData, 0, classData.length);
        }
    }
}
