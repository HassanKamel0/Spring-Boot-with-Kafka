package com.kafka.library_event_consumer.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kafka.library_event_consumer.entity.Book;
import com.kafka.library_event_consumer.entity.LibraryEvent;
import com.kafka.library_event_consumer.entity.LibraryEventType;
import com.kafka.library_event_consumer.jpa.FailureRecordRepository;
import com.kafka.library_event_consumer.jpa.LibraryEventRepository;
import com.kafka.library_event_consumer.service.LibraryEventService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@EmbeddedKafka(topics = { "${spring.kafka.template.default-topic}","libraryEvents.RETRY","libraryEvents.DLT"}, partitions = 3)
@TestPropertySource(properties = {"spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}"
        ,"spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}"
        , "retryListener.startup:false"})
class LibraryEventsConsumerIntegrationTest {
    @Value("${topics.retry}")
    private String retryTopic;

    @Value("${topics.dlt}")
    private String deadLetterTopic;
    @Autowired
    private EmbeddedKafkaBroker broker;
    @Autowired
    private KafkaTemplate<Integer, String> kafkaTemplate;
    @Autowired
    private KafkaListenerEndpointRegistry endpointRegistry; //where all the consumers you build gets registered
    @Autowired
    private LibraryEventRepository libraryEventRepository;
    @Autowired
    private FailureRecordRepository failureRecordRepository;
    private Consumer<Integer, String> consumer;

    @SpyBean
    LibraryEventsConsumer spyConsumer;
    @SpyBean
    LibraryEventService spyEventService;

    @BeforeEach
    void setUp() {
        //in case of there more than consumer and I want the test run for specific one, wait it to register
        MessageListenerContainer container= endpointRegistry.getListenerContainers().stream()
                .filter(messageListenerContainer -> Objects
                        .equals(messageListenerContainer.getGroupId(),
                                "libraryEvents-listener-group"))
                .toList().getFirst();

       ContainerTestUtils.waitForAssignment(container,broker.getPartitionsPerTopic());

       //in case of running the test for all consumers but wait them all till they up
//        for (MessageListenerContainer container : endpointRegistry.getListenerContainers()) {
//            ContainerTestUtils.waitForAssignment(container,broker.getPartitionsPerTopic());
//        }
    }

    @AfterEach
    void tearDown() {
        libraryEventRepository.deleteAll();
    }

    @Test
    void publishNewLibraryEvent() throws ChangeSetPersister.NotFoundException, JsonProcessingException, InterruptedException, ExecutionException {
        //given
            String json="{ \"libraryEventId\": null, \"libraryEventType\": \"NEW\", \"book\": { \"bookId\": 316, \"bookName\": \"Kafka Using Spring Boot\", \"bookAuthor\": \"Dilip\" } }";
            kafkaTemplate.sendDefault(json).get();
        //when
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(3, TimeUnit.SECONDS);

        //then
        verify(spyConsumer, times(1)).onMessage(isA(ConsumerRecord.class));
        verify(spyEventService, times(1)).processLibraryEvent(isA(ConsumerRecord.class));
        List<LibraryEvent> libraryEventList= (List<LibraryEvent>) libraryEventRepository.findAll();
        assert libraryEventList.size() == 1;
        libraryEventList.forEach(libraryEvent -> {
                assert libraryEvent.getLibraryEventId()!=null;
                assertEquals(316,libraryEvent.getBook().getBookId());
        });
    }

    @Test
    void publishUpdateLibraryEvent() throws JsonProcessingException, ExecutionException, InterruptedException{
        //given
        String json="{ \"libraryEventId\": null, \"libraryEventType\": \"NEW\", \"book\": { \"bookId\": 316, \"bookName\": \"Kafka Using Spring Boot\", \"bookAuthor\": \"Dilip\" } }";
        ObjectMapper objectMapper = new ObjectMapper();
        LibraryEvent libraryEvent = objectMapper.readValue(json, LibraryEvent.class);
        libraryEvent.getBook().setLibraryEvent(libraryEvent);
        libraryEventRepository.save(libraryEvent);

        Book updatedBook = Book.builder().bookId(316).bookName("Alchemist").bookAuthor("Palu Cualo").build();
        libraryEvent.setLibraryEventType(LibraryEventType.UPDATE);
        libraryEvent.setBook(updatedBook);
        String updatedJson= objectMapper.writeValueAsString(libraryEvent);
        kafkaTemplate.sendDefault(libraryEvent.getLibraryEventId(),updatedJson).get();

        //when
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(3, TimeUnit.SECONDS);
        //then
        LibraryEvent updatedLibraryEvent = libraryEventRepository.findById(libraryEvent.getLibraryEventId()).get();
        assertEquals("Alchemist",updatedLibraryEvent.getBook().getBookName());
    }

    @Test
    void publishUpdateLibraryEvent_null_LibraryEvent() throws JsonProcessingException, ExecutionException, InterruptedException, ChangeSetPersister.NotFoundException {
        //given
        String json="{ \"libraryEventId\": null, \"libraryEventType\": \"UPDATE\", \"book\": { \"bookId\": 316, \"bookName\": \"Kafka Using Spring Boot\", \"bookAuthor\": \"Dilip\" } }";
        kafkaTemplate.sendDefault(json).get();

        //when
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(5, TimeUnit.SECONDS);

        //then
        verify(spyConsumer, times(1)).onMessage(isA(ConsumerRecord.class));
        verify(spyEventService, times(1)).processLibraryEvent(isA(ConsumerRecord.class));

        HashMap<String, Object> configs= new HashMap<>(
                KafkaTestUtils.consumerProps("group2","true",broker));

        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"latest");
        consumer=new DefaultKafkaConsumerFactory<>(configs,new IntegerDeserializer(),new StringDeserializer())
                .createConsumer();

        broker.consumeFromAnEmbeddedTopic(consumer,deadLetterTopic);

        ConsumerRecord<Integer, String> consumerRecord =KafkaTestUtils.getSingleRecord(consumer,deadLetterTopic);
        System.out.println("Consumer Record from integration unit test: "+consumerRecord.value());
        assertEquals(json,consumerRecord.value());
    }
    @Test
    void publishUpdateLibraryEvent_null_LibraryEvent_failureRecord() throws JsonProcessingException, ExecutionException, InterruptedException, ChangeSetPersister.NotFoundException {
        //given
        String json="{ \"libraryEventId\": null, \"libraryEventType\": \"UPDATE\", \"book\": { \"bookId\": 316, \"bookName\": \"Kafka Using Spring Boot\", \"bookAuthor\": \"Dilip\" } }";
        kafkaTemplate.sendDefault(json).get();

        //when
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(5, TimeUnit.SECONDS);

        //then
        verify(spyConsumer, times(1)).onMessage(isA(ConsumerRecord.class));
        verify(spyEventService, times(1)).processLibraryEvent(isA(ConsumerRecord.class));
        assertEquals(1,failureRecordRepository.count());
        failureRecordRepository.findAll().forEach(failureRecord ->
                System.out.println("Failure Record: "+failureRecord));
    }
    @Test
    void publishUpdateLibraryEvent_999_LibraryEvent() throws JsonProcessingException, ExecutionException, InterruptedException, ChangeSetPersister.NotFoundException {
        //given
        String json="{ \"libraryEventId\": 999, \"libraryEventType\": \"UPDATE\", \"book\": { \"bookId\": 316, \"bookName\": \"Kafka Using Spring Boot\", \"bookAuthor\": \"Dilip\" } }";
        kafkaTemplate.sendDefault(json).get();

        //when
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(5, TimeUnit.SECONDS);

        //then
        verify(spyConsumer, times(3)).onMessage(isA(ConsumerRecord.class));
        verify(spyEventService, times(3)).processLibraryEvent(isA(ConsumerRecord.class));

        HashMap<String, Object> configs= new HashMap<>(
                KafkaTestUtils.consumerProps("group1","true",broker));

        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"latest");
        consumer=new DefaultKafkaConsumerFactory<>(configs,new IntegerDeserializer(),new StringDeserializer())
                .createConsumer();

        broker.consumeFromAnEmbeddedTopic(consumer,retryTopic);

        ConsumerRecord<Integer, String> consumerRecord =KafkaTestUtils.getSingleRecord(consumer,retryTopic);
        System.out.println("Consumer Record from integration unit test: "+consumerRecord.value());
        assertEquals(json,consumerRecord.value());
    }
}