//package com.kafka.library_event_consumer.scheduler;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.kafka.library_event_consumer.config.LibraryEventsConsumerConfig;
//import com.kafka.library_event_consumer.entity.FailureRecord;
//import com.kafka.library_event_consumer.jpa.FailureRecordRepository;
//import com.kafka.library_event_consumer.service.LibraryEventService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.kafka.clients.consumer.ConsumerRecord;
//import org.springframework.data.crossstore.ChangeSetPersister;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//@Component
//@Slf4j
//@RequiredArgsConstructor
//public class RetryScheduler {
//    private final FailureRecordRepository failureRecordRepository;
//    private final LibraryEventService libraryEventService;
//
//    @Scheduled(fixedRate = 10000)
//    public void retryFailedRecords() {
//        log.info("Retrying Scheduler ...");
//        failureRecordRepository.findAllByStatus(LibraryEventsConsumerConfig.RETRY)
//                .forEach(failureRecord -> {
//                    try {
//                        libraryEventService.processLibraryEvent(buildConsumerRecord(failureRecord));
//                        failureRecord.setStatus(LibraryEventsConsumerConfig.SUCCESS);
//                        log.info("Successfully processed library event {}", failureRecord);
//                    } catch (JsonProcessingException | ChangeSetPersister.NotFoundException e) {
//                        log.error("Failed to process failure record", e);
//                        throw new RuntimeException(e.getMessage());
//                    }
//                });
//        log.info("Retrying Scheduler done");
//    }
//
//    private ConsumerRecord<Integer, String> buildConsumerRecord(FailureRecord failureRecord) {
//        return new ConsumerRecord<>(failureRecord.getTopic()
//                ,failureRecord.getPartition()
//                ,failureRecord.getOffsetValue()
//                ,failureRecord.getKey()
//                ,failureRecord.getErrorRecord());
//    }
//}
