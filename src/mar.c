#include <assert.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <string.h>

#undef MAX
#undef MIN
#define MAX(a,b) ({ __typeof__ (a) _a = (a); __typeof__ (b) _b = (b); _a > _b ? _a : _b; })
#define MIN(a,b) ({ __typeof__ (a) _a = (a); __typeof__ (b) _b = (b); _a < _b ? _a : _b; })

typedef int      _VOID_;
typedef int      Bool;
typedef char     Char;
typedef float    Float;
typedef int      Int;
typedef uint8_t  U8;
typedef uint16_t U16;
typedef uint32_t U32;
typedef uint64_t U64;
typedef int8_t   S8;
typedef int16_t  S16;
typedef int32_t  S32;
typedef int64_t  S64;

#define _void_ 0
#define null   NULL
#define true   1
#define false  0

#define CAST(tp,v) (((union { tp a; typeof(v) b; }) {.b=v}).a)

int mar_sup (uint32_t sup, uint32_t sub) {
    //printf(">>> %X vs %X\n", sup, sub);
    for (int i=5; i>=0; i--) {
        uint32_t xsup = (sup & (0b11111<<(i*5)));
        uint32_t xsub = (sub & (0b11111<<(i*5)));
        //printf("\t[%d/%X] %X vs %X\n", i, (0b11111<<(i*5)), xsup, xsub);
        if (xsup==0 || xsup==xsub) {
            // ok
        } else {
            return 0;
        }
    }
    return 1;
}

// ESCAPE / EXCEPTION

#define __MAR_ESCAPE_NONE__  0
typedef struct Escape {
    int tag;
    char _[100];
} Escape;
Escape MAR_ESCAPE = { __MAR_ESCAPE_NONE__ };

#define __MAR_EXCEPTION_NONE__ 0
typedef struct Exception {
    int tag;
    char _[100];
} Exception;
Exception MAR_EXCEPTION = { __MAR_EXCEPTION_NONE__ };

// VECTORS

typedef struct Vector {
    int max, cur;
    char buf[];
} Vector;

void mar_vector_cat_pointer (Vector* dst, char* src, int len, int size) {
    int n = MIN(dst->max-dst->cur, len);
    memcpy(&dst->buf[dst->cur*size], src, n*size);
    dst->cur += n;
}

void mar_vector_cat_vector (Vector* dst, Vector* src, int size) {
    mar_vector_cat_pointer(dst, src->buf, src->cur, size);
}

// EXES / COROS / TASKS

typedef enum MAR_EXE_STATUS {
    MAR_EXE_STATUS_YIELDED = 0,
    MAR_EXE_STATUS_TOGGLED,
    MAR_EXE_STATUS_RUNNING,
    MAR_EXE_STATUS_COMPLETE,
} MAR_EXE_STATUS;

typedef enum MAR_EXE_ACTION {
    MAR_EXE_ACTION_RESUME,
    MAR_EXE_ACTION_ABORT,
} MAR_EXE_ACTION;

#define MAR_Exe_Fields(_pro_)   \
    int pc;                     \
    MAR_EXE_STATUS status;      \
    _pro_ pro;

// TASKS

struct MAR_Task;
typedef void (*Task_Pro) (MAR_EXE_ACTION, struct MAR_Task*, void*, int, void*);

typedef struct MAR_Task {
    MAR_Exe_Fields(Task_Pro)
    uintptr_t evt;
} MAR_Task;

int MAR_BROADCAST_N = 0; //// === MAR_BROADCAST_N === //;
MAR_Task* MAR_BROADCAST_TS[100];
void mar_broadcast (int tag, void* pay) {
    for (int i=0; i<MAR_BROADCAST_N; i++) {
        MAR_BROADCAST_TS[i]->pro(MAR_EXE_ACTION_RESUME, MAR_BROADCAST_TS[i], null, tag, pay);
    }
}

// TYPES
#define MAR_TAG_none 0
// === MAR_TYPES === //

// PROTOS
// === MAR_PROTOS === //

// MAIN
int main (void) {
    do {
        // === MAR_MAIN === //
    } while (0);
    if (MAR_EXCEPTION.tag != __MAR_EXCEPTION_NONE__) {
        puts("uncaught exception");
    }
}
