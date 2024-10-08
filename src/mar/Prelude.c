typedef enum CEU_EXE_STATUS {
    CEU_EXE_STATUS_YIELDED = 1,
    CEU_EXE_STATUS_TOGGLED,
    CEU_EXE_STATUS_RESUMED,
    CEU_EXE_STATUS_TERMINATED,
} CEU_EXE_STATUS;

typedef struct CEU_Exe {
    void* proto;
    CEU_EXE_STATUS status;
    int pc;
    char mem[0];
} CEU_Exe;

CEU_Exe* ceu_create_exe (void* proto, int mem_n) {
    CEU_Exe* ret = malloc(sizeof(CEU_Exe) + mem_n);
    assert(ret != NULL);
    *ret = (CEU_Exe) { proto, CEU_EXE_STATUS_YIELDED, 0, {} };
    return ret;
}
