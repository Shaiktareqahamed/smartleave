package com.leave.notificationservice.service;

import com.leave.notificationservice.dto.EmployeeDTO;
import com.leave.notificationservice.dto.LeaveEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendLeaveStatusEmail(LeaveEvent leaveEvent) {
        String employeeApiUrl = "http://employee-service/api/employees/" + leaveEvent.getEmployeeId();

        try {
            EmployeeDTO employee = restTemplate.getForObject(employeeApiUrl, EmployeeDTO.class);
            if (employee == null || employee.getEmail() == null) {
                log.error("Could not find employee details for ID: {}", leaveEvent.getEmployeeId());
                return;
            }
            sendEmailToEmployee(employee, leaveEvent);
            if (employee.getManagerId() != null && !employee.getManagerId().isEmpty()) {
                String managerApiUrl = "http://employee-service/api/employees/" + employee.getManagerId();
                EmployeeDTO manager = restTemplate.getForObject(managerApiUrl, EmployeeDTO.class);
                if (manager != null && manager.getEmail() != null) {
                    sendEmailToManager(manager, employee, leaveEvent);
                } else {
                    log.warn("Could not find manager details for manager ID: {}", employee.getManagerId());
                }
            }
        } catch (Exception e) {
            log.error("Failed to process notifications for employee {}: {}", leaveEvent.getEmployeeId(), e.getMessage());
        }
    }

    private void sendEmailToEmployee(EmployeeDTO employee, LeaveEvent leaveEvent) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(employee.getEmail());
        message.setSubject("Your Leave Request Status: " + leaveEvent.getStatus());
        String body = String.format(
            "Dear %s,\n\nYour leave request from %s to %s has been %s.\n\nReason: %s",
            employee.getFirstName(),
            leaveEvent.getStartDate(),
            leaveEvent.getEndDate(),
            leaveEvent.getStatus(),
            leaveEvent.getReason()
        );
        message.setText(body);
        mailSender.send(message);
        log.info("Leave status email sent successfully to employee: {}", employee.getEmail());
    }

    private void sendEmailToManager(EmployeeDTO manager, EmployeeDTO employee, LeaveEvent leaveEvent) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(manager.getEmail());
        message.setSubject(String.format("Leave Request Update for %s", employee.getFirstName()));
        String body = String.format(
            "Dear %s,\n\nThe leave request for your employee, %s, has been %s.\n\nDetails:\n- Start Date: %s\n- End Date: %s\n- Reason: %s",
            manager.getFirstName(),
            employee.getFirstName(),
            leaveEvent.getStatus(),
            leaveEvent.getStartDate(),
            leaveEvent.getEndDate(),
            leaveEvent.getReason()
        );
        message.setText(body);
        mailSender.send(message);
        log.info("Leave status email sent successfully to manager: {}", manager.getEmail());
    }
}