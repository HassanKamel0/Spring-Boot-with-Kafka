package com.kafka.library_events_producer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kafka.library_events_producer.dto.LibraryEvent;
import com.kafka.library_events_producer.producer.LibraryEventsProducer;
import com.kafka.library_events_producer.unit.TestUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LibraryEventsController.class)
class LibraryEventsControllerUnitTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    LibraryEventsProducer libraryEventsProducer;

    @Test
    void newLibraryEvent() throws Exception {
        //Given
        when(libraryEventsProducer.sendLibraryEvent_2ndApproach(isA(LibraryEvent.class)))
                .thenReturn(null);
        //When
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/libraryevent")
                        .content(new ObjectMapper().writeValueAsString(TestUtil.newLibraryEventRecordWithLibraryEventId()))
                        .contentType(MediaType.APPLICATION_JSON))
                //Then
                .andExpect(status().isCreated());


    }

    @Test
    void newLibraryEventWith_4xxError() throws Exception {
        //Given
        when(libraryEventsProducer.sendLibraryEvent_2ndApproach(isA(LibraryEvent.class)))
                .thenReturn(null);
        //When
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/libraryevent")
                        .content(new ObjectMapper().writeValueAsString(TestUtil.libraryEventRecordWithInvalidBook()))
                        .contentType(MediaType.APPLICATION_JSON))
                //Then
                .andExpect(status().is4xxClientError());
    }
}