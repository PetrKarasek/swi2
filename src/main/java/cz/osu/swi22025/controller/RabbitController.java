package cz.osu.swi22025.controller;

import cz.osu.swi22025.model.json.PayloadMessage;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin
public class RabbitController {

    @Autowired
    private RabbitAdmin admin;

    @Autowired
    private RabbitTemplate template;

    @GetMapping("/api/queue")
    public List<PayloadMessage> getMessages(@RequestParam String userId) {

        // 1) Validace – ať nikdy nedostaneš 500 kvůli blbému parametru
        if (userId == null || userId.trim().isEmpty()) {
            return List.of();
        }

        String queueName = "chatroom.queue." + userId.trim();

        // 2) Pokud queue neexistuje, NEPADNI – vrať prázdný seznam
        //    (a klidně ji rovnou vytvoř, aby další pokusy fungovaly)
        if (admin.getQueueInfo(queueName) == null) {
            admin.declareQueue(new Queue(queueName, true));
            return List.of();
        }

        // 3) Drain FIFO zpráv bezpečně (bez getMessageCount / race condition)
        List<PayloadMessage> received = new ArrayList<>();
        while (true) {
            Object obj = template.receiveAndConvert(queueName);
            if (obj == null) break;

            if (obj instanceof PayloadMessage msg) {
                received.add(msg);
            }
        }
        return received;
    }
}
