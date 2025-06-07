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
package com.eliasnogueira.paymentservice.payments;

import com.eliasnogueira.paymentservice.model.Payment;
import com.eliasnogueira.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.eliasnogueira.paymentservice.model.enums.PaymentStatus.PENDING;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected PaymentRepository paymentRepository;

    @BeforeEach
    void cleanDatabase() {
        paymentRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create a new payment and return 201 Created")
    void createPayment() throws Exception {
        String payload = """
                {
                  "transactionId": "txn_123",
                  "amount": 100.50
                }""";

        mockMvc.perform(post("/api/payments")
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId", is("txn_123")))
                .andExpect(jsonPath("$.amount", is(100.50)))
                .andExpect(jsonPath("$.status", is("PENDING")));
    }

    @Test
    @DisplayName("Should find a payment by ID and return 200 OK")
    void getPayment() throws Exception {
        var payment = Payment.builder().transactionId("txn_456").amount(BigDecimal.valueOf(200.75)).status(PENDING).build();

        var savedPayment = paymentRepository.save(payment);

        mockMvc.perform(get("/api/payments/{paymentId}", savedPayment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId", is("txn_456")))
                .andExpect(jsonPath("$.amount", is(200.75)))
                .andExpect(jsonPath("$.status", is("PENDING")));
    }

    @Test
    @DisplayName("Should find all payments and return 200 OK")
    void getAllPayments() throws Exception {
        var firstPayment = Payment.builder().transactionId("txn_1").amount(BigDecimal.valueOf(100.00))
                .status(PENDING).build();

        var secondPayment = Payment.builder().transactionId("txn_2").amount(BigDecimal.valueOf(200.00))
                .status(PENDING).build();

        paymentRepository.saveAll(List.of(firstPayment, secondPayment));

        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].transactionId", containsInAnyOrder("txn_1", "txn_2")))
                .andExpect(jsonPath("$[*].amount", containsInAnyOrder(100.00, 200.00)));
    }

    @Test
    @DisplayName("Should update a payment and return 200 OK")
    void updatePayment() throws Exception {
        var payload = """
                {
                  "status": "PAID"
                }""";

        var payment = Payment.builder().transactionId("txn_update").amount(BigDecimal.valueOf(300.00))
                .status(PENDING).build();

        var savedPayment = paymentRepository.save(payment);

        mockMvc.perform(put("/api/payments/{paymentId}", savedPayment.getId())
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PAID")));
    }

    @Test
    @DisplayName("Should return 404 when payment not found")
    void getPayment_ShouldReturn404WhenNotFound() throws Exception {
        var nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/payments/{paymentId}", nonExistentId))
                .andExpect(status().isNotFound());
    }
}