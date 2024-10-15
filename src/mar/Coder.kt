package mar

import java.io.File

fun String.coro_to_exec (): String {
    assert(this.take(8) == "CEU_Coro")
    return "CEU_Exec" + this.drop(8)
}

fun Var_Type.coder (pre: Boolean = false): String {
    val (id,tp) = this
    return tp.coder(pre) + " " + id.str
}
fun Type.coder (pre: Boolean = false): String {
    fun String.clean (): String {
        return this.replace('*','_')
    }
    return when (this) {
        is Type.Any        -> TODO()
        is Type.Basic      -> this.tk.str
        is Type.Unit       -> "_VOID_"
        is Type.Pointer    -> this.ptr.coder(pre) + (this.ptr !is Type.Proto).cond { "*" }
        is Type.Tuple      -> "CEU_Tuple__${this.ts.map { it.coder(pre) }.joinToString("__")}".clean()
        is Type.Union      -> "CEU_Union__${this.ts.map { it.coder(pre) }.joinToString("__")}".clean()
        is Type.Proto.Func -> "CEU_Func__${this.inps_.to_void().map { it.coder(pre) }.joinToString("__")}__${this.out.coder(pre)}".clean()
        is Type.Proto.Coro -> {
            val tps = (this.inps.to_void() + listOf(this.res,this.yld,this.out)).map { it.coder(pre) }.joinToString("__")
            "CEU_Coro__$tps".clean()
        }
        is Type.Exec       -> {
            val tps = (this.inps.to_void() + listOf(this.res,this.yld,this.out)).map { it.coder(pre) }.joinToString("__")
            "CEU_Exec__$tps".clean()
        }
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
                val exe = co.coro_to_exec()
                val itup = Type.Tuple(me.tp.tk,me.tp.inps)
                val itupx = itup.coder(pre)
                val iuni = Type.Union(me.tp.tk, listOf(itup, me.tp_.res))
                val iunix = iuni.coder(pre)
                val ouni = Type.Union(me.tp.tk, listOf(me.tp_.yld, me.tp_.out))
                val ounix = ouni.coder(pre)
                listOf("""
                    #ifndef __${itupx}__
                    typedef struct $itupx {
                        ${itup.ts.mapIndexed { i, tp ->
                            tp.coder(pre) + " _" + (i+1) + ";\n"
                        }.joinToString("")}
                    } $itupx;
                    #define __${itupx}__
                    #endif

                    #ifndef __${iunix}__
                    typedef union $iunix {
                        ${iuni.ts.mapIndexed { i, tp ->
                            tp.coder(pre) + " _" + (i+1) + ";\n"
                        }.joinToString("")}
                    } $iunix;
                    #define __${iunix}__
                    #endif
 
                    #ifndef __${ounix}__
                    typedef struct $ounix {
                        int tag;
                        union {
                            ${ouni.ts.mapIndexed { i, tp ->
                                tp.coder(pre) + " _" + (i+1) + ";\n"
                            }.joinToString("")}
                        };
                    } $ounix;
                    #define __${ounix}__
                    #endif
 
                    typedef struct $exe {
                        int pc;
                        $co co;
                        struct {
                            ${mem()}
                        } mem;
                    } $exe;
                """)
            }
            else -> emptyList()
        }
    }
    fun ft (me: Type): List<String> {
        return when (me) {
            is Type.Proto.Func -> listOf (
                "typedef ${me.out.coder(pre)} (*${me.coder(pre)}) (${me.inps_.map { it.coder(pre) }.joinToString(",")})"
            )
            is Type.Proto.Coro -> {
                val x = "struct " + me.coder(pre).coro_to_exec()
                val itup = Type.Tuple(me.tk,me.inps)
                val iuni = Type.Union(me.tk, listOf(itup, me.res))
                val ouni = Type.Union(me.tk, listOf(me.yld, me.out))
                ft(itup) + ft(iuni) + ft(ouni) + listOf (
                    x,
                    "typedef ${ouni.coder(pre)} (*${me.coder(pre)}) ($x*, ${iuni.coder(pre)})",
                )
            }
            is Type.Tuple -> {
                val x = me.coder(pre)
                listOf("""
                    #ifndef __${x}__
                    #define __${x}__
                    typedef struct $x {
                        ${me.ts.mapIndexed { i,tp ->
                            tp.coder() + " _" + (i+1) + ";\n"
                        }.joinToString("")}
                    } $x;
                    #endif
                """)
            }
            is Type.Union -> {
                val x = me.coder(pre)
                listOf("""
                    #ifndef __${x}__
                    #define __${x}__
                    typedef struct $x {
                        int tag;
                        union {
                            ${me.ts.mapIndexed { i,tp ->
                            tp.coder() + " _" + (i+1) + ";\n"
                        }.joinToString("")}
                        };
                    } $x;
                    #endif
                """)
            }
            else -> emptyList()
        }
    }
    val ts = G.outer!!.dn_collect_pos(::fs, null, ::ft)
    return ts.map {it + ";\n" }.joinToString("")
}

fun Stmt.coder (pre: Boolean = false): String {
    return when (this) {
        is Stmt.Proto -> {
            when (this) {
                is Stmt.Proto.Func ->
                    this.tp_.out.coder(pre) + " " + this.id.str + " (" + this.tp_.inps__.map { it.coder(pre) }.joinToString(",") + ")"
                is Stmt.Proto.Coro -> {
                    val x = this.tp.coder(pre).coro_to_exec()
                    val iuni = Type.Union(this.tk, listOf(Type.Tuple(this.tk,this.tp.inps), this.tp_.res))
                    val ouni = Type.Union(this.tk, listOf(this.tp_.yld, this.tp_.out))
                    ouni.coder(pre) + " " + this.id.str + " ($x* ceu_exe, ${iuni.coder(pre)} ceu_arg)"
                }
            } + """
            {
                ${(this is Stmt.Proto.Coro).cond {
                    this as Stmt.Proto.Coro
                    """                    
                    switch (ceu_exe->pc) {
                        case 0:
                            ${this.tp_.inps__.mapIndexed { i,vtp ->
                                vtp.first.coder(this.blk,pre) + " = ceu_arg._1._${i+1};\n"
                            }.joinToString("")}
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
        is Stmt.Return -> {
            this.up_first { it is Stmt.Proto.Func || it is Stmt.Proto.Coro}.let {
                when {
                    (it is Stmt.Proto.Func) -> "return (" + this.e.coder(pre) + ");"
                    (it is Stmt.Proto.Coro) -> {
                        val uni = Type.Union(it.tp.tk, listOf(it.tp_.yld, it.tp_.out))
                        "return (${uni.coder(pre)}) { .tag=2, ._2=${this.e.coder(pre)} };"
                    }
                    else -> error("impossible case")
                }
            }
        }

        is Stmt.Block  -> {
            """
            {
                ${this.to_dcls().map { (_, vt) ->
                    val (id,tp) = vt
                    when (tp) {
                        is Type.Proto.Func -> "auto " + tp.out.coder(pre) + " " + id.str + " (" + tp.inps_.map { it.coder(pre) }.joinToString(",") + ");\n"
                        is Type.Proto.Coro -> {
                            val iuni = Type.Union(tp.tk, listOf(Type.Tuple(tp.tk,tp.inps), tp.res))
                            val ouni = Type.Union(tp.tk, listOf(tp.yld, tp.out))
                            "auto " + ouni.coder(pre) + " " + id.str + " (${
                                tp.coder(pre).coro_to_exec()
                            }*, " + iuni.coder(pre) + ");\n"
                        }    
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
        is Stmt.Start -> {
            val exe = this.exe.coder(pre)
            val tp = this.exe.type() as Type.Exec
            val uni = Type.Union(tp.tk, listOf(Type.Tuple(tp.tk, tp.inps), tp.res))
            val args = "&$exe, (${uni.coder(pre)}) { ._1 = {" + this.args.map { it.coder(pre) }.joinToString(",") + "} }"
            """
            ${this.dst.cond { it.coder(pre) + " = "}} $exe.co($args);
            """
        }
        is Stmt.Resume -> {
            val exe = this.exe.coder(pre)
            val tp = this.exe.type() as Type.Exec
            val uni = Type.Union(tp.tk, listOf(Type.Tuple(tp.tk, tp.inps), tp.res))
            val args = "&$exe, (${uni.coder(pre)}) { ._2 = ${this.arg.coder(pre)} }"
            """
            ${this.dst.cond { it.coder(pre) + " = "}} $exe.co($args);
            """
        }
        is Stmt.Yield -> {
            val tp = (this.up_first { it is Stmt.Proto.Coro } as Stmt.Proto.Coro).tp_
            val uni = Type.Union(tp.tk, listOf(tp.yld, tp.out))
            """
            ceu_exe->pc = ${this.n};
            return (${uni.coder(pre)}) { .tag=1, ._1=${this.arg.coder(pre)} };
        case ${this.n}:
            ${(this.dst).cond { """
                ${it.coder(pre)} = ceu_arg._2;
            """ }}
        """
        }

        is Stmt.Nat    -> this.tk.str
        is Stmt.Call   -> this.call.coder(pre) + ";"
    }
}

fun Tk.Var.coder (fr: Any, pre: Boolean): String {
    return if (fr.up_first { it is Stmt.Proto } is Stmt.Proto.Coro) {
        "ceu_exe->mem.${this.str}"
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
        is Expr.Union -> "(${this.type().coder(pre)}) { .tag=${this.idx}, ._${this.idx}=${this.v.coder(pre) } }"
        is Expr.Index -> "(${this.col.coder(pre)}._${this.idx})"
        is Expr.Disc  -> "(${this.col.coder(pre)}._${this.idx})"
        is Expr.Pred  -> "(${this.col.coder(pre)}.tag == ${this.idx})"

        is Expr.Nat -> this.tk.str
        is Expr.Acc -> this.tk_.coder(this, pre)
        is Expr.Unit -> "_void_"
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
        
        typedef int     _VOID_;
        typedef int     Bool;
        typedef int     Int;
        typedef uint8_t U8;
        
        #define _void_ 0
        #define null   NULL
        #define true   1
        #define false  0
        
        ${File("src/mar/Prelude.c").readLines().joinToString("\n")}
        
        ${coder_types(pre)}
        
        int main (void) {
            ${G.outer!!.coder(pre)}
        }
    """
}
