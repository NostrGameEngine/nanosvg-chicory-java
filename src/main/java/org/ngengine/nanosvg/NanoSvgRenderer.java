package org.ngengine.nanosvg;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.runtime.Store;
import com.dylibso.chicory.wasm.WasmModule;
import org.ngengine.nanosvg.wasm.Nanosvg;

import java.nio.ByteBuffer;
import java.util.function.IntFunction;

public final class NanoSvgRenderer  {
    private final Instance instance;
    private final IntFunction<ByteBuffer> allocator;

    public NanoSvgRenderer(IntFunction<ByteBuffer> allocator) {
        this(allocator, Nanosvg.load());
    }

    public NanoSvgRenderer(IntFunction<ByteBuffer> allocator, WasmModule module) {
        this.allocator = allocator;
        Store store = new Store().addFunction(NanoSvgMathHostFunctions.create());
        this.instance = store.instantiate(
                "nanosvg",
                importValues -> Instance.builder(module)
                        .withMachineFactory(Nanosvg::create)
                        .withImportValues(importValues)
                        .withStart(false)
                        .build()
        );
    }
    

    public NanoSvgRenderResult render(ByteBuffer svgUtf8, int targetWidth, int targetHeight) {
        if (svgUtf8 == null) {
            throw new IllegalArgumentException("svgUtf8 is null");
        }
        if (targetWidth <= 0 || targetHeight <= 0) {
            throw new IllegalArgumentException("target dimensions must be > 0");
        }

        instance.export("wasm_reset").apply();

        ByteBuffer input = svgUtf8.duplicate();
        int inputSize = input.remaining();
        byte[] inputBytes = new byte[inputSize];
        input.get(inputBytes);

        int inputPtr = (int) instance.export("wasm_alloc").apply(inputSize)[0];
        Memory memory = instance.memory();
        memory.write(inputPtr, inputBytes);

        long[] renderResult = instance.export("nsvg_render_rgba").apply(inputPtr, inputSize, targetWidth, targetHeight);
        int outputPtr = (int) renderResult[0];
        if (outputPtr == 0) {
            throw new IllegalStateException("NanoSVG wasm render failed");
        }

        int outputSize = (int) instance.export("nsvg_get_last_output_size").apply()[0];
        int width = (int) instance.export("nsvg_get_last_width").apply()[0];
        int height = (int) instance.export("nsvg_get_last_height").apply()[0];
        int stride = (int) instance.export("nsvg_get_last_stride").apply()[0];

        byte[] rgba = memory.readBytes(outputPtr, outputSize);
        ByteBuffer directOutput = allocator.apply(outputSize);

        directOutput.clear();
        directOutput.put(rgba);
        directOutput.flip();
        return new NanoSvgRenderResult(directOutput, width, height, stride);
    }
}
