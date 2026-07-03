package org.oransc.rappmanager.dme.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.oransc.rappmanager.dme.configuration.DmeConfiguration;
import org.oransc.rappmanager.models.RappDeployer;
import org.oransc.rappmanager.models.rapp.Rapp;
import org.oransc.rappmanager.models.rappinstance.RappInstance;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DmeDeployer implements RappDeployer {

    private final DmeConfiguration dmeConfiguration;

    @Override
    public boolean deployRappInstance(Rapp rapp, RappInstance rappInstance) {
        log.info("Deploying rApp instance {} via DME at {}", rappInstance.getName(), dmeConfiguration.getBaseUrl());
        return true;
    }

    @Override
    public boolean undeployRappInstance(Rapp rapp, RappInstance rappInstance) {
        log.info("Undeploying rApp instance {} via DME", rappInstance.getName());
        return true;
    }

    @Override
    public boolean primeRapp(Rapp rapp) {
        log.info("Priming rApp {} via DME", rapp.getName());
        return true;
    }

    @Override
    public boolean deprimeRapp(Rapp rapp) {
        log.info("Depriming rApp {} via DME", rapp.getName());
        return true;
    }
}
