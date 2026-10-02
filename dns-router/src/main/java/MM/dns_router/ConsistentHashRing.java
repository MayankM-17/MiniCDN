package MM.dns_router;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;

public class ConsistentHashRing {

    private final TreeMap<Long,String> ring =new TreeMap<>();
    private final int virtualNodes;

    public ConsistentHashRing(List<String> nodes, int virtualNodes){
        this.virtualNodes=virtualNodes;
        nodes.forEach(this::add);
    }

    public void add(String node){
        IntStream.range(0,virtualNodes).forEach(i->ring.put(hash(node + "#vn" + i),node));
    }
    public void remove(String node){
        IntStream.range(0,virtualNodes).forEach(i->ring.remove(hash(node + "#vn"+i)));
    }

    public String get(String key){
        if(ring.isEmpty()) return null;
        Map.Entry<Long ,String> e=ring.ceilingEntry(hash(key));
        return (e!=null ? e:ring.firstEntry()).getValue();
    }
    private long hash(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.wrap(digest).getLong();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
