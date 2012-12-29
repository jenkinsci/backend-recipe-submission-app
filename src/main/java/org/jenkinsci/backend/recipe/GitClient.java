package org.jenkinsci.backend.recipe;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.kohsuke.stapler.openid.client.OpenIDIdentity;

import java.io.File;
import java.io.IOException;

/**
 * Encapsulates the way we access our own git repository.
 *
 * <p>
 * The only operation we expose is to overwrite/add a file and commit to the upstream repository.
 *
 * @author Kohsuke Kawaguchi
 */
public class GitClient {
    private final Git git;
    public final File ws;
    private final Parameters params;

    public GitClient(Git git, Parameters params) {
        this.git = git;
        this.params = params;
        this.ws = params.ws();
    }

    /**
     * Accepts the submission and push the result to the upstream.
     *
     * The access is synchronized because commits need to be serialized.
     */
    public synchronized void upload(OpenIDIdentity client, String location, String payload) throws IOException, GitAPIException, InterruptedException {
        File f = new File(ws, location);

        FileUtils.writeStringToFile(f, payload, "UTF-8");

        git.add().addFilepattern(".").call();

        String nick = client.getNick();
        String email = client.getEmail();
        if (email==null)
            email = nick +"@users.jenkins-ci.org";

        CommitCommand cmd = git.commit();
        cmd.setAuthor(nick, email);
        cmd.setMessage("Accepted via the recipe submission application");
        cmd.setAll(true);
        cmd.call();

        for (int retry=0; retry<3; retry++) {
            PushCommand push = git.push();
            if (needsRetry(push.call())) {
                Thread.sleep(1000);

                git.pull().call();
            } else {
                return; // success
            }
        }

        throw new IOException("Failed to push after retries");
    }

    /**
     * Check the output of "git push" to see if we need to retry after git-pull
     */
    private boolean needsRetry(Iterable<PushResult> results) throws IOException {
        for (PushResult r : results) {
            for (RemoteRefUpdate ru : r.getRemoteUpdates()) {
                switch (ru.getStatus()) {
                case UP_TO_DATE:
                case OK:
                    // good
                    break;
                case REJECTED_NONFASTFORWARD:
                case REJECTED_REMOTE_CHANGED:
                    return true;    // needs to pull and then retry
                default:
                    // fatal failure
                    throw new IOException("Failed to push: "+ru);
                }
            }
        }

        return false;
    }
}
