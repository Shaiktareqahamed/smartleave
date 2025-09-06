package com.leave.employeeservice.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "employees")
@NoArgsConstructor
@Data
public class Employee {

    @Id
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String department;
    private int availableLeaves;
    private boolean isAutoApprovalEnabled = true;
    private String managerId;
}