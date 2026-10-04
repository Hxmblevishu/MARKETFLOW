package com.marketflow.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Render Keep-Alive Scheduler Tests")
class RenderKeepAliveSchedulerTest {

    @Test
    @DisplayName("URL Resolution: Resolves APP_PUBLIC_URL when provided")
    void testResolveExplicitAppPublicUrl() {
        RenderKeepAliveScheduler scheduler = new RenderKeepAliveScheduler();
        scheduler.setAppPublicUrl("https://marketflow-backend.onrender.com");

        String url = scheduler.resolvePingUrl();
        assertEquals("https://marketflow-backend.onrender.com/api/ping", url);
    }

    @Test
    @DisplayName("URL Resolution: Automatically resolves RENDER_EXTERNAL_HOSTNAME when running on Render")
    void testResolveRenderExternalHostname() {
        RenderKeepAliveScheduler scheduler = new RenderKeepAliveScheduler();
        scheduler.setRenderExternalHostname("marketflow-backend-abc.onrender.com");

        String url = scheduler.resolvePingUrl();
        assertEquals("https://marketflow-backend-abc.onrender.com/api/ping", url);
    }

    @Test
    @DisplayName("URL Resolution: Returns null in local environment without throwing error")
    void testResolveLocalMode() {
        RenderKeepAliveScheduler scheduler = new RenderKeepAliveScheduler();
        scheduler.setAppPublicUrl("");
        scheduler.setRenderExternalHostname("");

        String url = scheduler.resolvePingUrl();
        assertNull(url);
        // Ensure pingSelf() handles null URL gracefully without exception
        assertDoesNotThrow(scheduler::pingSelf);
    }
}
