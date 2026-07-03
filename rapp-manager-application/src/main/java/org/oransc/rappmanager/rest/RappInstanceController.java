package org.oransc.rappmanager.rest;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.oransc.rappmanager.models.rappinstance.RappInstance;
import org.oransc.rappmanager.service.RappInstanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rapps/{rappName}/instances")
@RequiredArgsConstructor
public class RappInstanceController {

    private final RappInstanceService rappInstanceService;

    @GetMapping
    public List<RappInstance> listInstances(@PathVariable String rappName) {
        return rappInstanceService.listInstances(rappName);
    }

    @GetMapping("/{instanceName}")
    public RappInstance getInstance(@PathVariable String rappName, @PathVariable String instanceName) {
        return rappInstanceService.getInstance(rappName, instanceName);
    }

    @PostMapping
    public ResponseEntity<RappInstance> createInstance(
            @PathVariable String rappName,
            @Valid @RequestBody RappInstance instance) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(rappInstanceService.createInstance(rappName, instance));
    }

    @DeleteMapping("/{instanceName}")
    public ResponseEntity<Void> deleteInstance(
            @PathVariable String rappName,
            @PathVariable String instanceName) {
        rappInstanceService.deleteInstance(rappName, instanceName);
        return ResponseEntity.noContent().build();
    }
}
