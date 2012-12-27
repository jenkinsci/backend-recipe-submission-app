package org.jenkinsci.backend.recipe;

import org.kohsuke.stapler.config.ConfigurationLoader;
import org.kohsuke.stapler.framework.AbstractWebAppMain;

import java.io.File;

/**
 * @author Kohsuke Kawaguchi
 */
public class WebAppMain extends AbstractWebAppMain<Application> {
    public WebAppMain() {
        super(Application.class);
    }

    @Override
    protected String getApplicationName() {
        return "APP";
    }

    @Override
    protected Object createApplication() throws Exception {
        File file = new File("config.properties");
        ConfigurationLoader config;
        if (file.exists())      config = ConfigurationLoader.from(file);
        else                    config = ConfigurationLoader.fromSystemProperties();

        return new Application(context, config.as(Parameters.class));
    }
}
