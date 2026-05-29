package LLDSolutions.DesignURLShortner.Repository;

import LLDSolutions.DesignURLShortner.Model.UrlEntry;

import java.util.Optional;

public interface URLRepository {
    void save(String code, UrlEntry entry);
    Optional<UrlEntry> findByCode(String code);
    boolean existsByCode(String code);

    // For reverse lookup (dedup): same long URL → same short code
    Optional<String> findCodeByOriginalUrl(String originalUrl);
}
