package fr.thomasfar.jaf.controllers;

import fr.thomasfar.jaf.annotations.Controller;
import fr.thomasfar.jaf.annotations.Inject;
import fr.thomasfar.jaf.annotations.Route;
import fr.thomasfar.jaf.services.DefaultService;

@Controller(value = "/default")
public class DefaultController {

    @Inject private DefaultService defaultService;

    @Route(value = "/thomas", method = Route.HttpMethod.GET)
    public String index() {
        return defaultService.hello();
    }
}
