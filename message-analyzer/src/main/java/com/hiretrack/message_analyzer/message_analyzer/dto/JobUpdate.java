package com.hiretrack.message_analyzer.message_analyzer.dto;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class JobUpdate {
    private long sourceId;
    private String companyName;
    private String jobRole;
}
