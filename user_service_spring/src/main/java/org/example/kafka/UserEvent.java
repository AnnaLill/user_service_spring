package org.example.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserEvent(
        @JsonProperty("operation") String operation,
        @JsonProperty("email") String email
) {
}
