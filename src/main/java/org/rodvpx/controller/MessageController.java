package org.rodvpx.controller;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path( "/message")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MessageController {

    @GET
    public String getMessage() {
        return "Retorna todas as mensagens armazenadas";
    }

    @GET
    @Path( "/{id}")
    public String getMessageById(String id) {
        return "Retorna a mensagem pelo ID " + id;
    }

    @POST
    public String postMessage(String message) {
        return "Adiciona uma nova mensagem: " + message;
    }

    @DELETE
    @Path( "/{id}")
    public String deleteMessage(String id) {
        return "Deleta a mensagem pelo ID " + id;
    }
}
