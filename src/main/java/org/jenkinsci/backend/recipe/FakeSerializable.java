package org.jenkinsci.backend.recipe;

import java.io.Serializable;

/**
 * @author Kohsuke Kawaguchi
 */
public class FakeSerializable<T> implements Serializable {
    transient final T value;

    public FakeSerializable(T value) {
        this.value = value;
    }
}
