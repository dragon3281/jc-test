package com.detection.platform.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置
 */
@Configuration
public class RabbitMQConfig {

    // ==================== 检测任务队列 ====================
    
    /**
     * 检测任务队列
     */
    @Bean
    public Queue detectionQueue() {
        return QueueBuilder.durable("task.detection.queue")
                .withArgument("x-message-ttl", 3600000) // 消息TTL 1小时
                .build();
    }

    /**
     * 检测任务交换机
     */
    @Bean
    public DirectExchange detectionExchange() {
        return new DirectExchange("task.detection.exchange", true, false);
    }

    /**
     * 检测任务绑定
     */
    @Bean
    public Binding detectionBinding() {
        return BindingBuilder.bind(detectionQueue())
                .to(detectionExchange())
                .with("task.detection");
    }

    // ==================== 进度更新队列 ====================
    
    /**
     * 进度更新队列
     */
    @Bean
    public Queue progressQueue() {
        return QueueBuilder.durable("task.progress.queue")
                .withArgument("x-message-ttl", 300000) // 消息TTL 5分钟
                .build();
    }

    /**
     * 进度更新交换机
     */
    @Bean
    public DirectExchange progressExchange() {
        return new DirectExchange("task.progress.exchange", true, false);
    }

    /**
     * 进度更新绑定
     */
    @Bean
    public Binding progressBinding() {
        return BindingBuilder.bind(progressQueue())
                .to(progressExchange())
                .with("task.progress");
    }

    // ==================== 死信队列 ====================
    
    /**
     * 死信队列
     */
    @Bean
    public Queue deadLetterQueue() {
        return new Queue("task.dead.letter.queue", true);
    }

    /**
     * 死信交换机
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("task.dead.letter.exchange", true, false);
    }

    /**
     * 死信绑定
     */
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("dead.letter");
    }

    // ==================== 消息转换器 ====================
    
    /**
     * JSON消息转换器
     */
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate配置
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    /**
     * 监听器容器工厂配置
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setConcurrentConsumers(5); // 并发消费者数量
        factory.setMaxConcurrentConsumers(10); // 最大并发消费者数量
        factory.setPrefetchCount(10); // 预取数量
        return factory;
    }
}
