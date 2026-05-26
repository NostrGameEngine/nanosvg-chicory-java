package org.ngengine.nanosvg;

import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValType;

import java.util.List;

final class NanoSvgMathHostFunctions {
    private NanoSvgMathHostFunctions() {
    }

    static HostFunction[] create() {
        return new HostFunction[] {
                f32ToF32("sinf", (float x) -> (float) Math.sin(x)),
                f32ToF32("cosf", (float x) -> (float) Math.cos(x)),
                f32ToF32("tanf", (float x) -> (float) Math.tan(x)),
                f32ToF32("acosf", (float x) -> (float) Math.acos(x)),
                f32x2ToF32("atan2f", (float y, float x) -> (float) Math.atan2(y, x)),
                f32ToF32("ceilf", (float x) -> (float) Math.ceil(x)),
                f32ToF32("floorf", (float x) -> (float) Math.floor(x)),
                f32x2ToF32("fmodf", (float x, float y) -> (float) (x % y)),
                f32ToF32("sqrtf", (float x) -> (float) Math.sqrt(x)),
                f64ToF64("sqrt", Math::sqrt),
                f64x2ToF64("pow", Math::pow)
        };
    }

    private static HostFunction f32ToF32(String name, F32Unary fn) {
        return new HostFunction(
                "env",
                name,
                FunctionType.of(List.of(ValType.F32), List.of(ValType.F32)),
                (instance, args) -> new long[] {toF32(fn.apply(fromF32(args[0])))}
        );
    }

    private static HostFunction f32x2ToF32(String name, F32Binary fn) {
        return new HostFunction(
                "env",
                name,
                FunctionType.of(List.of(ValType.F32, ValType.F32), List.of(ValType.F32)),
                (instance, args) -> new long[] {toF32(fn.apply(fromF32(args[0]), fromF32(args[1])))}
        );
    }

    private static HostFunction f64ToF64(String name, F64Unary fn) {
        return new HostFunction(
                "env",
                name,
                FunctionType.of(List.of(ValType.F64), List.of(ValType.F64)),
                (instance, args) -> new long[] {Double.doubleToRawLongBits(fn.apply(Double.longBitsToDouble(args[0])))}
        );
    }

    private static HostFunction f64x2ToF64(String name, F64Binary fn) {
        return new HostFunction(
                "env",
                name,
                FunctionType.of(List.of(ValType.F64, ValType.F64), List.of(ValType.F64)),
                (instance, args) -> new long[] {
                        Double.doubleToRawLongBits(fn.apply(
                                Double.longBitsToDouble(args[0]),
                                Double.longBitsToDouble(args[1])
                        ))}
        );
    }

    private static float fromF32(long raw) {
        return Float.intBitsToFloat((int) raw);
    }

    private static long toF32(float value) {
        return Float.floatToRawIntBits(value) & 0xFFFFFFFFL;
    }

    @FunctionalInterface
    private interface F32Unary {
        float apply(float x);
    }

    @FunctionalInterface
    private interface F32Binary {
        float apply(float x, float y);
    }

    @FunctionalInterface
    private interface F64Unary {
        double apply(double x);
    }

    @FunctionalInterface
    private interface F64Binary {
        double apply(double x, double y);
    }
}
