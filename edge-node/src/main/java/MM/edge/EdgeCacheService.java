package MM.edge;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

//memory manager
@Service
public class EdgeCacheService {
    private final Cache<String,CachedContent> cache;
    private final OriginClient originClient;
    private final long ttlSeconds;

    public EdgeCacheService(OriginClient originClient,
                            @Value("${cdn.ttl-seconds:60}") long ttlSeconds,
                            @Value("${cdn.cache.max-weight-bytes:67108864}") long maxWeightBytes) {

        this.originClient = originClient;
        this.ttlSeconds = ttlSeconds;
        this.cache = Caffeine.newBuilder()
                .maximumWeight(maxWeightBytes)                       // LRU eviction by bytes
                .weigher((String k, CachedContent v) -> v.body().length)
                .expireAfterWrite(Duration.ofSeconds(ttlSeconds))    // TTL policy
                .recordStats()
                .build();

    }
    public Optional<CachedContent> peek(String path){

        return Optional.ofNullable(cache.getIfPresent(path));
    }

    public CachedContent fetchAndCache(String path){
        OriginResponse origin=originClient.fetch(path);
        if(!origin.found()) return null;
        CachedContent content =new CachedContent(path, origin.body(), origin.contentType(), Instant.now(), ttlSeconds);
        cache.put(path,content);
        return content;
    }
    public void invalidate(String path) { cache.invalidate(path); }
    public void invalidateAll()         { cache.invalidateAll(); }
    public CacheStats stats()           { return cache.stats(); }
}
