package service.data.dataservice.web;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.Resource;

@Configuration
public class RabbitMQConfig implements InitializingBean {

    /**
     * 自动注入RabbitTemplate模板
     */
    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送消息JSON序列化
     */
    @Override
    public void afterPropertiesSet() {
        // 使用JSON序列化
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
    }
}
