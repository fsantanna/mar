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
        is Type.Proto.Func -> "CEU_Func__${this.out.coder(pre)}__${this.inps.to_void().map { it.coder(pre) }.joinToString("__")}"
        is Type.Proto.Coro -> "CEU_Coro__${this.out.coder(pre)}__${this.res.coder(pre)}"
        is Type.XCoro -> "CEU_XCoro__${this.out.coder(pre)}__${this.res.coder(pre)}"
    }
}

fun coder_types_protos (pre: Boolean): String {
    fun ft (me: Type): List<String> {
        return when (me) {
            is Type.Proto.Func -> listOf (
                "typedef ${me.out.coder(pre)} (*${me.coder(pre)}) (${me.inps.map { it.coder(pre) }.joinToString(",")})"
            )
            is Type.Proto.Coro -> listOf (
                "typedef ${me.out.coder(pre)} (*${me.coder(pre)}) (CEUX, ...)"
            )
            else -> emptyList()
        }
    }
    val ts = G.outer!!.dn_collect({ emptyList() }, { emptyList() }, ::ft)
    return ts.asReversed().map {it + ";\n" }.joinToString("")
}
fun coder_types_xcoros (pre: Boolean): String {
    val xs = G.outer!!.dn_filter({it is Stmt.Proto.Coro}, {null}, {null}) as List<Stmt.Proto.Coro>
    return xs.map { X ->
        fun mem (): String {
            val blks = X.dn_collect({
                when (it) {
                    is Stmt.Proto -> if (it == X) emptyList() else null
                    is Stmt.Block -> listOf(it)
                    else -> emptyList()
                }
            }, {null}, {null})
            return blks.map { it.vs.map { it.coder(pre) + ";\n" } }.flatten().joinToString("")
        }
        val x = X.tp.out.coder(pre) + "__" + X.tp_.res.coder(pre)
        """
        typedef struct CEU_XCoro__$x {
            _CEU_Exe_
            CEU_Coro__$x proto;
            struct {
                ${mem()}
            } mem;
        } CEU_XCoro__$x;
    """ }.joinToString("")
}

fun Stmt.coder (pre: Boolean = false): String {
    return when (this) {
        is Stmt.Proto -> {
            when (this) {
                is Stmt.Proto.Func ->
                    this.tp_.out.coder(pre) + " " + this.id.str + " (" + this.tp_.inps__.map { it.coder(pre) }.joinToString(",") + ")"
                is Stmt.Proto.Coro -> {
                    val x = this.tp_.out.coder(pre) + "__" + this.tp_.res.coder(pre)
                    this.tp_.out.coder(pre) + " " + this.id.str + " (CEU_XCoro__$x* ceu_xcoro, ...)"
                }
            } + """
            {
                ${(this is Stmt.Proto.Coro).cond {
                    val tp = this.tp as Type.Proto.Coro.Vars
                    """                    
                    switch (ceu_xcoro->pc) {
                        case 0:
                            ${(tp.inps__.size > 0).cond { """
                                va_list ceu_args;
                                va_start(ceu_args, ceu_xcoro);
                                ${tp.inps__.map { (id,tp) ->
                                    val tpx = tp.coder(pre)
                                    "ceu_xcoro->mem.${id.str} = va_arg(ceu_args, $tpx);"
                                }.joinToString("")}
                                va_end(ceu_args);                                
                            """ }}
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
            val x = xtp.out.coder(pre) + "__" + xtp.res.coder(pre)
            """
            {
                CEU_Coro__$x ceu_coro_$n = (CEU_Coro__$x) ${this.co.coder(pre)};
                CEU_XCoro__$x ceu_xcoro_$n = { CEU_EXE_STATUS_YIELDED, 0, (CEU_Coro__${tp.out.coder(pre)}__${tp.res.coder(pre)}) ceu_coro_$n };
                ceu_coro_$n(${(listOf("&ceu_xcoro_$n") + this.args.map { it.coder(pre) }).joinToString(",")});
                ${this.dst.coder(pre)} = ceu_xcoro_$n;
            }
            """
        }
        is Stmt.Resume -> {
            val xtp = this.xco.type() as Type.XCoro
            val x = xtp.out.coder(pre) + "__" + xtp.res.coder(pre)
            val args = "&ceu_xcoro_$n" + if (this.arg.type() is Type.Unit) "" else ","+this.arg.coder(pre)
            """
            {
                CEU_XCoro__$x ceu_xcoro_$n = ${this.xco.coder(pre)};
                ${this.dst.cond { it.coder(pre) + " = "}} ceu_xcoro_$n.proto($args);
            }
        """
        }
        is Stmt.Yield -> {
            val tp = (this.up_first { it is Stmt.Proto.Coro } as Stmt.Proto.Coro).tp_
            """
            ${(tp.inps__.size > 0).cond { """
                va_list ceu_args;
                va_start(ceu_args, ceu_xcoro);
                ${tp.inps__.map { (id,tp) ->
                    val tpx = tp.coder(pre)
                    "ceu_xcoro->mem.${id.str} = va_arg(ceu_args, $tpx);"
                }.joinToString("")}
                va_end(ceu_args);                                
            """ }}
            """
        }

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
        is Expr.Nat -> {
            if (this.up_first { it is Stmt.Proto } is Stmt.Proto.Coro) {
                "ceu_xcoro->mem.${this.tk.str}"
            } else {
                this.tk.str
            }
        }

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
        ${coder_types_protos(pre)}
        ${coder_types_xcoros(pre)}
        
        int main (void) {
            ${G.outer!!.coder(pre)}
        }
    """
}
