package mar

fun String.clean (): String {
    return this.replace('*','_')
}

fun Type.Proto.Coro.x_coro_exec (pre: Boolean): Pair<String,String> {
    val tps = (this.inps.to_void() + listOf(this.res,this.yld,this.out)).map { it.coder(pre) }.joinToString("__").clean()
    return Pair("MAR_Coro__$tps", "MAR_Exec__$tps")
}
fun Type.Proto.Coro.x_inp_tup (pre: Boolean): Pair<String, Type.Tuple> {
    val tp = Type.Tuple(this.tk, this.inps.map { Pair(null,it) })
    val id = tp.coder(pre)
    return Pair(id, tp)
}
fun Type.Proto.Coro.x_inp_uni (pre: Boolean): Pair<String, Type.Union> {
    val tup = this.x_inp_tup(pre)
    val tp = Type.Union(this.tk, false, listOf(tup.second, this.res).map { Pair(null,it) })
    val id = tp.coder(pre)
    return Pair(id, tp)
}
fun Type.Proto.Coro.x_out_uni (pre: Boolean): Pair<String, Type.Union> {
    val tp = Type.Union(this.tk, true, listOf(this.yld, this.out).map { Pair(null,it) })
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
    val tp = Type.Tuple(this.tk, this.inps.map { Pair(null,it) })
    val id = tp.coder(pre)
    return Pair(id, tp)
}
fun Type.Exec.x_inp_uni (pre: Boolean): Pair<String, Type.Union> {
    val tup = this.x_inp_tup(pre)
    val tp = Type.Union(this.tk, false, mutableListOf(tup.second, this.res).map { Pair(null,it) })
    val id = tp.coder(pre)
    return Pair(id, tp)
}

fun Var_Type.coder (pre: Boolean): String {
    val (id,tp) = this
    return tp.coder(pre) + " " + id.str
}
fun Type.coder (pre: Boolean): String {
    return when (this) {
        is Type.Any        -> TODO()
        is Type.Prim      -> this.tk.str
        is Type.Data      -> {
            if (this.to_flat_hier() is Stmt.Flat) {
                this.ts.first().str
            } else {
                this.ts.coder(pre)
            }
        }
        is Type.Unit       -> "_VOID_"
        is Type.Pointer    -> this.ptr.coder(pre) + (this.ptr !is Type.Proto).cond { "*" }
        is Type.Tuple      -> "MAR_Tuple__${this.ts.map { (id,tp) -> tp.coder(pre)+id.cond {"_"+it.str} }.joinToString("__")}".clean()
        is Type.Union      -> "MAR_Union__${this.ts.map { (_,tp) -> tp.coder(pre) }.joinToString("__")}".clean()
        is Type.Proto.Func -> "MAR_Func__${this.inps.to_void().map { it.coder(pre) }.joinToString("__")}__${this.out.coder(pre)}".clean()
        is Type.Proto.Coro -> this.x_coro_exec(pre).first
        is Type.Exec       -> this.x_exec_coro(pre).first
    }
}

fun List<Tk.Type>.coder (pre: Boolean): String {
    return this.map { it.str }.joinToString("_")
}

fun coder_types (pre: Boolean): String {
    fun ft (me: Type): List<String> {
        me.coder(pre).let {
            if (G.types.contains(it)) {
                return emptyList()
            } else {
                G.types.add(it)
            }
        }
        return when (me) {
            is Type.Proto.Func -> listOf (
                "typedef ${me.out.coder(pre)} (*${me.coder(pre)}) (${me.inps.map { it.coder(pre) }.joinToString(",")});\n"
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
                        ${me.ts.mapIndexed { i,id_tp ->
                            val (id,tp) = id_tp
                            """
                            union {
                                ${tp.coder(pre)} _${i+1};
                                ${id.cond { "${tp.coder(pre)} ${it.str};" }}
                            };                                    
                            """
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
                            ${me.ts.mapIndexed { i,id_tp ->
                                val (id,tp) = id_tp
                                id.cond { tp.coder(pre) + " " + it.str + ";\n" } +
                                tp.coder(pre) + " _" + (i+1) + ";\n"
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
            is Stmt.Flat -> {
                fun f (tp: Type, s: List<String>): List<String> {
                    val ss = s.joinToString("_")
                    val SS = ss.uppercase()
                    //println(listOf(s, tp.to_str()))
                    val x1 = "typedef ${tp.coder(pre)} $ss;\n"
                    val x2 = if (tp !is Type.Union) {
                        emptyList()
                    } else {
                        listOf(
                            """
                            typedef enum MAR_TAGS_$SS {
                                __MAR_TAG_${SS}__,
                                ${
                                    tp.ts.mapIndexed { i,(id,_) ->
                                        """
                                        MAR_TAG_${SS}_${if (id==null) i else id.str.uppercase()},
                                        """
                                    }.joinToString("")
                                }
                            } MAR_TAGS_$SS;
                            """
                        ) + tp.ts.map { (id,t) ->
                            if (id == null) emptyList() else f(t,s+listOf(id.str))
                        }.flatten()
                    }
                    return listOf(x1) + x2
                }
                f(me.tp, listOf(me.t.str))
            }
            is Stmt.Hier -> {
                listOf("""
                    typedef struct ${me.ts.coder(pre)} {
                        int tag;
                        ${me.xtp!!.coder(pre)} tup;
                    } ${me.ts.coder(pre)};
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
                    }, {null}, {null})
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
    val ts = G.outer!!.dn_collect_pos(::fs, {emptyList()}, ::ft)
    return ts.joinToString("")
}

fun Stmt.coder (pre: Boolean): String {
    return when (this) {
        is Stmt.Flat, is Stmt.Hier -> ""
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
            val body = this.ss.map {
                it.coder(pre) + "\n"
            }.joinToString("")
            """
            { // BLOCK | ${this.dump()}
                ${this.to_dcls().map { (_,id,tp) ->
                    when (tp) {
                        is Type.Proto.Func -> "auto " + tp.out.coder(pre) + " " + id.str + " (" + tp.inps.map { it.coder(pre) }.joinToString(",") + ");\n"
                        is Type.Proto.Coro -> "auto ${tp.x_sig(pre, id.str)};\n"
                        else -> ""
                    }
                }.joinToString("")}
                ${G.defers[this.n].cond {
                    it.second
                }}
                $body
                ${G.defers[this.n].cond {
                    it.third
                }}
            }
        """
        }
        is Stmt.Dcl -> {
            val in_coro = this.up_first { it is Stmt.Proto } is Stmt.Proto.Coro
            (!in_coro).cond { this.xtp!!.coder(pre) + " " + this.id.str + ";" }
        }
        is Stmt.Set    -> {
            if (this.src is Expr.Yield) {
                this.src.coder(pre)
            } else {
                val dst = this.dst.type()
                if (this.src.type().is_sup_of(dst)) {
                    this.dst.coder(pre) + " = " + this.src.coder(pre) + ";"
                } else {
                    this.dst.coder(pre) + " = MAR_CAST(${dst.coder(pre)}, " + this.src.coder(pre) + ");"
                }
            }
        }

        is Stmt.Defer -> {
            val bup = this.up_first { it is Stmt.Block } as Stmt.Block
            val (ns,ini,end) = G.defers.getOrDefault(bup.n, Triple(mutableListOf(),"",""))
            val id = "mar_defer_$n"
            val inix = """
                int $id = 0;   // not yet reached
            """
            val endx = """
                if ($id) {     // if true: reached, finalize
                    ${this.blk.coder(pre)}
                }
            """
            ns.add(n)
            G.defers[bup.n] = Triple(ns, ini+inix, endx+end)
            """
            $id = 1;   // now reached
            """
        }
        is Stmt.Catch -> {
            """
            { // CATCH | ${this.dump()}
                do {
                    ${this.blk.coder(pre)}
                } while (0);
                if (MAR_EXCEPTION.tag == MAR_TAG_EXCEPTION_NONE) {
                    // no escape
                } else if (MAR_EXCEPTION.tag == MAR_TAG_EXCEPTION_${this.xtp!!.coder(pre).uppercase()}) {
                    MAR_EXCEPTION.tag = MAR_TAG_EXCEPTION_NONE;
                } else {
                    continue;
                }
            }
            """
        }
        is Stmt.Throw -> TODO()

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

        is Stmt.Print  -> {
            fun aux (tp: Type, v: String): String {
                return when (tp) {
                    is Type.Unit -> "printf(\"()\");"
                    is Type.Prim -> when (tp.tk_.str) {
                        "Bool" -> """
                            if ($v) {
                                printf("true");
                            } else {
                                printf("false");
                            }
                        """
                        "Int"  -> "printf(\"%d\", $v);"
                        else -> TODO()
                    }
                    is Type.Tuple -> {
                        """
                        {
                            printf("[");
                            ${tp.coder(pre)} mar_${tp.n} = $v;
                            ${tp.ts.mapIndexed { i,(_,t) ->
                                aux(t, "mar_${tp.n}._${i+1}")
                            }.joinToString("printf(\",\");")}
                            printf("]");
                        }
                        """
                    }
                    is Type.Union -> {
                        """
                        {
                            ${tp.coder(pre)} mar_${tp.n} = $v;
                            printf("<.%d=", mar_${tp.n}.tag);
                            switch (mar_${tp.n}.tag) {
                                ${tp.ts.mapIndexed { i,(_,t) ->
                                    """
                                    case ${i+1}:
                                        ${aux(t, "mar_${tp.n}._${i+1}")}
                                        break;
                                    """
                                }.joinToString("printf(\",\");")}
                            }
                            printf(">");
                        }
                        """
                    }
                    is Type.Data -> {
                        val s = tp.to_flat_hier()
                        val tpx = tp.no_data()!!
                        val par = (tpx !is Type.Tuple) && (tpx !is Type.Union) && (tpx !is Type.Unit)
                        """
                        printf("${tp.ts.to_str(pre)}");
                        ${par.cond2({ "printf(\"(\");" }, { "printf(\" \");" })}
                        ${aux(tpx, v + (s is Stmt.Hier).cond { ".tup" })}
                        ${par.cond { "printf(\")\");" }}
                    """
                    }
                    else -> TODO()
                }
            }
            aux(this.e.type(), this.e.coder(pre)) + """
                puts("");
            """
        }
        is Stmt.XExpr  -> this.e.coder(pre) + ";"
        is Stmt.Nat    -> this.tk.str + ";"
    }
}

fun Tk.Var.coder (fr: Any, pre: Boolean): String {
    return if (fr.up_first { it is Stmt.Proto } is Stmt.Proto.Coro) {
        "mar_exe->mem.${this.str}"
    } else {
        this.str
    }

}

fun Expr.coder (pre: Boolean): String {
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

        is Expr.Tuple -> "((${this.type().coder(pre)}) { ${this.vs.map { (_,tp) -> tp.coder(pre) }.joinToString(",") } })"
        is Expr.Union -> {
            val (i,_) = this.xtp!!.disc(this.idx)!!
            "((${this.type().coder(pre)}) { .tag=${i+1}, ._${i+1}=${this.v.coder(pre) } })"
        }
        is Expr.Field -> {
            val idx = this.idx.toIntOrNull().let {
                if (it == null) this.idx else "_"+it
            }
            val tp = this.col.type()
            if (tp !is Type.Data) {
                "(${this.col.coder(pre)}.$idx)"
            } else {
                val s = tp.to_flat_hier()
                when (s) {
                    is Stmt.Flat -> {
                        val sub = tp.ts.drop(1).map { it.str + "." }.joinToString("")
                        "(${this.col.coder(pre)}.$sub$idx)"
                    }
                    is Stmt.Hier -> "(${this.col.coder(pre)}.tup.$idx)"
                    else -> error("impossible case")
                }
            }
        }
        is Expr.Disc  -> {
            val tp = this.col.type()
            if (tp !is Type.Data) {
                val (i,_) = tp.discx(this.idx)!!
                """
                // DISC | ${this.dump()}
                (${this.col.coder(pre)}._${i+1})
                """
            } else {
                val s = tp.to_flat_hier()
                when (s) {
                    is Stmt.Flat -> {
                        val (i,_) = tp.discx(this.idx)!!
                        """
                        // DISC | ${this.dump()}
                        (${this.col.coder(pre)}._${i+1})
                        """
                    }
                    is Stmt.Hier -> {
                        """
                        // DISC | ${this.dump()}
                        MAR_CAST(${tp.coder(pre)}_${this.idx}, ${this.col.coder(pre)})
                        """
                    }
                    else -> error("impossible case")
                }
            }
        }
        is Expr.Pred  -> {
            val (i,_) = this.col.type().discx(this.idx)!!
            "(${this.col.coder(pre)}.tag==${i+1})"
        }
        is Expr.Cons  -> {
            val st = this.dat.to_flat_hier()
            when (st) {
                is Stmt.Flat -> {
                    var ret = "({"
                    for (i in this.dat.ts.size - 1 downTo 0) {
                        val tp = this.dat.ts.take(i + 1).coder(pre)
                        ret = ret + "$tp ceu_$i = " +
                                if (i == this.dat.ts.size - 1) {
                                    """
                                    ((${tp}) ${this.e.coder(pre)});
                                    """
                                } else {
                                    val nxt = this.dat.ts[i + 1].str
                                    """
                                    {
                                        .tag = MAR_TAG_${tp.uppercase()}_${nxt.uppercase()},
                                        { .$nxt = ceu_${i + 1} }
                                    };
                                    """
                                }
                    }
                    ret + " ceu_0; })"
                }
                is Stmt.Hier -> {
                    "((${this.dat.coder(pre)}) { 99, ${this.e.coder(pre)} })"
                }
                else -> error("impossible case")
            }
        }

        is Expr.Nat -> if (this.xtp == null) this.tk.str else {
            "MAR_CAST(${this.xtp!!.coder(pre)}, ${this.tk.str})"
        }
        is Expr.Acc -> this.tk_.coder(this, pre)
        is Expr.Unit -> "_void_"
        is Expr.Bool, is Expr.Char,
        is Expr.Null, is Expr.Num -> this.to_str(pre)

        is Expr.Create -> {
            val xtp = (this.xup as Stmt.Set).dst.type().coder(pre)
            """
            ($xtp) { 0, ${this.co.coder(pre)}, {} };
            """
        }
        is Expr.Start -> {
            val exe = this.exe.coder(pre)
            val tp = this.exe.type() as Type.Exec
            val (xuni,_) = tp.x_inp_uni(pre)
            """
            $exe.co (
                &$exe, ($xuni) { ._1 = {
                    ${this.args.map { it.coder(pre) }.joinToString(",")}
                } }
            );
            """
        }
        is Expr.Resume -> {
            val exe = this.exe.coder(pre)
            val tp = this.exe.type() as Type.Exec
            val (xuni,_) = tp.x_inp_uni(pre)
            """
            $exe.co (
                &$exe, ($xuni) { ._2 = ${this.arg.coder(pre)} }
            );
            """
        }
        is Expr.Yield -> {
            val tp = (this.up_first { it is Stmt.Proto.Coro } as Stmt.Proto.Coro).tp_
            val (xuni,_) = tp.x_out_uni(pre)
            """
            mar_exe->pc = ${this.n};
            return ($xuni) { .tag=1, ._1=${this.arg.coder(pre)} };
        case ${this.n}:
            ${(this.xup.let { if (it is Stmt.Set) it.dst else null }).cond { """
                ${it.coder(pre)} = mar_arg._2;
            """ }}
        """
        }
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
        
        #define MAR_CAST(tp,v) (*(tp*)({typeof(v) x=v; &x;}))
        
        ${coder_types(pre)}
        
        Exception MAR_EXCEPTION = { MAR_TAG_EXCEPTION_NONE, {} };

        int main (void) {
            ${G.outer!!.coder(pre)}
        }
    """
}
