package fr.thomasfar.jaf.annotations;

import lombok.AccessLevel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Entity {
    AccessLevel value() default AccessLevel.PUBLIC;

}
