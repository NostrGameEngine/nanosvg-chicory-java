package org.ngengine.nanosvg;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class NanoSvgRendererTest {
    private static final String TEST_SVG = """
            <svg xmlns="http://www.w3.org/2000/svg" width="32" height="16" viewBox="0 0 32 16">
              <rect x="0" y="0" width="16" height="16" fill="#ff0000"/>
              <rect x="16" y="0" width="16" height="16" fill="#00ff00"/>
              <rect x="8" y="4" width="16" height="8" fill="#0000ff"/>
            </svg>
            """;

    private NanoSvgRendererTest() {
    }

    public static void main(String[] args) {
        fullRenderUsesTargetDimensions();
        viewBoxRenderUsesSourceCoordinates();
    }

    private static void fullRenderUsesTargetDimensions() {
        NanoSvgRenderer renderer = new NanoSvgRenderer(ByteBuffer::allocateDirect);
        NanoSvgRenderResult full = renderer.render(svg(TEST_SVG), 64, 32);

        require(full.width() == 64, "full width");
        require(full.height() == 32, "full height");
        require(full.stride() == 256, "full stride");

        require(alpha(rgba(full, 32, 16)) > 0, "full render center alpha");
    }

    private static void viewBoxRenderUsesSourceCoordinates() {
        String sideBySideSvg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="10" viewBox="0 0 20 10">
                  <rect x="0" y="0" width="10" height="10" fill="#ff0000"/>
                  <rect x="10" y="0" width="10" height="10" fill="#0000ff"/>
                </svg>
                """;
        NanoSvgRenderer renderer = new NanoSvgRenderer(ByteBuffer::allocateDirect);
        NanoSvgRenderResult viewBox = renderer.renderViewBox(
                svg(sideBySideSvg), 12, 12, 10, 0, 10, 10, NanoSvgFitMode.CONTAIN);

        require(viewBox.width() == 12, "viewBox width");
        require(viewBox.height() == 12, "viewBox height");

        int center = rgba(viewBox, 6, 6);
        require(red(center) == 0, "viewBox should crop away the red source half");
        require(blue(center) > 0, "viewBox should render the blue source half");
    }

    private static ByteBuffer svg(String svg) {
        return StandardCharsets.UTF_8.encode(svg);
    }

    private static int rgba(NanoSvgRenderResult result, int x, int y) {
        ByteBuffer pixels = result.pixels().duplicate();
        int offset = y * result.stride() + x * 4;
        int r = pixels.get(offset) & 0xff;
        int g = pixels.get(offset + 1) & 0xff;
        int b = pixels.get(offset + 2) & 0xff;
        int a = pixels.get(offset + 3) & 0xff;
        return r | (g << 8) | (b << 16) | (a << 24);
    }

    private static int alpha(int rgba) {
        return (rgba >>> 24) & 0xff;
    }

    private static int red(int rgba) {
        return rgba & 0xff;
    }

    private static int blue(int rgba) {
        return (rgba >>> 16) & 0xff;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
