#ifndef MINI_MATH_H
#define MINI_MATH_H

#define FLT_MAX 3.402823466e+38F

float sinf(float x);
float cosf(float x);
float tanf(float x);
float acosf(float x);
float atan2f(float y, float x);
float ceilf(float x);
float floorf(float x);
float fmodf(float x, float y);
float sqrtf(float x);
double sqrt(double x);
double pow(double x, double y);
float fabsf(float x);
double fabs(double x);
float roundf(float x);

static inline int isnan(float x) {
    return x != x;
}

#endif
