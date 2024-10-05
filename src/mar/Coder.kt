package mar

fun Stmt.coder (pre: Boolean = false): String {
    return when (this) {
        is Stmt.Block -> "{\n" + this.vs.map { (id,tp) -> tp.str + " " + id.str + ";\n" }.joinToString("") + this.ss.map { it.coder(pre) + "\n" }.joinToString("") + "}"
        is Stmt.Set   -> this.dst.to_str(pre) + " = " + this.src.to_str(pre) + ";"
        is Stmt.Nat   -> this.tk.str
        is Stmt.Call  -> this.call.to_str(pre) + ";"
    }
}

fun coder_main (pre: Boolean): String {
    return """
        #include <stdio.h>
        #include <stdint.h>
        
        typedef int     Int;
        typedef uint8_t U8;
        
        #define null  NULL
        #define true  1
        #define false 0
        
        int main (void) {
            ${G.outer!!.coder(pre)}
        }
    """
}
