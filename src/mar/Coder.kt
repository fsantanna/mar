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
        is Type.Proto -> "CEU_Proto__${this.out.coder()}__${this.inps.map { it.coder() }.joinToString("_")}"
        is Type.XCoro -> TODO() //"CEU_Proto__${this.out.coder()}__${this.inp.coder()}"
    }
}

fun coder_types_protos (): String {
    fun ft (me: Type): List<Type.Proto.Func> {
        return when (me) {
            is Type.Proto.Func -> listOf(me)
            is Type.Proto.Coro -> {
                val res = if (me.res is Type.Unit) emptyList() else listOf(me.res)
                val tp1 = ft(Type.Proto.Func(me.tk_, me.inps, me.out))
                val tp2 = ft(Type.Proto.Func(me.tk_, res, me.out))
                tp1 + tp2
            }
            is Type.XCoro -> ft(Type.Proto.Func(me.tk_, listOf(me.inp), me.out))
            else -> emptyList()
        }
    }
    val ts = G.outer!!.dn_collect({ emptyList() }, { emptyList() }, ::ft)
    return ts.reversed().map { "typedef ${it.out_.coder()} (*${it.coder()}) (${it.inps_.map { it.coder() }.joinToString(",")});\n" }.joinToString("")
}
fun coder_types_xcoros (): String {
    val xs = G.outer!!.dn_filter({false}, {false}, { it is Type.XCoro }) as List<Type.XCoro>
    return xs.map {
        val pre = it.out.coder() + "__" + it.inp.coder()
        """
        typedef struct CEU_Exe__$pre {
            _CEU_Exe_
            CEU_Proto__$pre
            char mem[0];
        } CEU_Exe__$pre;
    """ }.joinToString("")
}

fun Stmt.coder (pre: Boolean = false): String {
    return when (this) {
        is Stmt.Proto -> {
            when (this) {
                is Stmt.Proto.Func ->
                    this.tp_.out.coder(pre) + " " + this.id.str + "(" + this.tp_.inps__.map { it.coder(pre) }.joinToString(",") + ")"
                is Stmt.Proto.Coro ->
                    this.tp_.out.coder(pre) + " " + this.id.str + "(" + this.tp_.inps__.map { it.coder(pre) }.joinToString(",") + ")"
            } + """
            {
                ${(this is Stmt.Proto.Coro).cond { """
                    return;     // first implicit yield
                """ }}
                ${this.blk.ss.map { it.coder(pre)+"\n" }.joinToString("")}
            }
            """
        }
        is Stmt.Return -> "return (" + this.e.coder(pre) + ");"
        is Stmt.Block  -> "{\n" + this.vs.filter { (_,tp) -> tp !is Type.Proto }.map { (id,tp) -> tp.coder(pre) + " " + id.str + ";\n" }.joinToString("") + this.ss.map { it.coder(pre) + "\n" }.joinToString("") + "}"
        is Stmt.Set    -> this.dst.coder(pre) + " = " + this.src.coder(pre) + ";"

        is Stmt.Spawn -> {
            val tp = this.dst.type() as Type.XCoro
            val x = tp.out.coder() + "__" + tp.inp.coder()
            """
            CEU_Exe_$x exe = { CEU_EXE_STATUS_YIELDED, 0, ${this.co.coder(pre)} };
            exe.proto(&exe, ${this.args.map { it.coder(pre) }.joinToString(",")});
            ${this.dst.coder(pre)} = exe;
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

fun coder_main (pre: Boolean): String {
    return """
        #include <assert.h>
        #include <stdint.h>
        #include <stdio.h>
        #include <stdlib.h>
        
        typedef int     Int;
        typedef uint8_t U8;
        
        #define null  NULL
        #define true  1
        #define false 0
        
        ${File("src/mar/Prelude.c").readLines().joinToString("\n")}
        ${coder_types_protos()}
        ${coder_types_xcoros()}
        
        int main (void) {
            ${G.outer!!.coder(pre)}
        }
    """
}
