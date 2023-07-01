package fr.thomasfar.jaf.annotations;

import java.lang.annotation.Annotation;

public enum Annotations {
    Route(fr.thomasfar.jaf.annotations.Route.class),
    Controller(fr.thomasfar.jaf.annotations.Controller.class),
    Service(fr.thomasfar.jaf.annotations.Service.class),
    Repository(fr.thomasfar.jaf.annotations.Repository.class),
    Inject(fr.thomasfar.jaf.annotations.Inject.class);

    private final Class<? extends Annotation> classObject;

    public Class<? extends Annotation> getAnnotation() {
        return classObject;
    }

    Annotations(Class<? extends Annotation> classObject) {
        this.classObject = classObject;
    }
}
