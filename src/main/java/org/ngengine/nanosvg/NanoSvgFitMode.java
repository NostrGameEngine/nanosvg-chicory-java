package org.ngengine.nanosvg;

public enum NanoSvgFitMode {
    CONTAIN(0),
    COVER(1);

    private final int wasmValue;

    NanoSvgFitMode(int wasmValue) {
        this.wasmValue = wasmValue;
    }

    int wasmValue() {
        return wasmValue;
    }
}
