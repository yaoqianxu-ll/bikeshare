package com.example.bickdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlacklistEntryResponse {

    private String ip;

    private String address;

    private String reason;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime expireAt;

    private Long remainingSeconds;
}
