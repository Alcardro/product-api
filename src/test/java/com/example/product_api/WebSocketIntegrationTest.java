package com.example.product_api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    private String token;
    private WebSocketStompClient stompClient;
    private final BlockingQueue<String> chatMessages = new LinkedBlockingDeque<>();
    private final BlockingQueue<String> productNotifications = new LinkedBlockingDeque<>();
    private StompSession stompSession;

    private final CountDownLatch subscriptionLatch = new CountDownLatch(2); // dos suscripciones

    @BeforeEach
    void setUp() throws Exception {
        // 1. Obtener token JWT (admin)
        RestTemplate restTemplate = new RestTemplate();
        Map<String, String> loginRequest = Map.of(
                "username", "admin",
                "password", "admin123"
        );
        String response = restTemplate.postForObject(
                "http://localhost:" + port + "/api/auth/login",
                loginRequest,
                String.class
        );
        token = new ObjectMapper().readTree(response).get("accessToken").asText();

        // 2. Configurar cliente STOMP sobre WebSocket nativo
        StandardWebSocketClient webSocketClient = new StandardWebSocketClient();
        stompClient = new WebSocketStompClient(webSocketClient);
        stompClient.setMessageConverter(new StringMessageConverter());

        // 3. Conectar con token en la URL
        String url = "ws://localhost:" + port + "/ws?token=" + token;
        stompSession = stompClient.connectAsync(url, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                session.subscribe("/topic/public", new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) { return String.class; }
                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        chatMessages.offer((String) payload);
                    }
                });
                session.subscribe("/topic/products", new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) { return String.class; }
                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        productNotifications.offer((String) payload);
                    }
                });
                // Liberamos el latch cuando ambas suscripciones estén registradas
                subscriptionLatch.countDown();
                subscriptionLatch.countDown();
            }
        }).get(10, TimeUnit.SECONDS);

        // Esperar a que las suscripciones se hayan completado (opcional, pero más seguro)
        if (!subscriptionLatch.await(10, TimeUnit.SECONDS)) {
            throw new RuntimeException("Las suscripciones no se completaron a tiempo");
        }
        // Pequeña pausa para garantizar que el contexto de escritura esté limpio
        Thread.sleep(200);
    }

    @Test
    void shouldSendAndReceiveChatMessage() throws Exception {
        stompSession.send("/app/chat.sendMessage", "Hola desde test");
        String received = chatMessages.poll(5, TimeUnit.SECONDS);
        assertThat(received).isEqualTo("Hola desde test");
    }

    @Test
    void shouldReceiveProductCreationNotification() throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(
                Map.of("name", "Producto WS Test", "price", 50.0),
                headers
        );
        restTemplate.postForObject("http://localhost:" + port + "/api/products", request, String.class);

        String notification = productNotifications.poll(5, TimeUnit.SECONDS);
        assertThat(notification).contains("Nuevo producto creado: Producto WS Test");
    }
}