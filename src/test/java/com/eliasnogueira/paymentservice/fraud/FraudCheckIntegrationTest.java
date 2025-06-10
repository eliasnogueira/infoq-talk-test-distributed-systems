package com.eliasnogueira.paymentservice.fraud;

import com.eliasnogueira.paymentservice.model.Payment;
import com.eliasnogueira.paymentservice.service.FraudCheckService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(properties = {
        "fraud.check.url=https://sandbox-qa-vendor.com:8087/api/fraud",
        "fraud.check.api-key=secret-fraud-key"})
class FraudCheckIntegrationTest {

    @Autowired
    private FraudCheckService fraudCheckService;

    @Test
    @DisplayName("Should return not fraudulent for a valid payment")
    void shouldReturnNotFraudulent() {
        var payment = Payment.builder()
                .amount(new BigDecimal("123.56")).transactionId("txn_123").build();

        boolean result = fraudCheckService.checkForFraud(payment);
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return fraudulent for a suspicious payment")
    void shouldReturnFraudulent() {
        var payment = Payment.builder()
                .amount(new BigDecimal("9999.99")).transactionId("txn_987").build();

        boolean result = fraudCheckService.checkForFraud(payment);
        assertTrue(result);
    }
}
