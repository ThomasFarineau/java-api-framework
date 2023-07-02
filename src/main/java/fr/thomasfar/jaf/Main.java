package fr.thomasfar.jaf;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import fr.thomasfar.jaf.utils.Response;

import java.io.IOException;
import java.net.InetSocketAddress;

class Main {
    public static void main(String[] args) {
        ApplicationContext applicationContext = new ApplicationContext("/api/v1/");
        applicationContext.initialize("fr.thomasfar.jaf");
        InetSocketAddress address = new InetSocketAddress(8080);
        try {
            HttpServer server = HttpServer.create(address, 0);
            HttpContext context = server.createContext("/");
            context.setHandler(httpExchange -> {
                String method = httpExchange.getRequestMethod();
                String path = httpExchange.getRequestURI().getPath();

                Response response = applicationContext.executeRoute(method, path);
                httpExchange.sendResponseHeaders(response.status(), response.body().length());
                httpExchange.getResponseHeaders().add("Content-Type", "application/json");
                httpExchange.getResponseBody().write(response.body().getBytes());
                httpExchange.close();
            });
            server.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
