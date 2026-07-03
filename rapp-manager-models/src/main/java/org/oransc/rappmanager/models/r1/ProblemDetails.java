package org.oransc.rappmanager.models.r1;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProblemDetails {

    private String type;
    private String title;
    private Integer status;
    private String detail;
    private String instance;
}
