package com.scalemart.worker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    @KafkaListener(topics = "${app.kafka.order-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(OrderEvent event) {
        // Placeholder for async tasks such as email/notification/payment confirmation.
        log.info(
            "Processed order event orderId={} user={} total={} createdAt={}",
            event.orderId(),
            event.userId(),
            event.totalAmount(),
            event.createdAt());
    }
}
