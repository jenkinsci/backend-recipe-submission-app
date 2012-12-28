package org.jenkinsci.backend.recipe;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
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
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * @author Kohsuke Kawaguchi
 */
public class Submission {
    public final Application app;
    private Document dom;
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
    public void doStart(StaplerRequest req, StaplerResponse rsp) throws IOException, SAXException, ParserConfigurationException, DocumentException {
        // first receive the submission so long as it's not too big
        String payload = req.getParameter("payload");

        // get some information out of this
        dom = new SAXReader().read(new StringReader(payload));

        rsp.setStatus(rsp.SC_SEE_OTHER);
        rsp.setHeader("Location","confirm");
    }

    public HttpResponse doSubmit() throws DocumentException, IOException, GitAPIException, InterruptedException {
        OpenIDIdentity a = openId.authenticate();

        // set the author
        Element author = dom.getRootElement().element("author");
        if (author==null)
            dom.getRootElement().addElement("author");
        author.setText(getFullName(a));

        StringWriter sw = new StringWriter();
        new XMLWriter(sw).write(dom);

        app.gitClient.upload(a,a.getNick(),sw.toString());
        dom = null;

        return HttpResponses.redirectTo("done");
    }

    private String getFullName(OpenIDIdentity a) {
        String fullName;
        if (a.getLastName()!=null && a.getFirstName()!=null)
            fullName = a.getFirstName()+' '+a.getLastName();
        else
            fullName = a.getNick();
        return fullName;
    }

}
