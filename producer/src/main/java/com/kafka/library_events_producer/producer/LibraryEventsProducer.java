package com.kafka.library_events_producer.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kafka.library_events_producer.dto.LibraryEvent;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class LibraryEventsProducer {

    @Value("${spring.kafka.template.default-topic}")
    private String topic;

    private final KafkaTemplate<Integer, String> kafkaTemplate;

    //async call
    public CompletableFuture<SendResult<Integer, String>> sendLibraryEvent(LibraryEvent libraryEvent) throws JsonProcessingException {
        Integer key = libraryEvent.libraryEventId();
        String value = new ObjectMapper().writeValueAsString(libraryEvent);

        return kafkaTemplate.send(topic, key, value)
                .whenComplete((sendResult, exception) -> {
                    if (exception != null) {
                        handleFailure(key, value, exception);
                    } else
                        handleSuccess(key, value, sendResult);
                });
    }

    //sync call
    public SendResult sendLibraryEvent_2ndApproach(LibraryEvent libraryEvent) throws JsonProcessingException, ExecutionException, InterruptedException, TimeoutException {
        Integer key = libraryEvent.libraryEventId();
        String value = new ObjectMapper().writeValueAsString(libraryEvent);

        SendResult<Integer, String> sendResult = kafkaTemplate.send(topic, key, value)
                .get(3, TimeUnit.SECONDS);

        handleSuccess(key, value, sendResult);
        return sendResult;
    }

    //async call
    public CompletableFuture sendLibraryEvent_3rdApproach(LibraryEvent libraryEvent) throws JsonProcessingException {
        Integer key = libraryEvent.libraryEventId();
        String value = new ObjectMapper().writeValueAsString(libraryEvent);

        ProducerRecord<Integer, String> producerRecord= buildProducerRecord(key,value);
        
        return kafkaTemplate.send(producerRecord)
                .whenComplete((sendResult, exception) -> {
                    if (exception != null) {
                        handleFailure(key, value, exception);
                    } else
                        handleSuccess(key, value, sendResult);
                });
    }

    public void updateLibraryEvent(int id, LibraryEvent libraryEvent) {

    }

    private ProducerRecord<Integer, String> buildProducerRecord(Integer key, String value) {

        List<Header> recordHeaders = List.of(new RecordHeader("event-source", "scanner".getBytes()));
        return new ProducerRecord<>(topic, null,key, value,recordHeaders);
    }

    private void handleSuccess(Integer key, String value, SendResult<Integer, String> sendResult) {
        log.info("Message is done successfully for the key: {} , value: {}, partition: {}"
                , key, value, sendResult.getRecordMetadata().partition());
    }

    private void handleFailure(Integer key, String value, Throwable exception) {
        log.error("handleFailure: {}", exception.getMessage(), exception);
    }
}