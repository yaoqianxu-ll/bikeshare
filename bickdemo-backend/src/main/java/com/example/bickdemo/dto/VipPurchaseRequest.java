package com.example.bickdemo.dto;

import lombok.Data;

@Data
public class VipPurchaseRequest {
    private String packageType; // MONTHLY/QUARTERLY/YEARLY
    private String paymentMethod; // CASH/POINTS
}