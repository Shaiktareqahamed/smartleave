package com.leave.aianalyzerservice.controller;

import com.leave.aianalyzerservice.dto.AISuggestionDTO;
import com.leave.aianalyzerservice.dto.LeaveDTO;
import com.leave.aianalyzerservice.service.AIAnalyzerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Autowired
    private AIAnalyzerService aiAnalyzerService;

    @PostMapping("/suggestion")
    public AISuggestionDTO getSuggestion(@RequestBody LeaveDTO leaveRequest) {
        return aiAnalyzerService.getSuggestion(leaveRequest);
    }
}
