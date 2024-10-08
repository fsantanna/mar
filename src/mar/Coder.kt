package mar

import java.io.File

fun Var_Type.coder (pre: Boolean = false): String {
    val (id,tp) = this
    return tp.coder(pre) + " " + id.str
}

fun Type.coder (pre: Boolean = false): String {
    return when (this) {
        is Type.Any -> TODO()
        is Type.Basic -> this.tk.str
        is Type.Unit -> "void"
        is Type.Pointer -> this.ptr.coder(pre) + (this.ptr !is Type.Proto).cond { "*" }
        is Type.Proto -> "CEU_Proto_$n"
    }
}

fun Stmt.coder (pre: Boolean = false): String {
    return when (this) {
        is Stmt.Proto -> {
            when (this) {
                is Stmt.Proto.Func ->
                    this.tp_.out.coder(pre) + " " + this.id.str + "(" + this.tp_.inps__.map { it.coder(pre) }.joinToString(",") + ") {\n" + this.blk.ss.map { it.coder(pre)+"\n" }.joinToString("") + "}"
                is Stmt.Proto.Coro ->
                    this.tp_.out.coder(pre) + " " + this.id.str + "(" + this.tp_.inps__.map { it.coder(pre) }.joinToString(",") + ") {\n" + this.blk.ss.map { it.coder(pre)+"\n" }.joinToString("") + "}"
            }
        }
        is Stmt.Return -> "return (" + this.e.coder(pre) + ");"
        is Stmt.Block  -> "{\n" + this.vs.filter { (_,tp) -> tp !is Type.Proto }.map { (id,tp) -> tp.coder(pre) + " " + id.str + ";\n" }.joinToString("") + this.ss.map { it.coder(pre) + "\n" }.joinToString("") + "}"
        is Stmt.Set    -> this.dst.coder(pre) + " = " + this.src.coder(pre) + ";"

        is Stmt.Spawn -> {
            """
            CEU_Exe* ceu_exe_$n = ceu_create_exe(sizeof(CEU_Exe), clo CEU_LEX_X(COMMA ceux->depth-1));
            
        """
        }
        is Stmt.Resume, is Stmt.Yield -> TODO()

        is Stmt.Nat    -> this.tk.str
        is Stmt.Call   -> this.call.coder(pre) + ";"
    }
}

fun Expr.coder (pre: Boolean = false): String {
    fun String.op_ceu_to_c (): String {
        return when (this) {
            "\\" -> "&"
            else -> this
        }
    }
    return when (this) {
        is Expr.Uno -> "(" + this.tk.str.op_ceu_to_c() + this.e.coder(pre) + ")"
        is Expr.Bin -> "(" + this.e1.coder(pre) + " " + this.tk.str.op_ceu_to_c() + " " + this.e2.coder(pre) + ")"
        is Expr.Call -> this.f.coder(pre) + "(" + this.args.map { it.coder(pre) }.joinToString(",") + ")"
        is Expr.Nat -> this.tk.str

        is Expr.Acc, is Expr.Bool, is Expr.Char,
        is Expr.Null, is Expr.Num, is Expr.Unit -> this.to_str(pre)
    }
}

fun coder_types (): String {
    fun ft (me: Type): List<Type.Proto.Func> {
        return when (me) {
            is Type.Proto.Func -> listOf(me)
            else -> emptyList()
        }
    }
    val ts = G.outer!!.dn_collect({ emptyList() }, { emptyList() }, ::ft)
    return ts.reversed().map { "typedef ${it.out_.coder()} (*CEU_Proto_${it.n}) (${it.inps_.map { it.coder() }.joinToString(",")});\n" }.joinToString("")
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
        
        ${File("src/mar/Prelude.c").readLines().joinToString("\n")}
        ${coder_types()}
        
        int main (void) {
            ${G.outer!!.coder(pre)}
        }
    """
}
