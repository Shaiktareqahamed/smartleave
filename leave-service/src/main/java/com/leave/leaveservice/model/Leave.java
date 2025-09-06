package com.leave.leaveservice.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;

@Document(collection = "leaves")
@Data
public class Leave {
    @Id
    private String id;
    private String employeeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String leaveType;
    private String reason;
    private LeaveStatus status = LeaveStatus.PENDING;

    public enum LeaveStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}