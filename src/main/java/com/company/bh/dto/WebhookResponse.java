package com.company.bh.dto;

import lombok.Data;

@Data
public class WebhookResponse {
    private String webhook;     // returned webhook URL (not strictly needed to submit)
    private String accessToken; // token to send in Authorization header
}
