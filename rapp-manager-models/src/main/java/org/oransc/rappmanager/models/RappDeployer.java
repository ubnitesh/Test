package org.oransc.rappmanager.models;

import org.oransc.rappmanager.models.rapp.Rapp;
import org.oransc.rappmanager.models.rappinstance.RappInstance;

/**
 * Deployer contract implemented by ACM, DME, and SME integration modules.
 */
public interface RappDeployer {

    boolean deployRappInstance(Rapp rapp, RappInstance rappInstance);

    boolean undeployRappInstance(Rapp rapp, RappInstance rappInstance);

    boolean primeRapp(Rapp rapp);

    boolean deprimeRapp(Rapp rapp);
}
