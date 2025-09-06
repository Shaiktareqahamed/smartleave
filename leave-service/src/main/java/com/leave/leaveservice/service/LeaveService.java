package com.leave.leaveservice.service;
import com.leave.leaveservice.dto.EmployeeDTO;
import com.leave.leaveservice.dto.LeaveDTO;
import com.leave.leaveservice.model.Leave;
import com.leave.leaveservice.repository.LeaveRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
public class LeaveService {
    @Autowired
    private LeaveRepository leaveRepository;
    @Autowired
    private RestTemplate restTemplate;
    public Leave applyForLeave(LeaveDTO leaveDTO) {
        Leave leave = new Leave();
        leave.setEmployeeId(leaveDTO.getEmployeeId());
        leave.setStartDate(leaveDTO.getStartDate());
        leave.setEndDate(leaveDTO.getEndDate());
        leave.setLeaveType(leaveDTO.getLeaveType());
        leave.setReason(leaveDTO.getReason());
        final String employeeApiUrl = "http://employee-service/api/employees/" + leave.getEmployeeId();
        EmployeeDTO employee = restTemplate.getForObject(employeeApiUrl, EmployeeDTO.class);
        if (employee == null) {
            throw new RuntimeException("Employee not found with ID: " + leave.getEmployeeId());
        }
        boolean isTopLevelManager = (employee.getManagerId() == null || employee.getManagerId().isEmpty());
        long requestedLeaveDays = ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate()) + 1;
        boolean hasSufficientLeaves = employee.getAvailableLeaves() >= requestedLeaveDays;
        if (isTopLevelManager) {
            leave.setStatus(Leave.LeaveStatus.APPROVED);
            if (!hasSufficientLeaves) {
                 leave.setReason(leave.getReason() + " (Approved with insufficient balance - HR review)");
            }
            int newLeaveBalance = employee.getAvailableLeaves() - (int) requestedLeaveDays;
            employee.setAvailableLeaves(newLeaveBalance);
            restTemplate.put(employeeApiUrl, employee);
        } else if (hasSufficientLeaves && employee.isAutoApprovalEnabled()) {
            leave.setStatus(Leave.LeaveStatus.APPROVED);
            int newLeaveBalance = employee.getAvailableLeaves() - (int) requestedLeaveDays;
            employee.setAvailableLeaves(newLeaveBalance);
            restTemplate.put(employeeApiUrl, employee);
        } else {
            leave.setStatus(Leave.LeaveStatus.PENDING);
            if (!hasSufficientLeaves) {
                leave.setReason("Insufficient leave balance. Manager approval required.");
            }
        }
        Leave savedLeave = leaveRepository.save(leave);
        try {
            log.info("Attempting to send notification for leave ID: {}", savedLeave.getId());
            String notificationUrl = "http://notification-service/api/notify";
            restTemplate.postForObject(notificationUrl, savedLeave, Void.class);
            log.info("Successfully sent notification request for leave ID: {}", savedLeave.getId());
        } catch (Exception e) {
            log.error("Could not send notification for leave ID {}. Reason: {}", savedLeave.getId(), e.getMessage());
        }

        return savedLeave;
    }

    public Leave updateLeaveStatus(String leaveId, String newStatus) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found with ID: " + leaveId));
        Leave.LeaveStatus statusToSet = Leave.LeaveStatus.valueOf(newStatus.toUpperCase());
        leave.setStatus(statusToSet);
        if (statusToSet == Leave.LeaveStatus.APPROVED) {
            String employeeApiUrl = "http://employee-service/api/employees/" + leave.getEmployeeId();
            EmployeeDTO employee = restTemplate.getForObject(employeeApiUrl, EmployeeDTO.class);        
            long requestedLeaveDays = ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate()) + 1;  
            int newLeaveBalance = employee.getAvailableLeaves() - (int) requestedLeaveDays;
            employee.setAvailableLeaves(newLeaveBalance);
            restTemplate.put(employeeApiUrl, employee);
        }   
        Leave savedLeave = leaveRepository.save(leave);
        try {
            log.info("Sending final status notification for leave ID: {}", savedLeave.getId());
            String notificationUrl = "http://notification-service/api/notify";
            restTemplate.postForObject(notificationUrl, savedLeave, Void.class);
        } catch (Exception e) {
            log.error("Could not send final notification for leave ID {}. Reason: {}", savedLeave.getId(), e.getMessage());
        }
        return savedLeave;
    }
}