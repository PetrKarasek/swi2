package cz.osu.pesa.swi22025.component;

import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class ChatMessageListener {
    private final RabbitListenerContainerFactory<?> factory;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public ChatMessageListener(@Qualifier("rabbitListenerContainerFactory")
                                   RabbitListenerContainerFactory<?> factory,
                               SimpMessagingTemplate messagingTemplate) {
        this.factory = factory;
        this.messagingTemplate = messagingTemplate;
    }

    public void startListeningForUser(String queueName) {
        MessageListenerContainer container = factory.createListenerContainer();
        container.setQueueNames(queueName);
        container.setupMessageListener(message -> {
            String userMessage = new String(message.getBody());
            String userId = queueName.replace("chatroom.queue.", "");
            System.out.println(userId + " received: " + userMessage);
        });
        container.start();
    }
}
