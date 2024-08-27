package com.kafka.library_event_consumer.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.util.backoff.FixedBackOff;

import java.util.List;

@Configuration
@EnableKafka
@Slf4j
@RequiredArgsConstructor
public class LibraryEventsConsumerConfig {

    private final KafkaTemplate<String, String> kafkaTemplate;
    @Value("${topics.retry}")
    private String retryTopic;

    @Value("${topics.dlt}")
    private String deadLetterTopic;

    public DeadLetterPublishingRecoverer publishingRecoverer() {
        return new DeadLetterPublishingRecoverer(
                kafkaTemplate, (r, e) -> {
            log.error("Exception in publishing Recoverer : {}", e.getMessage(), e);
            if (e.getCause() instanceof RecoverableDataAccessException) {
                log.error("Inside the recoverable DataAccessException");
                return new TopicPartition(retryTopic, r.partition());
            } else
                return new TopicPartition(deadLetterTopic, r.partition());
        }
        );
    }

    public DefaultErrorHandler errorHandler() {
        //retry 2 times separated with 1 second
        FixedBackOff fixedBackOff = new FixedBackOff(1000L, 2);

        ExponentialBackOffWithMaxRetries expBackOff = new ExponentialBackOffWithMaxRetries(2);
        expBackOff.setInitialInterval(1000L);
        expBackOff.setMultiplier(2.0);
        expBackOff.setMaxInterval(2000L);

//        DefaultErrorHandler defaultErrorHandler = new DefaultErrorHandler(fixedBackOff);
        DefaultErrorHandler defaultErrorHandler = new DefaultErrorHandler(publishingRecoverer(),expBackOff);
        List<Class<IllegalArgumentException>> ignoredExceptions= List.of(IllegalArgumentException.class);
        ignoredExceptions.forEach(defaultErrorHandler::addNotRetryableExceptions);

        defaultErrorHandler.setRetryListeners(((record, ex, deliveryAttempt) -> {
        log.info("Failed record in retry listener, Exeption {} , DeliveryAttempt {}"
                    , ex.getMessage(), deliveryAttempt);
        }));
        return defaultErrorHandler;
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory kafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
           ConsumerFactory<Object, Object> kafkaConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, kafkaConsumerFactory);
        factory.setConcurrency(3);
        factory.setCommonErrorHandler(errorHandler());
//        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }
}