package service.info.infoservice.web;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ListenFanoutQueue {

    @RabbitListener(queues = "queue")
    public void listenFanoutQueue(Msg msg) {
        System.out.println("INFO接收到DATA消息：【" + msg + "】");
    }
}