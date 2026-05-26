#ifndef MINI_STDLIB_H
#define MINI_STDLIB_H

#include <stddef.h>

void* malloc(size_t size);
void* realloc(void* ptr, size_t size);
void free(void* ptr);
long strtol(const char* nptr, char** endptr, int base);
long long strtoll(const char* nptr, char** endptr, int base);
void qsort(void* base, size_t nmemb, size_t size, int (*compar)(const void*, const void*));

#endif
