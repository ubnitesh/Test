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
public class NetworkKpiSummary {

    private Instant recordedAt;
    private int cellCount;
    private Double weightedRsrp;
    private Double weightedRsrq;
    private Double weightedActiveUsers;
    private Double weightedPrbUtilization;
}
