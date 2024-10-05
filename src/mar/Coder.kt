package mar

fun coder_main (): String {
    return """
        #include <stdio.h>
        int main (void) {
            printf("%d\n", ${G.outer!!.to_str()});
        }
    """
}
