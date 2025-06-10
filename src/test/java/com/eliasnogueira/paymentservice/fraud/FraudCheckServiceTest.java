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
package com.eliasnogueira.paymentservice.fraud;

import com.eliasnogueira.paymentservice.config.FraudCheckConfig;
import com.eliasnogueira.paymentservice.dto.FraudCheckResponse;
import com.eliasnogueira.paymentservice.model.Payment;
import com.eliasnogueira.paymentservice.service.FraudCheckService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static com.eliasnogueira.paymentservice.model.enums.PaymentStatus.PENDING;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;

@ExtendWith(MockitoExtension.class)
class FraudCheckServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private FraudCheckConfig fraudCheckConfig;

    @InjectMocks
    private FraudCheckService fraudCheckService;

    private Payment payment;

    @BeforeEach
    void setUp() {
        payment = Payment.builder().id(UUID.randomUUID()).transactionId("txn_test_123")
                .amount(BigDecimal.valueOf(100.50)).status(PENDING).build();

        when(fraudCheckConfig.getUrl()).thenReturn("http://fraud-check-service/api");
        when(fraudCheckConfig.getApiKey()).thenReturn("test-api-key");
    }

    @Test
    @DisplayName("No fraud detected")
    void shouldReturnFalseWhenNoFraudDetected() {
        var mockResponse = FraudCheckResponse.builder().fraudulent(false)
                .message("Transaction approved after successful fraud check.").build();

        when(restTemplate.exchange(
                anyString(),
                eq(GET),
                any(),
                eq(FraudCheckResponse.class))
        ).thenReturn(new ResponseEntity<>(mockResponse, OK));

        var response = fraudCheckService.checkForFraud(payment);
        assertAll(() -> {
            assertFalse(response.isFraudulent());
            assertEquals("Transaction approved after successful fraud check.", response.getMessage());
        });
    }

    @Test
    @DisplayName("Fraud detected")
    void shouldReturnTrueWhenFraudDetected() {
        var mockResponse = FraudCheckResponse.builder().fraudulent(true)
                .message("Transaction flagged as suspicious due to unusual spending patterns.").build();

        when(restTemplate.exchange(
                anyString(),
                eq(GET),
                any(),
                eq(FraudCheckResponse.class))
        ).thenReturn(new ResponseEntity<>(mockResponse, OK));

        var response = fraudCheckService.checkForFraud(payment);
        assertAll(() -> {
            assertTrue(response.isFraudulent());
            assertEquals("Transaction flagged as suspicious due to unusual spending patterns.", response.getMessage());
        });
    }
}
