typedef CEU_Exe {
    void* proto;
    CEU_EXE_STATUS status;
    int pc;
    char mem[];
} CEU_Exe;


CEU_Exe* ceu_create_exe_coro (void* proto, int mem_n) {
    CEU_Exe* ret = malloc(sizeof(CEU_Exe) + mem_n);
    assert(ret != NULL);
    *ret = (CEU_Exe) { proto, CEU_EXE_STATUS_YIELDED, 0, {} };
    return ret;
}
