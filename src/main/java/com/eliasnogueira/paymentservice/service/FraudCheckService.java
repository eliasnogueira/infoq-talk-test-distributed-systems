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

import com.eliasnogueira.paymentservice.config.FraudCheckConfig;
import com.eliasnogueira.paymentservice.dto.FraudCheckResponse;
import com.eliasnogueira.paymentservice.model.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import static org.springframework.http.HttpMethod.GET;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudCheckService {

    private final RestTemplate restTemplate;
    private final FraudCheckConfig fraudCheckConfig;

    public FraudCheckResponse checkForFraud(Payment payment) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-KEY", fraudCheckConfig.getApiKey());

            String url = fraudCheckConfig.getUrl() + "/check?amount=" + payment.getAmount() +
                    "&transactionId=" + payment.getTransactionId();

            ResponseEntity<FraudCheckResponse> response = restTemplate.exchange(url, GET, new HttpEntity<>(headers),
                    FraudCheckResponse.class
            );

            assert response.getBody() != null;
            log.info("Fraud check for payment ID {} returned: {}", payment.getId(), response.getBody());

            return response.getBody();
        } catch (Exception e) {
            log.error("Error checking fraud for payment ID: {}", payment.getId(), e);
            return null;
        }
    }
}
