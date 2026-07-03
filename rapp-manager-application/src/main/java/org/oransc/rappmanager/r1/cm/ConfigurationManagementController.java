package org.oransc.rappmanager.r1.cm;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.oransc.rappmanager.models.r1.cm.ConfigurationModificationRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * O-RAN R1 Configuration Management service (ETSI TS 104 231 clause 8.1).
 * Base path: {@code {apiRoot}/ran-oam-cm/v1}
 */
@RestController
@RequestMapping("/ran-oam-cm/v1")
@RequiredArgsConstructor
@Tag(name = "R1 Configuration Management")
public class ConfigurationManagementController {

    private final ConfigurationManagementService configurationManagementService;

    @GetMapping("/{*moiPath}")
    @Operation(summary = "Read configuration data", description = "Read MOI configuration (R1 CM GET)")
    public ResponseEntity<Map<String, Object>> readConfiguration(@PathVariable String moiPath) {
        return ResponseEntity.ok(configurationManagementService.readConfiguration(moiPath));
    }

    @PatchMapping("/{*moiPath}")
    @Operation(summary = "Write configuration changes", description = "Apply MOI configuration changes (R1 CM PATCH)")
    public ResponseEntity<Map<String, Object>> writeConfigurationChanges(
            @PathVariable String moiPath,
            @Valid @RequestBody ConfigurationModificationRequest request) {
        return ResponseEntity.ok(configurationManagementService.writeConfigurationChanges(moiPath, request));
    }
}
