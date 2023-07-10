package fr.thomasfar.jaf.core.reflections.entity;

import fr.thomasfar.jaf.annotations.entities.Json;

import java.lang.annotation.Annotation;
import java.util.Arrays;

public record FieldRegister(String name, Class<?> type, boolean parent, boolean isFinal, Annotation[] annotations) {

    public String descriptor() {
        return type.descriptorString();
    }

    public boolean hasAnnotation(Class<? extends Annotation> annotation) {
        return Arrays.stream(annotations).anyMatch(a -> a.annotationType().equals(annotation));
    }

    public Annotation getAnnotation(Class<Json> annotation) {
        return Arrays.stream(annotations).filter(a -> a.annotationType().equals(annotation)).findFirst().orElse(null);
    }
}

