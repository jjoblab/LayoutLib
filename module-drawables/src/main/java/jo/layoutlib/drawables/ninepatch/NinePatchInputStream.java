package jo.layoutlib.drawables.ninepatch;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * InputStream pour images NinePatch, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class NinePatchInputStream extends FilterInputStream {

    private boolean ninePatchDetected = false;

    public NinePatchInputStream(InputStream in) {
        super(in);
    }

    /**
     * @return true si l image est un NinePatch
     */
    public boolean isNinePatch() {
        return ninePatchDetected;
    }

    public void setNinePatch(boolean ninePatch) {
        this.ninePatchDetected = ninePatch;
    }

    @Override
    public int read() throws IOException {
        return super.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return super.read(b, off, len);
    }
}
