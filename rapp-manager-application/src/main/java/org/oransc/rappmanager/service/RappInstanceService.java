package org.oransc.rappmanager.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.oransc.rappmanager.models.exception.RappHandlerException;
import org.oransc.rappmanager.models.rappinstance.RappInstance;
import org.oransc.rappmanager.models.rappinstance.RappInstanceState;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RappInstanceService {

    private final RappService rappService;
    private final Map<String, RappInstance> instances = new ConcurrentHashMap<>();

    public List<RappInstance> listInstances(String rappName) {
        rappService.getRapp(rappName);
        return instances.values().stream()
                .filter(instance -> rappName.equals(instance.getRappName()))
                .toList();
    }

    public RappInstance getInstance(String rappName, String instanceName) {
        rappService.getRapp(rappName);
        RappInstance instance = instances.get(instanceKey(rappName, instanceName));
        if (instance == null) {
            throw new RappHandlerException("rApp instance not found: " + instanceName, 404);
        }
        return instance;
    }

    public RappInstance createInstance(String rappName, RappInstance instance) {
        rappService.getRapp(rappName);
        String key = instanceKey(rappName, instance.getName());
        if (instances.containsKey(key)) {
            throw new RappHandlerException("rApp instance already exists: " + instance.getName(), 409);
        }
        instance.setRappName(rappName);
        instance.setState(RappInstanceState.CREATED);
        instances.put(key, instance);
        return instance;
    }

    public void deleteInstance(String rappName, String instanceName) {
        if (instances.remove(instanceKey(rappName, instanceName)) == null) {
            throw new RappHandlerException("rApp instance not found: " + instanceName, 404);
        }
    }

    private String instanceKey(String rappName, String instanceName) {
        return rappName + ":" + instanceName;
    }
}
