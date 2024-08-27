package com.kafka.library_events_producer.controller;

import com.kafka.library_events_producer.dto.LibraryEvent;
import com.kafka.library_events_producer.dto.LibraryEventType;
import com.kafka.library_events_producer.producer.LibraryEventsProducer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class LibraryEventsController {
    private final LibraryEventsProducer libraryEventsProducer;

    @PostMapping("/v1/libraryevent")
    public ResponseEntity newLibraryEvent(@RequestBody @Valid LibraryEvent libraryEvent) {

        log.info("New library event: {}", libraryEvent);
//        libraryEventsProducer.sendLibraryEvent(libraryEvent);
        libraryEventsProducer.sendLibraryEvent_2ndApproach(libraryEvent);
//        libraryEventsProducer.sendLibraryEvent_3rdApproach(libraryEvent);
        log.info("After sending library event: {}");
        return new ResponseEntity(HttpStatus.CREATED);
    }

    @PutMapping("/v1/libraryevent/{id}")
    public ResponseEntity updateLibraryEvent(@PathVariable int id, @RequestBody @Valid LibraryEvent libraryEvent) {
        log.info("Update library event: {}", libraryEvent);

        if (libraryEvent.libraryEventType() != LibraryEventType.UPDATE)
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        libraryEventsProducer.sendLibraryEvent_3rdApproach(libraryEvent);
        log.info("After sending library event: {}");
        return new ResponseEntity(HttpStatus.OK);
    }
}
