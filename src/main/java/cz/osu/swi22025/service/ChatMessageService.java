package cz.osu.pesa.swi22025.service;

import cz.osu.pesa.swi22025.config.RabbitMQConfig;
import cz.osu.pesa.swi22025.model.json.PayloadMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class ChatMessageService {
    private final RabbitTemplate rabbitTemplate;

    public ChatMessageService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void broadcastMessage(PayloadMessage message) {
        rabbitTemplate.convertAndSend
                (RabbitMQConfig.CHATROOM_EXCHANGE, "", message);
    }
}
