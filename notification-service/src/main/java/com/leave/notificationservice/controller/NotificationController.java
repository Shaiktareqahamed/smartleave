package com.leave.notificationservice.controller;

import com.leave.notificationservice.dto.LeaveEvent;
import com.leave.notificationservice.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notify")
@Slf4j
public class NotificationController {

    @Autowired
    private EmailService emailService;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public void sendLeaveNotification(@RequestBody LeaveEvent leaveEvent) {
        log.info("Received API request to send notification for leave event: {}", leaveEvent);
        emailService.sendLeaveStatusEmail(leaveEvent);
    }
}