package com.udemy.estore.OrdersService.saga;

import com.udemy.estore.OrdersService.command.commands.ApproveOrderCommand;
import com.udemy.estore.OrdersService.command.commands.RejectOrderCommand;
import com.udemy.estore.OrdersService.core.events.OrderApprovedEvent;
import com.udemy.estore.OrdersService.core.events.OrderCreatedEvent;
import com.udemy.estore.OrdersService.core.events.OrderRejectedEvent;
import com.udemy.estore.core.commands.CancelProductReservationCommand;
import com.udemy.estore.core.commands.ProcessPaymentCommand;
import com.udemy.estore.core.commands.ReserveProductCommand;
import com.udemy.estore.core.events.PaymentProcessedEvent;
import com.udemy.estore.core.events.ProductReservationCancelledEvent;
import com.udemy.estore.core.events.ProductReservedEvent;
import com.udemy.estore.core.model.User;
import com.udemy.estore.core.query.FetchUserPaymentDetailsQuery;
import org.axonframework.commandhandling.CommandCallback;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.CommandResultMessage;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.spring.stereotype.Saga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Saga
public class OrderSaga {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderSaga.class);

    @Autowired
    private transient CommandGateway commandGateway;

    @Autowired
    private transient QueryGateway queryGateway;

    @StartSaga
    @SagaEventHandler(associationProperty = "orderId")
    public void handle(OrderCreatedEvent orderCreatedEvent){

        ReserveProductCommand reserveProductCommand = ReserveProductCommand.builder()
                .orderId(orderCreatedEvent.getOrderId())
                .productId(orderCreatedEvent.getProductId())
                .quantity(orderCreatedEvent.getQuantity())
                .userId(orderCreatedEvent.getUserId())
                .build();

        LOGGER.info("OrderCreatedEvent handled for orderId : " + reserveProductCommand.getOrderId()
        + " and productId : "+ reserveProductCommand.getProductId());

        commandGateway.send(reserveProductCommand, new CommandCallback<ReserveProductCommand, Object>() {

            @Override
            public void onResult(CommandMessage<? extends ReserveProductCommand> commandMessage,
                                 CommandResultMessage<? extends  Object> commandResultMessage){
                if (commandResultMessage.isExceptional()){
                    // start a compensating transaction
                }
            }
        });
    }

    @SagaEventHandler(associationProperty = "orderId")
    public void handle(ProductReservedEvent productReservedEvent){
        // process user payment
        LOGGER.info("ProductReservedEvent is called for orderId : " + productReservedEvent.getOrderId()
                + " and productId : "+ productReservedEvent.getProductId());

        FetchUserPaymentDetailsQuery fetchUserPaymentDetailsQuery = FetchUserPaymentDetailsQuery.builder()
                .userId(productReservedEvent.getUserId())
                .build();


        User userPaymentDetails;

        try {
            userPaymentDetails = queryGateway.query(fetchUserPaymentDetailsQuery,
                    ResponseTypes.instanceOf(User.class)).join();
        } catch (Exception ex){
            LOGGER.error(ex.getMessage());
            // Start compensating transaction
            cancelProductReservation(productReservedEvent, ex.getMessage());
            return;
        }
        if (userPaymentDetails == null){
            // Start compensating transaction
            cancelProductReservation(productReservedEvent, "Could not fetch user payment details");
            return;
        }
        LOGGER.info("Successfully fetched user payment details for user : "+ userPaymentDetails.getFirstName());

        ProcessPaymentCommand processPaymentCommand = ProcessPaymentCommand.builder()
                .orderId(productReservedEvent.getOrderId())
                .paymentDetails(userPaymentDetails.getPaymentDetails())
                .paymentId(UUID.randomUUID().toString())
                .build();
        
        String result = null;
        try {
            result = commandGateway.sendAndWait(processPaymentCommand, 10, TimeUnit.SECONDS);
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage());
            // Start compensating transaction
            cancelProductReservation(productReservedEvent, ex.getMessage());
            return;
        }
        if (result == null) {
            LOGGER.info("The ProcessPaymentCommand resulted in NULL. Initiating a compensating transaction");
            // Start compensating transaction
            cancelProductReservation(productReservedEvent, "Could not process user payment with provided payment details");
        }
    }

    private void cancelProductReservation(ProductReservedEvent productReservedEvent, String reason) {
        CancelProductReservationCommand cancelProductReservationCommand =
                CancelProductReservationCommand.builder()
                        .orderId(productReservedEvent.getOrderId())
                        .quantity(productReservedEvent.getQuantity())
                        .productId(productReservedEvent.getProductId())
                        .userId(productReservedEvent.getUserId())
                        .reason(reason)
                        .build();

        commandGateway.send(cancelProductReservationCommand);
    }

    @SagaEventHandler(associationProperty = "orderId")
    public void handle(PaymentProcessedEvent paymentProcessedEvent){
        // Send an Approved order command
        ApproveOrderCommand approveOrderCommand =
                new ApproveOrderCommand(paymentProcessedEvent.getOrderId());
        commandGateway.send(approveOrderCommand);
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "orderId")
    public void handle(OrderApprovedEvent orderApprovedEvent){
        LOGGER.info("Order is approved. Order Saga is complete for orderId : "+ orderApprovedEvent.getOrderId());
        //SagaLifecycle.end(); not required if we use the End Saga annotation
    }

    @SagaEventHandler(associationProperty = "orderId")
    public void handle(ProductReservationCancelledEvent productReservationCancelledEvent){
        // Create and send a RejectOrderCommand
        RejectOrderCommand rejectOrderCommand = new RejectOrderCommand(productReservationCancelledEvent.getOrderId(), productReservationCancelledEvent.getReason());

        commandGateway.send(rejectOrderCommand);
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "orderId")
    public void handle(OrderRejectedEvent orderRejectedEvent){
        LOGGER.info("Successfully rejected order with Id : " + orderRejectedEvent.getOrderId());
    }
}
