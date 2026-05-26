#include <stddef.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>

void wasm_heap_reset(void);

#define NSVG_EXPORT static
#define NANOSVG_IMPLEMENTATION
#include "nanosvg.h"

#define NANOSVGRAST_IMPLEMENTATION
#include "nanosvgrast.h"

static int g_last_output_ptr = 0;
static int g_last_output_size = 0;
static int g_last_width = 0;
static int g_last_height = 0;
static int g_last_stride = 0;

int wasm_alloc(int size) {
    if (size <= 0) {
        return 0;
    }
    return (int)(unsigned long)malloc((size_t)size);
}

void wasm_reset(void) {
    g_last_output_ptr = 0;
    g_last_output_size = 0;
    g_last_width = 0;
    g_last_height = 0;
    g_last_stride = 0;
    wasm_heap_reset();
}

static float minf(float a, float b) {
    return a < b ? a : b;
}

int nsvg_render_rgba(int svg_ptr, int svg_len, int target_width, int target_height) {
    if (svg_ptr == 0 || svg_len <= 0 || target_width <= 0 || target_height <= 0) {
        return 0;
    }

    char* svg_copy = (char*)malloc((size_t)svg_len + 1u);
    if (svg_copy == NULL) {
        return 0;
    }
    memcpy(svg_copy, (const void*)(unsigned long)svg_ptr, (size_t)svg_len);
    svg_copy[svg_len] = '\0';

    NSVGimage* image = nsvgParse(svg_copy, "px", 96.0f);
    if (image == NULL || image->width <= 0.0f || image->height <= 0.0f) {
        return 0;
    }

    NSVGrasterizer* rast = nsvgCreateRasterizer();
    if (rast == NULL) {
        return 0;
    }

    int stride = target_width * 4;
    int output_size = stride * target_height;
    unsigned char* output = (unsigned char*)malloc((size_t)output_size);
    if (output == NULL) {
        return 0;
    }
    memset(output, 0, (size_t)output_size);

    float scale = minf((float)target_width / image->width, (float)target_height / image->height);
    float scaled_width = image->width * scale;
    float scaled_height = image->height * scale;
    float tx = ((float)target_width - scaled_width) * 0.5f;
    float ty = ((float)target_height - scaled_height) * 0.5f;

    nsvgRasterize(rast, image, tx, ty, scale, output, target_width, target_height, stride);
    nsvgDeleteRasterizer(rast);
    nsvgDelete(image);

    g_last_output_ptr = (int)(unsigned long)output;
    g_last_output_size = output_size;
    g_last_width = target_width;
    g_last_height = target_height;
    g_last_stride = stride;
    return g_last_output_ptr;
}

int nsvg_get_last_output_ptr(void) { return g_last_output_ptr; }
int nsvg_get_last_output_size(void) { return g_last_output_size; }
int nsvg_get_last_width(void) { return g_last_width; }
int nsvg_get_last_height(void) { return g_last_height; }
int nsvg_get_last_stride(void) { return g_last_stride; }
