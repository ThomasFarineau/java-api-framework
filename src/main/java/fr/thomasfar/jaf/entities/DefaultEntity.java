package fr.thomasfar.jaf.entities;

import fr.thomasfar.jaf.annotations.Entity;
import fr.thomasfar.jaf.annotations.entities.Json;
import fr.thomasfar.jaf.annotations.entities.NotMapped;
import fr.thomasfar.jaf.annotations.entities.Required;

import java.util.UUID;

@Entity
public class DefaultEntity extends IEntity<UUID> {

    @Required
    private final String name;

    private String defaultDescription;

    @Json("test")
    private String tesT;

    @NotMapped
    private String notMapped;

    public DefaultEntity(String name) {
        super(UUID.randomUUID());
        this.name = name;
    }
}
