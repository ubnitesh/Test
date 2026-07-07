package org.oransc.rappmanager.models.dme;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CellKpiTelemetry {

    private String cellId;
    private Double rsrp;
    private Double rsrq;
    private Integer activeUsers;
    private Double prbUtilization;
    private Instant recordedAt;
}
