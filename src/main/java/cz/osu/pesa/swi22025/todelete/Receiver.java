package cz.osu.pesa.swi22025.todelete;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queues = "chatroom.queue", id = "listener")
public class Receiver {
    @RabbitHandler
    public void receive(String message) {
        System.out.println("Received: " + message);
    }
}
