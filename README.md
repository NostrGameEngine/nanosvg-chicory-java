# NanoSVG Chicory Java

This is a pure Java 11 build of [NanoSVG](https://github.com/memononen/nanosvg) compiled to wasm and then to java using [Chicory](https://chicory.dev/).

> [!NOTE]  
> The Chicory runtime used by this project is a [fork](https://github.com/NostrGameEngine/chicory) patched for improved compatibility with Nostr Game Engine, however it should run just fine with the official build.

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
