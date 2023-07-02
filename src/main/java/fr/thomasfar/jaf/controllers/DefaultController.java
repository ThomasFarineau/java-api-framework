package fr.thomasfar.jaf.controllers;

import fr.thomasfar.jaf.annotations.Controller;
import fr.thomasfar.jaf.annotations.Inject;
import fr.thomasfar.jaf.annotations.Route;
import fr.thomasfar.jaf.services.DefaultService;
import fr.thomasfar.jaf.utils.Response;

@Controller
public class DefaultController {

    @Inject private DefaultService defaultService;

    @Route(value = "/", method = Route.HttpMethod.GET)
    public Response index() {
        return Response.ok(defaultService.hello());
    }

    @Route(value = "/thomas/{id}/{id2}", method = Route.HttpMethod.GET)
    public Response index2(int id, int id2) {
        String r = defaultService.hello(id) + " " + defaultService.hello(id2);
        return Response.ok(r);
    }
}
