package org.janus.statestore.service;

import lombok.RequiredArgsConstructor;
import org.janus.statestore.mapper.DegradationStateUpdateMapper;
import org.janus.statestore.model.DegradationState;
import org.janus.statestore.model.DegradationStateUpdate;
import org.janus.statestore.model.DegradationStateUpdateSource;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@NullMarked
public class DegradationStateService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final DegradationStateUpdateMapper stateUpdateMapper;

    public List<DegradationState> getDegradationStates(List<String> degradationIds) {
        if (CollectionUtils.isEmpty(degradationIds)) {
            return Collections.emptyList();
        }

        int sourcesCount = DegradationStateUpdateSource.values().length;
        List<String> keys = new ArrayList<>(degradationIds.size() * sourcesCount);
        for (String id : degradationIds) {
            for (DegradationStateUpdateSource source : DegradationStateUpdateSource.values()) {
                keys.add(createStateKey(id, source));
            }
        }

        List<@Nullable String> values = redisTemplate.opsForValue()
                                                     .multiGet(keys);
        if (CollectionUtils.isEmpty(values)) {
            return Collections.emptyList();
        }

        Map<String, DegradationState> resolved = new LinkedHashMap<>();
        for (int i = 0; i < degradationIds.size(); ++i) {
            String id = degradationIds.get(i);
            for (int j = 0; j < sourcesCount; ++j) {
                String json = values.get(i * sourcesCount + j);
                if (json != null) {
                    resolved.put(id, objectMapper.readValue(json, DegradationState.class));
                    break;
                }
            }
        }

        return new ArrayList<>(resolved.values());
    }

    @SuppressWarnings("unchecked")
    public void updateDegradationStates(List<DegradationStateUpdate> updates) {
        if (CollectionUtils.isEmpty(updates)) {
            return;
        }

        redisTemplate.execute(new SessionCallback<>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                try {
                    for (DegradationStateUpdate update : updates) {
                        String key = createStateKey(update.degradationId(), update.source());
                        DegradationState state = stateUpdateMapper.fromModelToState(update);
                        String json = objectMapper.writeValueAsString(state);

                        Duration ttl = update.ttl();
                        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
                            throw new IllegalArgumentException("TTL must be positive for key " + key);
                        }

                        operations.opsForValue()
                                  .set(key, json, ttl);
                    }
                } catch (RuntimeException e) {
                    operations.discard();
                    throw e;
                }
                return operations.exec();
            }
        });
    }

    private static String createStateKey(String degradationId, DegradationStateUpdateSource source) {
        return "janus:state:" + degradationId + ":" + source.name()
                                                            .toLowerCase();
    }
}
