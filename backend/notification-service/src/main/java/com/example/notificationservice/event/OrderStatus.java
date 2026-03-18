package com.example.notificationservice.event;

public enum OrderStatus {

    ORDER_PLACED,
    ORDER_CONFIRMED,
    ORDER_CANCELLED,//implement logic after
    ORDER_UPDATED,//implement after
    DISPATCHED,
    OUT_FOR_DELIVERY,
    DELIVERED
}
