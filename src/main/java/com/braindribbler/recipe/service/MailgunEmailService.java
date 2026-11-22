package com.braindribbler.recipe.service;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
@PropertySource(value = "classpath:secret.properties", ignoreResourceNotFound = true)
public class MailgunEmailService {

    @Value("${mailgun.api.key}")
    private String apiKey;

    @Value("${mailgun.api.domain}")
    private String apiDomain;

    @Value("${mailgun.api.base-url}")
    private String apiBaseUrl;

    private final TemplateEngine templateEngine;

    public MailgunEmailService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    /**
     * Renders a Thymeleaf HTML template and sends it via Mailgun with tracking disabled.
     */
    public JsonNode sendHtmlEmail(String toEmail, String subject, String templateName,
                                  Map<String, Object> variables, String plainTextFallback) throws UnirestException {
        if (apiKey == null || apiDomain == null || apiBaseUrl == null) {
            throw new IllegalStateException(
                    "Required Mailgun configuration properties are missing from secret.properties!");
        }

        Context context = new Context();
        if (variables != null) {
            context.setVariables(variables);
        }
        String htmlBody = templateEngine.process(templateName, context);

        String finalApiUrl = apiBaseUrl + "/v3/" + apiDomain + "/messages";
        String friendlyFrom = "The Brain Dribbler <noreply@" + apiDomain + ">";

        HttpResponse<JsonNode> request = Unirest.post(finalApiUrl)
                .basicAuth("api", apiKey)
                .field("from", friendlyFrom)
                .field("to", toEmail)
                .field("subject", subject)
                .field("text", plainTextFallback)
                .field("html", htmlBody)
                .field("o:tracking", "no")         // Stops Mailgun from rewriting https:// to http:// tracking links
                .field("o:tracking-clicks", "no")  // Explicitly disables click tracking
                .field("o:tracking-opens", "no")   // Explicitly disables open tracking pixel
                .asJson();

        return request.getBody();
    }

    /**
     * Kept for backward compatibility if any other services still call sendEmail(...)
     */
    public JsonNode sendEmail(String toEmail, String subject, String textBody) throws UnirestException {
        String finalApiUrl = apiBaseUrl + "/v3/" + apiDomain + "/messages";
        String friendlyFrom = "The Brain Dribbler <noreply@" + apiDomain + ">";

        HttpResponse<JsonNode> request = Unirest.post(finalApiUrl)
                .basicAuth("api", apiKey)
                .field("from", friendlyFrom)
                .field("to", toEmail)
                .field("subject", subject)
                .field("text", textBody)
                .field("o:tracking", "no")
                .asJson();

        return request.getBody();
    }
}
