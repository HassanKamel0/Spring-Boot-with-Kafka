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
public class LibraryEventsRetryConsumer {

    private final LibraryEventService libraryEventService;

    @KafkaListener(topics = "${topics.retry}",autoStartup = "${retryListener.startup:false}", groupId = "retry-listener-group")
    public void onMessage(ConsumerRecord<Integer,String> consumerRecord) throws ChangeSetPersister.NotFoundException, JsonProcessingException {
        log.info("Consumer Record in Retry: {}", consumerRecord);
        consumerRecord.headers().forEach(header ->
                log.info("Header Name: {}, Header Value: {}", header.key(), new String(header.value())));
        libraryEventService.processLibraryEvent(consumerRecord);}
}
