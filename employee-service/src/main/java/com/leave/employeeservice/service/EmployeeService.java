package com.leave.employeeservice.service;

import com.leave.employeeservice.model.Employee;
import com.leave.employeeservice.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    public Employee createEmployee(Employee employee) {
        return employeeRepository.save(employee);
    }

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public Optional<Employee> getEmployeeById(String id) {
        return employeeRepository.findById(id);
    }

    public Optional<Employee> updateEmployee(String id, Employee employeeDetails) {
        return employeeRepository.findById(id).map(employee -> {
            employee.setFirstName(employeeDetails.getFirstName());
            employee.setLastName(employeeDetails.getLastName());
            employee.setEmail(employeeDetails.getEmail());
            employee.setDepartment(employeeDetails.getDepartment());
            employee.setAvailableLeaves(employeeDetails.getAvailableLeaves());
            employee.setAutoApprovalEnabled(employeeDetails.isAutoApprovalEnabled());
            employee.setManagerId(employeeDetails.getManagerId());
            return employeeRepository.save(employee);
        });
    }

    public Optional<Employee> toggleAutoApproval(String id) {
        return employeeRepository.findById(id).map(employee -> {
            employee.setAutoApprovalEnabled(!employee.isAutoApprovalEnabled());
            return employeeRepository.save(employee);
        });
    }
}