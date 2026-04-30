package org.janus.demo.client.integration;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
@NullMarked
public class DemoServerClient {

  private static final ParameterizedTypeReference<Map<String, Object>> TYPE =
      new ParameterizedTypeReference<>() {};

  private final RestClient client;

  @SuppressWarnings("unchecked")
  public List<String> fetchRecommendations(int limit) {
    var response =
        client
            .get()
            .uri(uri -> uri.path("/demo/recommendations").queryParam("limit", limit).build())
            .retrieve()
            .body(TYPE);

    if (response == null) {
      throw new IllegalStateException("Empty response");
    }

    return (List<String>) response.get("recommendations");
  }
}
