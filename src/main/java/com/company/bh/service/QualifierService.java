package com.company.bh.service;

import com.company.bh.dto.WebhookResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class QualifierService {

    private final RestTemplate restTemplate;

    @Value("${app.candidate.name}")  private String name;
    @Value("${app.candidate.regNo}") private String regNo;
    @Value("${app.candidate.email}") private String email;

    @Value("${app.endpoints.generateWebhook}") private String generateWebhookUrl;
    @Value("${app.endpoints.submitWebhook}")   private String submitWebhookUrl;

    @Value("${app.storage.solutionFile}") private String solutionFile;

    public void runFlow() {
        log.info("Starting qualifier flow...");

        WebhookResponse response = generateWebhook();

        int lastTwoDigits = extractLastTwoDigits(regNo);
        boolean isEven = (lastTwoDigits % 2 == 0);
        log.info("Reg No ends in {} → Assigned Question {}", lastTwoDigits, isEven ? "2 (Correct)" : "1 (Not yours)");

        // ✅ Your final SQL for Question 2
        String finalSql = """
SELECT
    e.EMP_ID,
    e.FIRST_NAME,
    e.LAST_NAME,
    d.DEPARTMENT_NAME,
    COUNT(e2.EMP_ID) AS YOUNGER_EMPLOYEES_COUNT
FROM EMPLOYEE e
JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID
LEFT JOIN EMPLOYEE e2
    ON e.DEPARTMENT = e2.DEPARTMENT
    AND e2.DOB > e.DOB
GROUP BY
    e.EMP_ID, e.FIRST_NAME, e.LAST_NAME, d.DEPARTMENT_NAME
ORDER BY
    e.EMP_ID DESC;
""";

        saveSqlToFile(finalSql);

        submitFinalSql(response.getAccessToken(), finalSql);

        log.info("✅ Flow completed successfully.");
    }

    private WebhookResponse generateWebhook() {
        Map<String, String> body = Map.of(
                "name", name.trim(),
                "regNo", regNo.trim(),
                "email", email.trim()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        log.info("📡 Sending request to generate webhook: {}", body);

        try {
            ResponseEntity<WebhookResponse> response =
                    restTemplate.exchange(generateWebhookUrl, HttpMethod.POST, entity, WebhookResponse.class);

            log.info("✅ generateWebhook Status = {}", response.getStatusCode());

            if (response.getBody() == null) {
                throw new IllegalStateException("Server returned empty webhook response");
            }
            return response.getBody();

        } catch (HttpStatusCodeException e) {
            log.error("❌ generateWebhook FAILED\nStatus: {}\nResponse Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("❌ generateWebhook unexpected error", e);
            throw e;
        }
    }

    private void submitFinalSql(String accessToken, String finalSql) {
        HttpHeaders headers = new HttpHeaders();
        // IMPORTANT: DO NOT use "Bearer " prefix
        headers.set("Authorization", accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String,String>> entity =
                new HttpEntity<>(Map.of("finalQuery", finalSql), headers);

        log.info("📡 Submitting final SQL to test webhook...");

        ResponseEntity<Void> response =
                restTemplate.exchange(submitWebhookUrl, HttpMethod.POST, entity, Void.class);

        log.info("✅ Submission Status = {}", response.getStatusCode());
    }

    private void saveSqlToFile(String sql) {
        try {
            Files.writeString(Path.of(solutionFile), sql);
            log.info("💾 Saved SQL to {}", solutionFile);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write SQL to file", e);
        }
    }

    private int extractLastTwoDigits(String regNo) {
        Matcher m = Pattern.compile("(\\d{1,2})(?!.*\\d)").matcher(regNo);
        if (m.find()) return Integer.parseInt(m.group(1));
        throw new IllegalArgumentException("regNo does not contain digits: " + regNo);
    }
}
