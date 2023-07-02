package fr.thomasfar.jaf.utils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Route {
    private final fr.thomasfar.jaf.annotations.Route.HttpMethod httpMethod;
    private final Pattern pattern;
    private final Class<?> controller;
    private final Method method;

    public Route(String path, fr.thomasfar.jaf.annotations.Route.HttpMethod httpMethod, Class<?> controller, Method method) {
        this.httpMethod = httpMethod;
        this.pattern = Pattern.compile("^" + path.replaceAll("\\{.*?}", "(.*?)") + "/?$");
        this.controller = controller;
        this.method = method;
    }

    public Method getMethod() {
        return method;
    }

    public Class<?> getController() {
        return controller;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public fr.thomasfar.jaf.annotations.Route.HttpMethod getHttpMethod() {
        return httpMethod;
    }

}
