package mar

import java.io.File

fun String.coro_to_xcoro (): String {
    assert(this.take(8) == "CEU_Coro")
    return "CEU_XCoro" + this.drop(8)
}

fun Var_Type.coder (pre: Boolean = false): String {
    val (id,tp) = this
    return tp.coder(pre) + " " + id.str
}
fun Type.coder (pre: Boolean = false): String {
    return when (this) {
        is Type.Any        -> TODO()
        is Type.Basic      -> this.tk.str
        is Type.Unit       -> "void"
        is Type.Pointer    -> this.ptr.coder(pre) + (this.ptr !is Type.Proto).cond { "*" }
        is Type.Tuple      -> "CEU_Tuple__${this.ts.map { it.coder(pre) }.joinToString("__")}"
        is Type.Union      -> "CEU_Union__${this.ts.map { it.coder(pre) }.joinToString("__")}"
        is Type.Proto.Func -> "CEU_Func__${this.out.coder(pre)}__${this.inp_.to_void().map { it.coder(pre) }.joinToString("__")}"
        is Type.Proto.Coro -> "CEU_Coro__${this.out.coder(pre)}__${this.inp_.to_void().map { it.coder(pre) }.joinToString("__")}"
        is Type.XCoro      -> "CEU_XCoro__${this.out.coder(pre)}__${this.inps.to_void().map { it.coder(pre) }.joinToString("__")}"
    }
}

fun coder_types (pre: Boolean): String {
    fun fs (me: Stmt): List<String> {
        return when (me) {
            is Stmt.Proto.Coro -> {
                fun mem (): String {
                    val blks = me.dn_collect_pre({
                        when (it) {
                            is Stmt.Proto -> if (it == me) emptyList() else null
                            is Stmt.Block -> listOf(it)
                            else -> emptyList()
                        }
                    }, null, null)
                    return blks.map { it.to_dcls().map { (_,vt) -> vt.coder(pre) + ";\n" } }.flatten().joinToString("")
                }
                val co  = me.tp.coder(pre)
                val xco = co.coro_to_xcoro()
                listOf("""
                    typedef struct $xco {
                        int pc;
                        $co proto;
                        struct {
                            ${mem()}
                        } mem;
                    } $xco;
                """)
            }
            else -> emptyList()
        }
    }
    fun ft (me: Type): List<String> {
        return when (me) {
            is Type.Proto.Func -> listOf (
                "typedef ${me.out.coder(pre)} (*${me.coder(pre)}) (${me.inp_.map { it.coder(pre) }.joinToString(",")})"
            )
            is Type.Proto.Coro -> {
                val xco = "struct " + me.coder(pre).coro_to_xcoro()
                listOf (
                    "typedef ${me.out.coder(pre)} (*${me.coder(pre)}) ($xco* ceu_xco ${me.inp_.map { ","+it.coder(pre) }.joinToString("")})",
                    xco,
                )
            }
            is Type.Tuple -> {
                val x = me.coder(pre)
                listOf("""
                    typedef struct $x {
                        ${me.ts.mapIndexed { i,tp ->
                            tp.coder() + " _" + (i+1) + ";\n"
                        }.joinToString("")}
                    } $x;
                """)
            }
            else -> emptyList()
        }
    }
    val ts = G.outer!!.dn_collect_pre(::fs, null, ::ft)
    return ts.asReversed().map {it + ";\n" }.joinToString("")
}

fun Stmt.coder (pre: Boolean = false): String {
    return when (this) {
        is Stmt.Proto -> {
            when (this) {
                is Stmt.Proto.Func ->
                    this.tp_.out.coder(pre) + " " + this.id.str + " (" + this.tp_.inp__.map { it.coder(pre) }.joinToString(",") + ")"
                is Stmt.Proto.Coro -> {
                    val x = this.tp.coder(pre).coro_to_xcoro()
                    this.tp_.out.coder(pre) + " " + this.id.str + " ($x* ceu_xco ${this.tp_.inp__.map { (_,tp) -> ", " + tp.coder(pre) + " ceu_arg" }.joinToString("")})"
                }
            } + """
            {
                ${(this is Stmt.Proto.Coro).cond {
                    this as Stmt.Proto.Coro
                    """                    
                    switch (ceu_xco->pc) {
                        case 0:
                            ${this.tp_.inp__.take(1).map { (id,_) -> id.coder(this.blk, pre) + " = ceu_arg;\n" }.joinToString("")}
                """ }}
                ${this.blk.ss.map {
                    it.coder(pre) + "\n"
                }.joinToString("")}
                ${(this is Stmt.Proto.Coro).cond { """
                    }
                """ }}
            }
            """
        }
        is Stmt.Return -> "return (" + this.e.coder(pre) + ");"

        is Stmt.Block  -> {
            """
            {
                ${this.to_dcls().map { (_, vt) ->
                    val (id,tp) = vt
                    when (tp) {
                        is Type.Proto.Func -> "auto " + tp.out.coder(pre) + " " + id.str + " (" + tp.inp_.map { it.coder(pre) }.joinToString(",") + ");\n"
                        is Type.Proto.Coro -> "auto " + tp.out.coder(pre) + " " + id.str + " (${tp.coder(pre).coro_to_xcoro()}* ceu_xco" + tp.inp_.map { ", " + it.coder(pre) }.joinToString("") + ");\n"
                        else -> ""
                    }
                }.joinToString("")}
                ${this.ss.map { it.coder(pre) + "\n" }.joinToString("")}
            }
        """
        }
        is Stmt.Dcl -> {
            val (id, tp) = this.var_type
            val in_coro = this.up_first { it is Stmt.Proto } is Stmt.Proto.Coro
            (!in_coro).cond { tp.coder(pre) + " " + id.str + ";" }
        }
        is Stmt.Set    -> this.dst.coder(pre) + " = " + this.src.coder(pre) + ";"

        is Stmt.If     -> """
            if (${this.cnd.coder(pre)}) {
                ${this.t.coder(pre)}
            } else {
                ${this.f.coder(pre)}
            }
        """
        is Stmt.Loop   -> """
            while (1) {
                ${this.blk.coder(pre)}
            }
        """
        is Stmt.Break -> "break;"

        is Stmt.Create -> {
            val xtp = this.dst.type().coder(pre)
            val dst = this.dst.coder(pre)
            """
            $dst = ($xtp) { 0, ${this.co.coder(pre)}, {} };
            """
        }
        is Stmt.Resume -> {
            val xco = this.xco.coder(pre)
            val args = "&$xco" + if (this.arg.type() is Type.Unit) "" else ","+this.arg.coder(pre)
            """
            ${this.dst.cond { it.coder(pre) + " = "}} $xco.proto($args);
            """
        }
        is Stmt.Yield -> """
            ceu_xco->pc = ${this.n};
            return ${this.arg.coder(pre)};
        case ${this.n}:
            ${(this.dst).cond { """
                ${it.coder(pre)} = ceu_arg;
            """ }}
        """

        is Stmt.Nat    -> this.tk.str
        is Stmt.Call   -> this.call.coder(pre) + ";"
    }
}

fun Tk.Var.coder (fr: Any, pre: Boolean): String {
    return if (fr.up_first { it is Stmt.Proto } is Stmt.Proto.Coro) {
        "ceu_xco->mem.${this.str}"
    } else {
        this.str
    }

}

fun Expr.coder (pre: Boolean = false): String {
    fun String.op_ceu_to_c (): String {
        return when (this) {
            "ref" -> "&"
            "deref" -> "*"
            else -> this
        }
    }
    return when (this) {
        is Expr.Uno -> "(" + this.tk.str.op_ceu_to_c() + this.e.coder(pre) + ")"
        is Expr.Bin -> "(" + this.e1.coder(pre) + " " + this.tk.str.op_ceu_to_c() + " " + this.e2.coder(pre) + ")"
        is Expr.Call -> this.f.coder(pre) + "(" + this.args.map { it.coder(pre) }.joinToString(",") + ")"

        is Expr.Tuple -> "(${this.type().coder(pre)}) { ${this.vs.map { it.coder(pre) }.joinToString(",") } }"
        is Expr.Union -> "(${this.type().coder(pre)}) { .${this.idx} ${this.v.coder(pre) } }"
        is Expr.Index -> "(${this.col.coder(pre)}.${this.idx})"
        is Expr.Disc  -> "(${this.col.coder(pre)}!${this.idx})"
        is Expr.Pred  -> "(${this.col.coder(pre)}?${this.idx})"

        is Expr.Nat -> this.tk.str
        is Expr.Acc -> this.tk_.coder(this, pre)
        is Expr.Unit -> ""
        is Expr.Bool, is Expr.Char,
        is Expr.Null, is Expr.Num -> this.to_str(pre)
    }
}

fun coder_main (pre: Boolean): String {
    return """
        #include <assert.h>
        #include <stdint.h>
        #include <stdio.h>
        #include <stdlib.h>
        #include <stdarg.h>
        
        typedef int     Bool;
        typedef int     Int;
        typedef uint8_t U8;
        
        #define null  NULL
        #define true  1
        #define false 0
        
        ${File("src/mar/Prelude.c").readLines().joinToString("\n")}
        
        ${coder_types(pre)}
        
        int main (void) {
            ${G.outer!!.coder(pre)}
        }
    """
}
