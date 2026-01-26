package com.demo.burnout.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "demo")
public class DemoConfiguration {
    private String repo = "roryp/burnout-demo";
    private String userId = "roryp";
    private FridayConfig friday = new FridayConfig();
    private ClockConfig clock = new ClockConfig();

    public String getRepo() { return repo; }
    public void setRepo(String repo) { this.repo = repo; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public FridayConfig getFriday() { return friday; }
    public void setFriday(FridayConfig friday) { this.friday = friday; }

    public ClockConfig getClock() { return clock; }
    public void setClock(ClockConfig clock) { this.clock = clock; }

    public static class FridayConfig {
        private boolean enabled = false;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class ClockConfig {
        private String zone = "UTC";
        private String fixed;
        public String getZone() { return zone; }
        public void setZone(String zone) { this.zone = zone; }
        public String getFixed() { return fixed; }
        public void setFixed(String fixed) { this.fixed = fixed; }
    }
}
