package com.leave.notificationservice.dto;

import lombok.Data;

@Data
public class EmployeeDTO {
    private String id;
    private String firstName;
    private String email;
    private String managerId;
}