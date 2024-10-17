package mar

import java.io.File

fun String.clean (): String {
    return this.replace('*','_')
}

fun Type.Proto.Coro.x_coro_exec (pre: Boolean): Pair<String,String> {
    val tps = (this.inps.to_void() + listOf(this.res,this.yld,this.out)).map { it.coder(pre) }.joinToString("__").clean()
    return Pair("MAR_Coro__$tps", "MAR_Exec__$tps")
}
fun Type.Proto.Coro.x_inp_tup (pre: Boolean): Pair<String, Type.Tuple> {
    val tp = Type.Tuple(this.tk, this.inps, null)
    val id = tp.coder(pre)
    return Pair(id, tp)
}
fun Type.Proto.Coro.x_inp_uni (pre: Boolean): Pair<String, Type.Union> {
    val tup = this.x_inp_tup(pre)
    val tp = Type.Union(this.tk, false, listOf(tup.second, this.res), null)
    val id = tp.coder(pre)
    return Pair(id, tp)
}
fun Type.Proto.Coro.x_out_uni (pre: Boolean): Pair<String, Type.Union> {
    val tp = Type.Union(this.tk, true, listOf(this.yld, this.out), null)
    val id = tp.coder(pre)
    return Pair(id, tp)
}
fun Type.Proto.Coro.x_sig (pre: Boolean, id: String): String {
    val x = this.x_coro_exec(pre).second
    val (xiuni,_) = this.x_inp_uni(pre)
    val (xouni,_) = this.x_out_uni(pre)
    return "$xouni $id ($x* mar_exe, $xiuni mar_arg)"
}

fun Type.Exec.x_exec_coro (pre: Boolean): Pair<String,String> {
    val tps = (this.inps.to_void() + listOf(this.res,this.yld,this.out)).map { it.coder(pre) }.joinToString("__").clean()
    return Pair("MAR_Exec__$tps", "MAR_Coro__$tps")
}
fun Type.Exec.x_inp_tup (pre: Boolean): Pair<String, Type.Tuple> {
    val tp = Type.Tuple(this.tk, this.inps, null)
    val id = tp.coder(pre)
    return Pair(id, tp)
}
fun Type.Exec.x_inp_uni (pre: Boolean): Pair<String, Type.Union> {
    val tup = this.x_inp_tup(pre)
    val tp = Type.Union(this.tk, false, listOf(tup.second, this.res), null)
    val id = tp.coder(pre)
    return Pair(id, tp)
}

fun Var_Type.coder (pre: Boolean = false): String {
    val (id,tp) = this
    return tp.coder(pre) + " " + id.str
}
fun Type.coder (pre: Boolean = false): String {
    return when (this) {
        is Type.Any        -> TODO()
        is Type.Prim      -> this.tk.str
        is Type.Data      -> this.tk.str
        is Type.Unit       -> "_VOID_"
        is Type.Pointer    -> this.ptr.coder(pre) + (this.ptr !is Type.Proto).cond { "*" }
        is Type.Tuple      -> {
            if (this.ids == null) {
                "MAR_Tuple__${this.ts.map { it.coder(pre) }.joinToString("__")}".clean()
            } else {
                "MAR_Tuple__${this.ts.zip(this.ids).map { (tp,id) -> tp.coder(pre)+"_"+id.str }.joinToString("__")}".clean()
            }
        }
        is Type.Union      -> "MAR_Union__${this.ts.map { it.coder(pre) }.joinToString("__")}".clean()
        is Type.Proto.Func -> "MAR_Func__${this.inps.to_void().map { it.coder(pre) }.joinToString("__")}__${this.out.coder(pre)}".clean()
        is Type.Proto.Coro -> this.x_coro_exec(pre).first
        is Type.Exec       -> this.x_exec_coro(pre).first
    }
}

fun coder_types (pre: Boolean): String {
    fun ft (me: Type): List<String> {
        me.coder().let {
            if (G.types.contains(it)) {
                return emptyList()
            } else {
                G.types.add(it)
            }
        }
        return when (me) {
            is Type.Proto.Func -> listOf (
                "typedef ${me.out.coder(pre)} (*${me.coder(pre)}) (${me.inps.map { it.coder(pre) }.joinToString(",")});"
            )
            is Type.Proto.Coro -> {
                val (co,exe) = me.x_coro_exec(pre)
                val (_,itup) = me.x_inp_tup(pre)
                val (xiuni,iuni) = me.x_inp_uni(pre)
                val (xouni,ouni) = me.x_out_uni(pre)
                val x = "struct " + exe
                ft(itup) + ft(iuni) + ft(ouni) + listOf (
                    x + ";\n",
                    "typedef $xouni (*$co) ($x*, $xiuni);\n",
                )
            }
            is Type.Tuple -> {
                val x = me.coder(pre)
                /*val ids = if (me.ids == null) emptyList() else {
                    ft(Type.Tuple(me.tk, me.ts, null))
                }
                ids +*/ listOf("""
                    typedef struct $x {
                        ${if (me.ids == null) {
                            me.ts.mapIndexed { i,tp ->
                                "${tp.coder()} _${i+1};\n"
                            }
                        } else {
                            me.ts.zip(me.ids).mapIndexed { i,tp_id ->
                                val (tp,id) = tp_id
                                """
                                union {
                                    ${tp.coder()} _${i+1};
                                    ${tp.coder()} ${id.str};
                                };                                    
                                """
                            }
                        }.joinToString("")}
                    } $x;
                """)
            }
            is Type.Union -> {
                val x = me.coder(pre)
                listOf("""
                    typedef struct $x {
                        ${me.tagged.cond { "int tag;" }}
                        union {
                            ${me.ts.mapIndexed { i,tp ->
                    tp.coder() + " _" + (i+1) + ";\n"
                }.joinToString("")}
                        };
                    } $x;
                """)
            }
            else -> emptyList()
        }
    }
    fun fs (me: Stmt): List<String> {
        return when (me) {
            is Stmt.Data -> {
                listOf("""
                    typedef ${me.tp.coder(pre)} ${me.id.str};
                """)
            }
            is Stmt.Proto.Coro -> {
                fun mem (): String {
                    val blks = me.dn_collect_pre({
                        when (it) {
                            is Stmt.Proto -> if (it == me) emptyList() else null
                            is Stmt.Block -> listOf(it)
                            else -> emptyList()
                        }
                    }, null, null)
                    return blks.map { it.to_dcls().map { (_,id,tp) -> Pair(id,tp!!).coder(pre) + ";\n" } }.flatten().joinToString("")
                }
                val (co,exe) = me.tp_.x_coro_exec(pre)
                listOf("""
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
    val ts = G.outer!!.dn_collect_pos(::fs, null, ::ft)
    return ts.joinToString("")
}

fun Stmt.coder (pre: Boolean = false): String {
    return when (this) {
        is Stmt.Data  -> ""
        is Stmt.Proto -> {
            when (this) {
                is Stmt.Proto.Func ->
                    this.tp_.out.coder(pre) + " " + this.id.str + " (" + this.tp_.inps_.map { it.coder(pre) }.joinToString(",") + ")"
                is Stmt.Proto.Coro -> this.tp_.x_sig(pre, this.id.str)
            } + """
            {
                ${(this is Stmt.Proto.Coro).cond {
                    this as Stmt.Proto.Coro
                    """                    
                    switch (mar_exe->pc) {
                        case 0:
                            ${this.tp_.inps_.mapIndexed { i,vtp ->
                                vtp.first.coder(this.blk,pre) + " = mar_arg._1._${i+1};\n"
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
                        val (xuni,_) = it.tp_.x_out_uni(pre)
                        "return ($xuni) { .tag=2, ._2=${this.e.coder(pre)} };"
                    }
                    else -> error("impossible case")
                }
            }
        }

        is Stmt.Block  -> {
            """
            {
                ${this.to_dcls().map { (_,id,tp) ->
                    when (tp) {
                        is Type.Proto.Func -> "auto " + tp.out.coder(pre) + " " + id.str + " (" + tp.inps.map { it.coder(pre) }.joinToString(",") + ");\n"
                        is Type.Proto.Coro -> "auto ${tp.x_sig(pre, id.str)};\n"
                        else -> ""
                    }
                }.joinToString("")}
                ${this.ss.map { it.coder(pre) + "\n" }.joinToString("")}
            }
        """
        }
        is Stmt.Dcl -> {
            val in_coro = this.up_first { it is Stmt.Proto } is Stmt.Proto.Coro
            (!in_coro).cond { this.tp!!.coder(pre) + " " + this.id.str + ";" }
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
            val (xuni,_) = tp.x_inp_uni(pre)
            val args = "&$exe, ($xuni) { ._1 = {" + this.args.map { it.coder(pre) }.joinToString(",") + "} }"
            """
            ${this.dst.cond { it.coder(pre) + " = "}} $exe.co($args);
            """
        }
        is Stmt.Resume -> {
            val exe = this.exe.coder(pre)
            val tp = this.exe.type() as Type.Exec
            val (xuni,_) = tp.x_inp_uni(pre)
            val args = "&$exe, ($xuni) { ._2 = ${this.arg.coder(pre)} }"
            """
            ${this.dst.cond { it.coder(pre) + " = "}} $exe.co($args);
            """
        }
        is Stmt.Yield -> {
            val tp = (this.up_first { it is Stmt.Proto.Coro } as Stmt.Proto.Coro).tp_
            val (xuni,_) = tp.x_out_uni(pre)
            """
            mar_exe->pc = ${this.n};
            return ($xuni) { .tag=1, ._1=${this.arg.coder(pre)} };
        case ${this.n}:
            ${(this.dst).cond { """
                ${it.coder(pre)} = mar_arg._2;
            """ }}
        """
        }

        is Stmt.Nat    -> this.tk.str
        is Stmt.Call   -> this.call.coder(pre) + ";"
    }
}

fun Tk.Var.coder (fr: Any, pre: Boolean): String {
    return if (fr.up_first { it is Stmt.Proto } is Stmt.Proto.Coro) {
        "mar_exe->mem.${this.str}"
    } else {
        this.str
    }

}

fun Expr.coder (pre: Boolean = false): String {
    fun String.op_mar_to_c (): String {
        return when (this) {
            "ref" -> "&"
            "deref" -> "*"
            else -> this
        }
    }
    return when (this) {
        is Expr.Uno -> "(" + this.tk.str.op_mar_to_c() + this.e.coder(pre) + ")"
        is Expr.Bin -> "(" + this.e1.coder(pre) + " " + this.tk.str.op_mar_to_c() + " " + this.e2.coder(pre) + ")"
        is Expr.Call -> this.f.coder(pre) + "(" + this.args.map { it.coder(pre) }.joinToString(",") + ")"

        is Expr.Tuple -> "((${this.type().coder(pre)}) { ${this.vs.map { it.coder(pre) }.joinToString(",") } })"
        is Expr.Union -> {
            val i = this.tp!!.disc_to_i(this.idx)!!
            "((${this.type().coder(pre)}) { .tag=$i, ._$i=${this.v.coder(pre) } })"
        }
        is Expr.Field -> {
            val idx = this.idx.toIntOrNull().let {
                if (it == null) this.idx else "_"+it
            }
            "(${this.col.coder(pre)}.$idx)"
        }
        is Expr.Disc  -> {
            val i = (this.col.type().no_data() as Type.Union).disc_to_i(this.idx)!!
            "(${this.col.coder(pre)}._$i)"
        }
        is Expr.Pred  -> {
            val i = (this.col.type().no_data() as Type.Union).disc_to_i(this.idx)!!
            "(${this.col.coder(pre)}.tag == $i)"
        }
        is Expr.Cons  -> "((${this.tk_.str}) ${this.e.coder(pre)})"

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
        typedef char    Char;
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
