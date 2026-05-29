package LLDSolutions.DesignURLShortner.Model;

import java.time.Instant;
import java.util.Optional;

public class UrlEntry {
    private final String originalUrl;
    private final Instant expiresAt; // null  = never expires

    public UrlEntry(String originalUrl, Instant expiresAt) {
        this.originalUrl = originalUrl;
        this.expiresAt = expiresAt;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public Optional<Instant> getExpiresAt() {
        return Optional.ofNullable(expiresAt);
    }
}
