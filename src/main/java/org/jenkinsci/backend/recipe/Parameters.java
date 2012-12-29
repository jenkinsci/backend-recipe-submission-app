package org.jenkinsci.backend.recipe;

import org.kohsuke.stapler.config.Configuration;

import java.io.File;

/**
 * @author Kohsuke Kawaguchi
 */
public interface Parameters {
    /**
     * Git repository URL.
     */
    String repo();

    /**
     * Local workspace.
     */
    File ws();

    @Configuration(defaultValue="inbound")
    String branch();

    /**
     * Passphrase for SSH private key to push into the recipe repository.
     */
    String passphrase();

    /**
     * SSH private key. null to use the one baked in this app.
     */
    File privateKey();
    /**
     * SSH public key.  null to use the one baked in this app.
     */
    File publicKey();
}
