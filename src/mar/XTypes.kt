package mar

fun String.clean (): String {
    return this.replace('*','_')
}

fun Var_Type.coder (tpls: Tpl_Map?, pre: Boolean): String {
    val (id,tp) = this
    return /*tp.coder(tpls) + " " +*/ id.str
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
            (me.xup is Stmt.Data) -> emptyList()
            (me is Stmt.Data) -> {
                val id = me.t.str
                val N = G.datas++
                //val (S, _, tpc) = XX.walk_tpl()
                val tpc = me.tp
                val ts = tpc.dn_collect_pos({ emptyList() }, ::ft)
                (ts + listOf(id to when {
                    (me.subs == null) -> {
                        fun f(tp: Type, s: List<String>): List<String> {
                            val ss = id+s.drop(1).map { "_"+it }.joinToString("")
                            val x1 = """
                                #define MAR_TAG_${me.t.str} $N
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
                        f(tpc, listOf(me.t.str)).joinToString("")                    }
                    else -> {
                        val sup = me.t.str
                        fun f(s: Stmt.Data, sup: String, l: List<Int>): String {
                            val xid = (if (sup == "") "" else sup + "_") + s.t.str
                            //println(listOf(me.tk.pos, me.to_str()))
                            assert(l.size <= 6)
                            var n = 0
                            var k = 25
                            for (i in 0..l.size - 1) {
                                n += l[i] shl k
                                k -= 5
                            }
                            //println(listOf(xid, n, n.toString(2)))
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

                        val xtplss: List<Tpl_Map?> = me.template_map_all() ?: listOf(null)
                        //println(xtplss)
                        val tpl = me.tpls.map { (id, _) -> id.str }.zip(emptyList<Tpl_Con>()).toMap()
                        f(me, "", listOf(N)) + """
                            typedef struct ${sup} {
                                union {
                                    struct {
                                        int tag;
                                        union {
                                            ${g(tpl, null, me, 1)}
                                        };
                                    };
                                };
                            } $sup;
                        """
                    }
                }))
            }
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
