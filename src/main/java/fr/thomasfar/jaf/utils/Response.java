package fr.thomasfar.jaf.utils;

import java.util.Objects;

public record Response(int status, String body) {
    public Response {
        if (status < 100 || status > 599) {
            throw new IllegalArgumentException("Status code must be between 100 and 599");
        }
    }

    public static Response ok() {
        return new Response(200, "Ok");
    }

    public static Response ok(String body) {
        return new Response(200, body);
    }

    public static Response created() {
        return new Response(201, "Created");
    }

    public static Response created(String body) {
        return new Response(201, body);
    }

    public static Response accepted() {
        return new Response(202, "Accepted");
    }

    public static Response accepted(String body) {
        return new Response(202, body);
    }

    public static Response noContent() {
        return new Response(204, "No Content");
    }

    public static Response noContent(String body) {
        return new Response(204, body);
    }

    public static Response badRequest() {
        return new Response(400, "Bad Request");
    }

    public static Response badRequest(String body) {
        return new Response(400, body);
    }

    public static Response unauthorized() {
        return new Response(401, "Unauthorized");
    }

    public static Response unauthorized(String body) {
        return new Response(401, body);
    }

    public static Response forbidden() {
        return new Response(403, "Forbidden");
    }

    public static Response forbidden(String body) {
        return new Response(403, body);
    }

    public static Response notFound() {
        return new Response(404, "Not Found");
    }

    public static Response notFound(String body) {
        return new Response(404, body);
    }

    public static Response methodNotAllowed() {
        return new Response(405, "Method Not Allowed");
    }

    public static Response methodNotAllowed(String body) {
        return new Response(405, body);
    }

    public static Response internalServerError() {
        return new Response(500, "Internal Server Error");
    }

    public static Response internalServerError(String body) {
        return new Response(500, body);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Response) obj;
        return this.status == that.status &&
                Objects.equals(this.body, that.body);
    }

    @Override
    public String toString() {
        return "Response[" +
                "status=" + status + ", " +
                "body=" + body + ']';
    }


}
