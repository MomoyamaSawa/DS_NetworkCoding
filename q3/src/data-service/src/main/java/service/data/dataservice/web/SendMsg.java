package service.data.dataservice.web;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class SendMsg {

    private final RabbitTemplate rabbitTemplate;

    public SendMsg(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(Msg msg) {
        rabbitTemplate.convertAndSend("fanout.exchange", "", msg);
    }
}