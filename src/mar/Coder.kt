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
        //is Type.Err,
        is Type.Any -> TODO()
        is Type.Nat        -> this.tk.str
        is Type.Prim       -> this.tk.str
        is Type.Data       -> this.ts.first().str
        is Type.Unit       -> "_VOID_"
        is Type.Pointer    -> this.ptr.coder(pre) + (this.ptr !is Type.Proto).cond { "*" }
        is Type.Tuple      -> "MAR_Tuple__${this.ts.map { (id,tp) -> tp.coder(pre)+id.cond {"_"+it.str} }.joinToString("__")}".clean()
        is Type.Union      -> "MAR_Union__${this.ts.map { (id,tp) -> tp.coder(pre)+id.cond {"_"+it.str} }.joinToString("__")}".clean()
        is Type.Vector     -> "MAR_Vector__${this.max.cond2({it.toString()},{"0"})}_${this.tp.coder(pre)}".clean()
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
        if (me is Type.Any) {
            return emptyList()
        }
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
            is Type.Vector -> {
                val x = me.coder(pre)
                listOf("""
                    typedef struct $x {
                        int max, cur;
                        ${me.tp.coder(pre)} buf[${me.max.cond2({it.toString()},{""})}];
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
    fun fe (me: Expr): List<String> {
        return when (me) {
            is Expr.Bin -> {
                if (me.tk.str == "++") {
                    val tp = me.typex() as Type.Vector
                    val x = tp.coder(pre)
                    if (G.types.contains(x)) {
                        return emptyList()
                    } else {
                        G.types.add(x)
                    }
                    val y = tp.tp.coder(pre)
                    return listOf("""
                        typedef struct $x {
                            int max, cur;
                            $y buf[${tp.max!!}];
                        } $x;
                    """)
                } else {
                    emptyList()
                }
            }
            else -> emptyList()
        }
    }
    fun fs (me: Stmt): List<String> {
        return when (me) {
            is Stmt.Data -> when {
                (me.xup is Stmt.Data) -> emptyList()
                (me.subs == null) -> {
                    fun f(tp: Type, s: List<String>): List<String> {
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
                                    tp.ts.mapIndexed { i, (id, _) ->
                                        """
                                        MAR_TAG_${SS}_${if (id == null) i else id.str.uppercase()},
                                        """
                                    }.joinToString("")
                                }
                            } MAR_TAGS_$SS;
                        """
                            ) + tp.ts.map { (id, t) ->
                                if (id == null) emptyList() else f(t, s + listOf(id.str))
                            }.flatten()
                        }
                        return listOf(x1) + x2
                    }
                    f(me.tp, listOf(me.t.str))
                }
                else -> {
                    val sup = me.t.str
                    fun f (s: Stmt.Data, sup: String, l: List<Int>): String {
                        val id = sup + "_" + s.t.str.uppercase()
                        //println(listOf(me.tk.pos, me.to_str()))
                        assert(l.size <= 6)
                        var n = 0
                        var k = 25
                        for (i in 0..l.size-1) {
                            n += l[i] shl k
                            k -= 5
                        }
                        return """
                            #define MAR_TAG$id $n
                        """ + s.subs!!.mapIndexed { i,ss ->
                            f(ss, id, l + listOf(i+1))
                        }.joinToString("")
                    }
                    fun g (sup: Pair<String,String>?, s: Stmt.Data, I: Int): String {
                        val cur = sup.cond { it.first+"_" } + s.t.str
                        val tup = s.tp as Type.Tuple
                        val flds = sup.cond { it.second } + tup.ts.mapIndexed { i, id_tp ->
                            val (id,tp) = id_tp
                            """
                            union {
                                ${tp.coder(pre)} _${I+i};
                                ${id.cond { "${tp.coder(pre)} ${it.str};" }}
                            };
                            """
                        }.joinToString("")
                        val subs = s.subs!!.map {
                            g(Pair(cur,flds), it, I+tup.ts.size)
                        }.joinToString("")
                        return """
                            struct {
                                
                                $flds
                            } $cur;
                            $subs
                        """
                    }
                    listOf (f(me, "", listOf(G.datas++)) + """
                        typedef struct ${sup} {
                            union {
                                struct {
                                    int tag;
                                    union {
                                        ${g(null, me, 1)}
                                    };
                                };
                            };
                        } $sup;
                    """)
                }
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
    val ts = G.outer!!.dn_collect_pos(::fs, ::fe, ::ft)
    return ts.joinToString("")
}

fun Stmt.coder (pre: Boolean): String {
    return when (this) {
        is Stmt.Data -> ""
        is Stmt.Proto -> {
            when (this) {
                is Stmt.Proto.Func ->
                    this.tp_.out.coder(pre) + " " + this.id.str + " (" + this.tp_.inps_.map { it.coder(pre) }.joinToString(",") + ")"
                is Stmt.Proto.Coro -> this.tp_.x_sig(pre, this.id.str)
            } + """
            {
                ${this.tp.out.coder(pre)} mar_ret;
                ${(this is Stmt.Proto.Func).cond {
                    this as Stmt.Proto.Func
                    this.tp_.inps_.map { (id,tp) ->
                        if (tp !is Type.Vector) "" else {
                            val xid = id.str
                            """
                            $xid.max = ${tp.max};
                            $xid.cur = MIN($xid.max, $xid.cur);                            
                            """
                        }
                    }.joinToString("")
                }}
                do {
                    ${(this is Stmt.Proto.Coro).cond {
                        this as Stmt.Proto.Coro
                        """                    
                        switch (mar_exe->pc) {
                            case 0:
                                ${this.tp_.inps_.mapIndexed { i,vtp ->
                                    val (id,tp) = vtp
                                    assert(tp !is Type.Vector)
                                    id.coder(this.blk,pre) + " = mar_arg._1._${i+1};\n"                                }.joinToString("")}
                    """ }}
                    ${this.blk.coder(pre)}
                    ${(this is Stmt.Proto.Coro).cond { """
                        }
                    """ }}
                } while (0);
                ${when {
                    (this is Stmt.Proto.Func) -> "return mar_ret;"
                    (this is Stmt.Proto.Coro) -> {
                        val (xuni,_) = this.tp_.x_out_uni(pre)
                        "return ($xuni) { .tag=2, ._2=mar_ret };"
                    }
                    else -> error("impossible case")
                }}
                
            }
            """
        }

        is Stmt.Block  -> {
            val body = this.ss.map {
                it.coder(pre) + "\n"
            }.joinToString("")
            val escs = this.dn_collect_pre({
                // TODO: should consider nested matching do/escape
                when (it) {
                    is Stmt.Proto  -> null
                    is Stmt.Escape -> listOf(it)
                    else -> emptyList()
                }
            }, {null}, {null}).let { !it.isEmpty() }
            """
            { // BLOCK | ${this.dump()}
                ${G.defers[this.n].cond {
                    it.second
                }}
                do {
                    ${this.to_dcls().map { (_,id,tp) ->
                        when (tp) {
                            is Type.Proto.Func -> "auto " + tp.out.coder(pre) + " " + id.str + " (" + tp.inps.map { it.coder(pre) }.joinToString(",") + ");\n"
                            is Type.Proto.Coro -> "auto ${tp.x_sig(pre, id.str)};\n"
                            else -> ""
                        }
                    }.joinToString("")}
                    $body
                } while (0);
                ${G.defers[this.n].cond {
                    it.third
                }}
                if (MAR_EXCEPTION.tag != __MAR_EXCEPTION_NONE__) {
                    continue;
                }
                // TODO: {this.check_aborted("continue")}
                ${escs.cond { """
                    if (MAR_ESCAPE.tag == __MAR_ESCAPE_NONE__) {
                        // no escape
                    ${this.esc.cond { """
                        } else if (mar_sup(MAR_TAG_${it.ts.coder(pre).uppercase()}, MAR_ESCAPE.tag)) {
                            MAR_ESCAPE.tag = __MAR_ESCAPE_NONE__;   // caught escape: go ahead
                            ${(it.ts.first().str == "Break").cond { """
                                goto MAR_LOOP_STOP_${this.xup!!.n};
                            """ }}                        
                    """ }}
                    } else {
                        continue;                               // uncaught escape: propagate up
                    }
                """ }}
            }
        """
        }
        is Stmt.Dcl -> {
            val dcl = if (this.up_first { it is Stmt.Proto } is Stmt.Proto.Coro) "" else {
                this.xtp!!.coder(pre) + " " + this.id.str + ";"
            }
            val ini = this.xtp.let {
                if (it !is Type.Vector) "" else """
                    ${this.id.str}.max = ${it.max};
                    ${this.id.str}.cur = 0;
                """
            }
            dcl + ini
        }
        is Stmt.SetE    -> {
            val dst = this.dst.coder(pre)
            val src = this.src.coder(pre)
            val tdst = this.dst.typex()
            val tsrc = this.src.typex()
            when {
                this.src.let { it is Expr.Yield || it is Expr.MatchT || it is Expr.MatchE } -> {
                    assert(tsrc !is Type.Vector)
                    this.src.coder(pre) + """
                        $dst = mar_${this.src.n};
                    """
                }
                tdst.is_num() -> "$dst = $src;"
                tdst.is_same_of(tsrc) -> "$dst = $src;"
                (tdst !is Type.Vector || tdst.max==null) -> {
                    "$dst = MAR_CAST(${tdst.coder(pre)}, $src);"
                }
                else -> {
                    """
                    {
                        typeof($dst)* mar_$n = &$dst;
                        *mar_$n = MAR_CAST(${tdst.coder(pre)}, $src);
                        mar_$n->max = ${tdst.max};
                        mar_$n->cur = MIN(mar_$n->max, mar_$n->cur);                        
                    }                        
                    """
                }
            }
        }
        is Stmt.SetS    -> {
            val dst = this.dst.coder(pre)
            val src = this.src.coder(pre)
            val tdst = this.dst.typex()
            val tsrc = this.src.typex()
            when {
                this.src.let { it is Expr.Yield || it is Stmt.MatchT || it is Expr.MatchE } -> {
                    assert(tsrc !is Type.Vector)
                    this.src.coder(pre) + """
                        $dst = mar_${this.src.n};
                    """
                }
                tdst.is_num() -> "$dst = $src;"
                tdst.is_same_of(tsrc) -> "$dst = $src;"
                (tdst !is Type.Vector || tdst.max==null) -> {
                    "$dst = MAR_CAST(${tdst.coder(pre)}, $src);"
                }
                else -> {
                    """
                    {
                        typeof($dst)* mar_$n = &$dst;
                        *mar_$n = MAR_CAST(${tdst.coder(pre)}, $src);
                        mar_$n->max = ${tdst.max};
                        mar_$n->cur = MIN(mar_$n->max, mar_$n->cur);                        
                    }                        
                    """
                }
            }
        }

        is Stmt.Escape -> """
            MAR_ESCAPE = MAR_CAST(Escape, ${this.e.coder(pre)});
            continue;            
        """
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
                if (MAR_EXCEPTION.tag == __MAR_EXCEPTION_NONE__) {
                    // no escape
                } else if (
                    ${this.tp.cond2({
                        "mar_sup(MAR_TAG_${it.ts.coder(pre).uppercase()}, MAR_EXCEPTION.tag)"
                    },{
                        "true"
                    })}
                ) {
                    MAR_EXCEPTION.tag = __MAR_EXCEPTION_NONE__;
                } else {
                    continue;
                }
            }
            """
        }

        is Stmt.Create -> {
            val xtp = (this.xup as Stmt.SetS).dst.typex().coder(pre)
            """
            ($xtp) { 0, ${this.co.coder(pre)}, {} };
            """
        }
        is Stmt.Start -> {
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
        is Stmt.Resume -> {
            val exe = this.exe.coder(pre)
            val tp = this.exe.type() as Type.Exec
            val (xuni,_) = tp.x_inp_uni(pre)
            """
            $exe.co (
                &$exe, ($xuni) { ._2 = ${this.arg.coder(pre)} }
            );
            """
        }
        is Stmt.Yield -> {
            val tp = (this.up_first { it is Stmt.Proto.Coro } as Stmt.Proto.Coro).tp_
            val (xuni,_) = tp.x_out_uni(pre)
            """
                mar_exe->pc = ${this.n};
                return ($xuni) { .tag=1, ._1=${this.arg.coder(pre)} };
            case ${this.n}:
                ${this.typex().coder(pre)} mar_$n = mar_arg._2;
            """
        }


        is Stmt.If     -> """
            if (${this.cnd.coder(pre)}) {
                ${this.t.coder(pre)}
            } else {
                ${this.f.coder(pre)}
            }
        """
        is Stmt.Loop   -> """
            // LOOP | ${this.dump()}
            MAR_LOOP_START_${this.n}:
                ${this.blk.coder(pre)}
                goto MAR_LOOP_START_${this.n};
                MAR_LOOP_STOP_${this.n}:
        """
        is Stmt.MatchT -> """
            // MATCH | ${this.dump()}
            switch (${this.tst.coder(pre)}.tag) {
                ${this.cases.map { (tst,e) -> """
                    ${tst.cond2({"case MAR_TAG_${it.ts.coder(pre).uppercase()}"},{"default"})}:
                        ${e.coder(pre)};
                    break;
                """ }.joinToString("")}
            }
        """
        is Stmt.MatchE -> """
            // MATCH | ${this.dump()}
            switch (${this.tst.coder(pre)}) {
                ${this.cases.map { (tst,e) -> """
                    ${tst.cond2({"case ${it.coder(pre)}"},{"default"})}:
                        ${e.coder(pre)};
                    break;
                """ }.joinToString("")}
            }
        """

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
                        "Char"  -> "printf(\"%c\", $v);"
                        "Int"   -> "printf(\"%d\", $v);"
                        "Float" -> "printf(\"%f\", $v);"
                        else -> TODO("2")
                    }
                    is Type.Pointer -> {
                        if (tp.ptr is Type.Prim && tp.ptr.tk.str=="Char") {
                            "printf(\"%s\", $v);"
                        } else {
                            TODO("1")
                        }
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
                    is Type.Vector -> {
                        """
                        {
                            printf("#[");
                            ${tp.coder(pre)} mar_${tp.n} = $v;
                            for (int i=0; i<mar_${tp.n}.cur; i++) {
                                if (i > 0) {
                                    printf(",");
                                }
                                ${aux(tp.tp, "mar_${tp.n}.buf[i]")};
                            }
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
                        val (s,_,tpx) = tp.walk()!!
                        val par = (tpx !is Type.Tuple) && (tpx !is Type.Union) && (tpx !is Type.Unit)
                        val x = if (s.subs == null) aux(tpx, v) else {
                            val tup = tpx as Type.Tuple
                            val ts = tp.ts.coder(pre)
                            """
                            {
                                printf("[");
                                ${tp.ts.first().str} mar_${tp.n} = $v;
                                ${tup.ts.mapIndexed { i,(_,t) ->
                                    aux(t, "mar_${tp.n}.$ts._${i+1}")
                                }.joinToString("printf(\",\");")}
                                printf("]");
                            }                                
                            """
                        }
                        """
                        printf("${tp.ts.to_str(pre)}");
                        ${par.cond2({ "printf(\"(\");" }, { "printf(\" \");" })}
                        $x
                        ${par.cond { "printf(\")\");" }}
                    """
                    }
                    else -> TODO("3")
                }
            }
            aux(this.e.typex(), this.e.coder(pre)) + """
                puts("");
            """
        }
        is Stmt.Pass  -> this.e.coder(pre) + ";"
    }
}

fun Tk.Var.coder (fr: Any, pre: Boolean): String {
    val dcl = this.to_xdcl(fr)!!.first
    return if (dcl.xup!!.up_first { it is Stmt.Proto } is Stmt.Proto.Coro) {
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
        is Expr.Uno -> {
            return when (this.tk.str) {
                "#"  -> "(" + this.e.coder(pre) + ".cur)"
                "##" -> "(" + this.e.coder(pre) + ".max)"
                else -> "(" + this.tk.str.op_mar_to_c() + this.e.coder(pre) + ")"
            }
        }
        is Expr.Bin -> {
            val e1 = this.e1.coder(pre)
            val e2 = this.e2.coder(pre)
            when (this.tk.str) {
                "++" -> {
                    val tp = this.typex() as Type.Vector
                    val one = tp.tp.coder(pre)
                    val xe1 = if (this.e1.typex() is Type.Vector) {
                        "mar_vector_cat_vector((MAR_Vector*)&mar_$n, (MAR_Vector*)&mar_e1_$n, sizeof($one));"
                    } else {
                        "mar_vector_cat_pointer((MAR_Vector*)&mar_$n, mar_e1_$n, strlen(mar_e1_$n), sizeof($one));"
                    }
                    val xe2 = if (this.e2.typex() is Type.Vector) {
                        "mar_vector_cat_vector((MAR_Vector*)&mar_$n, (MAR_Vector*)&mar_e2_$n, sizeof($one));"
                    } else {
                        "mar_vector_cat_pointer((MAR_Vector*)&mar_$n, mar_e2_$n, strlen(mar_e2_$n), sizeof($one));"
                    }
                    """
                    ({
                        ${tp.coder(pre)} mar_$n = { .max=${tp.max}, .cur=0 };
                        typeof($e1) mar_e1_$n = $e1;
                        typeof($e2) mar_e2_$n = $e2;
                        $xe1
                        $xe2
                        /*
                        ${tp.is_str().cond { 
                            """
                            if (mar_$n.cur < mar_$n.max) {
                                mar_$n.buf[mar_$n.cur] = '\0';
                            }
                            """
                         }}
                        */
                        mar_$n;
                    })
                    """                }
                else -> "(" + e1 + " " + this.tk.str.op_mar_to_c() + " " + e2 + ")"
            }
        }
        is Expr.Call -> {
            val inps = this.f.type().let {
                if (it !is Type.Proto) null else {
                    it.inps
                }
            }
            val call = "${this.f.coder(pre)} ( ${this.args.mapIndexed { i,arg ->
                val src = arg.coder(pre)
                val tdst = if (inps == null) null else inps[i]
                val tsrc = arg.typex()
                when {
                    (tdst == null) -> src
                    tdst.is_num() -> src
                    tdst.is_same_of(tsrc) -> src
                    else -> "MAR_CAST(${tdst.coder(pre)}, $src)"
                }
            }.joinToString(",")} )"
            """
            ({
                typeof($call) mar_$n = $call;
                if (MAR_EXCEPTION.tag != __MAR_EXCEPTION_NONE__) {
                    continue;
                }
                mar_$n;
             })
            """
        }

        is Expr.Tuple  -> "((${this.typex().coder(pre)}) { ${this.vs.map { (_,tp) -> "{"+tp.coder(pre)+"}" }.joinToString(",") } })"
        is Expr.Vector -> (this.typex() as Type.Vector).let {
            "((${it.coder(pre)}) { .max=${it.max}, .cur=${it.max}, .buf={${this.vs.map { it.coder(pre) }.joinToString(",") }} })"
        }
        is Expr.Union  -> {
            val (i,_) = this.xtp!!.disc(this.idx)!!
            "((${this.typex().coder(pre)}) { .tag=${i+1}, ._${i+1}=${this.v.coder(pre) } })"
        }
        is Expr.Field  -> {
            val idx = this.idx.toIntOrNull().let {
                if (it == null) this.idx else "_"+it
            }
            val tp = this.col.typex()
            if (tp !is Type.Data) {
                "(${this.col.coder(pre)}.$idx)"
            } else {
                val s = tp.walk()!!.first
                if (s.subs == null) {
                    val sub = tp.ts.drop(1).map { it.str + "." }.joinToString("")
                    "(${this.col.coder(pre)}.$sub$idx)"
                } else {
                    val ts = tp.ts.coder(pre)
                    "(${this.col.coder(pre)}.$ts.$idx)"
                }
            }
        }
        is Expr.Index  -> "${this.col.coder(pre)}.buf[${this.idx.coder(pre)}]"
        is Expr.Disc   -> {
            val tp = this.col.typex()
            val ret = if (tp !is Type.Data) {
                val (i,_) = tp.discx(this.idx)!!
                "${this.col.coder(pre)}._${i+1}"
            } else {
                val s = tp.walk()!!.first
                if (s.subs == null) {
                    val (i,_) = tp.discx(this.idx)!!
                    "${this.col.coder(pre)}._${i+1}"
                } else {
                    this.col.coder(pre)
                }
            }
            """
                // DISC | ${this.dump()}
                ($ret)
            """
        }
        is Expr.Pred   -> {
            val (i,_) = this.col.typex().discx(this.idx)!!
            "(${this.col.coder(pre)}.tag==${i+1})"
        }
        is Expr.Cons   -> {
            val s = this.walk(this.ts)!!.first
            if (s.subs == null) {
                var ret = "({"
                for (i in this.ts.size - 1 downTo 0) {
                    val tp = this.ts.take(i + 1).coder(pre)
                    ret = ret + "$tp ceu_$i = " +
                            if (i == this.ts.size - 1) {
                                """
                                ((${tp}) ${this.e.coder(pre)});
                                """
                            } else {
                                val nxt = this.ts[i + 1].str
                                """
                                {
                                    .tag = MAR_TAG_${tp.uppercase()}_${nxt.uppercase()},
                                    { .$nxt = ceu_${i + 1} }
                                };
                                """
                            }
                }
                ret + " ceu_0; })"
            } else {
                val tup = this.e as Expr.Tuple
                val vs = tup.vs.mapIndexed { i,(id,v) ->
                    "."+(id?.str ?: ("_"+(i+1))) + " = " + v.coder(pre)
                }.joinToString(",")
                val ts = this.ts.coder(pre)
                "((${this.ts.first().str}) { .tag=MAR_TAG_${ts.uppercase()}, .$ts={$vs} })"
            }
        }

        is Expr.Nat -> when {
            (this.tk.str == "mar_ret") -> this.tk.str
            (this.xup is Stmt.Pass)    -> this.tk.str
            (this.xtp is Type.Any)     -> this.tk.str
            (this.xtp is Type.Nat)     -> this.tk.str
            (this.xtp is Type.Prim)    -> this.tk.str
            (this.xtp is Type.Data)    -> "MAR_CAST(${this.xtp!!.coder(pre)}, ${this.tk.str})"
            else -> "((${this.xtp!!.coder(pre)}) ${this.tk.str})"
        }
        is Expr.Acc -> this.tk_.coder(this, pre)
        is Expr.Unit -> "_void_"
        is Expr.Bool, is Expr.Chr, is Expr.Str,
        is Expr.Null -> this.to_str(pre)
        is Expr.Num -> this.to_str(pre).let {
            if (this.xnum == null) it else {
                val tp = this.typex()
                val sup = tp.sup_vs(this.xnum!!)
                if (sup == tp) it else {
                    "((" + sup!!.coder(pre) + ")" + it + ")"
                }
            }
        }

        is Expr.Throw -> """
            ({
                assert(sizeof(Exception) >= sizeof(${this.e.typex().coder(pre)}));
                MAR_EXCEPTION = MAR_CAST(Exception, ${this.e.coder(pre)});
                continue;
                ${this.xtp!!.coder(pre)} mar_$n ; mar_$n;
            })
        """

        is Expr.If -> "((${this.cnd.coder(pre)}) ? (${this.t.coder(pre)}) : (${this.f.coder(pre)}))"
        is Expr.MatchT -> """
            ${this.typex().coder(pre)} mar_$n;
            switch (${this.tst.coder(pre)}.tag) {
                ${this.cases.map { (tst,e) -> """
                    ${tst.cond2({"case MAR_TAG_${it.ts.coder(pre).uppercase()}"},{"default"})}:
                        mar_$n = ${e.coder(pre)};
                    break;
                """ }.joinToString("")}
            }
        """
        is Expr.MatchE -> """
            ${this.typex().coder(pre)} mar_$n;
            switch (${this.tst.coder(pre)}) {
                ${this.cases.map { (tst,e) -> """
                    ${tst.cond2({"case ${it.coder(pre)}"},{"default"})}:
                        mar_$n = ${e.coder(pre)};
                    break;
                """ }.joinToString("")}
            }
        """
    }.let {
        when (this) {
            is Expr.Num, is Expr.MatchT, is Expr.MatchE, is Expr.Yield -> it
            else -> {
                val tp = this.typex()
                when {
                    (this.xnum == null) -> it
                    (this.xnum!!.tk.str == tp.tk.str) -> it
                    else -> {
                        val sup = tp.sup_vs(this.xnum!!)
                        if (sup == tp) it else {
                            "((" + sup!!.coder(pre) + ")" + it + ")"
                        }
                    }
                }
            }
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
        #include <string.h>
        
        #undef MAX
        #undef MIN
        #define MAX(a,b) ({ __typeof__ (a) _a = (a); __typeof__ (b) _b = (b); _a > _b ? _a : _b; })
        #define MIN(a,b) ({ __typeof__ (a) _a = (a); __typeof__ (b) _b = (b); _a < _b ? _a : _b; })

        typedef int     _VOID_;
        typedef int     Bool;
        typedef char    Char;
        typedef float   Float;
        typedef int     Int;
        typedef uint8_t U8;
        
        #define _void_ 0
        #define null   NULL
        #define true   1
        #define false  0
        
        #define MAR_CAST(tp,v) (((union { tp a; typeof(v) b; }) {.b=v}).a)
        
        int mar_sup (uint32_t sup, uint32_t sub) {
            //printf(">>> %X vs %X\n", sup, sub);
            for (int i=5; i>=0; i--) {
                uint32_t xsup = (sup & (0b11111<<(i*5)));
                uint32_t xsub = (sub & (0b11111<<(i*5)));
                //printf("\t[%d/%X] %X vs %X\n", i, (0b11111<<(i*5)), xsup, xsub);
                if (xsup==0 || xsup==xsub) {
                    // ok
                } else {
                    return 0;
                }
            }
            return 1;
        }
        
        ${coder_types(pre)}
        
        #define __MAR_ESCAPE_NONE__  0
        typedef struct Escape {
            int tag;
            char _[100];
        } Escape;
        Escape MAR_ESCAPE = { __MAR_ESCAPE_NONE__ };

        #define __MAR_EXCEPTION_NONE__ 0
        typedef struct Exception {
            int tag;
            char _[100];
        } Exception;
        Exception MAR_EXCEPTION = { __MAR_EXCEPTION_NONE__ };
        
        typedef struct MAR_Vector {
            int max, cur;
            char buf[];
        } MAR_Vector;
        
        void mar_vector_cat_pointer (MAR_Vector* dst, char* src, int len, int size) {
            int n = MIN(dst->max-dst->cur, len);
            memcpy(&dst->buf[dst->cur*size], src, n*size);
            dst->cur += n;
        }
        
        void mar_vector_cat_vector (MAR_Vector* dst, MAR_Vector* src, int size) {
            mar_vector_cat_pointer(dst, src->buf, src->cur, size);
        }
        
        int main (void) {
            do {
                ${G.outer!!.coder(pre)}
            } while (0);
            if (MAR_EXCEPTION.tag != __MAR_EXCEPTION_NONE__) {
                puts("uncaught exception");
            }
        }
    """
}
