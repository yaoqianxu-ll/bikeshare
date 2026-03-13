package com.example.bickdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForumPostListResponse {

    private List<ForumPostResponse> records;
    private long total;
    private long current;
    private long size;
    private boolean hasMore;
}
