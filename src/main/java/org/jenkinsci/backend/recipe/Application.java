package org.jenkinsci.backend.recipe;

import com.jcraft.jsch.Session;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.kohsuke.stapler.AttributeKey;
import org.kohsuke.stapler.StaplerFallback;
import org.kohsuke.stapler.framework.adjunct.AdjunctManager;
import org.openid4java.OpenIDException;
import org.openid4java.consumer.ConsumerException;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.InMemoryConsumerAssociationStore;
import org.openid4java.consumer.InMemoryNonceVerifier;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class Application implements StaplerFallback {
    public final ServletContext context;
    public final Parameters params;
    public final Git git;
    public final ConsumerManager manager;
    public final GitClient gitClient;

    public final AdjunctManager adjuncts;

    /*package*/ final AttributeKey<FakeSerializable<Submission>> submission = AttributeKey.sessionScoped();

    public Application(ServletContext context, Parameters params) throws IOException, ConsumerException {
        this.context = context;
        this.params = params;

        // AFAICT jgit doesn't offer easy way to set our own Transport per command,
        // so our only option is to set the VM-wide default session factory.
        SshSessionFactory.setInstance(new SshSessionFactoryImpl(params, new JschConfigSessionFactory() {
            @Override
            protected void configure(Host hc, Session session) {
                // nothing
            }
        }));

        this.git = checkOutRepository();
        this.gitClient = new GitClient(git,params);
        this.adjuncts = new AdjunctManager(context,getClass().getClassLoader(),"adjuncts");

        manager = new ConsumerManager();
        manager.setAssociations(new InMemoryConsumerAssociationStore());
        manager.setNonceVerifier(new InMemoryNonceVerifier(5000));
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

    public Submission getStaplerFallback() {
        try {
            FakeSerializable<Submission> v = submission.get();
            if (v==null || v.value==null)
                submission.set(v=new FakeSerializable<Submission>(new Submission(this)));
            return v.value;
        } catch (OpenIDException e) {
            throw new Error(e);
        } catch (IOException e) {
            throw new Error(e);
        }
    }
}
