package fr.thomasfar.jaf.services;

import fr.thomasfar.jaf.annotations.Inject;
import fr.thomasfar.jaf.annotations.Service;
import fr.thomasfar.jaf.entities.DefaultEntity;
import fr.thomasfar.jaf.repositories.DefaultRepository;

@Service
public class DefaultService {

    @Inject private DefaultRepository defaultRepository;

    public String hello() {
        DefaultEntity defaultEntity = new DefaultEntity( "Thomas");
        defaultEntity.setName("test");
        return defaultEntity.toJson();
    }

    public String hello(int id) {
        return "Hello World! " + id;
    }
}
