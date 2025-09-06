package com.leave.aianalyzerservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AISuggestionDTO {
    private String suggestion; 
    private String reason;
}