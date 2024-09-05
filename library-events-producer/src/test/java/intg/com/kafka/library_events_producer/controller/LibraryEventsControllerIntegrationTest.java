package com.kafka.library_events_producer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kafka.library_events_producer.dto.LibraryEvent;
import com.kafka.library_events_producer.unit.TestUtil;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(topics = { "${spring.kafka.template.default-topic}" }, partitions = 3)
@TestPropertySource(properties = {"spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.admin.properties.bootstrap.servers=${spring.embedded.kafka.brokers}"})
class LibraryEventsControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    private Consumer<Integer, String> consumer;

    @BeforeEach
    void setUp() {
        HashMap<String, Object> configs= new HashMap<>(
                KafkaTestUtils.consumerProps("group1","true",embeddedKafka));
        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"latest");
        consumer=new DefaultKafkaConsumerFactory<>(configs,new IntegerDeserializer(),new StringDeserializer())
                                                .createConsumer();

        embeddedKafka.consumeFromAllEmbeddedTopics(consumer);

        this.webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @AfterEach
    void tearDown() {
        consumer.close();
    }

    @Test
    void newLibraryEvent() {
        // Given
        // Assuming TestUtil.libraryEventRecord() returns a LibraryEvent
        LibraryEvent libraryEvent = TestUtil.libraryEventRecord();

        // When
        webTestClient.post()
                .uri("/v1/libraryevent")
                .bodyValue(libraryEvent)
                .exchange()
         //Then
                .expectStatus().isCreated();


        ConsumerRecords<Integer,String> consumerRecords =KafkaTestUtils.getRecords(consumer);
        assert consumerRecords.count() == 1;
        consumerRecords.forEach( record -> {
            LibraryEvent libraryEventActual =TestUtil
                    .parseLibraryEventRecord(new ObjectMapper(), record.value());
            System.out.println("LibraryEvent: " + libraryEventActual);
            assertEquals(libraryEventActual,libraryEvent);
        });
    }
}