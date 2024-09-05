package com.kafka.library_event_consumer.service;

import com.kafka.library_event_consumer.entity.FailureRecord;
import com.kafka.library_event_consumer.jpa.FailureRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class FailureRecordService {
    private final FailureRecordRepository failureRecordRepository;

    public void saveFailureRecord(ConsumerRecord<Integer, String> consumerRecord, Exception e, String status) {
        FailureRecord failureRecord = new FailureRecord((Integer) null
                ,consumerRecord.topic()
                ,consumerRecord.key()
                ,consumerRecord.value()
                ,consumerRecord.partition()
                , Math.toIntExact(consumerRecord.offset())
                ,e.getCause().getMessage(),
                status);
        failureRecordRepository.save(failureRecord);
    }
}
