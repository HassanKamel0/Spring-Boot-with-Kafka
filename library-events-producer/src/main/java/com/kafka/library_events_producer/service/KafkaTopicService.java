package com.kafka.library_events_producer.service;

import org.apache.kafka.clients.admin.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

@Service
public class KafkaTopicService {

    @Value("${spring.kafka.producer.bootstrap-servers}")
    private String bootstrapServers;

    public TopicDescription getTopicDescription(String topicName) {
        Properties config = new Properties();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        try (AdminClient adminClient = AdminClient.create(config)) {
            DescribeTopicsResult result = adminClient.describeTopics(Collections.singletonList(topicName));
            Map<String, TopicDescription> topics = result.all().get();
            return topics.get(topicName);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Map<String, TopicDescription> getAllTopicDescriptions() {
        Properties config = new Properties();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        try (AdminClient adminClient = AdminClient.create(config)) {
            // List all topics
            ListTopicsResult topicsResult = adminClient.listTopics();
            List<String> topicNames = topicsResult.names().get().stream().toList();

            // Describe all topics
            DescribeTopicsResult result = adminClient.describeTopics(topicNames);
            return result.all().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }
}
