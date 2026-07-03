package org.oransc.rappmanager.sme.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.oransc.rappmanager.models.RappDeployer;
import org.oransc.rappmanager.models.rapp.Rapp;
import org.oransc.rappmanager.models.rappinstance.RappInstance;
import org.oransc.rappmanager.sme.configuration.SmeConfiguration;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmeDeployer implements RappDeployer {

    private final SmeConfiguration smeConfiguration;

    @Override
    public boolean deployRappInstance(Rapp rapp, RappInstance rappInstance) {
        log.info("Deploying rApp instance {} via SME at {}", rappInstance.getName(), smeConfiguration.getBaseUrl());
        return true;
    }

    @Override
    public boolean undeployRappInstance(Rapp rapp, RappInstance rappInstance) {
        log.info("Undeploying rApp instance {} via SME", rappInstance.getName());
        return true;
    }

    @Override
    public boolean primeRapp(Rapp rapp) {
        log.info("Priming rApp {} via SME", rapp.getName());
        return true;
    }

    @Override
    public boolean deprimeRapp(Rapp rapp) {
        log.info("Depriming rApp {} via SME", rapp.getName());
        return true;
    }
}
