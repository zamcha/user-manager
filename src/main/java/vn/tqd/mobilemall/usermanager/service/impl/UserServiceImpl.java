package vn.tqd.mobilemall.usermanager.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.tqd.mobilemall.usermanager.client.NotificationClient;
import vn.tqd.mobilemall.usermanager.client.payload.request.SendNotificationRequest;
import vn.tqd.mobilemall.usermanager.dto.request.CreateAccountRequest;
import vn.tqd.mobilemall.usermanager.dto.response.CreateAccountResponse;
import vn.tqd.mobilemall.usermanager.entity.User;
import vn.tqd.mobilemall.usermanager.repository.UserRepository;
import vn.tqd.mobilemall.usermanager.service.UserService;
import vn.tqd.mobilemall.usermanager.repository.UserRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationClient notificationClient;
    private final RabbitTemplate rabbitTemplate;

    @Value("${queue.notification.routing-key}")
    private String routingKey;

    @Value("${queue.notification.queue}")
    private String queueName;

    @Value("${queue.notification.exchange}")
    private String exchangeName;

    @Override
    public CreateAccountResponse createAccount(CreateAccountRequest request) {
        log.info("(createAccount)Create account request: {}", request);
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        User userSaved = userRepository.save(user);
        CreateAccountResponse response = new CreateAccountResponse();
        response.setUsername(userSaved.getUsername());
        response.setEmail(userSaved.getEmail());

        SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
        sendNotificationRequest.setContent("Welcome");
        sendNotificationRequest.setTitle("Welcome email");
        sendNotificationRequest.setSendTo(userSaved.getEmail());
//    notificationClient.sendNotification(sendNotificationRequest);
//        //Fire and forget
        rabbitTemplate.convertAndSend(exchangeName, routingKey, sendNotificationRequest);

        log.info("(createAccount)Send notification request: {}", request);
        log.debug("(createAccount)Send notification request: {}", request);

        //
        //
        return response;
    }
}