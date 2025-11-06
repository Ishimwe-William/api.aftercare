package com.bunsen.api.aftercare.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    @MessageMapping("/update-task") // Client sends to /app/update-task
    @SendTo("/topic/tasks") // Broadcasts to subscribers
    public String handleTaskUpdate(String message) {
        return "Task updated: " + message; // In real use, return a DTO
    }
}