package com.kafka.library_event_consumer.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.kafka.library_event_consumer.service.LibraryEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class LibraryEventsConsumer {
    private final LibraryEventService libraryEventService;

    @KafkaListener(topics = "libraryEvents", groupId = "libraryEvents-listener-group")
    public void onMessage(ConsumerRecord<Integer,String> consumerRecord) throws ChangeSetPersister.NotFoundException, JsonProcessingException {
        log.info("Consumer Record: {}", consumerRecord);
        libraryEventService.processLibraryEvent(consumerRecord);
        log.info("Processing done!");
    }
}
