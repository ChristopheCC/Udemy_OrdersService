/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.udemy.estore.OrdersService.core.model;

import lombok.Value;

@Value
public class OrderSummary {

    private final String orderId;
    private final OrderStatus orderStatus;
    private final String message;
}