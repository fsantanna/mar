typedef enum CEU_EXE_STATUS {
    CEU_EXE_STATUS_YIELDED = 1,
    CEU_EXE_STATUS_TOGGLED,
    CEU_EXE_STATUS_RESUMED,
    CEU_EXE_STATUS_TERMINATED,
} CEU_EXE_STATUS;

#define _CEU_Exe_           \
    CEU_EXE_STATUS status;  \
    int pc;
