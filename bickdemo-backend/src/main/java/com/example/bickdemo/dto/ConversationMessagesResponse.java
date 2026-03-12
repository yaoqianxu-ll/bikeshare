package com.example.bickdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMessagesResponse {
    private List<ChatMessageResponse> records;
    private Long total;
    private Integer page;
    private Integer size;
    private Boolean hasMore;
}
