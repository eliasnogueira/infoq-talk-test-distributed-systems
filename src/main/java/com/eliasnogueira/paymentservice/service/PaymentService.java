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
package com.eliasnogueira.paymentservice.service;

import com.eliasnogueira.paymentservice.dto.PaymentRequest;
import com.eliasnogueira.paymentservice.dto.PaymentResponse;
import com.eliasnogueira.paymentservice.dto.PaymentUpdateRequest;
import com.eliasnogueira.paymentservice.events.PaymentEvent;
import com.eliasnogueira.paymentservice.events.PaymentKafkaProducer;
import com.eliasnogueira.paymentservice.exceptions.PaymentNotFoundException;
import com.eliasnogueira.paymentservice.model.Payment;
import com.eliasnogueira.paymentservice.model.enums.PaymentStatus;
import com.eliasnogueira.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final FraudCheckService fraudCheckService;
    private final PaymentKafkaProducer kafkaProducer;

    @Transactional
    public PaymentResponse createPayment(PaymentRequest paymentRequest) {
        var payment = Payment.builder()
                .transactionId(paymentRequest.getTransactionId())
                .amount(paymentRequest.getAmount())
                .status(PaymentStatus.PENDING)
                .build();

        var savedPayment = paymentRepository.save(payment);
        log.info("Payment created with ID: {}", savedPayment.getId());

        kafkaProducer.send(new PaymentEvent(
                Instant.now(),
                PaymentEvent.EventType.CREATED,
                savedPayment
        ));

        return new ModelMapper().map(savedPayment, PaymentResponse.class);
    }

    @Transactional
    public PaymentResponse updatePayment(UUID paymentId, PaymentUpdateRequest updateRequest) {
        var payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with ID: " + paymentId));

        var newStatus = updateRequest.getStatus();

        // when updating to PAID, perform fraud check
        if (newStatus == PaymentStatus.PAID) {
            var response = fraudCheckService.checkForFraud(payment);
            if (response.isFraudulent()) newStatus = PaymentStatus.FRAUD;
        }

        payment.setStatus(newStatus);
        var updatedPayment = paymentRepository.save(payment);
        log.info("Payment updated with ID: {}, new status: {}", paymentId, newStatus);

        kafkaProducer.send(new PaymentEvent(
                Instant.now(),
                PaymentEvent.EventType.UPDATED,
                updatedPayment
        ));


        return new ModelMapper().map(updatedPayment, PaymentResponse.class);
    }

    public PaymentResponse getPaymentById(UUID paymentId) {
        var payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with ID: " + paymentId));
        return new ModelMapper().map(payment, PaymentResponse.class);
    }

    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(payment -> new ModelMapper().map(payment, PaymentResponse.class))
                .collect(Collectors.toList());
    }
}