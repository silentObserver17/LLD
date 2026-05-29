package LLDSolutions.DesignURLShortner;

import LLDSolutions.DesignURLShortner.Encoding.Base62Encoder;
import LLDSolutions.DesignURLShortner.Encoding.URLEncoder;
import LLDSolutions.DesignURLShortner.Repository.InMemoryURLRepo;
import LLDSolutions.DesignURLShortner.Repository.URLRepository;

import java.time.Duration;

public class Main {
    public static void main(String[] args) {
        URLEncoder encoder = new Base62Encoder(6);
        URLRepository repository = new InMemoryURLRepo();
        URLShortner shortner = new URLShortner(encoder, repository, "https://short.ly");

        // Basic Shorten
        String s1 = shortner.shorten(ShortenRequest.of("https://www.google.com/search?q=java+lld"));
        System.out.println("Shortened: " + s1);

        // Dedup — same URL should return same code
        String s2 = shortner.shorten(ShortenRequest.of("https://www.google.com/search?q=java+lld"));
        System.out.println("Dedup same: " + s2);
        System.out.println("Same code? " + s1.equals(s2)); // true

        // Resolve
        String original = shortner.resolve(s1);
        System.out.println("Resolved: " + original);

        // Custom alias
        String s3 = shortner.shorten(new ShortenRequest("https://github.com/jm", "github-jm", null));
        System.out.println("Custom alias: " + s3);


        // With TTL
        String s4 = shortner.shorten(new ShortenRequest(
                "https://example.com/promo", null, Duration.ofSeconds(2)
        ));
        System.out.println("With TTL: " + s4);

        try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
        try {
            shortner.resolve(s4); // should throw
        } catch (IllegalStateException e) {
            System.out.println("Expired: " + e.getMessage());
        }
    }
}
