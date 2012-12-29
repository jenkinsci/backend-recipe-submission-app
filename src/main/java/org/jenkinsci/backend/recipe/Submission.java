package org.jenkinsci.backend.recipe;

import org.dom4j.DocumentException;
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

import static javax.servlet.http.HttpServletResponse.*;

/**
 * Session-scoped object that handles the recipe file submission from users.
 *
 * @author Kohsuke Kawaguchi
 */
public class Submission {
    public final Application app;
    private Recipe recipe;
    public final OpenIdSession openId;

    public Submission(Application app) throws OpenIDException, IOException {
        this.app = app;

        this.openId = new OpenIdSession(app.manager,"https://jenkins-ci.org/account/openid/","/openId");
    }

    /**
     * Once authenticated, return the user's jenkins-ci.org account ID.
     */
    public String getNick() {
        return openId.authenticate().getNick();
    }

    public Recipe getRecipe() {
        return recipe;
    }

    /**
     * This initiates the protocol. It firsts accepts a submission.
     */
    @RequirePOST
    public void doStart(StaplerRequest req, StaplerResponse rsp) throws IOException, SAXException, ParserConfigurationException, DocumentException {
        // first receive the submission so long as it's not too big
        String payload = req.getParameter("payload");

        // get some information out of this
        recipe = new Recipe(payload);

        rsp.sendRedirect(SC_SEE_OTHER, "confirm");
    }

    public HttpResponse doSubmit() throws DocumentException, IOException, GitAPIException, InterruptedException {
        OpenIDIdentity a = openId.authenticate();

        // set the author
        recipe.setAuthor(getFullName(a));

        app.gitClient.upload(a, recipe.getFileName(), recipe.toString());
        recipe = null;

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
