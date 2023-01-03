/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.udemy.estore.OrdersService.query;

import com.udemy.estore.OrdersService.core.data.OrderEntity;
import com.udemy.estore.OrdersService.core.data.OrdersRepository;
import com.udemy.estore.OrdersService.core.events.OrderApprovedEvent;
import com.udemy.estore.OrdersService.core.events.OrderCreatedEvent;
import com.udemy.estore.OrdersService.core.events.OrderRejectedEvent;
import com.udemy.estore.OrdersService.core.model.OrderSummary;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
public class OrderQueriesHandler {

    OrdersRepository ordersRepository;

    public OrderQueriesHandler(OrdersRepository ordersRepository){
        this.ordersRepository = ordersRepository;
    }

    @QueryHandler
    public OrderSummary findOrder(FindOrderQuery findOrderQuery){
        OrderEntity orderEntity = ordersRepository.findByOrderId(findOrderQuery.getOrderId());
        return new OrderSummary(orderEntity.getOrderId(), orderEntity.getOrderStatus(), "");
    }
}
