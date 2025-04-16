package mar



fun String.clean (): String {
    return this.replace('*','_')
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
        //is Type.Data       -> this.ts.map { it.str }.joinToString("_") + this.xtpls!!.map { (t,e) -> "_" + t.cond { it.to_str() } + e.cond { it.to_str() } }.joinToString("")
        is Type.Unit       -> "_VOID_"
        is Type.Pointer    -> this.ptr.coder(tpls) + (this.ptr !is Type.Proto).cond { "*" }
        is Type.Tuple      -> "Tuple__${this.ts.map { (id,tp) -> tp.coder(tpls)+id.cond {"_"+it.str} }.joinToString("__")}".clean()
        is Type.Union      -> "Union__${this.ts.map { (id,tp) -> tp.coder(tpls)+id.cond {"_"+it.str} }.joinToString("__")}".clean()
        is Type.Vector     -> "Vector__${this.max.cond2({it.static_int_eval(tpls).toString()},{"0"})}_${this.tp.coder(tpls)}".clean()
        is Type.Proto.Func -> "Func__${this.inps.to_void().map { it.coder(tpls) }.joinToString("__")}__${this.out.coder(tpls)}".clean()
        is Type.Proto.Coro -> this.x_pro_exe(tpls).first
        is Type.Proto.Task -> this.x_pro_exe(tpls).first
        is Type.Exec.Coro  -> this.x_pro_exe(tpls).second
        is Type.Exec.Task  -> this.x_pro_exe(tpls).second
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

fun coder_types (x: Stmt.Proto?, s: Stmt, tpls: Map<String, Tpl_Con>?, pre: Boolean): List<Pair<String,String>> {
    fun ft (me: Type): List<Pair<String,String>> {
        when {
            (me is Type.Any) -> return emptyList()
            (tpls==null && me.has_tpls_dn()) -> return emptyList()
            (tpls!=null && !me.has_tpls_dn()) -> return emptyList()
        }
        val id = me.coder(tpls)
        return when (me) {
            is Type.Proto.Func -> {
                val v = """
                    typedef ${me.out.coder(tpls)} (*$id) (${
                        me.inps.map { it.coder(tpls) }.joinToString(",")
                    });
                """
                listOf(id to v)
            }
            is Type.Proto.Coro -> {
                val (pro, exe) = me.x_pro_exe(null)
                val xexe = me.to_exe()!!
                val (_, itup) = me.inps().x_inp_tup(me.tk, null, pre)
                val (xinps,inps) = me.inps().x_inp_tup(me.tk,null, pre)
                val res = me.res().coder(null)
                val (xouni, ouni) = me.x_out(null, pre)
                val v = """
                    struct $exe;
                    typedef void (*$pro) (MAR_EXE_ACTION, struct $exe*, $xinps*, $res*, $xouni*);
                """
                ft(itup) + ft(inps) + ft(ouni) + /*ft(xexe) +*/ listOf(id to v)
            }
            is Type.Proto.Task -> {
                val (pro, exe) = me.x_pro_exe(null)
                val xexe = Type.Exec.Task(me.tk, me.xpro, me.inps, me.out)
                val (_, itup) = me.inps().x_inp_tup(me.tk, null, pre)
                val (xinps,inps) = me.inps().x_inp_tup(me.tk,null, pre)
                val (_,out) = me.x_out(null, pre)
                val v = """
                    struct $exe;
                    typedef void (*$pro) (MAR_EXE_ACTION, struct $exe*, $xinps*, int, void*);
                """
                ft(itup) + ft(inps) + ft(out) + /*ft(xexe) +*/ listOf(id to v)
            }
            is Type.Exec.Coro -> {
                val (pro, exe) = me.x_pro_exe(null)
                val xpro = me.to_pro()
                val (_, itup) = me.inps().x_inp_tup(me.tk, null, pre)
                val (xinps,inps) = me.inps().x_inp_tup(me.tk,null, pre)
                val res = me.res().coder(null)
                val (xouni, ouni) = me.x_out(null, pre)
                val v = """
                    struct $exe;
                    typedef void (*$pro) (MAR_EXE_ACTION, struct $exe*, $xinps*, $res*, $xouni*);
                """
                ft(itup) + ft(inps) + ft(ouni) + /*ft(xpro) +*/ listOf(id to v)
            }
            is Type.Exec.Task -> {
                val (pro, exe) = me.x_pro_exe(null)
                val xpro = me.to_pro()
                val (_, itup) = me.inps().x_inp_tup(me.tk, null, pre)
                val (xinps,inps) = me.inps().x_inp_tup(me.tk,null, pre)
                val (_, out) = me.x_out(null, pre)
                val v = """
                    struct $exe;
                    typedef void (*$pro) (MAR_EXE_ACTION, struct $exe*, $xinps*, int, void*);
                """
                ft(itup) + ft(inps) + ft(out) + /*ft(xpro) +*/ listOf(id to v)
            }
            is Type.Tuple -> {
                listOf(id to """
                    typedef struct $id {
                        ${me.ts.mapIndexed { i, id_tp ->
                        val (id, tp) = id_tp
                        """
                            union {
                                ${tp.coder(tpls)} _${i + 1};
                                ${id.cond { "${tp.coder(tpls)} ${it.str};" }}
                            };                                    
                            """
                    }.joinToString("")}
                    } $id;
                """)
            }
            is Type.Vector -> {
                //println(me.xup!!.to_str())
                listOf(id to """
                    typedef struct $id {
                        int max, cur;
                        ${me.tp.coder(tpls)} buf[${me.max.cond2({ it.coder(tpls,pre) }, { "" })}];
                    } $id;
                """)
            }
            is Type.Union -> {
                listOf(id to """
                    typedef enum MAR_TAGS_${id} {
                        __MAR_TAG_${id}__,
                        ${me.ts.mapIndexed { i, (xid, _) ->
                        """
                            MAR_TAG_${id}_${if (xid == null) i else xid.str},
                            """
                    }.joinToString("")}
                    } MAR_TAGS_${id};
                    typedef struct $id {
                        ${me.tagged.cond { "int tag;" }}
                        union {
                            ${me.ts.mapIndexed { i, id_tp ->
                        val (id, tp) = id_tp
                        id.cond { tp.coder(tpls) + " " + it.str + ";\n" } +
                                tp.coder(tpls) + " _" + (i + 1) + ";\n"
                    }.joinToString("")}
                        };
                    } $id;
                """)
            }
            is Type.Data -> {
                val N = G.datas++
                val (S, _, tpc) = me.walk_tpl()
                val ts = tpc.dn_collect_pos({ emptyList() }, ::ft)
                (ts + listOf(id to when {
                    (S.subs == null) -> {
                        fun f(tp: Type, s: List<String>): List<String> {
                            val ss = id+s.drop(1).map { "_"+it }.joinToString("")
                            val x1 = """
                                #define MAR_TAG_${me.ts.first().str} $N
                                typedef ${tp.coder(tpls)} $ss;
                            """
                            val x2 = if (tp !is Type.Union) {
                                emptyList()
                            } else {
                                listOf("""
                                    typedef enum MAR_TAGS_${ss} {
                                        __MAR_TAG_${ss}__,
                                        ${tp.ts.mapIndexed { i, (id, _) ->
                                            """
                                            MAR_TAG_${ss}_${if (id == null) i else id.str},
                                            """
                                        }.joinToString("")}
                                    } MAR_TAGS_${ss};
                                """) + tp.ts.map { (id, t) ->
                                    if (id == null) emptyList() else f(t, s + listOf(id.str))
                                }.flatten()
                            }
                            return listOf(x1) + x2
                        }
                        f(tpc, listOf(S.t.str)).joinToString("")                    }
                    else -> {
                        val sup = S.t.str
                        fun f(s: Stmt.Data, sup: String, l: List<Int>): String {
                            val xid = (if (sup == "") "" else sup + "_") + s.t.str
                            //println(listOf(S.tk.pos, S.to_str()))
                            assert(l.size <= 6)
                            var n = 0
                            var k = 25
                            for (i in 0..l.size - 1) {
                                n += l[i] shl k
                                k -= 5
                            }
                            return """
                                #define MAR_TAG_${xid} 0b${n.toString(2)}
                            """ + s.subs!!.mapIndexed { i, ss ->
                                f(ss, xid, l + listOf(i + 1))
                            }.joinToString("")
                        }

                        fun g(tpls: Tpl_Map?, sup: Pair<String, String>?, s: Stmt.Data, I: Int): String {
                            val cur = sup.cond { it.first + "_" } + s.t.str
                            val tup = s.tp as Type.Tuple
                            val flds = sup.cond { it.second } + tup.ts.mapIndexed { i, id_tp ->
                                val (xid, tp) = id_tp
                                """
                                union {
                                    ${tp.coder(tpls)} _${I + i};
                                    ${xid.cond { "${tp.coder(tpls)} ${it.str};" }}
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
                        f(S, "", listOf(N)) + """
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
                    }
                }))
            }
            else -> emptyList()
        }
    }
    fun fe (me: Expr): List<Pair<String,String>> {
        return when (me) {
            is Expr.Bin -> {
                if (me.tk.str == "++") {
                    val tp = me.typex() as Type.Vector
                    val a = tp.coder(tp.assert_no_tpls_up())
                    val b = tp.tp.coder(tp.tp.assert_no_tpls_up())
                    listOf(a to """
                        typedef struct $a {
                            int max, cur;
                            $b buf[${tp.max!!.coder(tpls,pre)}];
                        } $a;
                    """)
                } else {
                    emptyList()
                }
            }
            else -> emptyList()
        }
    }
    fun fs (me: Stmt): List<Pair<String,String>> {
        return when {
            (me is Stmt.Data) -> emptyList()
            (me !is Stmt.Proto) -> emptyList()
            (me == x) -> emptyList() // HACK-01
            else -> {
                fun mem (): String {
                    val blks = me.dn_collect_pre({
                        when (it) {
                            is Stmt.Proto -> if (it == me) emptyList() else null
                            is Stmt.Block -> listOf(it)
                            else -> emptyList()
                        }
                    }, {null}, {null})
                    return blks.map { blk ->
                        //println(listOf(blk.n,G.defers[blk.n]))
                        val defs = G.defers.getOrDefault(blk, emptyList()).map {
                            "int defer_${it.n};\n"
                        }
                        val vars = blk.to_dcls().filter { (_,_,tp) -> tp !is Type.Proto }.map { (_,id,tp) ->
                            Pair(id,tp!!).coder(tp.assert_no_tpls_up(),pre) + ";\n"
                        }
                        (defs + vars)
                    }.flatten().joinToString("")
                }

                val xx: List<Pair<String,String>> = if (me is Stmt.Proto.Func) emptyList() else {
                    val (pro,exe) = me.tp.x_pro_exe(me.tp.assert_no_tpls_up())
                    //println(listOf(me.n, me.tp.n, exe))
                    listOf(exe to """
                        typedef struct $exe {
                            MAR_Exe_Fields($pro)
                            ${(me is Stmt.Proto.Task).cond { """
                                uintptr_t evt;
                            """ }}
                            struct {
                                ${mem()}    // TODO: unions for non-coexisting blocks
                            } mem;
                        } $exe;
                    """)
                }

                val xtplss: List<Tpl_Map?> = me.template_map_all() ?: listOf(null)

                val yy: List<Pair<String,String>> = xtplss.map { xtpls ->
                    coder_types(me, me, xtpls, pre) //+ // HACK-01: x===me above prevents stack overflow
                }.flatten()

                val zz = xtplss.map {
                    listOf(me.n.toString() to me.x_sig(it, pre) + ";\n")
                }.flatten()

                //println(listOf(me.tp.coder(null), xx))

                xx + yy + zz
            }
        }
    }
    val ts = s.dn_collect_pos(::fs, ::fe, ::ft)
    return ts //.unionAll() //.values.joinToString("") //.let { println(it);it }
}

fun coder_protos (pre: Boolean): String {
    fun fs (me: Stmt): List<String> {
        return when (me) {
            !is Stmt.Proto -> emptyList()
            else -> {
                val xtplss: List<Tpl_Map?> = me.template_map_all() ?: listOf(null)
                val cods = xtplss.distinctBy {
                    me.proto(it)
                }.map { xtpls ->
                    val sig = me.x_sig(xtpls, pre)
                    //println(listOf("read", me, G.tsks_blks[me]))
                    """
                    // PROTO | ${me.dump()}
                    $sig {
                        ${me.tp.out.coder(xtpls)} mar_ret;
                        ${(me is Stmt.Proto.Func).cond {
                            me as Stmt.Proto.Func
                            me.tp_.inps_.map { (id,tp) ->
                                if (tp !is Type.Vector) "" else {
                                    val xxid = id.str
                                    """
                                    $xxid.max = ${tp.max!!.coder(xtpls,pre)};
                                    $xxid.cur = MIN($xxid.max, $xxid.cur);                            
                                    """
                                }
                            }.joinToString("")
                        }}
                        do {
                            ${(me !is Stmt.Proto.Func).cond {
                                """
                                if (mar_exe->status != MAR_EXE_STATUS_YIELDED) {
                                    return;
                                }
                                if (mar_exe->pc == 0) {
                                    if (mar_act == MAR_EXE_ACTION_ABORT) {
                                        return;
                                    }
                                    ${me.tp.inps_().mapIndexed { i,vtp ->
                                        val (id,tp) = vtp
                                        assert(tp !is Type.Vector)
                                        id.coder(me.blk,pre) + " = mar_inps->_${i+1};\n"
                                    }.joinToString("")}
                                }
                                """
                            }}
                            ${(me is Stmt.Proto.Coro).cond { """
                                switch (mar_exe->pc) {
                                    case 0:
                            """ }}
                            ${me.blk.coder(xtpls,pre)}
                            ${(me is Stmt.Proto.Coro).cond { """
                                }
                            """ }}
                        } while (0);
                        ${when (me) {
                            is Stmt.Proto.Func -> "return mar_ret;"
                            is Stmt.Proto.Coro -> {
                                val (xuni,_) = me.tp.x_out(null,pre)
                                """
                                    mar_exe->status = MAR_EXE_STATUS_COMPLETE;
                                    if (mar_act != MAR_EXE_ACTION_ABORT) {
                                        *mar_out = ($xuni) { .tag=2, ._2=mar_ret };
                                    }
                                """
                            }
                            is Stmt.Proto.Task -> """
                                mar_exe->status = MAR_EXE_STATUS_COMPLETE;
                                if (mar_act != MAR_EXE_ACTION_ABORT) {
                                    Event mar_${me.n} = { .Event_Task={mar_exe} };
                                    mar_broadcast(MAR_TAG_Event_Task, &mar_${me.n});
                                }
                                return;
                            """
                        }}
                    }
                    """
                }
                listOf(cods.joinToString("") + "\n")
            }
        }
    }
    val ts = G.outer!!.dn_collect_pre(::fs, {null}, {null})
    return ts.joinToString("")
}

fun Stmt.coder (tpls: Tpl_Map?, pre: Boolean): String {
    return when (this) {
        is Stmt.Data  -> ""
        is Stmt.Proto -> ""

        is Stmt.Block  -> {
            val escs = this.dn_collect_pre({
                // TODO: should consider nested matching do/escape
                when (it) {
                    is Stmt.Proto  -> null
                    is Stmt.Escape -> listOf(it)
                    else -> emptyList()
                }
            }, {null}, {null}).let { !it.isEmpty() }
            val exes = this.to_dcls().filter { (_,_,tp) -> tp is Type.Exec }
            val tsks = exes.filter { (_,_,tp) -> tp is Type.Exec.Task }
            val defers = G.defers.getOrDefault(this, emptyList())

            val ss = this.ss.map { it.coder(tpls,pre) }.joinToString("\n")
            val body = """
                ${defers.map {
                    val id = "defer_${it.n}".coder(this,pre)
                    """
                    ${(this.up_exe() == null).cond { "int" }} $id = 0;   // defer not yet reached
                    """
                }.joinToString("")}
                ${exes.map { (_,id,tp) ->
                    val x = id.coder(this,pre)
                    """
                        ${(this.up_exe() == null).cond { " ${tp!!.coder(tpls)} $x;" }}
                        $x.pro = NULL;  // uninitialized Exec
                        """
                }.joinToString("")}
                ${(this == G.outer).cond { """
                    {
                    ${tsks.map { (_,id,_) ->
                        val x = id.coder(this,pre)
                        """
                        MAR_BROADCAST_TS[MAR_BROADCAST_N++] = (MAR_Task*) &$x;
                        """
                    }.joinToString("")}
                    }
                """ }}
                do {
                    ${this.to_dcls().map { (s,_,tp) ->
                        if (tp !is Type.Proto.Func) emptyList() else {
                            s as Stmt.Proto
                            val xtplss: List<Tpl_Map?> = s.template_map_all() ?: listOf(null)
                            xtplss.distinctBy {
                                s.proto(it)
                            }.map {
                                "//auto " + s.x_sig(it, pre) + ";\n"
                            }
                        }
                    }.flatten().joinToString("")}
                    $ss
                } while (0);
                ${exes.map { (_,id,tp) ->
                    val exe = id.coder(this,pre)
                    val args = if (tp is Type.Exec.Coro) "NULL, NULL" else "0, NULL"
                    """
                        if ($exe.pro != NULL) {
                            $exe.pro(MAR_EXE_ACTION_ABORT, &$exe, NULL, $args);
                        }
                        """
                }.joinToString("")}
                ${defers.reversed().map {
                    val id = "defer_${it.n}".coder(this,pre)
                    """
                    if ($id) {     // defer if true: reached, finalize
                        ${it.blk.coder(tpls,pre)}
                    }
                    """
                }.joinToString("")}
                if (MAR_EXCEPTION.tag != __MAR_EXCEPTION_NONE__) {
                    continue;
                }
                ${escs.cond { """
                    if (MAR_ESCAPE.tag == __MAR_ESCAPE_NONE__) {
                        // no escape
                    ${this.esc.cond { """
                        } else if (mar_sup(MAR_TAG_${it.ts.coder(null,pre)}, MAR_ESCAPE.tag)) {
                            MAR_ESCAPE.tag = __MAR_ESCAPE_NONE__;   // caught escape: go ahead
                            ${(it.ts.first().str == "Break").cond { """
                                goto MAR_LOOP_STOP_${this.xup!!.n};
                            """ }}                        
                    """ }}
                    } else {
                        continue; // uncaught escape: propagate up
                    }
                """ }}
            """

            if (G.tsks_blks_awts[this] == null) {
                """
                { // BLOCK | ${this.dump()}
                    $body
                }
                """
            } else {
                val enus = this.ups_until { it is Stmt.Proto }
                    .filter { it is Stmt.Block }
                    .drop(1)  // skip outermost block
                    .map { G.tsks_blks_awts[it]!! }
                //val hex = enus
                //    .map { it.toString(16) }
                //    .joinToString("")
                //val depth = (7 - enus.size)
                val enu = G.tsks_blks_awts[this]!!.toString(16)
                """
                { // BLOCK [${enus.joinToString(",")}] | ${this.dump()}
                    ${(this.xup !is Stmt.Proto).cond {
                        // skip outermost block
                        "case 0x$enu:"
                    }}
                        // BROADCAST
                        if ((mar_exe->pc >> (4*${(7 - enus.size)})) != 0) {
                            ${exes.map { (_, id, _) ->
                                val exe = id.coder(this, pre)
                                """
                                $exe.pro(MAR_EXE_ACTION_RESUME, &$exe, NULL, mar_evt_tag, mar_evt_pay);
                                if (mar_exe->status == MAR_EXE_STATUS_COMPLETE) {
                                    return;
                                }
                                if (MAR_EXCEPTION.tag != __MAR_EXCEPTION_NONE__) {
                                    continue;
                                }
                                """
                            }.joinToString("")}
                        }
                        switch (mar_exe->pc >> (4*${7 - enus.size})) {
                            case 0:
                                mar_exe->status = MAR_EXE_STATUS_RUNNING;
                                $body
                                break;
                        }
                    /*break;*/
                }
                """
            }
        }
        is Stmt.Dcl    -> {
            val dcl = when {
                (this.xtp is Type.Exec) -> ""
                (this.up_exe() != null) -> ""
                else -> this.xtp!!.coder(tpls) + " " + this.id.str + ";"
            }
            val ini = this.xtp.let {
                if (it !is Type.Vector) "" else """
                    ${this.id.str}.max = ${it.max!!.coder(tpls,pre)};
                    ${this.id.str}.cur = 0;
                """
            }
            dcl + ini
        }
        is Stmt.SetE   -> {
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
        is Stmt.SetS   -> this.src.coder(tpls,pre)

        is Stmt.Escape -> """
            MAR_ESCAPE = CAST(Escape, ${this.e.coder(tpls,pre)});
            continue;            
        """
        is Stmt.Defer  -> {
            val bup = this.up_first { it is Stmt.Block } as Stmt.Block
            val id = "defer_$n".coder(bup,pre)
            """
            $id = 1;   // defer reached
            """
        }
        is Stmt.Catch  -> {
            val uni = this.type()?.coder(tpls)
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
                        ${set.dst.coder(tpls,pre)} = ($uni) { .tag=MAR_TAG_${uni}_Ok };
                        """
                     }}
                } else if (
                    ${this.tp.cond2({
                        "mar_sup(MAR_TAG_${it.ts.coder(null,pre)}, MAR_EXCEPTION.tag)"
                    },{
                        "true"
                    })}
                ) {
                    ${(this.xup is Stmt.SetS && this.tp!=null).cond {
                        val set = this.xup as Stmt.SetS
                        """
                        ${set.dst.coder(tpls,pre)} = ($uni) { .tag=MAR_TAG_${uni}_Err, .Err=CAST(${this.tp!!.coder(tpls)}, MAR_EXCEPTION) };
                        """
                     }}
                    MAR_EXCEPTION.tag = __MAR_EXCEPTION_NONE__;
                } else {
                    continue;
                }
            }
            """
        }
        is Stmt.Throw  -> """
            assert(sizeof(Exception) >= sizeof(${this.e.typex().coder(tpls)}));
            MAR_EXCEPTION = CAST(Exception, ${this.e.coder(tpls,pre)});
            continue;
        """

        is Stmt.Create -> {
            val tp = this.pro.typex() as Type.Proto
            val exe = tp.to_exe()!!.coder(null)
            (this.xup is Stmt.SetS).cond {
                val set = this.xup as Stmt.SetS
                """
                ${set.dst.coder(tpls,pre)} =
                """
            } + """
            // CREATE | ${this.dump()}
            ($exe) { 0, MAR_EXE_STATUS_YIELDED, (${tp.x_sig(pre)}) ${this.pro.coder(tpls,pre)} ${(tp is Type.Proto.Task).cond {", 0"}}, {} };
            """
        }
        is Stmt.Start  -> {
            val tp = this.exe.type() as Type.Exec
            val exe = this.exe.coder(tpls,pre)
            val (xinps,_) = tp.inps().x_inp_tup(tp.tk,null,pre)
            val (xouni,_) = tp.x_out(null,pre)
            (this.xup is Stmt.SetS).cond {
                val set = this.xup as Stmt.SetS
                """
                ${set.dst.coder(tpls,pre)} =
                """
            } + """
            // START | ${this.dump()}
            ({
                typeof($exe)* mar_exe_$n = &$exe;
                $xinps mar_inps_$n = { ${this.args.map { it.coder(tpls,pre) }.joinToString(",")} };
                ${(tp is Type.Exec.Coro).cond { """
                    $xouni mar_out_$n;
                    mar_exe_$n->pro(MAR_EXE_ACTION_RESUME, mar_exe_$n, &mar_inps_$n, NULL, &mar_out_$n);
                    mar_out_$n;
                """}}
                ${(tp is Type.Exec.Task).cond { """
                    mar_exe_$n->pro(MAR_EXE_ACTION_RESUME, mar_exe_$n, &mar_inps_$n, 0, NULL);
                """}}
            });
            """
        }
        is Stmt.Resume -> {
            val exe = this.exe.coder(tpls,pre)
            val tp = this.exe.type() as Type.Exec.Coro
            val res = tp.res().coder(null)
            val (xouni,_) = tp.x_out(null, pre)
            (this.xup is Stmt.SetS).cond {
                val set = this.xup as Stmt.SetS
                """
                ${set.dst.coder(tpls,pre)} =
                """
            } + """
            ({
                // RESUME | ${this.dump()}
                $xouni mar_out_$n;
                $res mar_res_$n = ${this.arg.coder(tpls,pre)};
                $exe.pro(MAR_EXE_ACTION_RESUME, &$exe, NULL, &mar_res_$n, &mar_out_$n);
                mar_out_$n;
            });
            """
        }
        is Stmt.Yield  -> {
            val tp = (this.up_first { it is Stmt.Proto.Coro } as Stmt.Proto.Coro).tp_
            val (xuni,_) = tp.x_out(null,pre)
            """
                mar_exe->status = MAR_EXE_STATUS_YIELDED;
                mar_exe->pc = ${this.n};
                *mar_out = ($xuni) { .tag=1, ._1=${this.arg.coder(tpls,pre)} };
                return;
            case ${this.n}:
                if (mar_act == MAR_EXE_ACTION_ABORT) {
                    continue;
                }
                mar_exe->status = MAR_EXE_STATUS_RUNNING;
                ${(this.xup is Stmt.SetS).cond {
                    val set = this.xup as Stmt.SetS
                    """
                    ${set.dst.coder(tpls,pre)} = *mar_res;
                    """
                }}
            """
        }
        is Stmt.Await  -> {
            val enus = this.ups_until { it is Stmt.Proto }
                .filter { it is Stmt.Block }
                .drop(1)  // skip outermost block
                .map { G.tsks_blks_awts[it]!! }
            val hex = (enus + G.tsks_blks_awts[this]!!).map {
                it.toString(16)
            }.joinToString("")
            val hexs = hex + "0".repeat(7-enus.size);
            """
            // AWAIT [${enus.joinToString(",")}] | ${this.dump()}
                mar_exe->pc = 0x$hexs;
                ${when (this) {
                    is Stmt.Await.Data -> """
                        mar_exe->status = MAR_EXE_STATUS_YIELDED;
                        return;
            case 0x$hex:
                        // Stmt.Await.Data
                        if (mar_act == MAR_EXE_ACTION_ABORT) {
                            continue;
                        }
                        if (mar_evt_tag != MAR_TAG_${this.tp.path("_")}) {
                            return;
                        }
                        if (!(${this.cnd.coder(null,pre)})) {
                            return;
                        }
                        mar_exe->status = MAR_EXE_STATUS_RUNNING;
                    """
                    is Stmt.Await.Task -> {
                        val exe = this.exe.coder(null,pre)
                        """
                            if ($exe.status != MAR_EXE_STATUS_COMPLETE) {
                                mar_exe->evt = (uintptr_t) & $exe;
                                mar_exe->status = MAR_EXE_STATUS_YIELDED;
                                return;
            case 0x$hex:
                                // Stmt.Await.Task
                                if (mar_act == MAR_EXE_ACTION_ABORT) {
                                    continue;
                                }
                                if (
                                    (mar_evt_tag != MAR_TAG_Event_Task) ||
                                    (mar_exe->evt != (uintptr_t)((Event*)mar_evt_pay)->Event_Task.tsk)
                                ) {
                                    return;
                                }
                            }
                            mar_exe->status = MAR_EXE_STATUS_RUNNING;
                        """
                    }
                    is Stmt.Await.Clock -> """
                        mar_exe->status = MAR_EXE_STATUS_YIELDED;
                        mar_exe->evt = ${this.ms.coder(null,pre)};
                        return;
            case 0x$hex:
                        // Stmt.Await.Clock
                        if (mar_act == MAR_EXE_ACTION_ABORT) {
                            continue;
                        }
                        if (mar_evt_tag != MAR_TAG_Event_Clock) {
                            return;
                        }
                        mar_exe->evt -= ((Event*)mar_evt_pay)->Event_Clock.ms;
                        if (mar_exe->evt > 0) {
                            return;
                        }
                        mar_exe->status = MAR_EXE_STATUS_RUNNING;
                    """
                    is Stmt.Await.Bool -> when (this.tk.str) {
                        "false" -> """
                            mar_exe->status = MAR_EXE_STATUS_YIELDED;
                            return;
            case 0x$hex:
                            // Stmt.Await.Bool(false)
                            if (mar_act == MAR_EXE_ACTION_ABORT) {
                                continue;
                            }
                            return;
                        """
                        "true" -> """
                            mar_exe->status = MAR_EXE_STATUS_YIELDED;
                            return;
            case 0x$hex:
                            // Stmt.Await.Bool(true)
                            if (mar_act == MAR_EXE_ACTION_ABORT) {
                                continue;
                            }
                            mar_exe->status = MAR_EXE_STATUS_RUNNING;
                        """
                        else -> error("impossible case")
                    }
                    is Stmt.Await.Any -> {
                        val nok = this.exes.map {
                            "(${it.coder(tpls,pre)}.status != MAR_EXE_STATUS_COMPLETE)"
                        }.joinToString(" && ")
                        """
                        mar_exe->status = MAR_EXE_STATUS_YIELDED;
                        return;
            case 0x$hex:
                        // Stmt.Await.Any
                        if (mar_act == MAR_EXE_ACTION_ABORT) {
                            continue;
                        }
                        if ($nok) {
                            return;
                        }
                        mar_exe->status = MAR_EXE_STATUS_RUNNING;
                        """
                    }
                }}
                ${(this.xup is Stmt.SetS).cond {
                    val set = this.xup as Stmt.SetS
                    val dst = set.dst.coder(tpls,pre)
                    """
                    $dst = * (typeof($dst)*) mar_evt_pay;
                    """
                }}
            """
        }
        is Stmt.Emit   -> {
            val e = this.e.coder(tpls, pre)
            """
            // EMIT | ${this.dump()}
            typeof($e) mar_$n = $e;
            mar_broadcast(MAR_TAG_${(this.e.typex() as Type.Data).path("_")}, &mar_$n);
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
                ${(this.up_exe() is Stmt.Proto.Task).cond { """
                    mar_exe->pc = 0;
                """ }}
                goto MAR_LOOP_START_${this.n};
                MAR_LOOP_STOP_${this.n}:
        """
        is Stmt.MatchT -> """
            // MATCH | ${this.dump()}
            switch (${this.tst.coder(tpls,pre)}.tag) {
                ${this.cases.map { (tst,e) -> """
                    ${tst.cond2({"case MAR_TAG_${it.ts.coder(null,pre).uppercase()}"},{"default"})}:
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

        is Stmt.Print -> {
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
            """
            // PRINT | ${this.dump()}
            """ + aux(this.e.typex(), this.e.coder(tpls,pre)) + """
                puts("");
            """
        }
        is Stmt.Pass  -> this.e.coder(tpls,pre) + ";"
    }
}

fun Tk.Var.coder (fr: Any, pre: Boolean): String {
    val dcl = this.to_xdcl(fr)!!.first
    //println(listOf(this.str, dcl.to_str()))
    return when {
        (dcl is Stmt.Proto) -> this.str
        (dcl.xup!!.up_exe() != null) -> "mar_exe->mem.${this.str}"
        else -> this.str
    }
}

fun String.coder (fr: Stmt.Block, pre: Boolean): String {
    return if (fr.xup?.up_exe() != null) {
        "mar_exe->mem.$this"
    } else {
        this
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
                            (xtp is Type.Data) -> f(xx, xtp.walk()!!.third, xe1, xe2)
                            (xtp is Type.Tuple) -> {
                                val (uno,op) = if (this.tk.str == "==") Pair(1,"&&") else Pair(0,"||")
                                val vs = xtp.ts.mapIndexed { i,(_,xxtp) ->
                                    //println(xxtp.to_str())
                                    op + " " + f(xx+1, xxtp, "mar_1_${n}_$xx._"+(i+1), "mar_2_${n}_$xx._"+(i+1))
                                }.joinToString("")
                                """({
                                    typeof($xe1) mar_1_${n}_$xx = $xe1;
                                    typeof($xe2) mar_2_${n}_$xx = $xe2;
                                    $uno $vs;
                                })"""
                            }
                            (xtp is Type.Vector) -> {
                                """({
                                    typeof($xe1) mar_1_${n}_$xx = $xe1;
                                    typeof($xe2) mar_2_${n}_$xx = $xe2;
                                    (mar_1_${n}_$xx.cur == mar_2_${n}_$xx.cur) && ({
                                        int mar_ok_${n}_$xx = 1;
                                        for (int i=0; i<mar_1_${n}_$xx.cur; i++) {
                                            if (!${f(xx+1, xtp.tp, "mar_1_${n}_$xx.buf[i]", "mar_2_${n}_$xx.buf[i]")}) {
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
            val (f,xxx) = this.xtpls.let {
                val id = this.f.coder(tpls,pre)
                when {
                    (it == null) -> Pair(id, tpls)
                    it.isEmpty() -> Pair(id, tpls)
                    else -> {
                        val f = this.f as Expr.Acc
                        Pair (
                            id.proto(it),
                            template_map(f.to_xdcl()!!.first.to_tpl_abss(), it)
                        )
                    }
                }
            }
            val inps = this.f.type().let {
                if (it !is Type.Proto) null else {
                    it.inps
                }
            }
            val call = "$f ( ${this.args.mapIndexed { i,arg ->
                val src = arg.coder(xxx,pre)
                val tdst = when {
                    (inps == null) -> null
                    inps[i].has_tpls_dn() -> inps[i].template_apply(xxx!!)
                    else -> inps[i]
                }
                val tsrc = arg.typex().let { 
                    if (!it.has_tpls_dn()) it else {
                        it.template_apply(xxx!!)!!
                    }
                 }
                when {
                    (tdst == null) -> src
                    tdst.is_num() -> src
                    tdst.is_same_of(tsrc) -> src
                    else -> "CAST(${tdst.coder(xxx)}, $src)"
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
            val tp = this.col.typex()
            val (i,_) = tp.discx(this.idx)!!
            val tag = when (tp) {
                is Type.Union -> i + 1
                is Type.Data -> "MAR_TAG_${tp.ts.first().str}_${this.idx}"
                else -> error("impossible case")
            }
            return "(${this.col.coder(tpls,pre)}.tag==$tag)"
        }
        is Expr.Cons   -> {
            val s = this.walk(this.tp.ts)!!.first
            if (s.subs == null) {
                var ret = "({"
                for (i in this.tp.ts.size - 1 downTo 0) {
                    val tp = this.tp.ts.take(i + 1).coder(this.tp.xtpls,pre)
                    ret = ret + "$tp ceu_$i = " +
                            if (i == this.tp.ts.size - 1) {
                                """
                                ((${tp}) ${this.e.coder(tpls,pre)});
                                """
                            } else {
                                val nxt = this.tp.ts[i + 1].str
                                """
                                {
                                    .tag = MAR_TAG_${tp}_${nxt},
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
                "((${this.tp.ts.first().str}) { .tag=MAR_TAG_$ts, .$ts={$vs} })"
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
        is Expr.It -> {
            val tp = this.typex()
            "(*(${tp.coder(null)}*)mar_evt_pay)"
        }
        is Expr.Unit -> "_void_"
        is Expr.Null -> "null"
        is Expr.Bool, is Expr.Chr, is Expr.Str -> this.tk.str
        is Expr.Num -> this.to_str(pre).let {
            if (this.xnum == null) it else {
                val tp = this.typex()
                val sup = tp.sup_vs(this.xnum!!)
                if (sup == tp) it else {
                    "((" + sup!!.coder(tpls) + ")" + it + ")"
                }
            }
        }

        is Expr.If -> "((${this.cnd.coder(tpls,pre)}) ? (${this.t.coder(tpls,pre)}) : (${this.f.coder(tpls,pre)}))"
        is Expr.MatchT -> """
            ${this.typex().coder(TODO())} mar_$n;
            switch (${this.tst.coder(tpls,pre)}.tag) {
                ${this.cases.map { (tst,e) -> """
                    ${tst.cond2({"case MAR_TAG_${it.ts.coder(TODO(),pre)}"},{"default"})}:
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
    val types = coder_types(null, G.outer!!, null, pre)
    val protos = coder_protos(pre)
    val code = G.outer!!.coder(null,pre)
    val c = object{}::class.java.getResourceAsStream("/mar.c")!!.bufferedReader().readText()
        //.replace("// === MAR_TYPES === //", types.toMap().values.joinToString(""))
        .replace("// === MAR_TYPES === //",
            // first reverse to keep first map entry, second reverse to keep original order
            types.distinctBy { it.first }.map { it.second }.joinToString(""))
        .replace("// === MAR_PROTOS === //", protos)
        .replace("// === MAR_MAIN === //", code)
        //.replace("// === MAR_BROADCAST_N === //", "TODO")

    return c
}
