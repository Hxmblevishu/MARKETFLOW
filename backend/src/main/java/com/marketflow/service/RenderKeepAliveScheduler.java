package com.marketflow.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;

/**
 * Scheduled service to keep Render free tier web services awake.
 *
 * Render Free Tier automatically puts web services into a sleeping/idling state
 * after 15 minutes of inactivity (no inbound HTTP requests).
 *
 * This scheduler runs every 13 minutes (780,000 ms), sending a lightweight GET request
 * to its own public URL (/api/ping). The incoming request resets Render's 15-minute
 * idle timer, preventing cold starts and ensuring 24/7 availability.
 */
@Service
@ConditionalOnProperty(name = "render.keep-alive.enabled", havingValue = "true", matchIfMissing = true)
public class RenderKeepAliveScheduler {

    private static final Logger log = LoggerFactory.getLogger(RenderKeepAliveScheduler.class);

    private final RestClient restClient;

    @Value("${APP_PUBLIC_URL:}")
    private String appPublicUrl;

    @Value("${RENDER_EXTERNAL_HOSTNAME:}")
    private String renderExternalHostname;

    @Value("${server.port:8080}")
    private String serverPort;

    public RenderKeepAliveScheduler() {
        this.restClient = RestClient.builder()
                .build();
    }

    public RenderKeepAliveScheduler(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Executes every 13 minutes (780,000 ms) with an initial delay of 1 minute (60,000 ms).
     */
    @Scheduled(fixedRate = 780000, initialDelay = 60000)
    public void pingSelf() {
        String targetUrl = resolvePingUrl();

        if (targetUrl == null || targetUrl.isBlank()) {
            log.debug("Render keep-alive scheduler: No public URL or external hostname configured. Skipping self-ping in local mode.");
            return;
        }

        try {
            log.info("Sending 13-minute keep-alive ping to: {}", targetUrl);
            String response = restClient.get()
                    .uri(URI.create(targetUrl))
                    .retrieve()
                    .body(String.class);

            log.info("Render keep-alive ping successful: {}", response);
        } catch (Exception ex) {
            log.warn("Render keep-alive ping to {} failed: {}", targetUrl, ex.getMessage());
        }
    }

    /**
     * Resolves the ping URL in order of priority:
     * 1. Explicit APP_PUBLIC_URL (e.g., https://my-app.onrender.com)
     * 2. Render automatic RENDER_EXTERNAL_HOSTNAME (e.g., my-app.onrender.com)
     * 3. Localhost fallback if configured
     */
    public String resolvePingUrl() {
        if (appPublicUrl != null && !appPublicUrl.isBlank()) {
            String baseUrl = appPublicUrl.endsWith("/") ? appPublicUrl.substring(0, appPublicUrl.length() - 1) : appPublicUrl;
            return baseUrl.endsWith("/api/ping") ? baseUrl : baseUrl + (baseUrl.endsWith("/api") ? "/ping" : "/api/ping");
        }

        if (renderExternalHostname != null && !renderExternalHostname.isBlank()) {
            return "https://" + renderExternalHostname + "/api/ping";
        }

        return null;
    }

    public void setAppPublicUrl(String appPublicUrl) {
        this.appPublicUrl = appPublicUrl;
    }

    public void setRenderExternalHostname(String renderExternalHostname) {
        this.renderExternalHostname = renderExternalHostname;
    }
}
