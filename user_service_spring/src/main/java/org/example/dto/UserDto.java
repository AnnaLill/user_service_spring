package org.example.dto;

import java.time.LocalDateTime;

public record UserDto(
        Long id,
        String name,
        String email,
        int age,
        LocalDateTime createdAt
) {
}

