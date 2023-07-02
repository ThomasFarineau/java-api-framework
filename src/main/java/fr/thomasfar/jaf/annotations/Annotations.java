package fr.thomasfar.jaf.annotations;

import java.lang.annotation.Annotation;

public enum Annotations {
    Route(fr.thomasfar.jaf.annotations.Route.class),
    Controller(fr.thomasfar.jaf.annotations.Controller.class),
    Service(fr.thomasfar.jaf.annotations.Service.class),
    Repository(fr.thomasfar.jaf.annotations.Repository.class),
    Entity(fr.thomasfar.jaf.annotations.Entity.class),
    Inject(fr.thomasfar.jaf.annotations.Inject.class),
    Json(fr.thomasfar.jaf.annotations.entities.Json.class),
    NotMapped(fr.thomasfar.jaf.annotations.entities.NotMapped.class);

    private final Class<? extends Annotation> clazz;

    Annotations(Class<? extends Annotation> classObject) {
        this.clazz = classObject;
    }

    public Class<? extends Annotation> clazz() {
        return clazz;
    }
}
