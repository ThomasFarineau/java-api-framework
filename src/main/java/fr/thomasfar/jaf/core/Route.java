package fr.thomasfar.jaf.core;

import fr.thomasfar.jaf.utils.HttpMethod;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

public class Route {
    private final HttpMethod httpMethod;
    private final Pattern pattern;
    private final Class<?> controller;
    private final Method method;

    public Route(String path, HttpMethod httpMethod, Class<?> controller, Method method) {
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

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

}
