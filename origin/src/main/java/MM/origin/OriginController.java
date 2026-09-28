package MM.origin;

import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/origin")
public class OriginController {
    // In real life: S3 / blob storage. In-memory map for the demo.
    private final Map<String, byte[]> contentStore = new ConcurrentHashMap<>();
    @PostMapping("/seed")
    public String seed() {
        contentStore.putIfAbsent("images/logo.png", "fake-png-bytes-v1".getBytes());
        contentStore.putIfAbsent("static/app.js","console.log('hello from origin');".getBytes());
        contentStore.putIfAbsent("videos/demo.mp4", "fake-mp4-bytes".getBytes());
        return "seeded 3 objects";
    }
    @GetMapping("/{*path}")
    public ResponseEntity<byte[]> get(@PathVariable String path){
        String key= strip(path);
        byte[] body = contentStore.get(key);
        if(body==null) return ResponseEntity.notFound().build();
        MediaType type = MediaTypeFactory.getMediaType(key).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().contentType(type).header("X-Served-By", "origin").body(body);
    }
    @PutMapping("/{*path}")
    public ResponseEntity<Void> put(@PathVariable String path, @RequestBody byte[] body) {
        contentStore.put(strip(path), body);
        return ResponseEntity.ok().build();
    }
    private String strip(String p) {
        return p.startsWith("/") ? p.substring(1) : p;
    }
}
