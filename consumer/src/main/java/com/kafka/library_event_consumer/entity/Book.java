package com.kafka.library_event_consumer.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity @Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Book {
    @Id
    private Integer bookId;
    @NotBlank
    private String bookName;
    @NotBlank
    private String bookAuthor;

    @OneToOne
        @JoinColumn(name = "libraryEventId")
    private LibraryEvent libraryEvent;
}
