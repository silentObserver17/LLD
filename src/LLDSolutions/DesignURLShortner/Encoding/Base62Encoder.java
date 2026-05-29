package LLDSolutions.DesignURLShortner.Encoding;

public class Base62Encoder implements URLEncoder {
    private static final String ALPHABET = "abcdefghijklmopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int BASE = 62;
    private final int minLength;

    public Base62Encoder(int minLength) {
        this.minLength = minLength;
    }

    @Override
    public String encode(long id) {
        StringBuilder sb = new StringBuilder();
        while(id > 0) {
            sb.append(ALPHABET.charAt((int)(id % BASE)));
            id /= BASE;
        }

        // pad to min length if needed.
        while(sb.length() < minLength) {
            sb.append(ALPHABET.charAt(0));
        }

        return sb.reverse().toString();
    }
}
