package com.leave.aianalyzerservice.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class LeaveDTO {
    private String id;
    private String employeeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String employeeName;
}