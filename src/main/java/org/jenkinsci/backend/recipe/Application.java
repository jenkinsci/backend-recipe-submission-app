package org.jenkinsci.backend.recipe;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * @author Kohsuke Kawaguchi
 */
public class Application {
    public final ServletContext context;
    public final Parameters params;
    public final Git git;

    public Application(ServletContext context, Parameters params) throws IOException {
        this.context = context;
        this.params = params;
        this.git = checkOutRepository();
    }

    private Git checkOutRepository() throws IOException {
        File ws = params.ws();
        if (!ws.isDirectory()) {
            CloneCommand cmd = new CloneCommand();
            cmd.setURI(params.repo());
            cmd.setBranch("refs/heads/" + params.branch());
            cmd.setDirectory(ws);
            return cmd.call();
        } else {
            try {
                Git git = new Git(new FileRepositoryBuilder().setWorkTree(ws).build());
                PullCommand cmd = git.pull();
                cmd.call();
                return git;
            } catch (GitAPIException e) {
                throw new IOException("Failed to update the repository",e);
            }
        }
    }

    public synchronized HttpResponse doTest() throws IOException, GitAPIException, InterruptedException {
        File aaa = new File(params.ws(), "aaa");

        FileUtils.writeStringToFile(aaa, System.currentTimeMillis() + "");

        git.add().addFilepattern(".").call();

        CommitCommand cmd = git.commit();
        String name = "test" + (iota++);
        cmd.setAuthor(name, name + "@kohsuke.org");
        cmd.setMessage("Submitted on " + new Date());
        cmd.setAll(true);
        cmd.call();

        for (int retry=0; retry<3; retry++) {
            PushCommand push = git.push();
            if (needsRetry(push.call())) {
                Thread.sleep(1000);

                git.pull().call();
            } else {
                return HttpResponses.ok();
            }
        }

        throw new IOException("Failed to push after retries");
    }

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

    private int iota=0;
}
