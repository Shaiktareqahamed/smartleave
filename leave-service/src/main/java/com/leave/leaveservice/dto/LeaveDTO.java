package com.leave.leaveservice.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class LeaveDTO {
    private String employeeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String leaveType;
    private String reason;
}