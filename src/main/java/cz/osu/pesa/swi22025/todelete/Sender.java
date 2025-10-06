package cz.osu.pesa.swi22025.todelete;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Sender {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private Queue queue;

    public void send(String msg) {
        rabbitTemplate.convertAndSend(queue.getName(), msg);
        System.out.println("Sent message: " + msg);
    }
}
