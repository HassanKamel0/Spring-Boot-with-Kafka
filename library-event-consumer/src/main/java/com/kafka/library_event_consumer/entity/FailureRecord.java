package com.kafka.library_event_consumer.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity @Table(name = "Failure_Record")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FailureRecord {
    @Id
    @GeneratedValue
    private Integer id;
    private String topic;
    private Integer key;
    private String errorRecord;
    private Integer partition;
    private Integer offsetValue;
    private String exception;
    private String status;
}
