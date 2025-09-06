package com.leave.aianalyzerservice.service;

import com.leave.aianalyzerservice.dto.AISuggestionDTO;
import com.leave.aianalyzerservice.dto.EmployeeDTO;
import com.leave.aianalyzerservice.dto.LeaveDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AIAnalyzerService {

    @Autowired
    private RestTemplate restTemplate;

    public AISuggestionDTO getSuggestion(LeaveDTO leaveRequest) {
        // Step 1: Get all employees to find the team
        String allEmployeesUrl = "http://employee-service/api/employees";
        ResponseEntity<List<EmployeeDTO>> employeesResponse = restTemplate.exchange(
                allEmployeesUrl, HttpMethod.GET, null, new ParameterizedTypeReference<List<EmployeeDTO>>() {});
        List<EmployeeDTO> allEmployees = employeesResponse.getBody() != null ? employeesResponse.getBody() : Collections.emptyList();

        // Find the manager of the employee requesting leave
        String managerId = allEmployees.stream()
                .filter(e -> e.getId().equals(leaveRequest.getEmployeeId()))
                .map(EmployeeDTO::getManagerId)
                .findFirst().orElse(null);

        if (managerId == null) {
            return new AISuggestionDTO("Approve", "Employee has no manager, auto-approved.");
        }

        // Find all members of that manager's team (excluding the person requesting leave)
        List<String> teamMemberIds = allEmployees.stream()
                .filter(e -> managerId.equals(e.getManagerId()) && !e.getId().equals(leaveRequest.getEmployeeId()))
                .map(EmployeeDTO::getId)
                .collect(Collectors.toList());

        // Step 2: Get all approved leaves from the leave-service
        String allLeavesUrl = "http://leave-service/api/leaves";
        ResponseEntity<List<LeaveDTO>> allLeavesResponse = restTemplate.exchange(
            allLeavesUrl, HttpMethod.GET, null, new ParameterizedTypeReference<List<LeaveDTO>>() {});
        List<LeaveDTO> allLeaves = allLeavesResponse.getBody() != null ? allLeavesResponse.getBody() : Collections.emptyList();

        // Step 3: Calculate the "Team Overlap" feature
        long overlappingLeaves = allLeaves.stream()
                .filter(leave -> "APPROVED".equals(leave.getStatus()) && teamMemberIds.contains(leave.getEmployeeId())) // Is it a team member's approved leave?
                .filter(leave -> datesOverlap(leave.getStartDate(), leave.getEndDate(), leaveRequest.getStartDate(), leaveRequest.getEndDate())) // Do the dates overlap?
                .count();

        // Step 4: Apply the simple "Decision Tree" model logic
        if (overlappingLeaves >= 2) {
            return new AISuggestionDTO("Reject", "High team overlap. " + overlappingLeaves + " other members are on leave.");
        } else {
            return new AISuggestionDTO("Approve", "Team availability is good. Only " + overlappingLeaves + " other member on leave.");
        }
    }

    private boolean datesOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return !start1.isAfter(end2) && !start2.isAfter(end1);
    }
}
