package com.kafka.library_events_producer.controller;

import com.kafka.library_events_producer.service.KafkaTopicService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.TopicDescription;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class KafkaController {
    private final KafkaTopicService kafkaTopicService;

    @GetMapping("/topic-info")
    public String getTopicInfo(@RequestParam String topicName) {
        TopicDescription description = kafkaTopicService.getTopicDescription(topicName);

        if (description != null) {
            int partitions = description.partitions().size();
            int replicas = description.partitions().get(0).replicas().size();  // Assuming all partitions have the same replication factor
            return String.format("Topic %s has %d partitions and %d replicas.", topicName, partitions, replicas);
        } else {
            return "Topic not found or error occurred.";
        }
    }

    @GetMapping("/topics-info")
    public String getAllTopicsInfo() {
        Map<String, TopicDescription> descriptions = kafkaTopicService.getAllTopicDescriptions();

        if (!descriptions.isEmpty()) {
            StringBuilder response = new StringBuilder("Topic details:\n");
            for (Map.Entry<String, TopicDescription> entry : descriptions.entrySet()) {
                String topicName = entry.getKey();
                TopicDescription description = entry.getValue();
                int partitions = description.partitions().size();
                int replicas = description.partitions().get(0).replicas().size();  // Assuming all partitions have the same replication factor

                response.append(String.format("Topic %s: %d partitions, %d replicas\n", topicName, partitions, replicas));
            }
            return response.toString();
        } else {
            return "No topics found or an error occurred.";
        }
    }
}

