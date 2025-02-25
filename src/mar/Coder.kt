package mar

fun String.clean (): String {
    return this.replace('*','_')
}

fun Type.Proto.Coro.x_coro_exec (tpls: Tpl_Map?): Pair<String,String> {
    val tps = (this.inps.to_void() + listOf(this.res,this.yld,this.out)).map { it.coder(tpls) }.joinToString("__").clean()
    return Pair("Coro__$tps", "Exec__$tps")
}
fun Type.Proto.Coro.x_inp_tup (tpls: Tpl_Map?, pre: Boolean): Pair<String, Type.Tuple> {
    val tp = Type.Tuple(this.tk, this.inps.map { Pair(null,it) })
    val id = tp.coder(tpls)
    return Pair(id, tp)
}
fun Type.Proto.Coro.x_inp_uni (tpls: Tpl_Map?, pre: Boolean): Pair<String, Type.Union> {
    val tup = this.x_inp_tup(tpls,pre)
    val tp = Type.Union(this.tk, false, listOf(tup.second, this.res).map { Pair(null,it) })
    val id = tp.coder(tpls)
    return Pair(id, tp)
}
fun Type.Proto.Coro.x_out_uni (tpls: Tpl_Map?, pre: Boolean): Pair<String, Type.Union> {
    val tp = Type.Union(this.tk, true, listOf(this.yld, this.out).map { Pair(null,it) })
    val id = tp.coder(tpls)
    return Pair(id, tp)
}
fun Type.Proto.Coro.x_sig (pre: Boolean, id: String): String {
    val x = this.x_coro_exec(null).second
    val (xiuni,_) = this.x_inp_uni(null,pre)
    val (xouni,_) = this.x_out_uni(null,pre)
    return "$xouni $id ($x* mar_exe, $xiuni mar_arg)"
}

fun Type.Exec.x_exec_coro (tpls: Tpl_Map?): Pair<String,String> {
    val tps = (this.inps.to_void() + listOf(this.res,this.yld,this.out)).map { it.coder(tpls) }.joinToString("__").clean()
    return Pair("Exec__$tps", "Coro__$tps")
}
fun Type.Exec.x_inp_tup (tpls: Tpl_Map?, pre: Boolean): Pair<String, Type.Tuple> {
    val tp = Type.Tuple(this.tk, this.inps.map { Pair(null,it) })
    val id = tp.coder(tpls)
    return Pair(id, tp)
}
fun Type.Exec.x_inp_uni (tpls: Tpl_Map?, pre: Boolean): Pair<String, Type.Union> {
    val tup = this.x_inp_tup(tpls,pre)
    val tp = Type.Union(this.tk, false, mutableListOf(tup.second, this.res).map { Pair(null,it) })
    val id = tp.coder(tpls)
    return Pair(id, tp)
}

fun Var_Type.coder (tpls: Tpl_Map?, pre: Boolean): String {
    val (id,tp) = this
    return tp.coder(tpls) + " " + id.str
}

fun Type.coder (tpls: Tpl_Map?): String {
    return when (this) {
        //is Type.Err,
        is Type.Any, is Type.Bot, is Type.Top -> TODO()
        is Type.Tpl        -> tpls!![this.tk.str]!!.first!!.coder(tpls)
        is Type.Nat        -> this.tk.str
        is Type.Prim       -> this.tk.str
        is Type.Data       -> this.ts.first().str + this.xtpls!!.map { (t,e) -> "_" + t.cond { it.to_str() } + e.cond { it.to_str() } }.joinToString("")
        is Type.Unit       -> "_VOID_"
        is Type.Pointer    -> this.ptr.coder(tpls) + (this.ptr !is Type.Proto).cond { "*" }
        is Type.Tuple      -> "Tuple__${this.ts.map { (id,tp) -> tp.coder(tpls)+id.cond {"_"+it.str} }.joinToString("__")}".clean()
        is Type.Union      -> "Union__${this.ts.map { (id,tp) -> tp.coder(tpls)+id.cond {"_"+it.str} }.joinToString("__")}".clean()
        is Type.Vector     -> "Vector__${this.max.cond2({it.static_int_eval(tpls).toString()},{"0"})}_${this.tp.coder(tpls)}".clean()
        is Type.Proto.Func -> "Func__${this.inps.to_void().map { it.coder(tpls) }.joinToString("__")}__${this.out.coder(tpls)}".clean()
        is Type.Proto.Coro -> this.x_coro_exec(tpls).first
        is Type.Exec       -> this.x_exec_coro(tpls).first
    }
}

fun String.proto (tpls: List<Tpl_Con>?): String {
    return this + tpls!!.map { (t,e) ->
        "_" + t.cond { it.to_str() } + e.cond { it.to_str() }
    }.joinToString("")
}

fun Stmt.Proto.proto (tpls: Map<String,Tpl_Con>?): String {
    return this.id.str + this.tpls.map {
        tpls!![it.first.str]!!.let { (t,e) ->
            "_" + t.cond { it.to_str() } + e.cond { it.to_str() }
        }
    }.joinToString("")
}

fun List<Tk.Type>.coder (tpls: List<Tpl_Con>?, pre: Boolean): String {
    return this.map { it.str }.let {
        if (tpls == null) it else {
            val ts = tpls.map { it.first.cond { it.to_str() } + it.second.cond { it.to_str() } }
            listOf(it.first()) + ts + it.drop(1)
        }
    }.joinToString("_")
}

fun coder_types (s: Stmt, tpls: Map<String, Tpl_Con>?, pre: Boolean): String {
    fun ft (me: Type): List<String> {
        when {
            (me is Type.Any) -> return emptyList()
            (tpls==null && me.has_tpls_dn()) -> return emptyList()
            (tpls!=null && !me.has_tpls_dn()) -> return emptyList()
        }
        when (me) {
            is Type.Proto.Func, is Type.Proto.Coro, is Type.Data,
            is Type.Tuple, is Type.Vector, is Type.Union -> me.coder(tpls).let {
                if (G.types.contains(it)) {
                    return emptyList()
                } else {
                    G.types.add(it)
                }
            }
            else -> {}
        }
        //println(listOf("AAA", me.coder(null,pre)))

        return when (me) {
            is Type.Proto.Func -> {
                listOf(
                    "typedef ${me.out.coder(tpls)} (*${me.coder(tpls)}) (${
                        me.inps.map { it.coder(tpls) }.joinToString(",")
                    });\n"
                )
            }
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
                val x = me.coder(tpls)
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
                                    ${tp.coder(tpls)} _${i + 1};
                                    ${id.cond { "${tp.coder(tpls)} ${it.str};" }}
                                };                                    
                                """
                        }.joinToString("")
                    }
                        } $x;
                    """
                )
            }
            is Type.Vector -> {
                //println(me.xup!!.to_str())
                val x = me.coder(tpls)
                listOf("""
                    typedef struct $x {
                        int max, cur;
                        ${me.tp.coder(tpls)} buf[${me.max.cond2({ it.coder(tpls,pre) }, { "" })}];
                    } $x;
                """)
            }
            is Type.Union -> {
                val x = me.coder(tpls)
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
                            id.cond { tp.coder(tpls) + " " + it.str + ";\n" } +
                                    tp.coder(tpls) + " _" + (i + 1) + ";\n"
                        }.joinToString("")
                    }
                            };
                        } $x;
                    """
                )
            }
            is Type.Data -> {
                val ID = me.coder(tpls)
                val (S, _, tpc) = me.walk_tpl()
                //println(me.to_str())
                //println(tpc.to_str())
                val ts = tpc.dn_collect_pos({ emptyList() }, ::ft)
                ts + when {
                    (S.subs == null) -> {
                        fun f(tp: Type, s: List<String>): List<String> {
                            //println(listOf(s, tp.to_str()))
                            val ss = ID+s.drop(1).map { "_"+it }.joinToString("")
                            val SS = ss.uppercase()
                            val x1 = "typedef ${tp.coder(tpls)} $ss;\n"
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

                        fun g(tpls: Tpl_Map?, sup: Pair<String, String>?, s: Stmt.Data, I: Int): String {
                            val cur = sup.cond { it.first + "_" } + s.t.str
                            val tup = s.tp as Type.Tuple
                            val flds = sup.cond { it.second } + tup.ts.mapIndexed { i, id_tp ->
                                val (id, tp) = id_tp
                                """
                                union {
                                    ${tp.coder(tpls)} _${I + i};
                                    ${id.cond { "${tp.coder(tpls)} ${it.str};" }}
                                };
                                """
                            }.joinToString("")
                            val subs = s.subs!!.map {
                                g(tpls, Pair(cur, flds), it, I + tup.ts.size)
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
                    if (G.types.contains(x)) {
                        return emptyList()
                    } else {
                        G.types.add(x)
                    }
                    val y = tp.tp.coder(tp.tp.assert_no_tpls_up())
                    return listOf("""
                        typedef struct $x {
                            int max, cur;
                            $y buf[${tp.max!!.coder(tpls,pre)}];
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
        if (me !is Stmt.Proto) {
            return emptyList()
        }
        val xtplss: List<Tpl_Map?> = if (me.tpls.isEmpty()) emptyList() else {
            G.tpls[me]?.values?.map { me.tpls.map { (id, _) -> id.str }.zip(it).toMap() }
                ?: emptyList()
        }
        val x = xtplss.map { xtpls ->
            "/* XXX */\n" + coder_types(me.blk, xtpls, pre)
        }

        if (me !is Stmt.Proto.Coro) {
            return x
        }

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
        return x + listOf("""
            typedef struct $exe {
                int pc;
                $co co;
                struct {
                    ${mem()}
                } mem;
            } $exe;
        """)
    }
    val ts = s.dn_collect_pos(::fs, ::fe, ::ft)
    return ts.joinToString("")
}

fun Stmt.coder (tpls: Tpl_Map?, pre: Boolean): String {
    return when (this) {
        is Stmt.Data -> ""
        is Stmt.Proto -> {
            assert((tpls == null) || this.tpls.isEmpty()) { "TODO: merge tpls" }
            val xtplss: List<Tpl_Map?> = if (this.tpls.isEmpty()) listOf(null) else {
                G.tpls[this]?.values?.map { this.tpls.map { (id, _) -> id.str }.zip(it).toMap() }
                    ?: emptyList()
            }
            xtplss.map { xtpls ->
                when (this) {
                    is Stmt.Proto.Func ->
                        this.tp_.out.coder(xtpls) + " " + this.proto(xtpls) + " (" + this.tp_.inps_.map { it.coder(it.second.assert_no_tpls_up(),pre) }.joinToString(",") + ")"
                    is Stmt.Proto.Coro -> this.tp_.x_sig(pre, this.proto(xtpls))
                } + """
                {
                    ${this.tp.out.coder(xtpls)} mar_ret;
                    ${(this is Stmt.Proto.Func).cond {
                        this as Stmt.Proto.Func
                        this.tp_.inps_.map { (id,tp) ->
                            if (tp !is Type.Vector) "" else {
                                val xid = id.str
                                """
                                $xid.max = ${tp.max!!.coder(tpls,pre)};
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
                        ${this.blk.coder(xtpls,pre)}
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
            }.joinToString("")
        }

        is Stmt.Block  -> {
            val body = this.ss.map {
                it.coder(tpls,pre) + "\n"
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
                    ${this.to_dcls().map { (s,_,tp) ->
                        if (tp !is Type.Proto) emptyList() else {
                            s as Stmt.Proto
                            val xtplss: List<Tpl_Map?> = if (s.tpls.isEmpty()) listOf(null) else {
                                G.tpls[s]?.values?.map { s.tpls.map { (id, _) -> id.str }.zip(it).toMap() }
                                    ?: emptyList()
                            }
                            xtplss.map { xtpls ->
                                when (tp) {
                                    is Type.Proto.Func -> "auto " + tp.out.coder(tpls) + " " + s.proto(xtpls) + " (" + tp.inps.map { it.coder(
                                        it.assert_no_tpls_up()
                                    ) }.joinToString(",") + ");\n"
                                    is Type.Proto.Coro -> "auto ${tp.x_sig(pre,s.proto(xtpls))};\n"
                                }
                            }
                        }
                    }.flatten().joinToString("")}
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
                this.xtp!!.coder(tpls) + " " + this.id.str + ";"
            }
            val ini = this.xtp.let {
                if (it !is Type.Vector) "" else """
                    ${this.id.str}.max = ${it.max!!.coder(tpls,pre)};
                    ${this.id.str}.cur = 0;
                """
            }
            dcl + ini
        }
        is Stmt.SetE    -> {
            val dst = this.dst.coder(tpls,pre)
            val src = this.src.coder(tpls,pre)
            val tdst = this.dst.typex()
            val tsrc = this.src.typex()
            when {
                this.src.let { it is Expr.MatchT || it is Expr.MatchE } -> {
                    assert(tsrc !is Type.Vector)
                    this.src.coder(tpls,pre) + """
                        $dst = mar_${this.src.n};
                    """
                }
                tdst.is_num() -> "$dst = $src;"
                (tdst is Type.Vector) -> """
                    {
                        typeof($dst)* mar_$n = &$dst;
                        *mar_$n = CAST(${tdst.coder(tpls)}, $src);
                        mar_$n->max = ${tdst.max!!.coder(tpls,pre)};
                        mar_$n->cur = MIN(mar_$n->max, mar_$n->cur);                        
                    }                        
                """
                tdst.is_same_of(tsrc) -> "$dst = $src;"
                else -> {
                    "$dst = CAST(${tdst.coder(tpls)}, $src);"
                }
            }
        }
        is Stmt.SetS    -> this.src.coder(tpls,pre)

        is Stmt.Escape -> """
            MAR_ESCAPE = CAST(Escape, ${this.e.coder(tpls,pre)});
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
                    ${this.blk.coder(tpls,pre)}
                }
            """
            ns.add(n)
            G.defers[bup.n] = Triple(ns, ini+inix, endx+end)
            """
            $id = 1;   // now reached
            """
        }
        is Stmt.Catch -> {
            val uni  = this.type()?.coder(tpls)
            val xuni = uni?.uppercase()
            """
            { // CATCH | ${this.dump()}
                do {
                    ${this.blk.coder(tpls,pre)}
                } while (0);
                if (MAR_EXCEPTION.tag == __MAR_EXCEPTION_NONE__) {
                    // no escape
                    ${(this.xup is Stmt.SetS).cond {
                        val set = this.xup as Stmt.SetS
                        """
                        ${set.dst.coder(tpls,pre)} = ($uni) { .tag=${xuni}_OK_TAG };
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
                        ${set.dst.coder(tpls,pre)} = ($uni) { .tag=${xuni}_ERR_TAG, .Err=CAST(${this.tp!!.coder(tpls)}, MAR_EXCEPTION) };
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
            val xtp = (this.xup as Stmt.SetS).dst.typex().coder(tpls)
            (this.xup is Stmt.SetS).cond {
                val set = this.xup as Stmt.SetS
                """
                ${set.dst.coder(tpls,pre)} =
                """
            } + """
            ($xtp) { 0, ${this.co.coder(tpls,pre)}, {} };
            """
        }
        is Stmt.Start -> {
            val exe = this.exe.coder(tpls,pre)
            val tp = this.exe.type() as Type.Exec
            val (xuni,_) = tp.x_inp_uni(null,pre)
            (this.xup is Stmt.SetS).cond {
                val set = this.xup as Stmt.SetS
                """
                ${set.dst.coder(tpls,pre)} =
                """
            } + """
            $exe.co (
                &$exe, ($xuni) { ._1 = {
                    ${this.args.map { it.coder(tpls,pre) }.joinToString(",")}
                } }
            );
            """
        }
        is Stmt.Resume -> {
            val exe = this.exe.coder(tpls,pre)
            val tp = this.exe.type() as Type.Exec
            val (xuni,_) = tp.x_inp_uni(null,pre)
            (this.xup is Stmt.SetS).cond {
                val set = this.xup as Stmt.SetS
                """
                ${set.dst.coder(tpls,pre)} =
                """
            } + """
            $exe.co (
                &$exe, ($xuni) { ._2 = ${this.arg.coder(tpls,pre)} }
            );
            """
        }
        is Stmt.Yield -> {
            val tp = (this.up_first { it is Stmt.Proto.Coro } as Stmt.Proto.Coro).tp_
            val (xuni,_) = tp.x_out_uni(null,pre)
            """
                mar_exe->pc = ${this.n};
                return ($xuni) { .tag=1, ._1=${this.arg.coder(tpls,pre)} };
            case ${this.n}:
                ${(this.xup is Stmt.SetS).cond {
                    val set = this.xup as Stmt.SetS
                    """
                    ${set.dst.coder(tpls,pre)} = mar_arg._2;
                    """
                }}
            """
        }


        is Stmt.If     -> """
            if (${this.cnd.coder(tpls,pre)}) {
                ${this.t.coder(tpls,pre)}
            } else {
                ${this.f.coder(tpls,pre)}
            }
        """
        is Stmt.Loop   -> """
            // LOOP | ${this.dump()}
            MAR_LOOP_START_${this.n}:
                ${this.blk.coder(tpls,pre)}
                goto MAR_LOOP_START_${this.n};
                MAR_LOOP_STOP_${this.n}:
        """
        is Stmt.MatchT -> """
            // MATCH | ${this.dump()}
            switch (${this.tst.coder(tpls,pre)}.tag) {
                ${this.cases.map { (tst,e) -> """
                    ${tst.cond2({"case ${it.ts.coder(null,pre).uppercase()}_TAG"},{"default"})}:
                        ${e.coder(tpls,pre)};
                    break;
                """ }.joinToString("")}
            }
        """
        is Stmt.MatchE -> """
            // MATCH | ${this.dump()}
            switch (${this.tst.coder(tpls,pre)}) {
                ${this.cases.map { (tst,e) -> """
                    ${tst.cond2({"case ${it.coder(tpls,pre)}"},{"default"})}:
                        ${e.coder(tpls,pre)};
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
                            ${tp.coder(tpls)} mar_${tp.n} = $v;
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
                            ${tp.coder(tpls)} mar_${tp.n} = $v;
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
                            ${tp.coder(tpls)} mar_${tp.n} = $v;
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
            aux(this.e.typex(), this.e.coder(tpls,pre)) + """
                puts("");
            """
        }
        is Stmt.Pass  -> this.e.coder(tpls,pre) + ";"
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

fun Expr.coder (tpls: Tpl_Map?, pre: Boolean): String {
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
                "#"  -> "(" + this.e.coder(tpls,pre) + ".cur)"
                "##" -> "(" + this.e.coder(tpls,pre) + ".max)"
                "ref" -> {
                    val x = this.e.coder(tpls,pre)
                    if (this.e.is_lval()) {
                        "(&$x)"
                    } else {
                        "({ typeof($x) mar_$n=$x; &mar_$n; })"
                    }
                }
                else -> "(" + this.tk.str.op_mar_to_c() + this.e.coder(tpls,pre) + ")"
            }
        }
        is Expr.Bin -> {
            val e1 = this.e1.coder(tpls,pre)
            val e2 = this.e2.coder(tpls,pre)
            when (this.tk.str) {
                "++" -> {
                    val tp = this.typex() as Type.Vector
                    val one = tp.tp.coder(tpls)
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
                        ${tp.coder(tpls)} mar_$n = { .max=${tp.max!!.coder(tpls,pre)}, .cur=0 };
                        typeof($e1) mar_e1_$n = $e1;
                        typeof($e2) mar_e2_$n = $e2;
                        $xe1
                        $xe2
                        mar_$n;
                    })
                    """                }
                "==", "!=" -> {
                    fun f (xx: Int, xtp: Type, xe1: String, xe2: String): String {
                        //println(xtp.to_str())
                        return when {
                            (xtp is Type.Tuple) -> {
                                val (uno,op) = if (this.tk.str == "==") Pair(1,"&&") else Pair(0,"||")
                                val vs = xtp.ts.mapIndexed { i,(_,xxtp) ->
                                    //println(xxtp.to_str())
                                    op + " " + f(xx+1, xxtp, "mar_1_${n}_$xx._"+(i+1), "mar_2_${n}_$xx._"+(i+1))
                                }.joinToString("")
                                """({
                                    ${xtp.coder(tpls)} mar_1_${n}_$xx = $xe1;
                                    ${xtp.coder(tpls)} mar_2_${n}_$xx = $xe2;
                                    $uno $vs;
                                })"""
                            }
                            (xtp is Type.Vector) -> {
                                """({
                                    ${xtp.coder(tpls)} mar_1_${n}_$xx = $xe1;
                                    ${xtp.coder(tpls)} mar_2_${n}_$xx = $xe2;
                                    (mar_1_${n}_$xx.cur == mar_2_${n}_$xx.cur) && ({
                                        int mar_ok_${n}_$xx = 1;
                                        for (int i=0; i<mar_1_$n.cur; i++) {
                                            if (${f(xx+1, xtp.tp, "mar_1_${n}_$xx.buf[i]", "mar_2_${n}_$xx.buf[i]")}) {
                                                mar_ok_${n}_$xx = 0;
                                                break;
                                            }
                                        };
                                        mar_ok_${n}_$xx;
                                    });
                                })"""
                            }
                            else -> "($xe1 ${this.tk.str} $xe2)"
                        }
                    }
                    f(11, this.e1.typex(), e1, e2)
                }
                else -> "(" + e1 + " " + this.tk.str.op_mar_to_c() + " " + e2 + ")"
            }
        }
        is Expr.Call -> {
            val f = this.xtpls.let {
                val id = this.f.coder(tpls,pre)
                when {
                    (it == null) -> id
                    it.isEmpty() -> id
                    else -> {
                        assert(this.f is Expr.Acc)
                        id.proto(it)
                    }
                }
            }
            val inps = this.f.type().let {
                if (it !is Type.Proto) null else {
                    it.inps
                }
            }
            val call = "$f ( ${this.args.mapIndexed { i,arg ->
                val src = arg.coder(tpls,pre)
                val tdst = if (inps == null) null else inps[i]
                val tsrc = arg.typex()
                when {
                    (tdst == null) -> src
                    tdst.is_num() -> src
                    tdst.is_same_of(tsrc) -> src
                    else -> "CAST(${tdst.coder(tpls)}, $src)"
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

        is Expr.Tuple  -> "((${this.typex().coder(tpls)}) { ${this.vs.map { (_,tp) -> "{"+tp.coder(tpls,pre)+"}" }.joinToString(",") } })"
        is Expr.Vector -> (this.typex() as Type.Vector).let {
            val max = it.max!!.coder(tpls,pre)
            "((${it.coder(tpls)}) { .max=$max, .cur=$max, .buf={${this.vs.map { it.coder(tpls,pre) }.joinToString(",") }} })"
        }
        is Expr.Union  -> {
            val (i,_) = this.xtp!!.disc(this.idx)!!
            "((${this.typex().coder(tpls)}) { .tag=${i+1}, ._${i+1}=${this.v.coder(tpls,pre) } })"
        }
        is Expr.Field  -> {
            val idx = this.idx.toIntOrNull().let {
                if (it == null) this.idx else "_"+it
            }
            val tp = this.col.typex()
            if (tp !is Type.Data) {
                "(${this.col.coder(tpls,pre)}.$idx)"
            } else {
                val s = tp.walk()!!.first
                if (s.subs == null) {
                    val sub = tp.ts.drop(1).map { it.str + "." }.joinToString("")
                    "(${this.col.coder(tpls,pre)}.$sub$idx)"
                } else {
                    val ts = tp.ts.coder(null,pre)
                    "(${this.col.coder(tpls,pre)}.$ts.$idx)" // v.A_B_C.x
                }
            }
        }
        is Expr.Index  -> "${this.col.coder(tpls,pre)}.buf[${this.idx.coder(tpls,pre)}]"
        is Expr.Disc   -> {
            val tp = this.col.typex()
            val ret = if (tp !is Type.Data) {
                val (i,_) = tp.discx(this.idx)!!
                "${this.col.coder(tpls,pre)}._${i+1}"
            } else {
                val s = tp.walk()!!.first
                if (s.subs == null) {
                    val (i,_) = tp.discx(this.idx)!!
                    "${this.col.coder(tpls,pre)}._${i+1}"
                } else {
                    this.col.coder(tpls,pre)
                }
            }
            """
                // DISC | ${this.dump()}
                ($ret)
            """
        }
        is Expr.Pred   -> {
            val (i,_) = this.col.typex().discx(this.idx)!!
            "(${this.col.coder(tpls,pre)}.tag==${i+1})"
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
                                ((${tp}) ${this.e.coder(tpls,pre)});
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
                    "."+(id?.str ?: ("_"+(i+1))) + " = " + v.coder(tpls,pre)
                }.joinToString(",")
                val ts = this.tp.ts.coder(null,pre)
                "((${this.tp.ts.first().str}) { .tag=${ts.uppercase()}_TAG, .$ts={$vs} })"
            }
        }

        is Expr.Tpl -> tpls!![this.tk.str]!!.second!!.coder(tpls,pre)
        is Expr.Nat -> when {
            (this.tk.str == "mar_ret") -> this.tk.str
            (this.xup is Stmt.Pass)    -> this.tk.str
            (this.xtp is Type.Any)     -> this.tk.str
            (this.xtp is Type.Nat)     -> this.tk.str
            (this.xtp is Type.Prim)    -> this.tk.str
            (this.xtp is Type.Data)    -> "CAST(${this.xtp!!.coder(tpls)}, ${this.tk.str})"
            else -> "((${this.xtp!!.coder(tpls)}) ${this.tk.str})"
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
                    "((" + sup!!.coder(tpls) + ")" + it + ")"
                }
            }
        }

        is Expr.Throw -> """
            ({
                assert(sizeof(Exception) >= sizeof(${this.e.typex().coder(tpls)}));
                MAR_EXCEPTION = CAST(Exception, ${this.e.coder(tpls,pre)});
                continue;
                ${this.xtp!!.coder(tpls)} mar_$n ; mar_$n;
            })
        """

        is Expr.If -> "((${this.cnd.coder(tpls,pre)}) ? (${this.t.coder(tpls,pre)}) : (${this.f.coder(tpls,pre)}))"
        is Expr.MatchT -> """
            ${this.typex().coder(TODO())} mar_$n;
            switch (${this.tst.coder(tpls,pre)}.tag) {
                ${this.cases.map { (tst,e) -> """
                    ${tst.cond2({"case ${it.ts.coder(TODO(),pre).uppercase()}_TAG"},{"default"})}:
                        mar_$n = ${e.coder(tpls,pre)};
                    break;
                """ }.joinToString("")}
            }
        """
        is Expr.MatchE -> """
            ${this.typex().coder(tpls)} mar_$n;
            switch (${this.tst.coder(tpls,pre)}) {
                ${this.cases.map { (tst,e) -> """
                    ${tst.cond2({"case ${it.coder(tpls,pre)}"},{"default"})}:
                        mar_$n = ${e.coder(tpls,pre)};
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
                            "((" + sup!!.coder(tpls) + ")" + it + ")"
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
        
        ${coder_types(G.outer!!, null, pre)}
        
        int main (void) {
            do {
                ${G.outer!!.coder(null,pre)}
            } while (0);
            if (MAR_EXCEPTION.tag != __MAR_EXCEPTION_NONE__) {
                puts("uncaught exception");
            }
        }
    """
}
