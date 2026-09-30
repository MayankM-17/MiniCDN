package MM.edge;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@RestController
public class EdgeController {
    private final EdgeCacheService cache;

    @Value("${cdn.region:unknown}")
    private String region;

    public EdgeController(EdgeCacheService cache){
        this.cache=cache;
    }

    @GetMapping("/cdn/{*path}")
    public ResponseEntity<byte[]> serve(@PathVariable String path){
        String key=strip(path);

        Optional<CachedContent> hit = cache.peek(key);
        if(hit.isPresent()) return respond(hit.get(),true);

        CachedContent content = cache.fetchAndCache(key);
        if(content==null) {
            return ResponseEntity.notFound().header("X-Cache", "MISS").build();
         }
            return respond(content,false);
       }
        private ResponseEntity<byte[]> respond(CachedContent c,boolean hit){
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(c.contentType()))
                    .header("X-Cache", hit ? "HIT" : "MISS")
                    .header("X-Edge-Region", region)
                    .header("Age", String.valueOf(c.ageSeconds()))
                    .header("Cache-Control", "public, max-age=" + c.ttlSeconds())
                    .body(c.body());
        }

        @DeleteMapping("/admin/cache")
        public ResponseEntity<Void> invalidate(@RequestParam(required = false) String path){
            if(path==null || path.isBlank()) cache.invalidateAll();
            else cache.invalidate(strip(path));
            return ResponseEntity.noContent().build();
        }
        @GetMapping("/admin/stats")
        public Map<String, Object> stats(){
            CacheStats s=cache.stats();
            return Map.of(
                    "requests",s.requestCount(),
                    "hits",s.hitCount(),
                    "misses",s.missCount(),
                    "hitRatio",s.hitRate(),
                    "evictions",s.evictionCount()
            );
        }
        private String strip(String p) {
            return p.startsWith("/")?p.substring(1):p;
        }
    }

