package org.ngengine.nanosvg;

import java.nio.ByteBuffer;

public final class NanoSvgRenderResult {
    private final ByteBuffer pixels;
    private final int width;
    private final int height;
    private final int stride;

    public NanoSvgRenderResult(ByteBuffer pixels, int width, int height, int stride) {
        this.pixels = pixels;
        this.width = width;
        this.height = height;
        this.stride = stride;
    }

    public ByteBuffer pixels() {
        return pixels;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int stride() {
        return stride;
    }
}
