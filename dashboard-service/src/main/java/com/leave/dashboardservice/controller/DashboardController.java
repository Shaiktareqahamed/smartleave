package com.leave.dashboardservice.controller;
import com.leave.dashboardservice.dto.AISuggestionDTO;
import com.leave.dashboardservice.dto.EmployeeDTO;
import com.leave.dashboardservice.dto.LeaveDTO;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@Slf4j
public class DashboardController {

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/")
    public String showLoginPage(HttpSession session) {
        session.invalidate();
        return "login";
    }

    @PostMapping("/login")
    public String handleLogin(@RequestParam String employeeId, HttpSession session, Model model) {
        String employeeApiUrl = "http://employee-service/api/employees/" + employeeId;
        try {
            ResponseEntity<EmployeeDTO> response = restTemplate.getForEntity(employeeApiUrl, EmployeeDTO.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                session.setAttribute("loggedInUserId", employeeId);
                session.setAttribute("loggedInUserFirstName", response.getBody().getFirstName());
                return "redirect:/dashboard";
            }
        } catch (HttpClientErrorException.NotFound e) {
            model.addAttribute("error", "Invalid Employee ID. Please try again.");
            return "login";
        } catch (Exception e) {
            model.addAttribute("error", "An error occurred. Please try again later.");
            return "login";
        }
        model.addAttribute("error", "Invalid Employee ID. Please try again.");
        return "login";
    }

    @GetMapping("/dashboard")
    public String showDashboard(HttpSession session, Model model) {
        String employeeId = (String) session.getAttribute("loggedInUserId");
        if (employeeId == null) {
            return "redirect:/";
        }

        String employeeApiUrl = "http://employee-service/api/employees/" + employeeId;
        EmployeeDTO employee = restTemplate.getForObject(employeeApiUrl, EmployeeDTO.class);
        model.addAttribute("employee", employee);

        String allEmployeesUrl = "http://employee-service/api/employees";
        ResponseEntity<List<EmployeeDTO>> employeesResponse = restTemplate.exchange(
                allEmployeesUrl, HttpMethod.GET, null, new ParameterizedTypeReference<List<EmployeeDTO>>() {});
        List<EmployeeDTO> allEmployees = employeesResponse.getBody();

        boolean isManager = allEmployees.stream().anyMatch(e -> employeeId.equals(e.getManagerId()));
        model.addAttribute("isManager", isManager);
        
        if (isManager) {
            List<EmployeeDTO> teamMembers = allEmployees.stream()
                .filter(e -> employeeId.equals(e.getManagerId()))
                .collect(Collectors.toList());
            model.addAttribute("teamMembers", teamMembers);

            String allLeavesUrl = "http://leave-service/api/leaves";
             ResponseEntity<List<LeaveDTO>> allLeavesResponse = restTemplate.exchange(
                allLeavesUrl, HttpMethod.GET, null, new ParameterizedTypeReference<List<LeaveDTO>>() {});
            List<LeaveDTO> allLeaves = allLeavesResponse.getBody() != null ? allLeavesResponse.getBody() : Collections.emptyList();

            List<String> teamMemberIds = teamMembers.stream().map(EmployeeDTO::getId).collect(Collectors.toList());
            List<LeaveDTO> teamLeaves = allLeaves.stream()
                .filter(l -> teamMemberIds.contains(l.getEmployeeId()))
                .collect(Collectors.toList());

            LocalDate today = LocalDate.now();
            List<LeaveDTO> onLeaveToday = teamLeaves.stream()
                .filter(l -> "APPROVED".equals(l.getStatus()) && !today.isBefore(l.getStartDate()) && !today.isAfter(l.getEndDate()))
                .collect(Collectors.toList());
            
            onLeaveToday.forEach(leave -> {
                teamMembers.stream()
                    .filter(tm -> tm.getId().equals(leave.getEmployeeId()))
                    .findFirst()
                    .ifPresent(tm -> leave.setEmployeeName(tm.getFirstName() + " " + tm.getLastName()));
            });

            List<LeaveDTO> pendingRequests = teamLeaves.stream()
                .filter(l -> "PENDING".equals(l.getStatus()))
                .collect(Collectors.toList());
             pendingRequests.forEach(leave -> {
                teamMembers.stream()
                    .filter(tm -> tm.getId().equals(leave.getEmployeeId()))
                    .findFirst()
                    .ifPresent(tm -> leave.setEmployeeName(tm.getFirstName() + " " + tm.getLastName()));
            });
            
            String aiSuggestionUrl = "http://ai-analyzer-service/api/ai/suggestion";
            pendingRequests.forEach(req -> {
                try {
                    AISuggestionDTO suggestion = restTemplate.postForObject(aiSuggestionUrl, req, AISuggestionDTO.class);
                    if (suggestion != null) {
                        req.setAiSuggestion(suggestion.getSuggestion() + " (" + suggestion.getReason() + ")");
                    }
                } catch (Exception e) {
                    req.setAiSuggestion("Could not get suggestion.");
                    log.error("AI Service Error: {}", e.getMessage());
                }
            });
            
            String leaveHistoryUrl = "http://leave-service/api/leaves/employee/" + employeeId;
            try {
                ResponseEntity<List<LeaveDTO>> leavesResponse = restTemplate.exchange(
                    leaveHistoryUrl, HttpMethod.GET, null, new ParameterizedTypeReference<List<LeaveDTO>>() {});
                model.addAttribute("leaves", leavesResponse.getBody());
            } catch (Exception e) {
                log.error("Could not fetch manager's own leave history for employee {}: {}", employeeId, e.getMessage());
                model.addAttribute("leaves", Collections.emptyList());
            }

            model.addAttribute("onLeaveToday", onLeaveToday);
            model.addAttribute("pendingRequests", pendingRequests);

            return "manager-dashboard";
        } else {
            // EMPLOYEE LOGIC
            String leaveHistoryUrl = "http://leave-service/api/leaves/employee/" + employeeId;
            try {
                ResponseEntity<List<LeaveDTO>> leavesResponse = restTemplate.exchange(
                    leaveHistoryUrl, HttpMethod.GET, null, new ParameterizedTypeReference<List<LeaveDTO>>() {});
                model.addAttribute("leaves", leavesResponse.getBody());
            } catch (Exception e) {
                log.error("Could not fetch leave history for employee {}: {}", employeeId, e.getMessage());
                model.addAttribute("leaves", Collections.emptyList());
            }
            return "employee-dashboard";
        }
    }
    
    @GetMapping("/add-user")
    public String showAddUserForm(Model model) {
        String allEmployeesUrl = "http://employee-service/api/employees";
        try {
            ResponseEntity<List<EmployeeDTO>> employeesResponse = restTemplate.exchange(
                    allEmployeesUrl, HttpMethod.GET, null, new ParameterizedTypeReference<List<EmployeeDTO>>() {});
            model.addAttribute("allEmployees", employeesResponse.getBody());
        } catch (Exception e) {
            model.addAttribute("allEmployees", Collections.emptyList());
        }
        return "add-user";
    }

    @PostMapping("/add-user")
    public String handleAddUser(@RequestParam String firstName, @RequestParam String lastName,
                                @RequestParam String email, @RequestParam String department,
                                @RequestParam int availableLeaves, @RequestParam(required = false) String managerId,
                                RedirectAttributes redirectAttributes) {
        
        EmployeeDTO newEmployee = new EmployeeDTO();
        newEmployee.setFirstName(firstName);
        newEmployee.setLastName(lastName);
        newEmployee.setEmail(email);
        newEmployee.setDepartment(department);
        newEmployee.setAvailableLeaves(availableLeaves);
        if (managerId != null && !managerId.isEmpty()) {
            newEmployee.setManagerId(managerId);
        }

        String createEmployeeUrl = "http://employee-service/api/employees";
        try {
            ResponseEntity<EmployeeDTO> response = restTemplate.postForEntity(createEmployeeUrl, newEmployee, EmployeeDTO.class);
            if(response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                String newId = response.getBody().getId();
                redirectAttributes.addFlashAttribute("successMessage", "User '" + firstName + "' created! New Employee ID: " + newId);
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating employee: " + e.getMessage());
        }
        
        return "redirect:/";
    }
    
    @PostMapping("/applyLeave")
    public String applyForLeave(@RequestParam LocalDate startDate,
                                @RequestParam LocalDate endDate,
                                @RequestParam String leaveType,
                                @RequestParam String reason,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        
        String employeeId = (String) session.getAttribute("loggedInUserId");
        if (employeeId == null) {
            return "redirect:/";
        }

        LeaveDTO newLeaveRequest = new LeaveDTO();
        newLeaveRequest.setEmployeeId(employeeId);
        newLeaveRequest.setStartDate(startDate);
        newLeaveRequest.setEndDate(endDate);
        newLeaveRequest.setLeaveType(leaveType);
        newLeaveRequest.setReason(reason);

        String applyLeaveUrl = "http://leave-service/api/leaves";
        restTemplate.postForObject(applyLeaveUrl, newLeaveRequest, LeaveDTO.class);

        redirectAttributes.addFlashAttribute("successMessage", "Your leave request was submitted successfully!");
        return "redirect:/dashboard";
    }
    
    @PostMapping("/update-leave-status")
    public String updateLeaveStatus(@RequestParam String leaveId, @RequestParam String status, RedirectAttributes redirectAttributes) {
        String updateUrl = "http://leave-service/api/leaves/" + leaveId + "/status";
        restTemplate.postForObject(updateUrl, Map.of("status", status), Void.class);
        redirectAttributes.addFlashAttribute("successMessage", "Leave request has been " + status.toLowerCase() + ".");
        return "redirect:/dashboard";
    }

    @PostMapping("/toggle-auto-approval")
    public String toggleAutoApproval(@RequestParam String employeeId, RedirectAttributes redirectAttributes) {
        String toggleUrl = "http://employee-service/api/employees/" + employeeId + "/toggle-auto-approval";
        restTemplate.postForObject(toggleUrl, null, Void.class);
        redirectAttributes.addFlashAttribute("successMessage", "Auto-approval setting updated.");
        return "redirect:/dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}