package fr.thomasfar.jaf;

import fr.thomasfar.jaf.core.ApplicationContext;

public class Main {
    public static void main(String[] args) {
        new ApplicationContext("/api/v1/", Main.class.getPackageName());
    }
}
