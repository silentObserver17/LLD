package LLDSolutions.DesignURLShortner.Repository;

import LLDSolutions.DesignURLShortner.Model.UrlEntry;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryURLRepo implements URLRepository {
    // code -> entry
    private final Map<String, UrlEntry> codeToEntry = new HashMap<>();

    // original URL -> code (for deduplication)
    private final Map<String, String> urlToCode = new HashMap<>();


    @Override
    public void save(String code, UrlEntry entry) {
        codeToEntry.put(code, entry);
        urlToCode.put(entry.getOriginalUrl(), code);
    }

    @Override
    public Optional<UrlEntry> findByCode(String code) {
        return Optional.ofNullable(codeToEntry.get(code));
    }

    @Override
    public boolean existsByCode(String code) {
        return codeToEntry.containsKey(code);
    }

    @Override
    public Optional<String> findCodeByOriginalUrl(String originalUrl) {
        return Optional.ofNullable(urlToCode.get(originalUrl));
    }
}
