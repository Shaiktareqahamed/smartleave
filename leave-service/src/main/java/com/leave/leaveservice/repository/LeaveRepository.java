package com.leave.leaveservice.repository;

import com.leave.leaveservice.model.Leave;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveRepository extends MongoRepository<Leave, String> {

    List<Leave> findByEmployeeId(String employeeId);

}