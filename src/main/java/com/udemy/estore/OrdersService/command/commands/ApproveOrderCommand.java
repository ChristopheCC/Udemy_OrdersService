/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.udemy.estore.OrdersService.command.commands;

import com.udemy.estore.OrdersService.core.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Builder
@AllArgsConstructor
@Data
public class ApproveOrderCommand {
        
    @TargetAggregateIdentifier
    public final String orderId;
}
