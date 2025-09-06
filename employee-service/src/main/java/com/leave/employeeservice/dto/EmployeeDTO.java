package com.leave.employeeservice.dto;

import lombok.Data;

@Data
public class EmployeeDTO {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String department;
    private int availableLeaves;
    private boolean isAutoApprovalEnabled;
    private String managerId;
}
