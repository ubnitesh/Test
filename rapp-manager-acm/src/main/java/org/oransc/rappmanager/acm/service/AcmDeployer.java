package org.oransc.rappmanager.acm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.oransc.rappmanager.acm.configuration.AcmConfiguration;
import org.oransc.rappmanager.models.RappDeployer;
import org.oransc.rappmanager.models.rapp.Rapp;
import org.oransc.rappmanager.models.rappinstance.RappInstance;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcmDeployer implements RappDeployer {

    private final AcmConfiguration acmConfiguration;

    @Override
    public boolean deployRappInstance(Rapp rapp, RappInstance rappInstance) {
        log.info("Deploying rApp instance {} via ACM at {}", rappInstance.getName(), acmConfiguration.getBaseUrl());
        return true;
    }

    @Override
    public boolean undeployRappInstance(Rapp rapp, RappInstance rappInstance) {
        log.info("Undeploying rApp instance {} via ACM", rappInstance.getName());
        return true;
    }

    @Override
    public boolean primeRapp(Rapp rapp) {
        log.info("Priming rApp {} via ACM", rapp.getName());
        return true;
    }

    @Override
    public boolean deprimeRapp(Rapp rapp) {
        log.info("Depriming rApp {} via ACM", rapp.getName());
        return true;
    }
}
