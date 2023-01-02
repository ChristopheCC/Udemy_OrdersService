/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.udemy.estore.OrdersService.command.commands;

import com.udemy.estore.OrdersService.core.model.OrderStatus;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Value
public class RejectOrderCommand {
        
    @TargetAggregateIdentifier
    private final String orderId;
    private final String reason;
}
