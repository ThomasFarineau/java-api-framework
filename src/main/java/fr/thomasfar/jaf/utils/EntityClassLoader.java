package fr.thomasfar.jaf.utils;

public class EntityClassLoader extends ClassLoader {
    public void defineClass(String name, byte[] b) {
        defineClass(name, b, 0, b.length);
    }
}
