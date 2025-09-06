package com.leave.leaveservice.controller;
import com.leave.leaveservice.dto.LeaveDTO;
import com.leave.leaveservice.dto.UpdateLeaveStatusDTO;
import com.leave.leaveservice.model.Leave;
import com.leave.leaveservice.repository.LeaveRepository;
import com.leave.leaveservice.service.LeaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @Autowired
    private LeaveRepository leaveRepository;

    @PostMapping
    public ResponseEntity<Leave> applyForLeave(@RequestBody LeaveDTO leaveDTO) {
        Leave createdLeave = leaveService.applyForLeave(leaveDTO);
        return new ResponseEntity<>(createdLeave, HttpStatus.CREATED);
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<Leave>> getLeavesByEmployeeId(@PathVariable("employeeId") String employeeId) {
        return new ResponseEntity<>(leaveRepository.findByEmployeeId(employeeId), HttpStatus.OK);
    }
    @GetMapping
    public ResponseEntity<List<Leave>> getAllLeaves() {
        return new ResponseEntity<>(leaveRepository.findAll(), HttpStatus.OK);
    }

    @PostMapping("/{leaveId}/status")
    public ResponseEntity<Leave> updateLeaveStatus(@PathVariable("leaveId") String leaveId, @RequestBody UpdateLeaveStatusDTO statusDTO) {
        Leave updatedLeave = leaveService.updateLeaveStatus(leaveId, statusDTO.getStatus());
        return new ResponseEntity<>(updatedLeave, HttpStatus.OK);
    }
}