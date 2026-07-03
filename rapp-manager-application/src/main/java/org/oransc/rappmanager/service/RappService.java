package org.oransc.rappmanager.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.oransc.rappmanager.configuration.RappManagerConfiguration;
import org.oransc.rappmanager.models.exception.RappHandlerException;
import org.oransc.rappmanager.models.rapp.Rapp;
import org.oransc.rappmanager.models.rapp.RappState;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RappService {

    private final RappManagerConfiguration configuration;
    private final Map<String, Rapp> rapps = new ConcurrentHashMap<>();

    public List<Rapp> listRapps() {
        return new ArrayList<>(rapps.values());
    }

    public Rapp getRapp(String name) {
        Rapp rapp = rapps.get(name);
        if (rapp == null) {
            throw new RappHandlerException("rApp not found: " + name, 404);
        }
        return rapp;
    }

    public Rapp createRapp(Rapp rapp) {
        if (rapps.containsKey(rapp.getName())) {
            throw new RappHandlerException("rApp already exists: " + rapp.getName(), 409);
        }
        rapp.setState(RappState.CREATED);
        rapps.put(rapp.getName(), rapp);
        return rapp;
    }

    public void deleteRapp(String name) {
        if (rapps.remove(name) == null) {
            throw new RappHandlerException("rApp not found: " + name, 404);
        }
    }

    public String getCsarLocation() {
        return configuration.getCsarLocation();
    }
}
