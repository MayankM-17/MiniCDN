package MM.edge;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;


 record OriginResponse(boolean found, byte[] body, String contentType) {
    public static OriginResponse notFound() { return new OriginResponse(false, null, null); }
}

@Component
public class OriginClient {

    private final RestClient rest;

    public OriginClient(@Value("${cdn.origin.url}") String originUrl) {
        this.rest = RestClient.builder().baseUrl(originUrl).build();
    }

    public OriginResponse fetch(String path) {
        try {
            ResponseEntity<byte[]> resp = rest.get()
                    .uri("/origin/" + path)
                    .retrieve()
                    .toEntity(byte[].class);
            String ct = resp.getHeaders().getContentType() != null
                    ? resp.getHeaders().getContentType().toString()
                    : MediaType.APPLICATION_OCTET_STREAM_VALUE;
            return new OriginResponse(true, resp.getBody(), ct);
        } catch (HttpClientErrorException.NotFound e) {
            return OriginResponse.notFound();
        }
    }
}