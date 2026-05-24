package com.smartqueue.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentRequest {
    private Long slotId;
    private String sectorType;
    private Long referenceId;
    private String serviceName;
}
