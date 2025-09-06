
package com.leave.dashboardservice.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class LeaveDTO {
    private String id;
    private String employeeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String leaveType;
    private String reason;
    private String status;
    private String employeeName;
    private String aiSuggestion;
}