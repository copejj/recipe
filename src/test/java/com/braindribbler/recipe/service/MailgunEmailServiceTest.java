package com.braindribbler.recipe.service;

import com.mashape.unirest.http.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class MailgunEmailServiceTest {

    @Value("${mailgun.api.test-email}")
    private String testRecipient;

    @Autowired
    private MailgunEmailService emailService;

    @Test
    public void testSendEmailSuccessfully() throws Exception {
        // Arrange dynamic arguments
        String testSubject = "Spring Boot Integration Test";
        String testBody = "If you are reading this, your Mailgun abstraction and secret.properties file are working flawlessly!";

        // Act - Trigger the actual network call
        JsonNode responseBody = emailService.sendEmail(testRecipient, testSubject, testBody);

        // Assert - Verify Mailgun received and acknowledged the payload
        assertNotNull(responseBody);

        String rawResponse = responseBody.toString();
        System.out.println("Mailgun API Response: " + rawResponse);

        // Mailgun API returns a message block containing "Queued" or "Thank you" on
        // success
        assertTrue(rawResponse.contains("message") || rawResponse.contains("id"));
    }
}
