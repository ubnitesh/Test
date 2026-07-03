package org.oransc.rappmanager.rest;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.oransc.rappmanager.models.rapp.Rapp;
import org.oransc.rappmanager.service.RappService;
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
@RequestMapping("/rapps")
@RequiredArgsConstructor
public class RappController {

    private final RappService rappService;

    @GetMapping
    public List<Rapp> listRapps() {
        return rappService.listRapps();
    }

    @GetMapping("/{name}")
    public Rapp getRapp(@PathVariable String name) {
        return rappService.getRapp(name);
    }

    @PostMapping
    public ResponseEntity<Rapp> createRapp(@Valid @RequestBody Rapp rapp) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rappService.createRapp(rapp));
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> deleteRapp(@PathVariable String name) {
        rappService.deleteRapp(name);
        return ResponseEntity.noContent().build();
    }
}
