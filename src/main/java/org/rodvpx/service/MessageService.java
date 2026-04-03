package org.rodvpx.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.rodvpx.entity.MessageEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class MessageService {

    private List<MessageEntity> messages = new ArrayList<>();
    private AtomicLong counter = new AtomicLong();

    public List<MessageEntity> findAllMessages() {
        return new ArrayList<>(messages);
    }

    public Optional<MessageEntity> findbyId(Long id) {
        return messages.stream()
                .filter(m -> m.id.equals(id))
                .findFirst();
    }

    public MessageEntity sendMessage(String sender, String content) {
        var message = new MessageEntity();
        message.id = counter.incrementAndGet();
        message.sender = sender;
        message.content = content;
        message.timestamp = java.time.LocalDateTime.now();
        messages.add(message);
        return message;
    }

    public boolean deleteMessage(Long id) {
        return messages.removeIf(m -> m.id.equals(id));
    }
}
