package com.scalemart.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderEventProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private final String topic;

    public OrderEventProducer(
        KafkaTemplate<String, OrderEvent> kafkaTemplate,
        @Value("${app.kafka.order-topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(OrderEvent event) {
        kafkaTemplate.send(topic, String.valueOf(event.orderId()), event);
        log.info("Published order event orderId={} topic={}", event.orderId(), topic);
    }
}
