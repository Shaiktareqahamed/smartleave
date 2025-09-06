package com.leave.employeeservice.repository; // Changed here

import com.leave.employeeservice.model.Employee; // Changed here
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeRepository extends MongoRepository<Employee, String> {
}