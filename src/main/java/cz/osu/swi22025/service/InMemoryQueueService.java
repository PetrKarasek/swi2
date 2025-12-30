package cz.osu.swi22025.service;

import cz.osu.swi22025.model.json.PayloadMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryQueueService {
    
    private final ConcurrentHashMap<String, List<PayloadMessage>> userQueues = new ConcurrentHashMap<>();
    
    public void addMessageToQueue(String userId, PayloadMessage message) {
        userQueues.computeIfAbsent(userId, k -> new ArrayList<>()).add(message);
    }
    
    public List<PayloadMessage> getMessagesForUser(String userId) {
        List<PayloadMessage> messages = userQueues.getOrDefault(userId, new ArrayList<>());
        userQueues.remove(userId); // Clear the queue after retrieval
        return messages;
    }
    
    public int getQueueSize(String userId) {
        return userQueues.getOrDefault(userId, new ArrayList<>()).size();
    }
}
