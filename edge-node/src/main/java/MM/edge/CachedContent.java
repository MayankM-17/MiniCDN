package MM.edge;


import java.time.Duration;
import java.time.Instant;

//By simply declaring it as a record, Java automatically generates the constructor, getter methods (e.g., path(), body()), equals(), hashCode(), and toString() methods behind the scenes, eliminating boilerplate code
public record CachedContent(String path, byte[] body, String contentType, Instant cachedAt,long ttlSeconds) {
    public long ageSeconds() {
        return Math.max(0, Duration.between(cachedAt, Instant.now()).getSeconds());
    }
}
