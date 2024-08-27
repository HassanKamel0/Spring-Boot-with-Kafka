package com.kafka.library_event_consumer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kafka.library_event_consumer.entity.LibraryEvent;
import com.kafka.library_event_consumer.jpa.LibraryEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class LibraryEventService {
    private final LibraryEventRepository libraryEventRepository;

    public void processLibraryEvent(ConsumerRecord<Integer, String> record) throws JsonProcessingException, ChangeSetPersister.NotFoundException {
        LibraryEvent libraryEvent= new ObjectMapper().readValue(record.value(), LibraryEvent.class);
        log.info("Library event received: {}", libraryEvent);

        if (libraryEvent!=null && (libraryEvent.getLibraryEventId()!=null && libraryEvent.getLibraryEventId()==999)) {
            throw new RecoverableDataAccessException("Network issue");
        }

        switch (libraryEvent.getLibraryEventType()) {
            case NEW:
                saveLibraryEvent(libraryEvent);
                break;
            case UPDATE:
                validate(libraryEvent);
                saveLibraryEvent(libraryEvent);
                break;
            default:
                log.info("Unknown library event type: {}", libraryEvent.getLibraryEventType());
        }
    }

    private void saveLibraryEvent(LibraryEvent libraryEvent) {
        libraryEvent.getBook().setLibraryEvent(libraryEvent);
        libraryEventRepository.save(libraryEvent);
        log.info("Library event saved to database");
    }

    private void validate(LibraryEvent libraryEvent)  {
        if(libraryEvent.getLibraryEventId()==null)
            throw new IllegalArgumentException("Library event id is missing: ");

        Optional<LibraryEvent> existedLibraryEvent=libraryEventRepository.findById(libraryEvent.getLibraryEventId());
        if(existedLibraryEvent.isEmpty())
            throw new IllegalArgumentException("Not found library event with id: " + libraryEvent.getLibraryEventId());
        log.info("Library event validated");
    }
}