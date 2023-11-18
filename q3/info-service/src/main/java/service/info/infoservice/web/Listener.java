package service.info.infoservice.web;

import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import service.info.infoservice.pojo.Data;
import service.info.infoservice.service.Service;

@Component
public class Listener {

    @Autowired
    private Service service;

    @Value("${spring.application.name}")
    private String applicationName;

    @RabbitListener(queues = "queue")
    public void listenFanoutQueue(Msg msg) {
        System.out.println("INFO接收到DATA消息：【" + msg + "】");

        String id = UUID.randomUUID().toString();
        service.addData(new Data(id, msg.getIndex(), applicationName, msg.getSize(), msg.getUuid()));
        service.incrementNum(msg.getUuid());
    }
}