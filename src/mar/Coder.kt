package mar

fun Var_Type.coder (pre: Boolean = false): String {
    val (id,tp) = this
    return tp.coder(pre) + " " + id.str
}

fun Type.coder (pre: Boolean = false): String {
    return when (this) {
        is Type.Any -> TODO()
        is Type.Basic -> this.tk.str
        is Type.Unit -> "()"
        is Type.Func -> {
            val inps = if (this is Type.Func.Vars) {
                this.inps_.map { it.to_str(pre) }.joinToString(",")
            } else {
                this.inps.map { it.to_str(pre) }.joinToString(",")
            }
            "func (" + inps + ") -> " + this.out.to_str(pre)
        }
    }
}

fun Stmt.coder (pre: Boolean = false): String {
    return when (this) {
        is Stmt.Func   -> {
            this.tp.out.to_str(pre) + " " + this.tk.str + "(" + this.tp.inps_.map { it.coder(pre) }.joinToString(",") + ") {\n" + this.blk.ss.map { it.coder(pre)+"\n" }.joinToString("") + "}"
        }
        is Stmt.Return -> "return (" + this.e.coder(pre) + ");"
        is Stmt.Block  -> "{\n" + this.vs.filter { (_,tp) -> tp !is Type.Func }.map { (id,tp) -> tp.to_str(pre) + " " + id.str + ";\n" }.joinToString("") + this.ss.map { it.coder(pre) + "\n" }.joinToString("") + "}"
        is Stmt.Set    -> this.dst.coder(pre) + " = " + this.src.coder(pre) + ";"
        is Stmt.Nat    -> this.tk.str
        is Stmt.Call   -> this.call.coder(pre) + ";"
    }
}

fun Expr.coder (pre: Boolean = false): String {
    return when (this) {
        is Expr.Nat -> this.tk.str
        is Expr.Bin -> "(" + this.e1.coder(pre) + " " + this.tk.str + " " + this.e2.coder(pre) + ")"
        is Expr.Call -> this.f.coder(pre) + "(" + this.args.map { it.coder(pre) }.joinToString(",") + ")"
        else -> this.to_str(pre)
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
