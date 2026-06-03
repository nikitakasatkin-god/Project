package org.example.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class IntegrationSettings {

    @Value("${onec.integration.enabled:false}")
    private boolean onecIntegrationEnabled;

    @Value("${onec.sync.interval:60000}")
    private long syncInterval;

    @Value("${onec.api.url:http://localhost:8082/api}")
    private String onecApiUrl;

    @Value("${onec.api.username:exchange}")
    private String onecUsername;

    @Value("${onec.api.password:exchange123}")
    private String onecPassword;

    public boolean isOnecIntegrationEnabled() { return onecIntegrationEnabled; }
    public void setOnecIntegrationEnabled(boolean enabled) { this.onecIntegrationEnabled = enabled; }

    public long getSyncInterval() { return syncInterval; }
    public void setSyncInterval(long syncInterval) { this.syncInterval = syncInterval; }

    public String getOnecApiUrl() { return onecApiUrl; }
    public void setOnecApiUrl(String onecApiUrl) { this.onecApiUrl = onecApiUrl; }

    public String getOnecUsername() { return onecUsername; }
    public void setOnecUsername(String onecUsername) { this.onecUsername = onecUsername; }

    public String getOnecPassword() { return onecPassword; }
    public void setOnecPassword(String onecPassword) { this.onecPassword = onecPassword; }
}