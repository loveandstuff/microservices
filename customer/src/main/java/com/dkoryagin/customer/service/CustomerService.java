package com.dkoryagin.customer.service;

import com.dkoryagin.amqp.RabbitMQMessageProducer;
import com.dkoryagin.clients.fraud.FraudCheckResponse;
import com.dkoryagin.clients.fraud.FraudClient;
import com.dkoryagin.clients.notification.NotificationClient;
import com.dkoryagin.clients.notification.NotificationRequest;
import com.dkoryagin.customer.model.Customer;
import com.dkoryagin.customer.model.CustomerRegistrationRequest;
import com.dkoryagin.customer.repository.CustomerRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final FraudClient fraudClient;
    private final RabbitMQMessageProducer rabbitMQMessageProducer;

    public void registerCustomer(CustomerRegistrationRequest request) {
        Customer customer = Customer.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .build();
        //todo: check if email valid
        //todo: check if email not taken
        //todo: check if fraudster
        //todo: send notification
        customerRepository.saveAndFlush(customer);

        FraudCheckResponse fraudCheckResponse = fraudClient.isFraudster(customer.getId());
        if (fraudCheckResponse.isFraudster()){
            throw new IllegalStateException("fraudster");
        }

        NotificationRequest notificationRequest = new NotificationRequest(
                customer.getId(),
                customer.getEmail(),
                String.format("Hi %s, welcome...",
                        customer.getFirstName())
        );
        rabbitMQMessageProducer.publish(
                notificationRequest,
                "internal.exchange",
                "internal.notification.routing-key"
        );

    }
}
