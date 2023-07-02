package fr.thomasfar.jaf.entities;

import java.util.Date;

public class IEntity<T> {
    private T id;
    private final Date createdAt = new Date();

    private Date updatedAt = new Date();

    public IEntity(T id) {
        this.id = id;
    }

    public String toJson() {
        return "{}";
    }
}
