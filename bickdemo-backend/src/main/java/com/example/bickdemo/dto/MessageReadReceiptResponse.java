package com.example.bickdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageReadReceiptResponse {
    private Long readerId;
    private String readerUsername;
    private Long contactUserId;
    private List<Long> messageIds;
    private LocalDateTime readAt;
}
