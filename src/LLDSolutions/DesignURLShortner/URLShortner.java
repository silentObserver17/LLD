package LLDSolutions.DesignURLShortner;

import LLDSolutions.DesignURLShortner.Encoding.URLEncoder;
import LLDSolutions.DesignURLShortner.Model.UrlEntry;
import LLDSolutions.DesignURLShortner.Repository.URLRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class URLShortner {
    private final URLEncoder encoder;
    private final URLRepository repository;
    private final String baseUrl;
    private final AtomicLong counter;

    public URLShortner(URLEncoder encoder, URLRepository repository, String baseUrl) {
        this.encoder = encoder;
        this.repository = repository;
        this.baseUrl = baseUrl;
        this.counter = new AtomicLong(100_000L);
    }

    public String shorten(ShortenRequest request) {
        // 1. Dedup: if same URL was already shortened (no TTL consideration), return existing
        if(request.getCustomAlias().isEmpty()){
            Optional<String> existing = repository.findCodeByOriginalUrl(request.getOriginalUrl());
            if(existing.isPresent()){
                return buildShortUrl(existing.get());
            }
        }

        // 2. Determine the code
        String code = request.getCustomAlias().orElseGet(() -> {
            long id = counter.getAndIncrement();
            return encoder.encode(id);
        });

        // 3. Conflict check for custom aliases
        if (repository.existsByCode(code)) {
            throw new IllegalArgumentException(
                    "Alias '" + code + "' is already taken."
            );
        }

        // 4. Build entry with optional expiry
        Instant expiresAt = request.getTtl()
                .map(ttl -> Instant.now().plus(ttl))
                .orElse(null);

        UrlEntry entry = new UrlEntry(request.getOriginalUrl(), expiresAt);
        repository.save(code, entry);

        return buildShortUrl(code);
    }

    public String resolve(String shortUrl) {
        String code = extractCode(shortUrl);

        UrlEntry entry = repository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Short URL not found: " + shortUrl));

        if(entry.isExpired()) {
            throw new IllegalStateException("Short URL is expired: " + shortUrl);
        }

        return entry.getOriginalUrl();
    }

    private String buildShortUrl(String code) {
        return baseUrl + "/" + code;
    }

    private String extractCode(String shortUrl) {
        // handles full url and bares code
        int idx = shortUrl.lastIndexOf('/');
        return idx >= 0 ? shortUrl.substring(idx + 1) : shortUrl;
    }
}
