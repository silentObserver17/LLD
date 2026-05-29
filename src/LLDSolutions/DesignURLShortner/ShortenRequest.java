package LLDSolutions.DesignURLShortner;

import java.time.Duration;
import java.util.Optional;

public class ShortenRequest {
    private final String originalUrl;
    private final String customAlias; // nullable
    private final Duration ttl;

    public ShortenRequest(String originalUrl, String customAlias, Duration ttl) {
        this.originalUrl = originalUrl;
        this.customAlias = customAlias;
        this.ttl = ttl;
    }

    // Convenience factory for simple case
    public static ShortenRequest of(String url) {
        return new ShortenRequest(url, null, null);
    }

    public String getOriginalUrl() { return originalUrl; }
    public Optional<String> getCustomAlias() { return Optional.ofNullable(customAlias); }
    public Optional<Duration> getTtl() { return Optional.ofNullable(ttl); }
}
