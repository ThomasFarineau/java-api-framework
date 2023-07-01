package fr.thomasfar.jaf;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

class Main {
    public static void main(String[] args) {
        ApplicationContext applicationContext = new ApplicationContext();
        applicationContext.initialize("fr.thomasfar.jaf");
        InetSocketAddress address = new InetSocketAddress(8080);
        try {
            HttpServer server = HttpServer.create(address, 0);
            HttpContext context = server.createContext("/");
            context.setHandler(httpExchange -> {
                String method = httpExchange.getRequestMethod();
                String path = httpExchange.getRequestURI().getPath();
                String response = applicationContext.executeRoute(method, path);
                httpExchange.sendResponseHeaders(200, response.length());
                httpExchange.getResponseBody().write(response.getBytes());
                httpExchange.close();
            });
            server.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
