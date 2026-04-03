package org.rodvpx.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.rodvpx.entity.MessageEntity;
import org.rodvpx.service.MessageService;

@Path( "/message")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MessageController {

    @Inject
    MessageService messageService;

    @GET
    public Response findAll() {
        var messages = messageService.findAllMessages();
        if (messages.isEmpty()) {
            return Response.noContent().build();
        }
        return Response.ok(messages).build();
    }

    @GET
    @Path( "/{id}")
    public Response findById(@PathParam("id") Long id) {
        var message = messageService.findbyId(id);
        if (message.isPresent()) {
            return Response.ok(message.get()).build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @POST
    public Response sendMessage(MessageEntity message) {
        if (message == null
                || message.sender == null || message.sender.isBlank()
                || message.content == null || message.content.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("sender and content are required")
                    .build();
        }
        var createdMessage = messageService.sendMessage(message.sender, message.content);
        return Response.status(Response.Status.CREATED).entity(createdMessage).build();
    }

    @DELETE
    @Path( "/{id}")
    public Response deleteMessage(@PathParam("id") Long id) {
        var deleted = messageService.deleteMessage(id);
        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }
}
