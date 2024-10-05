package mar

fun Stmt.coder (pre: Boolean = false): String {
    return when (this) {
        is Stmt.Block -> this.es.coder(pre)
        is Stmt.Dcl   -> this.var_type.let { (id,tp) -> tp.str + " " + id.str + ";" }
        is Stmt.Set   -> this.dst.to_str(pre) + " = " + this.src.to_str(pre) + ";"
        is Stmt.Nat   -> this.tk.str
        is Stmt.Call  -> this.call.to_str(pre) + ";"
        else          -> TODO()
    }
}

fun List<Stmt>.coder (pre: Boolean): String {
    return this.map { it.coder(pre) + "\n" }.joinToString("")
}

fun coder_main (pre: Boolean): String {
    return """
        #include <stdio.h>
        #include <stdint.h>
        
        typedef int     Int;
        typedef uint8_t U8;
        
        int main (void) {
            ${G.outer!!.coder(pre)}
        }
    """
}
