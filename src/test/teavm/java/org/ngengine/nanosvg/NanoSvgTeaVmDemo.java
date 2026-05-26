package org.ngengine.nanosvg;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class NanoSvgTeaVmDemo {
    private static NanoSvgRenderer renderer;
    private static byte[] svgBytes;
    private static int viewportWidth = 960;
    private static int viewportHeight = 720;

    private NanoSvgTeaVmDemo() {
    }

    public static void main(String[] args) {
        renderer = new NanoSvgRenderer(ByteBuffer::allocate, loadTeaVmModule());
        setupPage(NanoSvgTeaVmDemo::onSvgDropped, NanoSvgTeaVmDemo::onResize);
        setStatus("Drop an SVG file into the page");
        String fallbackSvg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"128\" height=\"128\" viewBox=\"0 0 128 128\">"
                + "<rect width=\"128\" height=\"128\" fill=\"#111827\"/>"
                + "<circle cx=\"64\" cy=\"64\" r=\"44\" fill=\"#22d3ee\"/>"
                + "</svg>";
        svgBytes = fallbackSvg.getBytes(StandardCharsets.UTF_8);
        renderNow();
    }

    private static void onSvgDropped(String svgContent) {
        if (svgContent == null || svgContent.isEmpty()) {
            setStatus("Dropped file is empty");
            return;
        }
        svgBytes = svgContent.getBytes(StandardCharsets.UTF_8);
        renderNow();
    }

    private static void onResize(int width, int height) {
        viewportWidth = Math.max(width, 1);
        viewportHeight = Math.max(height, 1);
        renderNow();
    }

    private static void renderNow() {
        if (svgBytes == null) {
            setStatus("Drop an SVG file into the page");
            return;
        }
        int width = Math.max(viewportWidth, 1);
        int height = Math.max(viewportHeight, 1);
        try {
            NanoSvgRenderResult result = renderer.render(ByteBuffer.wrap(svgBytes), width, height);
            byte[] rgba = new byte[result.pixels().remaining()];
            result.pixels().duplicate().get(rgba);
            drawRgba(rgba, result.width(), result.height(), width, height);
            setStatus("Rendered " + result.width() + "x" + result.height() + " into viewport " + width + "x" + height);
        } catch (RuntimeException ex) {
            setStatus("Render failed: " + ex.getMessage());
        }
    }

    @JSFunctor
    private interface SvgDropHandler extends JSObject {
        void accept(String svgText);
    }

    @JSFunctor
    private interface ResizeHandler extends JSObject {
        void accept(int width, int height);
    }

    private static WasmModule loadTeaVmModule() {
        String binaryText = loadNanosvgMetaBinaryText();
        if (binaryText == null || binaryText.isEmpty()) {
            throw new IllegalStateException("Failed to load Nanosvg.meta from build/teavm");
        }
        byte[] metaBytes = new byte[binaryText.length()];
        for (int i = 0; i < binaryText.length(); i++) {
            metaBytes[i] = (byte) (binaryText.charAt(i) & 0xFF);
        }
        return Parser.parse(metaBytes);
    }

    @JSBody(script = ""
            + "var xhr=new XMLHttpRequest();"
            + "xhr.open('GET','./Nanosvg.meta',false);"
            + "xhr.overrideMimeType('text/plain; charset=x-user-defined');"
            + "xhr.send(null);"
            + "if((xhr.status>=200&&xhr.status<300)||xhr.status===0){"
            + "return xhr.responseText?String(xhr.responseText):null;"
            + "}"
            + "return null;")
    private static native String loadNanosvgMetaBinaryText();

    @JSBody(params = {"onSvgDrop", "onResize"}, script = ""
            + "var body=document.body;"
            + "body.style.margin='0';"
            + "body.style.background='#111827';"
            + "body.style.overflow='hidden';"
            + "body.style.fontFamily='ui-sans-serif, system-ui, -apple-system, Segoe UI, sans-serif';"
            + "var canvas=document.createElement('canvas');"
            + "canvas.id='nanosvg-canvas';"
            + "canvas.style.display='block';"
            + "canvas.style.width='100vw';"
            + "canvas.style.height='100vh';"
            + "body.appendChild(canvas);"
            + "var status=document.createElement('div');"
            + "status.id='nanosvg-status';"
            + "status.style.position='fixed';"
            + "status.style.left='12px';"
            + "status.style.top='12px';"
            + "status.style.padding='8px 10px';"
            + "status.style.borderRadius='8px';"
            + "status.style.color='#e5e7eb';"
            + "status.style.background='rgba(0,0,0,0.55)';"
            + "status.style.fontSize='13px';"
            + "status.style.pointerEvents='none';"
            + "body.appendChild(status);"
            + "var controls=document.createElement('div');"
            + "controls.style.position='fixed';"
            + "controls.style.right='12px';"
            + "controls.style.top='12px';"
            + "controls.style.zIndex='2';"
            + "var uploadButton=document.createElement('button');"
            + "uploadButton.type='button';"
            + "uploadButton.textContent='Upload SVG';"
            + "uploadButton.style.padding='8px 12px';"
            + "uploadButton.style.border='1px solid #334155';"
            + "uploadButton.style.borderRadius='8px';"
            + "uploadButton.style.background='#0f172a';"
            + "uploadButton.style.color='#e2e8f0';"
            + "uploadButton.style.cursor='pointer';"
            + "uploadButton.style.fontSize='13px';"
            + "controls.appendChild(uploadButton);"
            + "body.appendChild(controls);"
            + "var uploadInput=document.createElement('input');"
            + "uploadInput.type='file';"
            + "uploadInput.accept='.svg,image/svg+xml';"
            + "uploadInput.style.display='none';"
            + "body.appendChild(uploadInput);"
            + "var readSvgFile=function(file){"
            + "if(!file){return;}"
            + "if(!file.name||!file.name.toLowerCase().endsWith('.svg')){status.textContent='Upload failed: '+(file.name||'file')+' is not .svg';return;}"
            + "var reader=new FileReader();"
            + "reader.onload=function(){onSvgDrop(reader.result?String(reader.result):'');};"
            + "reader.onerror=function(){status.textContent='Upload failed: file read error';};"
            + "reader.readAsText(file);"
            + "};"
            + "uploadButton.addEventListener('click', function(){uploadInput.click();});"
            + "uploadInput.addEventListener('change', function(){"
            + "var files=uploadInput.files;"
            + "if(files&&files.length){readSvgFile(files[0]);}"
            + "uploadInput.value='';"
            + "});"
            + "var blocker=function(e){e.preventDefault();};"
            + "body.addEventListener('dragenter', blocker);"
            + "body.addEventListener('dragover', blocker);"
            + "body.addEventListener('drop', function(e){"
            + "e.preventDefault();"
            + "var files=e.dataTransfer&&e.dataTransfer.files;"
            + "if(!files||!files.length){return;}"
            + "readSvgFile(files[0]);"
            + "});"
            + "var scheduleResize=function(){"
            + "var w=Math.max(window.innerWidth||1,1);"
            + "var h=Math.max(window.innerHeight||1,1);"
            + "onResize(w,h);"
            + "};"
            + "window.addEventListener('resize', scheduleResize);"
            + "scheduleResize();")
    private static native void setupPage(SvgDropHandler onSvgDrop, ResizeHandler onResize);

    @JSBody(params = {"rgba", "imageWidth", "imageHeight", "viewportWidth", "viewportHeight"}, script = ""
            + "var canvas=document.getElementById('nanosvg-canvas');"
            + "if(!canvas)return;"
            + "canvas.width=viewportWidth;"
            + "canvas.height=viewportHeight;"
            + "var ctx=canvas.getContext('2d');"
            + "if(!ctx)return;"
            + "var pixels=new Uint8ClampedArray(rgba.length);"
            + "for(var j=0;j<rgba.length;j++){"
            + "pixels[j]=rgba[j]&255;"
            + "}"
            + "for(var i=0;i<pixels.length;i+=4){"
            + "if(pixels[i+3]===0){pixels[i]=0;pixels[i+1]=0;pixels[i+2]=0;}"
            + "}"
            + "var data=new ImageData(pixels, imageWidth, imageHeight);"
            + "ctx.putImageData(data,0,0);")
    private static native void drawRgba(byte[] rgba, int imageWidth, int imageHeight, int viewportWidth, int viewportHeight);

    @JSBody(params = {"message"}, script = ""
            + "var status=document.getElementById('nanosvg-status');"
            + "if(status){status.textContent=message;}")
    private static native void setStatus(String message);
}
