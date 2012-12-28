package org.jenkinsci.backend.recipe;

import org.apache.commons.io.output.ThresholdingOutputStream;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Capacity capped output stream.
 * @author Kohsuke Kawaguchi
 */
class CappedOutputStream extends ThresholdingOutputStream {
    private final OutputStream out;

    public CappedOutputStream(OutputStream out, int threshold) {
        super(threshold);
        this.out = out;
    }

    @Override
    protected OutputStream getStream() throws IOException {
        return out;
    }

    @Override
    protected void thresholdReached() throws IOException {
        throw new IOException("submission too large");
    }
}
