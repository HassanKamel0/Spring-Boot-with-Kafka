package com.kafka.library_event_consumer.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LibraryEvent {
    @Id
    @GeneratedValue
    private Integer libraryEventId;
    @Enumerated(EnumType.STRING)
    private LibraryEventType libraryEventType;

    @OneToOne(mappedBy = "libraryEvent", cascade = CascadeType.ALL)
    @ToString.Exclude
    private Book book;

}
