#ifndef MINI_STDIO_H
#define MINI_STDIO_H

#include <stddef.h>
#include <stdarg.h>

typedef struct FILE FILE;
struct FILE { int unused; };

#define SEEK_SET 0
#define SEEK_CUR 1
#define SEEK_END 2

int printf(const char* format, ...);
int fprintf(FILE* stream, const char* format, ...);
int sscanf(const char* str, const char* format, ...);
FILE* fopen(const char* filename, const char* mode);
size_t fread(void* ptr, size_t size, size_t nmemb, FILE* stream);
int fseek(FILE* stream, long offset, int whence);
long ftell(FILE* stream);
int fclose(FILE* stream);

#endif
