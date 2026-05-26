package org.ngengine.nanosvg;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public final class NanoSvgAwtDragDropTest {
    private final NanoSvgRenderer renderer = new NanoSvgRenderer(ByteBuffer::allocateDirect);
    private final ExecutorService renderExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "nanosvg-render-worker");
        t.setDaemon(true);
        return t;
    });
    private final AtomicLong renderRequestSeq = new AtomicLong(0);
    private volatile byte[] svgBytes;
    private volatile BufferedImage renderedImage;
    private volatile String status = "Drag and drop an .svg file into this window";

    public static void main(String[] args) {
        new NanoSvgAwtDragDropTest().start();
    }

    private void start() {
        Frame frame = new Frame("NanoSvg AWT Drag-and-Drop Test");
        frame.setSize(960, 720);
        frame.setMinimumSize(new Dimension(320, 240));

        Canvas canvas = new Canvas() {
            @Override
            public void paint(Graphics g) {
                BufferedImage image = renderedImage;
                int w = getWidth();
                int h = getHeight();

                g.setColor(new Color(30, 30, 30));
                g.fillRect(0, 0, w, h);

                if (image != null) {
                    g.drawImage(image, 0, 0, w, h, null);
                }

                g.setColor(new Color(240, 240, 240));
                g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
                g.drawString(status, 12, 22);
            }
        };

        canvas.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                requestRender(canvas);
            }
        });

        DropTargetAdapter dropHandler = new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent event) {
                try {
                    event.acceptDrop(DnDConstants.ACTION_COPY);
                    Object transferData = event.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (!(transferData instanceof List)) {
                        status = "Drop failed: no file list";
                        canvas.repaint();
                        event.dropComplete(false);
                        return;
                    }

                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) transferData;
                    if (files.isEmpty()) {
                        status = "Drop failed: no file";
                        canvas.repaint();
                        event.dropComplete(false);
                        return;
                    }

                    File file = files.get(0);
                    if (!file.getName().toLowerCase().endsWith(".svg")) {
                        status = "Drop failed: " + file.getName() + " is not .svg";
                        canvas.repaint();
                        event.dropComplete(false);
                        return;
                    }

                    svgBytes = Files.readAllBytes(file.toPath());
                    status = "Loaded: " + file.getName();
                    requestRender(canvas);
                    event.dropComplete(true);
                } catch (Exception ex) {
                    status = "Drop failed: " + ex.getMessage();
                    canvas.repaint();
                    event.dropComplete(false);
                }
            }
        };

        new DropTarget(canvas, DnDConstants.ACTION_COPY, dropHandler, true);
        new DropTarget(frame, DnDConstants.ACTION_COPY, dropHandler, true);

        frame.add(canvas);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                renderExecutor.shutdownNow();
                frame.dispose();
                System.exit(0);
            }
        });

        frame.setVisible(true);
    }

    private void requestRender(Canvas canvas) {
        byte[] localSvg = svgBytes;
        if (localSvg == null) {
            canvas.repaint();
            return;
        }

        int targetWidth = Math.max(canvas.getWidth(), 1);
        int targetHeight = Math.max(canvas.getHeight(), 1);
        long requestId = renderRequestSeq.incrementAndGet();
        status = "Rendering " + targetWidth + "x" + targetHeight + "...";
        canvas.repaint();

        renderExecutor.execute(() -> {
            try {
                NanoSvgRenderResult result = renderer.render(ByteBuffer.wrap(localSvg), targetWidth, targetHeight);
                BufferedImage image = rgbaToBufferedImage(result);
                EventQueue.invokeLater(() -> {
                    if (requestId != renderRequestSeq.get()) {
                        return;
                    }
                    renderedImage = image;
                    status = "Rendered " + result.width() + "x" + result.height() + " in "
                            + targetWidth + "x" + targetHeight;
                    canvas.repaint();
                });
            } catch (RuntimeException ex) {
                EventQueue.invokeLater(() -> {
                    if (requestId != renderRequestSeq.get()) {
                        return;
                    }
                    renderedImage = null;
                    status = "Render failed: " + ex.getMessage();
                    canvas.repaint();
                });
            }
        });
    }

    private static BufferedImage rgbaToBufferedImage(NanoSvgRenderResult result) {
        int width = result.width();
        int height = result.height();
        ByteBuffer rgba = result.pixels().duplicate();
        rgba.rewind();

        int[] argb = new int[width * height];
        for (int i = 0; i < argb.length; i++) {
            int r = rgba.get() & 0xFF;
            int g = rgba.get() & 0xFF;
            int b = rgba.get() & 0xFF;
            int a = rgba.get() & 0xFF;
            argb[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, argb, 0, width);
        return image;
    }
}
