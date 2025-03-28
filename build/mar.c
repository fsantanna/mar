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

typedef int     _VOID_;
typedef int     Bool;
typedef char    Char;
typedef float   Float;
typedef int     Int;
typedef uint8_t U8;

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
    MAR_EXE_STATUS_RESUMED,
    MAR_EXE_STATUS_TERMINATED,
} MAR_EXE_STATUS;

#define MAR_Exe_Fields(_pro_)   \
    int pc;                     \
    MAR_EXE_STATUS status;      \
    _pro_ pro;

// TASKS

typedef struct Task_Await {
    int evt;
    struct Task* prv;
    struct Task* nxt;
} Task_Await;

typedef int (*Task_Pro) (struct Task*, void*, void*);

typedef struct Task {
    MAR_Exe_Fields(Task_Pro)
    Task_Await awt;
} Task;

enum {
    MAR_EVENT_NONE = 0,
    // === MAR_EVENTS === //
};

Task* MAR_AWAITS = NULL;

void mar_awaits_add (Task* tsk, int evt_id) {
    tsk->awt.evt = evt_id;
    if (MAR_AWAITS == NULL) {
        tsk->awt.prv = tsk;
        tsk->awt.nxt = tsk;
        MAR_AWAITS = tsk;
    } else {
        MAR_AWAITS->awt.prv->awt.nxt = tsk;
        tsk->awt.prv = MAR_AWAITS->awt.prv;
        tsk->awt.nxt = MAR_AWAITS;
        MAR_AWAITS->awt.prv = tsk;
    }
}

void mar_awaits_rem (Task* tsk) {
    if (MAR_AWAITS == tsk) {
        MAR_AWAITS = (tsk->awt.nxt == tsk) ? NULL : tsk->awt.nxt;
    }
    tsk->awt.prv->awt.nxt = tsk->awt.nxt;
    tsk->awt.nxt->awt.prv = tsk->awt.prv;
}

void mar_awaits_emt (int evt_id, void* evt_pay) {
    Task* tsk = MAR_AWAITS;
    while (tsk != NULL) {
        if (tsk->awt.evt == evt_id) {
            tsk->awt.evt = MAR_EVENT_NONE;
            int x = tsk->pro(tsk, NULL, evt_pay);
            Task* cur = tsk;
            tsk = (tsk->awt.nxt == MAR_AWAITS) ? NULL : tsk->awt.nxt;
            mar_awaits_rem(cur);
            if (x != MAR_EVENT_NONE) {
                mar_awaits_add(cur, x);
            }
        } else {
            tsk = (tsk->awt.nxt == MAR_AWAITS) ? NULL : tsk->awt.nxt;
        }
    }
}

#if 0
void mar_awaits_dmp () {
    Task* tsk = MAR_AWAITS;
    while (tsk != NULL) {
        printf("%p <= %p => %p\n", tsk->awt.prv, tsk, tsk->awt.nxt);
        tsk = (tsk->awt.nxt == MAR_AWAITS) ? NULL : tsk->awt.nxt;
    }
}
#endif

// TYPES
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
