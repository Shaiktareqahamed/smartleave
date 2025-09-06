package com.leave.notificationservice.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class LeaveEvent {
    private String id;
    private String employeeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String reason;
}