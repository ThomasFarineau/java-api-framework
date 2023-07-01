package fr.thomasfar.jaf.services;

import fr.thomasfar.jaf.annotations.Inject;
import fr.thomasfar.jaf.annotations.Service;
import fr.thomasfar.jaf.repositories.DefaultRepository;

@Service
public class DefaultService {

    @Inject private DefaultRepository defaultRepository;

    public String hello() {
        return "Hello World!";
    }
}
