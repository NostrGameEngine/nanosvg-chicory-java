# NanoSVG -> Wasm -> Java

This project compiles NanoSVG to wasm and then to java with Chicory's build-time.


## What this exposes

The public Java API is centered on `NanoSvgRenderer`:

```java
NanoSvgRenderer renderer = new NanoSvgRenderer(ByteBuffer::allocateDirect);
NanoSvgRenderResult result = renderer.render(svgUtf8ByteBuffer, 256, 256);
ByteBuffer rgba = result.pixels();
```

## Build requirements

- JDK 11+
- Gradle 8+
- Clang/LLD with `wasm32` support available as `clang`
- WebAssembly linker available as `wasm-ld` or `ld.lld` (usually provided by package `lld`)

## Build
 
```bash
./gradlew clean build
```
