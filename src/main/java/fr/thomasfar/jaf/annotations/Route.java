package fr.thomasfar.jaf.annotations;

import fr.thomasfar.jaf.utils.HttpMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Route annotation is used to define a route.
 * Each Route annoted method should return a Response object.
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Route {
    String value();
    HttpMethod method() default HttpMethod.GET;
}

