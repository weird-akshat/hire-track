package com.hiretrack.message_analyzer.message_analyzer.dto;

import lombok.*;

@Data
@AllArgsConstructor
@ToString
@RequiredArgsConstructor
public class JobNotification {
    private String companyName;
    private String jobRole;
    private long sourceId;
    public String title;
    public String body;
}
