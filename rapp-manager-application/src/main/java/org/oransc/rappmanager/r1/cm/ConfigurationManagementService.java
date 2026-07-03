package org.oransc.rappmanager.r1.cm;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.oransc.rappmanager.models.exception.R1ApiException;
import org.oransc.rappmanager.models.r1.ProblemDetails;
import org.oransc.rappmanager.models.r1.cm.ConfigurationModificationRequest;
import org.oransc.rappmanager.models.r1.cm.ModifyOperator;
import org.oransc.rappmanager.models.r1.cm.MoModification;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ConfigurationManagementService {

    private final Map<String, Map<String, Object>> managedObjects = new ConcurrentHashMap<>();

    public ConfigurationManagementService() {
        seedSampleData();
    }

    public Map<String, Object> readConfiguration(String moiPath) {
        String normalizedPath = normalizeMoiPath(moiPath);
        Map<String, Object> configuration = managedObjects.get(normalizedPath);
        if (configuration == null) {
            throw notFound("Managed object instance not found: " + normalizedPath);
        }
        return deepCopy(configuration);
    }

    public Map<String, Object> writeConfigurationChanges(
            String moiPath, ConfigurationModificationRequest request) {
        String normalizedPath = normalizeMoiPath(moiPath);
        Map<String, Object> configuration = new LinkedHashMap<>(
                managedObjects.getOrDefault(normalizedPath, new LinkedHashMap<>()));

        for (MoModification modification : request.getModifications()) {
            applyModification(configuration, modification);
        }

        managedObjects.put(normalizedPath, configuration);
        log.info("Updated configuration for MOI {}", normalizedPath);
        return deepCopy(configuration);
    }

    private void applyModification(Map<String, Object> configuration, MoModification modification) {
        String attribute = modification.getPath();
        if (attribute.startsWith("/")) {
            attribute = attribute.substring(1);
        }

        switch (modification.getModifyOperator()) {
            case ADD, REPLACE -> configuration.put(attribute, modification.getValue());
            case REMOVE -> configuration.remove(attribute);
            default -> throw badRequest("Unsupported modify operator: " + modification.getModifyOperator());
        }
    }

    private void seedSampleData() {
        Map<String, Object> gnbDu = new LinkedHashMap<>();
        gnbDu.put("administrativeState", "UNLOCKED");
        gnbDu.put("operationalState", "ENABLED");
        gnbDu.put("priorityLabel", 1);
        managedObjects.put("SubNetwork=SN1,GNBDUFunction=1", gnbDu);
    }

    private String normalizeMoiPath(String moiPath) {
        if (moiPath == null || moiPath.isBlank()) {
            throw badRequest("MOI path must not be empty");
        }
        return moiPath.startsWith("/") ? moiPath.substring(1) : moiPath;
    }

    private Map<String, Object> deepCopy(Map<String, Object> source) {
        return new LinkedHashMap<>(source);
    }

    private R1ApiException notFound(String detail) {
        return new R1ApiException(ProblemDetails.builder()
                .title("Not Found")
                .status(404)
                .detail(detail)
                .build());
    }

    private R1ApiException badRequest(String detail) {
        return new R1ApiException(ProblemDetails.builder()
                .title("Bad Request")
                .status(400)
                .detail(detail)
                .build());
    }
}
