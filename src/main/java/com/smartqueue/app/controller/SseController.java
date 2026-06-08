package com.smartqueue.app.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/sse")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5176", "http://localhost:5177", "http://localhost:5178", "http://127.0.0.1:5173", "http://127.0.0.1:5174", "http://127.0.0.1:5175", "http://127.0.0.1:5176", "http://127.0.0.1:5177", "http://127.0.0.1:5178"})
public class SseController {

    // Store emitters for all connected users (maps user identifier to their emitters list)
    private static final Map<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam String userId) {
        // Set timeout to 24 hours to prevent frequent disconnects
        SseEmitter emitter = new SseEmitter(24 * 60 * 60 * 1000L);
        
        // Register to user-specific list and global "all" channel
        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitters.computeIfAbsent("all", k -> new CopyOnWriteArrayList<>()).add(emitter);
        
        emitter.onCompletion(() -> cleanup(userId, emitter));
        emitter.onTimeout(() -> cleanup(userId, emitter));
        emitter.onError((e) -> cleanup(userId, emitter));
        
        // Send initial connection event to confirm success
        try {
            emitter.send(SseEmitter.event()
                    .name("INIT")
                    .data("{\"status\":\"CONNECTED\"}"));
        } catch (IOException e) {
            cleanup(userId, emitter);
        }
        
        return emitter;
    }

    private void cleanup(String userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> userList = emitters.get(userId);
        if (userList != null) {
            userList.remove(emitter);
            if (userList.isEmpty()) {
                emitters.remove(userId);
            }
        }
        
        CopyOnWriteArrayList<SseEmitter> allList = emitters.get("all");
        if (allList != null) {
            allList.remove(emitter);
            if (allList.isEmpty()) {
                emitters.remove("all");
            }
        }
    }

    // Broadcast a queue/slot update globally to all connected dashboards
    public static void notifyQueueUpdate() {
        broadcast("QUEUE_UPDATE", "{\"updated\":true}");
    }

    // Broadcast an event to all subscribers on a given key (e.g., "all")
    public static void broadcast(String eventName, Object data) {
        CopyOnWriteArrayList<SseEmitter> allList = emitters.get("all");
        if (allList != null) {
            for (SseEmitter emitter : allList) {
                try {
                    emitter.send(SseEmitter.event()
                            .name(eventName)
                            .data(data));
                } catch (IOException e) {
                    // Handled by completion/error callbacks
                }
            }
        }
    }

    // Send a message directly to a specific user
    public static void sendToUser(String userId, String eventName, Object data) {
        CopyOnWriteArrayList<SseEmitter> userList = emitters.get(userId);
        if (userList != null) {
            for (SseEmitter emitter : userList) {
                try {
                    emitter.send(SseEmitter.event()
                            .name(eventName)
                            .data(data));
                } catch (IOException e) {
                    // Handled by completion/error callbacks
                }
            }
        }
    }
}
