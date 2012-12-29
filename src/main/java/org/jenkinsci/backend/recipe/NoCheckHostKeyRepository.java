package org.jenkinsci.backend.recipe;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.UserInfo;

/**
 * @author Kohsuke Kawaguchi
 */
class NoCheckHostKeyRepository implements HostKeyRepository {
    public int check(String host, byte[] key) {
        return OK;
    }

    public void add(HostKey hostkey, UserInfo ui) {
    }

    public void remove(String host, String type) {
    }

    public void remove(String host, String type, byte[] key) {
    }

    public String getKnownHostsRepositoryID() {
        return null;
    }

    public HostKey[] getHostKey() {
        return null;
    }

    public HostKey[] getHostKey(String host, String type) {
        return null;
    }
}
