/*
 * MIT License
 *
 * Copyright (c) 2025 Elias Nogueira
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.eliasnogueira.paymentservice.events;

import com.eliasnogueira.paymentservice.model.Payment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.eliasnogueira.paymentservice.model.enums.PaymentStatus.PENDING;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
class PaymentKafkaIntegrationTest {

    @Autowired
    private PaymentKafkaProducer producer;

    @Autowired
    private PaymentKafkaConsumer consumer;

    private static Payment payment;

    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        kafka.start();
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @BeforeAll
    static void beforeAll() {
        payment = Payment.builder().id(UUID.randomUUID())
                .amount(new BigDecimal("200.00")).status(PENDING).build();
    }

    @Test
    @DisplayName("Should fail because the event takes too long to be consumed")
    void shouldFail() {
        var event = new PaymentEvent(
                Instant.now(),
                PaymentEvent.EventType.CREATED,
                payment
        );

        producer.send(event);
        List<PaymentEvent> consumedEvents = consumer.getConsumedEvents();
        assertThat(consumedEvents).anySatisfy(paymentEvent -> {
            assertThat(paymentEvent.getType()).isEqualTo(PaymentEvent.EventType.CREATED);
            assertThat(paymentEvent.getPayment().getId()).isEqualTo(payment.getId());
            assertThat(paymentEvent.getTimestamp()).isNotNull();
        });


        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(consumer.getConsumedEvents())
                        .anySatisfy(e -> {
                            assertThat(e.getType()).isEqualTo(PaymentEvent.EventType.CREATED);
                            assertThat(e.getPayment().getId()).isEqualTo(payment.getId());
                            assertThat(e.getTimestamp()).isNotNull();
                        }));
    }

    @Test
    @DisplayName("Should consume published event")
    void shouldConsumePublishedEvent() {
        var event = new PaymentEvent(
                Instant.now(),
                PaymentEvent.EventType.CREATED,
                payment
        );

        producer.send(event);

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(consumer.getConsumedEvents())
                        .anySatisfy(e -> {
                            assertThat(e.getType()).isEqualTo(PaymentEvent.EventType.CREATED);
                            assertThat(e.getPayment().getId()).isEqualTo(payment.getId());
                            assertThat(e.getTimestamp()).isNotNull();
                        }));
    }
}
