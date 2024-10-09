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
        is Type.Proto.Func -> "CEU_Func__${this.out.coder()}__${this.inps.to_void().map { it.coder() }.joinToString("__")}"
        is Type.Proto.Coro -> "CEU_Coro__${this.out.coder()}__${listOf(this.res).pre_ceux(this.res.tk).trim().map { it.coder() }.joinToString("__") }"
        is Type.XCoro -> "CEU_XCoro__${this.out.coder()}__${listOf(this.res).pre_ceux(this.res.tk).trim().map { it.coder() }.joinToString("__") }"
    }
}

fun coder_types_protos (): String {
    fun ft (me: Type): List<String> {
        return when (me) {
            is Type.Proto.Func -> listOf (
                "typedef ${me.out.coder()} (*${me.coder()}) (${me.inps.map { it.coder() }.joinToString(",")})"
            )
            is Type.Proto.Coro -> listOf (
                "typedef ${me.out.coder()} (*${me.coder()}) (CEUX, ...)"
            )
            is Type.XCoro -> ft(Type.Proto.Func(me.tk_, listOf(me.res).pre_ceux(me.tk), me.out))
            else -> emptyList()
        }
    }
    val ts = G.outer!!.dn_collect({ emptyList() }, { emptyList() }, ::ft)
    return ts.asReversed().map {it + ";\n" }.joinToString("")
}
fun coder_types_xcoros (): String {
    val xs = G.outer!!.dn_filter({false}, {false}, { it is Type.XCoro }) as List<Type.XCoro>
    return xs.map {
        val pre = (listOf(it.out.coder()) + listOf(it.res).pre_ceux(it.tk).map { it.coder() }).joinToString("__")
        """
        typedef struct CEU_XCoro__$pre {
            _CEU_Exe_
            CEU_Coro__$pre proto;
            char mem[0];
        } CEU_XCoro__$pre;
    """ }.joinToString("")
}

fun Stmt.coder (pre: Boolean = false): String {
    return when (this) {
        is Stmt.Proto -> {
            when (this) {
                is Stmt.Proto.Func ->
                    this.tp_.out.coder(pre) + " " + this.id.str + " (" + this.tp_.inps__.map { it.coder(pre) }.joinToString(",") + ")"
                is Stmt.Proto.Coro -> {
                    val x = this.tp_.out.coder() + "__" + listOf(this.tp_.res).pre_ceux(this.tp_.res.tk).trim().map { it.coder() }.joinToString("__")
                    this.tp_.out.coder(pre) + " " + this.id.str + " (CEU_XCoro__$x* ceu_xcoro, ...)"
                }
            } + """
            {
                ${(this is Stmt.Proto.Coro).cond {
                    val tp = this.tp as Type.Proto.Coro.Vars
                    """
                    va_list ceu_args;
                    va_start(ceu_args, ceu_xcoro);
                    ${tp.inps__.map { (id,tp) ->
                        val tpx = tp.coder(pre)
                        """
                        $tpx ${id.str} = va_arg(ceu_args, $tpx);
                        """
                    }.joinToString("")}
                    va_end(ceu_args);
                    
                    switch (ceu_xcoro->pc) {
                        case 0:
                            ceu_xcoro->pc++;
                            return;     // first implicit yield
                        case 1:
                """ }}
                ${this.blk.ss.map { it.coder(pre)+"\n" }.joinToString("")}
                ${(this is Stmt.Proto.Coro).cond { """
                    }
                """ }}
            }
            """
        }
        is Stmt.Return -> "return (" + this.e.coder(pre) + ");"
        is Stmt.Block  -> "{\n" + this.vs.filter { (_,tp) -> tp !is Type.Proto }.map { (id,tp) -> tp.coder(pre) + " " + id.str + ";\n" }.joinToString("") + this.ss.map { it.coder(pre) + "\n" }.joinToString("") + "}"
        is Stmt.Set    -> this.dst.coder(pre) + " = " + this.src.coder(pre) + ";"

        is Stmt.Spawn -> {
            val tp = this.co.type() as Type.Proto.Coro
            val xtp = this.dst.type() as Type.XCoro
            val x = xtp.out.coder() + "__" + listOf(xtp.res).pre_ceux(xtp.res.tk).trim().map { it.coder() }.joinToString("__")
            """
            {
                CEU_Coro__$x ceu_coro_$n = (CEU_Coro__$x) ${this.co.coder(pre)};
                CEU_XCoro__$x ceu_xcoro_$n = { CEU_EXE_STATUS_YIELDED, 0, (CEU_Coro__${tp.out.coder(pre)}__${listOf(tp.res).pre_ceux(tp.res.tk).trim().map { it.coder() }.joinToString("__") }) ceu_coro_$n };
                ceu_coro_$n(${(listOf("&ceu_xcoro_$n") + this.args.map { it.coder(pre) }).joinToString(",")});
                ${this.dst.coder(pre)} = ceu_xcoro_$n;
            }
            """
        }
        is Stmt.Resume -> {
            val xtp = this.xco.type() as Type.XCoro
            val x = xtp.out.coder() + "__" + listOf(xtp.res).pre_ceux(xtp.res.tk).trim().map { it.coder() }.joinToString("__")
            """
            {
                CEU_XCoro__$x ceu_xcoro_$n = ${this.xco.coder(pre)};
                ${this.dst.cond { it.coder(pre) + " = "}} ceu_xcoro_$n.proto(&ceu_xcoro_$n);
            }
        """
        }
        is Stmt.Yield -> TODO()

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
        #include <stdarg.h>
        
        typedef void*   CEUX;
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
