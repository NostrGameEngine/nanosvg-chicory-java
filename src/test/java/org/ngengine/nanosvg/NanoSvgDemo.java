package org.ngengine.nanosvg;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class NanoSvgDemo {
    public static void main(String[] args) {
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"64\" height=\"64\" viewBox=\"0 0 64 64\">"
                + "<rect width=\"64\" height=\"64\" fill=\"#00000000\"/>"
                + "<circle cx=\"32\" cy=\"32\" r=\"24\" fill=\"#ff6600\"/>"
                + "</svg>";

        NanoSvgRenderer renderer = new NanoSvgRenderer(ByteBuffer::allocateDirect);
        NanoSvgRenderResult result = renderer.render(StandardCharsets.UTF_8.encode(svg), 128, 128);
        System.out.println("Rendered " + result.width() + "x" + result.height() + ", stride=" + result.stride() + ", bytes=" + result.pixels().remaining());
    }
}
