package mar

fun String.clean (): String {
    return this.replace('*','_')
}

fun Type.Proto.Coro.x_coro_exec (tpl: Tpl_Map?): Pair<String,String> {
    val tps = (this.inps.to_void() + listOf(this.res,this.yld,this.out)).map { it.coder(tpl) }.joinToString("__").clean()
    return Pair("Coro__$tps", "Exec__$tps")
}
fun Type.Proto.Coro.x_inp_tup (tpl: Tpl_Map?, pre: Boolean): Pair<String, Type.Tuple> {
    val tp = Type.Tuple(this.tk, this.inps.map { Pair(null,it) })
    val id = tp.coder(tpl)
    return Pair(id, tp)
}
fun Type.Proto.Coro.x_inp_uni (tpl: Tpl_Map?, pre: Boolean): Pair<String, Type.Union> {
    val tup = this.x_inp_tup(tpl,pre)
    val tp = Type.Union(this.tk, false, listOf(tup.second, this.res).map { Pair(null,it) })
    val id = tp.coder(tpl)
    return Pair(id, tp)
}
fun Type.Proto.Coro.x_out_uni (tpl: Tpl_Map?, pre: Boolean): Pair<String, Type.Union> {
    val tp = Type.Union(this.tk, true, listOf(this.yld, this.out).map { Pair(null,it) })
    val id = tp.coder(tpl)
    return Pair(id, tp)
}
fun Type.Proto.Coro.x_sig (pre: Boolean, id: String): String {
    val x = this.x_coro_exec(null).second
    val (xiuni,_) = this.x_inp_uni(null,pre)
    val (xouni,_) = this.x_out_uni(null,pre)
    return "$xouni $id ($x* mar_exe, $xiuni mar_arg)"
}

fun Type.Exec.x_exec_coro (tpl: Tpl_Map?): Pair<String,String> {
    val tps = (this.inps.to_void() + listOf(this.res,this.yld,this.out)).map { it.coder(tpl) }.joinToString("__").clean()
    return Pair("Exec__$tps", "Coro__$tps")
}
fun Type.Exec.x_inp_tup (tpl: Tpl_Map?, pre: Boolean): Pair<String, Type.Tuple> {
    val tp = Type.Tuple(this.tk, this.inps.map { Pair(null,it) })
    val id = tp.coder(tpl)
    return Pair(id, tp)
}
fun Type.Exec.x_inp_uni (tpl: Tpl_Map?, pre: Boolean): Pair<String, Type.Union> {
    val tup = this.x_inp_tup(tpl,pre)
    val tp = Type.Union(this.tk, false, mutableListOf(tup.second, this.res).map { Pair(null,it) })
    val id = tp.coder(tpl)
    return Pair(id, tp)
}

fun Var_Type.coder (tpl: Tpl_Map?, pre: Boolean): String {
    val (id,tp) = this
    return tp.coder(tpl) + " " + id.str
}
fun Type.coder (tpl: Tpl_Map?): String {
    return when (this) {
        //is Type.Err,
        is Type.Any -> TODO()
        is Type.Tpl        -> if (tpl == null) "_TPL_" else tpl[this.tk.str]!!.first!!.coder(tpl)
        is Type.Nat        -> this.tk.str
        is Type.Prim       -> this.tk.str
        is Type.Data       -> this.ts.first().str + this.xtpls!!.map { (t,e) -> "_" + t.cond { it.to_str() } + e.cond { it.to_str() } }.joinToString("")
        is Type.Unit       -> "_VOID_"
        is Type.Pointer    -> this.ptr.coder(tpl) + (this.ptr !is Type.Proto).cond { "*" }
        is Type.Tuple      -> "Tuple__${this.ts.map { (id,tp) -> tp.coder(tpl)+id.cond {"_"+it.str} }.joinToString("__")}".clean()
        is Type.Union      -> "Union__${this.ts.map { (id,tp) -> tp.coder(tpl)+id.cond {"_"+it.str} }.joinToString("__")}".clean()
        is Type.Vector     -> "Vector__${this.max.cond2({it.tk.str},{"0"})}_${this.tp.coder(tpl)}".clean()
        is Type.Proto.Func -> "Func__${this.inps.to_void().map { it.coder(tpl) }.joinToString("__")}__${this.out.coder(tpl)}".clean()
        is Type.Proto.Coro -> this.x_coro_exec(tpl).first
        is Type.Exec       -> this.x_exec_coro(tpl).first
    }
}

fun List<Tk.Type>.coder (tpl: List<Tpl_Con>?, pre: Boolean): String {
    return this.map { it.str }.let {
        if (tpl == null) it else {
            val ts = tpl.map { it.first.cond { it.to_str() } + it.second.cond { it.to_str() } }
            listOf(it.first()) + ts + it.drop(1)
        }
    }.joinToString("_")
}

fun coder_types (pre: Boolean): String {
    val CACHE = mutableSetOf<String>()
    var FT_DATA = 0
    fun ft (me: Type): List<String> {
        when {
            (me is Type.Any) -> return emptyList()
            (FT_DATA==0 && me.up_any { it is Stmt.Data }) -> return emptyList()
                // Stmt.Data is abstract, we use concrete Type.Data
        }
        when (me) {
            is Type.Proto.Func, is Type.Proto.Coro, is Type.Data,
            is Type.Tuple, is Type.Vector, is Type.Union -> me.coder(null).let {
                if (CACHE.contains(it)) {
                    return emptyList()
                } else {
                    CACHE.add(it)
                }
            }
            else -> {}
        }
        //println(listOf("AAA", me.coder(null,pre)))

        return when (me) {
            is Type.Proto.Func -> listOf(
                "typedef ${me.out.coder(null)} (*${me.coder(null)}) (${
                    me.inps.map { it.coder(null) }.joinToString(",")
                });\n"
            )
            is Type.Proto.Coro -> {
                val (co, exe) = me.x_coro_exec(null)
                val (_, itup) = me.x_inp_tup(null, pre)
                val (xiuni, iuni) = me.x_inp_uni(null, pre)
                val (xouni, ouni) = me.x_out_uni(null, pre)
                val x = "struct " + exe
                ft(itup) + ft(iuni) + ft(ouni) + listOf(
                    x + ";\n",
                    "typedef $xouni (*$co) ($x*, $xiuni);\n",
                )
            }
            is Type.Tuple -> {
                val x = me.coder(null)
                /*val ids = if (me.ids == null) emptyList() else {
                        ft(Type.Tuple(me.tk, me.ts, null))
                    }
                    ids +*/ listOf(
                    """
                        typedef struct $x {
                            ${
                        me.ts.mapIndexed { i, id_tp ->
                            val (id, tp) = id_tp
                            """
                                union {
                                    ${tp.coder(null)} _${i + 1};
                                    ${id.cond { "${tp.coder(null)} ${it.str};" }}
                                };                                    
                                """
                        }.joinToString("")
                    }
                        } $x;
                    """
                )
            }
            is Type.Vector -> {
                val x = me.coder(null)
                listOf("""
                    typedef struct $x {
                        int max, cur;
                        ${me.tp.coder(null)} buf[${me.max.cond2({ it.tk.str }, { "" })}];
                    } $x;
                """)
            }
            is Type.Union -> {
                val x = me.coder(null)
                val xx = x.uppercase()
                listOf(
                    """
                        typedef enum ${xx}_TAGS {
                            __${xx}_TAG__,
                            ${
                        me.ts.mapIndexed { i, (id, _) ->
                            """
                                ${xx}_${if (id == null) i else id.str.uppercase()}_TAG,
                                """
                        }.joinToString("")
                    }
                        } ${xx}_TAGS;
                        typedef struct $x {
                            ${me.tagged.cond { "int tag;" }}
                            union {
                                ${
                        me.ts.mapIndexed { i, id_tp ->
                            val (id, tp) = id_tp
                            id.cond { tp.coder(null) + " " + it.str + ";\n" } +
                                    tp.coder(null) + " _" + (i + 1) + ";\n"
                        }.joinToString("")
                    }
                            };
                        } $x;
                    """
                )
            }
            is Type.Data -> {
                val ID = me.coder(null)
                val (S, _, tpc) = me.walk_tpl()
                FT_DATA++
                val ts = tpc.dn_collect_pos({ emptyList() }, ::ft)
                FT_DATA--
                ts + when {
                    (S.subs == null) -> {
                        fun f(tp: Type, s: List<String>): List<String> {
                            //println(listOf(s, tp.to_str()))
                            val ss = ID+s.drop(1).map { "_"+it }.joinToString("")
                            val SS = ss.uppercase()
                            val x1 = "typedef ${tp.coder(null)} $ss;\n"
                            val x2 = if (tp !is Type.Union) {
                                emptyList()
                            } else {
                                listOf("""
                                    typedef enum ${SS}_TAGS {
                                        __${SS}_TAG__,
                                        ${tp.ts.mapIndexed { i, (id, _) ->
                                            """
                                            ${SS}_${if (id == null) i else id.str.uppercase()}_TAG,
                                            """
                                        }.joinToString("")}
                                    } ${SS}_TAGS;
                                """) + tp.ts.map { (id, t) ->
                                    if (id == null) emptyList() else f(t, s + listOf(id.str))
                                }.flatten()
                            }
                            return listOf(x1) + x2
                        }
                        f(tpc, listOf(S.t.str))
                    }
                    else -> {
                        val sup = S.t.str
                        fun f(s: Stmt.Data, sup: String, l: List<Int>): String {
                            val id = (if (sup == "") "" else sup + "_") + s.t.str.uppercase()
                            //println(listOf(S.tk.pos, S.to_str()))
                            assert(l.size <= 6)
                            var n = 0
                            var k = 25
                            for (i in 0..l.size - 1) {
                                n += l[i] shl k
                                k -= 5
                            }
                            return """
                            #define ${id}_TAG $n
                        """ + s.subs!!.mapIndexed { i, ss ->
                                f(ss, id, l + listOf(i + 1))
                            }.joinToString("")
                        }

                        fun g(tpl: Tpl_Map?, sup: Pair<String, String>?, s: Stmt.Data, I: Int): String {
                            val cur = sup.cond { it.first + "_" } + s.t.str
                            val tup = s.tp as Type.Tuple
                            val flds = sup.cond { it.second } + tup.ts.mapIndexed { i, id_tp ->
                                val (id, tp) = id_tp
                                """
                                union {
                                    ${tp.coder(tpl)} _${I + i};
                                    ${id.cond { "${tp.coder(tpl)} ${it.str};" }}
                                };
                                """
                            }.joinToString("")
                            val subs = s.subs!!.map {
                                g(tpl, Pair(cur, flds), it, I + tup.ts.size)
                            }.joinToString("")
                            return """
                                struct {
                                    $flds
                                } $cur;
                                $subs
                            """
                        }

                        val tpl = S.tpls.map { (id, _) -> id.str }.zip(me.xtpls!!).toMap()
                        listOf(
                            f(S, "", listOf(G.datas++)) + """
                                typedef struct ${sup} {
                                    union {
                                        struct {
                                            int tag;
                                            union {
                                                ${g(tpl, null, S, 1)}
                                            };
                                        };
                                    };
                                } $sup;
                            """
                        )
                    }
                }
            }
            else -> emptyList()
        }
    }
    fun fe (me: Expr): List<String> {
        return when (me) {
            is Expr.Bin -> {
                if (me.tk.str == "++") {
                    val tp = me.typex() as Type.Vector
                    val x = tp.coder(tp.assert_no_tpls_up())
                    if (CACHE.contains(x)) {
                        return emptyList()
                    } else {
                        CACHE.add(x)
                    }
                    val y = tp.tp.coder(tp.tp.assert_no_tpls_up())
                    return listOf("""
                        typedef struct $x {
                            int max, cur;
                            $y buf[${tp.max!!.tk.str}];
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
            is Stmt.Proto.Coro -> {
                    fun mem (): String {
                        val blks = me.dn_collect_pre({
                            when (it) {
                                is Stmt.Proto -> if (it == me) emptyList() else null
                                is Stmt.Block -> listOf(it)
                                else -> emptyList()
                            }
                        }, {null}, {null})
                        return blks.map { it.to_dcls().map { (_,id,tp) -> Pair(id,tp!!).coder(tp.assert_no_tpls_up(),pre) + ";\n" } }.flatten().joinToString("")
                    }
                    val (co,exe) = me.tp_.x_coro_exec(me.tp_.assert_no_tpls_up())
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
                    this.tp_.out.coder(null) + " " + this.id.str + " (" + this.tp_.inps_.map { it.coder(it.second.assert_no_tpls_up(),pre) }.joinToString(",") + ")"
                is Stmt.Proto.Coro -> this.tp_.x_sig(pre, this.id.str)
            } + """
            {
                ${this.tp.out.coder(null)} mar_ret;
                ${(this is Stmt.Proto.Func).cond {
                    this as Stmt.Proto.Func
                    this.tp_.inps_.map { (id,tp) ->
                        if (tp !is Type.Vector) "" else {
                            val xid = id.str
                            """
                            $xid.max = ${tp.max!!.tk.str};
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
                        val (xuni,_) = this.tp_.x_out_uni(null,pre)
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
                            is Type.Proto.Func -> "auto " + tp.out.coder(null) + " " + id.str + " (" + tp.inps.map { it.coder(
                                it.assert_no_tpls_up()
                            ) }.joinToString(",") + ");\n"
                            is Type.Proto.Coro -> "auto ${tp.x_sig(pre,id.str)};\n"
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
                        } else if (mar_sup(${it.ts.coder(null,pre).uppercase()}_TAG, MAR_ESCAPE.tag)) {
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
                this.xtp!!.coder(null) + " " + this.id.str + ";"
            }
            val ini = this.xtp.let {
                if (it !is Type.Vector) "" else """
                    ${this.id.str}.max = ${it.max!!.tk.str};
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
                this.src.let { it is Expr.MatchT || it is Expr.MatchE } -> {
                    assert(tsrc !is Type.Vector)
                    this.src.coder(pre) + """
                        $dst = mar_${this.src.n};
                    """
                }
                tdst.is_num() -> "$dst = $src;"
                (tdst is Type.Vector) -> """
                    {
                        typeof($dst)* mar_$n = &$dst;
                        *mar_$n = CAST(${tdst.coder(null)}, $src);
                        mar_$n->max = ${tdst.max!!.tk.str};
                        mar_$n->cur = MIN(mar_$n->max, mar_$n->cur);                        
                    }                        
                """
                tdst.is_same_of(tsrc) -> "$dst = $src;"
                else -> {
                    "$dst = CAST(${tdst.coder(null)}, $src);"
                }
            }
        }
        is Stmt.SetS    -> this.src.coder(pre)

        is Stmt.Escape -> """
            MAR_ESCAPE = CAST(Escape, ${this.e.coder(pre)});
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
            val uni  = this.type()?.coder(null)
            val xuni = uni?.uppercase()
            """
            { // CATCH | ${this.dump()}
                do {
                    ${this.blk.coder(pre)}
                } while (0);
                if (MAR_EXCEPTION.tag == __MAR_EXCEPTION_NONE__) {
                    // no escape
                    ${(this.xup is Stmt.SetS).cond {
                        val set = this.xup as Stmt.SetS
                        """
                        ${set.dst.coder(pre)} = ($uni) { .tag=${xuni}_OK_TAG };
                        """
                     }}
                } else if (
                    ${this.tp.cond2({
                        "mar_sup(${it.ts.coder(null,pre).uppercase()}_TAG, MAR_EXCEPTION.tag)"
                    },{
                        "true"
                    })}
                ) {
                    ${(this.xup is Stmt.SetS && this.tp!=null).cond {
                        val set = this.xup as Stmt.SetS
                        """
                        ${set.dst.coder(pre)} = ($uni) { .tag=${xuni}_ERR_TAG, .Err=CAST(${this.tp!!.coder(null)}, MAR_EXCEPTION) };
                        """
                     }}
                    MAR_EXCEPTION.tag = __MAR_EXCEPTION_NONE__;
                } else {
                    continue;
                }
            }
            """
        }

        is Stmt.Create -> {
            val xtp = (this.xup as Stmt.SetS).dst.typex().coder(null)
            (this.xup is Stmt.SetS).cond {
                val set = this.xup as Stmt.SetS
                """
                ${set.dst.coder(pre)} =
                """
            } + """
            ($xtp) { 0, ${this.co.coder(pre)}, {} };
            """
        }
        is Stmt.Start -> {
            val exe = this.exe.coder(pre)
            val tp = this.exe.type() as Type.Exec
            val (xuni,_) = tp.x_inp_uni(null,pre)
            (this.xup is Stmt.SetS).cond {
                val set = this.xup as Stmt.SetS
                """
                ${set.dst.coder(pre)} =
                """
            } + """
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
            val (xuni,_) = tp.x_inp_uni(null,pre)
            (this.xup is Stmt.SetS).cond {
                val set = this.xup as Stmt.SetS
                """
                ${set.dst.coder(pre)} =
                """
            } + """
            $exe.co (
                &$exe, ($xuni) { ._2 = ${this.arg.coder(pre)} }
            );
            """
        }
        is Stmt.Yield -> {
            val tp = (this.up_first { it is Stmt.Proto.Coro } as Stmt.Proto.Coro).tp_
            val (xuni,_) = tp.x_out_uni(null,pre)
            """
                mar_exe->pc = ${this.n};
                return ($xuni) { .tag=1, ._1=${this.arg.coder(pre)} };
            case ${this.n}:
                ${(this.xup is Stmt.SetS).cond {
                    val set = this.xup as Stmt.SetS
                    """
                    ${set.dst.coder(pre)} = mar_arg._2;
                    """
                }}
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
                    ${tst.cond2({"case ${it.ts.coder(null,pre).uppercase()}_TAG"},{"default"})}:
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
                    is Type.Tpl  -> TODO("8") //aux(tpl, TODO(), v)
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
                            ${tp.coder(null)} mar_${tp.n} = $v;
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
                            ${tp.coder(null)} mar_${tp.n} = $v;
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
                            ${tp.coder(null)} mar_${tp.n} = $v;
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
                        val (s,_,tpx) = tp.walk_tpl()
                        val par = (tpx !is Type.Tuple) && (tpx !is Type.Union) && (tpx !is Type.Unit)
                        //println(listOf("XXX", tp2.to_str(), tp.to_str()))
                        val x = if (s.subs == null) aux(tpx, v) else {
                            val tup = tpx as Type.Tuple
                            val ts = tp.ts.coder(null,pre)
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
                "ref" -> {
                    val x = this.e.coder(pre)
                    if (this.e.is_lval()) {
                        "(&$x)"
                    } else {
                        "({ typeof($x) mar_$n=$x; &mar_$n; })"
                    }
                }
                else -> "(" + this.tk.str.op_mar_to_c() + this.e.coder(pre) + ")"
            }
        }
        is Expr.Bin -> {
            val e1 = this.e1.coder(pre)
            val e2 = this.e2.coder(pre)
            when (this.tk.str) {
                "++" -> {
                    val tp = this.typex() as Type.Vector
                    val one = tp.tp.coder(null)
                    val xe1 = if (this.e1.typex() is Type.Vector) {
                        "mar_vector_cat_vector((Vector*)&mar_$n, (Vector*)&mar_e1_$n, sizeof($one));"
                    } else {
                        "mar_vector_cat_pointer((Vector*)&mar_$n, mar_e1_$n, strlen(mar_e1_$n), sizeof($one));"
                    }
                    val xe2 = if (this.e2.typex() is Type.Vector) {
                        "mar_vector_cat_vector((Vector*)&mar_$n, (Vector*)&mar_e2_$n, sizeof($one));"
                    } else {
                        "mar_vector_cat_pointer((Vector*)&mar_$n, mar_e2_$n, strlen(mar_e2_$n), sizeof($one));"
                    }
                    """
                    ({
                        ${tp.coder(null)} mar_$n = { .max=${tp.max!!.tk.str}, .cur=0 };
                        typeof($e1) mar_e1_$n = $e1;
                        typeof($e2) mar_e2_$n = $e2;
                        $xe1
                        $xe2
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
                    else -> "CAST(${tdst.coder(null)}, $src)"
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

        is Expr.Tuple  -> "((${this.typex().coder(null)}) { ${this.vs.map { (_,tp) -> "{"+tp.coder(pre)+"}" }.joinToString(",") } })"
        is Expr.Vector -> (this.typex() as Type.Vector).let {
            "((${it.coder(null)}) { .max=${it.max!!.tk.str}, .cur=${it.max!!.tk.str}, .buf={${this.vs.map { it.coder(pre) }.joinToString(",") }} })"
        }
        is Expr.Union  -> {
            val (i,_) = this.xtp!!.disc(this.idx)!!
            "((${this.typex().coder(null)}) { .tag=${i+1}, ._${i+1}=${this.v.coder(pre) } })"
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
                    val ts = tp.ts.coder(null,pre)
                    "(${this.col.coder(pre)}.$ts.$idx)" // v.A_B_C.x
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
            val s = this.walk(this.tp.ts)!!.first
            if (s.subs == null) {
                var ret = "({"
                for (i in this.tp.ts.size - 1 downTo 0) {
                    val tp = this.tp.ts.take(i + 1).coder(this.tp.xtpls,pre)
                    ret = ret + "$tp ceu_$i = /* xxx */" +
                            if (i == this.tp.ts.size - 1) {
                                """
                                ((${tp}) ${this.e.coder(pre)});
                                """
                            } else {
                                val nxt = this.tp.ts[i + 1].str
                                """
                                {
                                    .tag = ${tp.uppercase()}_${nxt.uppercase()}_TAG,
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
                val ts = this.tp.ts.coder(null,pre)
                "((${this.tp.ts.first().str}) { .tag=${ts.uppercase()}_TAG, .$ts={$vs} })"
            }
        }

        is Expr.Tpl -> TODO("Expr.Tpl.coder()")
        is Expr.Nat -> when {
            (this.tk.str == "mar_ret") -> this.tk.str
            (this.xup is Stmt.Pass)    -> this.tk.str
            (this.xtp is Type.Any)     -> this.tk.str
            (this.xtp is Type.Nat)     -> this.tk.str
            (this.xtp is Type.Prim)    -> this.tk.str
            (this.xtp is Type.Data)    -> "CAST(${this.xtp!!.coder(null)}, ${this.tk.str})"
            else -> "((${this.xtp!!.coder(null)}) ${this.tk.str})"
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
                    "((" + sup!!.coder(null) + ")" + it + ")"
                }
            }
        }

        is Expr.Throw -> """
            ({
                assert(sizeof(Exception) >= sizeof(${this.e.typex().coder(null)}));
                MAR_EXCEPTION = CAST(Exception, ${this.e.coder(pre)});
                continue;
                ${this.xtp!!.coder(null)} mar_$n ; mar_$n;
            })
        """

        is Expr.If -> "((${this.cnd.coder(pre)}) ? (${this.t.coder(pre)}) : (${this.f.coder(pre)}))"
        is Expr.MatchT -> """
            ${this.typex().coder(TODO())} mar_$n;
            switch (${this.tst.coder(pre)}.tag) {
                ${this.cases.map { (tst,e) -> """
                    ${tst.cond2({"case ${it.ts.coder(TODO(),pre).uppercase()}_TAG"},{"default"})}:
                        mar_$n = ${e.coder(pre)};
                    break;
                """ }.joinToString("")}
            }
        """
        is Expr.MatchE -> """
            ${this.typex().coder(null)} mar_$n;
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
            is Expr.Num, is Expr.MatchT, is Expr.MatchE -> it
            else -> {
                val tp = this.typex()
                when {
                    (this.xnum == null) -> it
                    (this.xnum!!.tk.str == tp.tk.str) -> it
                    else -> {
                        val sup = tp.sup_vs(this.xnum!!)
                        if (sup == tp) it else {
                            "((" + sup!!.coder(null) + ")" + it + ")"
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
        
        #define CAST(tp,v) (((union { tp a; typeof(v) b; }) {.b=v}).a)
        
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
        
        typedef struct Vector {
            int max, cur;
            char buf[];
        } Vector;
        
        void mar_vector_cat_pointer (Vector* dst, char* src, int len, int size) {
            int n = MIN(dst->max-dst->cur, len);
            memcpy(&dst->buf[dst->cur*size], src, n*size);
            dst->cur += n;
        }
        
        void mar_vector_cat_vector (Vector* dst, Vector* src, int size) {
            mar_vector_cat_pointer(dst, src->buf, src->cur, size);
        }
        
        ${coder_types(pre)}
        
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
