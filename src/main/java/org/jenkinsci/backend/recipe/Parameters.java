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
}
