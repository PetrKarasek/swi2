package cz.osu.swi22025.controller;

import cz.osu.swi22025.model.json.PayloadMessage;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RestController
@CrossOrigin
public class RabbitController {
    @Autowired
    private RabbitAdmin admin;
    @Autowired
    private RabbitTemplate template;

    @GetMapping(value = "/api/queue")
    public List<PayloadMessage> getMessages(@RequestParam String userId) {
        List<PayloadMessage> receivedMessages = new ArrayList<>();
        while (Objects.requireNonNull(admin.getQueueInfo("chatroom.queue." + userId))
                .getMessageCount() != 0) {
            PayloadMessage foo = (PayloadMessage) template
                    .receiveAndConvert("chatroom.queue." + userId);
            System.out.println("Received message: " + foo.getContent());
            receivedMessages.add(foo);
        }
        return receivedMessages;
    }
}
