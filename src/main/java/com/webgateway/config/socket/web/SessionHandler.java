package com.webgateway.config.socket.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Component
public class SessionHandler {
    private Map<WebSocketSession, Instant> sessions = new ConcurrentHashMap<>();
    private Consumer<WebSocketSession> handler = session -> {
        if (Duration.between(sessions.get(session), Instant.now()).getSeconds() > 15) {
            sessions.remove(session);
            try {
                logger.debug("Close {}", session);
                session.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };
    private static Logger logger = LoggerFactory.getLogger(SessionHandler.class);

    public void afterConnectionEstablished(WebSocketSession session) {
        logger.debug("After connection {}", session);
        sessions.put(session, Instant.now());
    }

    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        logger.debug("Handle {}", session);
        sessions.put(session, Instant.now());
    }

    @PostConstruct
    public void timeout() {
        logger.debug("timeout");
        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(15000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (sessions.isEmpty()) {
                    continue;
                }
                sessions.keySet().forEach(handler);
            }
        }).start();
    }
}