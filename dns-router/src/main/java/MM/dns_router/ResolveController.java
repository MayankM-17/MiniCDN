package MM.dns_router;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


@RestController
public class ResolveController {

    private final Map<String, List<String>> edgesByRegion;
    private final Map<String, ConsistentHashRing> ringsByRegion = new ConcurrentHashMap<>();

    public ResolveController(@Value("#{${router.regions}}") Map<String, String> rawRegions) {
        this.edgesByRegion = rawRegions.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e ->
                        Arrays.stream(e.getValue().split(",")).map(String::trim).toList()));
    }

    /** Simulated GeoDNS: route region → nearest edge pool  */
    @GetMapping("/resolve")
    public Map<String, Object> resolve(@RequestParam(defaultValue = "us-east") String region,
                                       @RequestParam(defaultValue = "/") String path) {
        List<String> edges = edgesByRegion.getOrDefault(region, List.of());
        if (edges.isEmpty()) return Map.of("error", "unknown region: " + region);

        String edge = edges.size() == 1
                ? edges.get(0)
                : ringsByRegion.computeIfAbsent(region,
                r -> new ConsistentHashRing(edges, 100)).get(path);

        return Map.of("region", region,
                "edgeUrl", edge,
                "strategy", edges.size() == 1 ? "single-edge" : "consistent-hashing");
    }
}