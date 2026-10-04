package com.marketflow.service;

import com.marketflow.dto.ExecutionEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class ExecutionWebSocketBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(ExecutionWebSocketBroadcaster.class);

    private final SimpMessagingTemplate messagingTemplate;

    public ExecutionWebSocketBroadcaster(@Autowired(required = false) SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastEvent(ExecutionEventDto event) {
        if (messagingTemplate == null) {
            log.debug("WebSocket messagingTemplate not available; skipping broadcast");
            return;
        }

        try {
            if (event.getExecutionId() != null) {
                String destination = "/topic/executions/" + event.getExecutionId();
                messagingTemplate.convertAndSend(destination, event);
                log.debug("Broadcasted WebSocket event [{}] to destination [{}]", event.getEventType(), destination);
            }

            if (event.getWorkflowId() != null) {
                String wfDestination = "/topic/workflows/" + event.getWorkflowId();
                messagingTemplate.convertAndSend(wfDestination, event);
            }
        } catch (Exception ex) {
            log.warn("Failed to broadcast WebSocket execution event: {}", ex.getMessage());
        }
    }
}
