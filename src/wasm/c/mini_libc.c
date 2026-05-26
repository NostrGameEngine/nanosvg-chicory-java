#include <stddef.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>

typedef unsigned char u8;

typedef struct BlockHeader {
    size_t size;
} BlockHeader;

extern unsigned char __heap_base;

static u8* heap_base_ptr = NULL;
static u8* heap_ptr = NULL;

static u8* align8(u8* ptr) {
    unsigned int p = (unsigned int)(unsigned long)ptr;
    p = (p + 7u) & ~7u;
    return (u8*)(unsigned long)p;
}

static void heap_init(void) {
    if (heap_base_ptr == NULL) {
        heap_base_ptr = align8(&__heap_base);
        heap_ptr = heap_base_ptr;
    }
}

void wasm_heap_reset(void) {
    heap_init();
    heap_ptr = heap_base_ptr;
}

void* malloc(size_t size) {
    heap_init();
    if (size == 0) {
        size = 1;
    }
    u8* p = align8(heap_ptr);
    BlockHeader* header = (BlockHeader*)p;
    header->size = size;
    heap_ptr = p + sizeof(BlockHeader) + ((size + 7u) & ~7u);
    return (void*)(p + sizeof(BlockHeader));
}

void* realloc(void* ptr, size_t size) {
    if (ptr == NULL) {
        return malloc(size);
    }
    if (size == 0) {
        return NULL;
    }
    BlockHeader* old_header = (BlockHeader*)((u8*)ptr - sizeof(BlockHeader));
    size_t old_size = old_header->size;
    void* new_ptr = malloc(size);
    if (new_ptr == NULL) {
        return NULL;
    }
    size_t copy = old_size < size ? old_size : size;
    memcpy(new_ptr, ptr, copy);
    return new_ptr;
}

void free(void* ptr) {
    (void)ptr;
}

void* memcpy(void* dest, const void* src, size_t n) {
    u8* d = (u8*)dest;
    const u8* s = (const u8*)src;
    for (size_t i = 0; i < n; ++i) {
        d[i] = s[i];
    }
    return dest;
}

void* memset(void* dest, int c, size_t n) {
    u8* d = (u8*)dest;
    for (size_t i = 0; i < n; ++i) {
        d[i] = (u8)c;
    }
    return dest;
}

size_t strlen(const char* s) {
    size_t n = 0;
    while (s[n] != '\0') {
        ++n;
    }
    return n;
}

int strcmp(const char* a, const char* b) {
    while (*a && (*a == *b)) {
        ++a;
        ++b;
    }
    return ((unsigned char)*a) - ((unsigned char)*b);
}

int strncmp(const char* a, const char* b, size_t n) {
    for (size_t i = 0; i < n; ++i) {
        unsigned char ca = (unsigned char)a[i];
        unsigned char cb = (unsigned char)b[i];
        if (ca != cb || ca == 0 || cb == 0) {
            return (int)ca - (int)cb;
        }
    }
    return 0;
}

char* strncpy(char* dest, const char* src, size_t n) {
    size_t i = 0;
    while (i < n && src[i] != '\0') {
        dest[i] = src[i];
        ++i;
    }
    while (i < n) {
        dest[i++] = '\0';
    }
    return dest;
}

char* strchr(const char* s, int c) {
    char ch = (char)c;
    while (*s) {
        if (*s == ch) {
            return (char*)s;
        }
        ++s;
    }
    return ch == '\0' ? (char*)s : NULL;
}

char* strstr(const char* haystack, const char* needle) {
    if (*needle == '\0') {
        return (char*)haystack;
    }
    size_t nlen = strlen(needle);
    while (*haystack) {
        if (strncmp(haystack, needle, nlen) == 0) {
            return (char*)haystack;
        }
        ++haystack;
    }
    return NULL;
}

static int is_space(char c) {
    return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f' || c == '\v';
}

static int is_digit(char c) {
    return c >= '0' && c <= '9';
}

static int hex_value(char c) {
    if (c >= '0' && c <= '9') return c - '0';
    if (c >= 'a' && c <= 'f') return 10 + (c - 'a');
    if (c >= 'A' && c <= 'F') return 10 + (c - 'A');
    return -1;
}

long long strtoll(const char* nptr, char** endptr, int base) {
    if (base != 10) {
        if (endptr) *endptr = (char*)nptr;
        return 0;
    }
    const char* s = nptr;
    while (is_space(*s)) ++s;
    int neg = 0;
    if (*s == '+' || *s == '-') {
        neg = (*s == '-');
        ++s;
    }
    long long value = 0;
    const char* start = s;
    while (is_digit(*s)) {
        value = value * 10 + (long long)(*s - '0');
        ++s;
    }
    if (endptr) *endptr = (char*)(s == start ? nptr : s);
    return neg ? -value : value;
}

long strtol(const char* nptr, char** endptr, int base) {
    return (long)strtoll(nptr, endptr, base);
}

static int parse_exact_hex(const char* str, int digits, unsigned int* out) {
    unsigned int value = 0;
    for (int i = 0; i < digits; ++i) {
        int h = hex_value(str[i]);
        if (h < 0) return 0;
        value = (value << 4) | (unsigned int)h;
    }
    *out = value;
    return 1;
}

int sscanf(const char* str, const char* format, ...) {
    va_list ap;
    va_start(ap, format);
    int matched = 0;

    if (strcmp(format, "#%2x%2x%2x") == 0) {
        unsigned int* a = va_arg(ap, unsigned int*);
        unsigned int* b = va_arg(ap, unsigned int*);
        unsigned int* c = va_arg(ap, unsigned int*);
        if (str[0] == '#' && parse_exact_hex(str + 1, 2, a) && parse_exact_hex(str + 3, 2, b) && parse_exact_hex(str + 5, 2, c)) {
            matched = 3;
        }
    } else if (strcmp(format, "#%1x%1x%1x") == 0) {
        unsigned int* a = va_arg(ap, unsigned int*);
        unsigned int* b = va_arg(ap, unsigned int*);
        unsigned int* c = va_arg(ap, unsigned int*);
        if (str[0] == '#') {
            unsigned int v0, v1, v2;
            if (parse_exact_hex(str + 1, 1, &v0) && parse_exact_hex(str + 2, 1, &v1) && parse_exact_hex(str + 3, 1, &v2)) {
                *a = v0;
                *b = v1;
                *c = v2;
                matched = 3;
            }
        }
    } else if (strcmp(format, "rgb(%u, %u, %u)") == 0) {
        unsigned int* a = va_arg(ap, unsigned int*);
        unsigned int* b = va_arg(ap, unsigned int*);
        unsigned int* c = va_arg(ap, unsigned int*);
        const char* s = str;
        if (strncmp(s, "rgb(", 4) == 0) {
            s += 4;
            char* end;
            *a = (unsigned int)strtol(s, &end, 10);
            if (end == s) goto done;
            s = end;
            while (*s == ' ') ++s;
            if (*s != ',') goto done;
            ++s;
            while (*s == ' ') ++s;
            *b = (unsigned int)strtol(s, &end, 10);
            if (end == s) goto done;
            s = end;
            while (*s == ' ') ++s;
            if (*s != ',') goto done;
            ++s;
            while (*s == ' ') ++s;
            *c = (unsigned int)strtol(s, &end, 10);
            if (end == s) goto done;
            s = end;
            while (*s == ' ') ++s;
            if (*s != ')') goto done;
            matched = 3;
        }
    }

done:
    va_end(ap);
    return matched;
}

static void byte_swap(u8* a, u8* b, size_t size) {
    for (size_t i = 0; i < size; ++i) {
        u8 t = a[i];
        a[i] = b[i];
        b[i] = t;
    }
}

void qsort(void* base, size_t nmemb, size_t size, int (*compar)(const void*, const void*)) {
    u8* data = (u8*)base;
    for (size_t i = 1; i < nmemb; ++i) {
        size_t j = i;
        while (j > 0) {
            u8* left = data + (j - 1) * size;
            u8* right = data + j * size;
            if (compar(left, right) <= 0) {
                break;
            }
            byte_swap(left, right, size);
            --j;
        }
    }
}

FILE* fopen(const char* filename, const char* mode) {
    (void)filename;
    (void)mode;
    return NULL;
}

size_t fread(void* ptr, size_t size, size_t nmemb, FILE* stream) {
    (void)ptr;
    (void)size;
    (void)nmemb;
    (void)stream;
    return 0;
}

int fseek(FILE* stream, long offset, int whence) {
    (void)stream;
    (void)offset;
    (void)whence;
    return -1;
}

long ftell(FILE* stream) {
    (void)stream;
    return -1;
}

int fclose(FILE* stream) {
    (void)stream;
    return 0;
}

int printf(const char* format, ...) {
    (void)format;
    return 0;
}

int fprintf(FILE* stream, const char* format, ...) {
    (void)stream;
    (void)format;
    return 0;
}

double fabs(double x) {
    return x < 0.0 ? -x : x;
}

float fabsf(float x) {
    return x < 0.0f ? -x : x;
}

float roundf(float x) {
    return x >= 0.0f ? floorf(x + 0.5f) : ceilf(x - 0.5f);
}
