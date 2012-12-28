package org.jenkinsci.backend.recipe;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.kohsuke.stapler.openid.client.OpenIDIdentity;
import org.kohsuke.stapler.openid.client.OpenIdSession;
import org.openid4java.OpenIDException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;

/**
 * @author Kohsuke Kawaguchi
 */
public class Submission {
    public final Application app;
    private String payload;
    public final OpenIdSession openId;

    public Submission(Application app) throws OpenIDException, IOException {
        this.app = app;

        this.openId = new
                OpenIdSession(app.manager,"https://jenkins-ci.org/account/openid/","/openId");
    }

    public String getNick() {
        return openId.authenticate().getNick();
    }

    /**
     * This initiates the protocol. It firsts accepts a submission.
     */
    @RequirePOST
    public void doStart(StaplerRequest req, StaplerResponse rsp) throws IOException, SAXException, ParserConfigurationException {
        // first receive the submission so long as it's not too big
        payload = req.getParameter("payload");

        rsp.setStatus(rsp.SC_SEE_OTHER);
        rsp.setHeader("Location","confirm");
    }

    public HttpResponse doSubmit() throws DocumentException, IOException, GitAPIException, InterruptedException {
        OpenIDIdentity a = openId.authenticate();

        a.getNick();
        a.getEmail();

        // get some information out of this
        Document dom = new SAXReader().read(new StringReader(payload));

        app.gitClient.upload(a,a.getNick(),payload);
        payload = null;

        return HttpResponses.redirectTo("done");
    }

}