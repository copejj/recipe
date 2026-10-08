package com.braindribbler.recipe.service;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

@Service
@PropertySource(value = "classpath:secret.properties", ignoreResourceNotFound = true)
public class MailgunEmailService {

    @Value("${mailgun.api.key}")
    private String apiKey;

    @Value("${mailgun.api.domain}")
    private String apiDomain;

    @Value("${mailgun.api.base-url}")
    private String apiBaseUrl;

    /**
     * Sends a generic plain-text email using dynamically injected properties.
     *
     * @param toEmail  The recipient's email address
     * @param subject  The subject line of the email
     * @param textBody The text content of the message body
     * @return The underlying JsonNode response from Mailgun
     * @throws UnirestException If the HTTP connection fails
     */
    public JsonNode sendEmail(String toEmail, String subject, String textBody) throws UnirestException {
        // Validation check to ensure all properties injected properly
        if (apiKey == null || apiDomain == null || apiBaseUrl == null) {
            throw new IllegalStateException(
                    "Required Mailgun configuration properties are missing from secret.properties!");
        }

        // Dynamically compile target endpoints and identity headers
        String finalApiUrl = apiBaseUrl + "/v3/" + apiDomain + "/messages";
        String friendlyFrom = "The Brain Dribbler <noreply@" + apiDomain + ">";

        HttpResponse<JsonNode> request = Unirest.post(finalApiUrl)
                .basicAuth("api", apiKey)
                .queryString("from", friendlyFrom)
                .queryString("to", toEmail)
                .queryString("subject", subject)
                .queryString("text", textBody)
                .asJson();

        return request.getBody();
    }
}
