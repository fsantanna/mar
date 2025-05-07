package mar

fun Stmt.infer (tpe: Type?): Type? {
    return when (this) {
        is Stmt.Catch -> if (this.tp == null) null else {
            Type.Union(this.tk, true, listOf(
                Pair(Tk.Type("Ok",this.tk.pos), Type.Unit(this.tk)),
                Pair(Tk.Type("Err",this.tk.pos), this.tp)
            ))
        }

        is Stmt.Create -> {
            val xtp = tpe?.to_exe()
            this.pro.infer(xtp)?.to_exe() ?: tpe
        }
        is Stmt.Start -> {
            val exe = this.exe.infer(null)
            when (exe) {
                is Type.Exec.Coro -> {
                    this.args.mapIndexed { i,e ->
                        e.infer(exe.inps[i])
                    }
                    Type.Union(this.tk, true, listOf(exe.yld,exe.out).map { Pair(null,it) })
                }
                is Type.Exec.Task -> {
                    this.args.mapIndexed { i,e ->
                        e.infer(exe.inps[i])
                    }
                    Type.Union(this.tk, true, listOf(Type.Unit(this.tk),exe.out).map { Pair(null,it) })
                }
                else -> {
                    this.args.map {
                        it.infer(null)
                    }
                    null
                }
            }
        }
        is Stmt.Resume -> {
            val exe = this.exe.infer(null)
            when (exe) {
                is Type.Exec.Coro -> {
                    this.arg.infer(exe.res)
                    Type.Union(this.tk, true, listOf(exe.yld,exe.out).map { Pair(null,it) })
                }
                else -> {
                    this.arg.infer(null)
                    null
                }
           }
        }
        is Stmt.Yield -> {
            val coro = (this.up_first { it is Stmt.Proto.Coro } as Stmt.Proto.Coro).tp_
            this.arg.infer(coro.yld)
            coro.res
        }
        is Stmt.Await -> this.typex()

       else -> error("impossible case")
   }
}

fun Expr.infer (tpe: Type?): Type? {
    //tp?.assert_no_tpls()
    when (this) {
        is Expr.Acc, is Expr.Bool, is Expr.Str, is Expr.Chr,
        is Expr.Null, is Expr.Unit, is Expr.Num -> {}

        is Expr.It -> {
            val dat = this.up_first { it is Stmt.Await.Data } as Stmt.Await.Data
            this.xtp = dat.tp
            this.xtp
        }

        is Expr.Nat -> {
            if (this.xtp == null) {
                this.xtp = tpe ?: Type.Any(this.tk)
            }
            this.xtp?.infer(null)
        }

        is Expr.Table -> {
            val up = this.xtp.sub_vs_null(tpe)
            //val up = this.xtp ?: tpx
            //println(listOf("infer-tuple", this.to_str(), this.xtp?.to_str(), xtp?.to_str()))
            //println(listOf(up?.to_str(), upx?.to_str()))
            val vs = this.vs.mapIndexed { i,(tk,e) ->
                Pair(tk, e.infer(if (up !is Type.Tuple || up.ts.size<i+1) null else up.ts[i].second))
            }
            if (vs.any { it.second == null }) {
                // infer error
            } else {
                vs as List<Pair<Tk.Var?, Type>>
                val dn = Type.Tuple(this.tk,
                    if (up !is Type.Tuple) {
                        vs
                    } else {
                        vs.zip(up.ts).map { (vs,ts) ->
                            Pair(vs.first ?: ts.first, vs.second)
                        }
                    }
                )
                //println(listOf("infer-tuple-dn", dn.to_str()))
                if (this.xtp == null) {
                    this.xtp = if (up !is Type.Tuple) {
                        dn
                    } else {
                        //println(listOf("infer-tuple-dn", up.to_str(), dn.to_str()))
                        (up.sub_vs(dn) ?: dn) as Type.Tuple  // up first b/c of int/float
                    }
                }
            }
            this.xtp?.infer(null)
        }
        is Expr.Field -> this.col.infer(null)
        is Expr.Index -> {
            this.col.infer(Type.Prim(Tk.Type("Int",this.tk.pos)))
            this.col.infer(null)
        }
        is Expr.Pred -> this.col.infer(null)
        is Expr.Disc -> this.col.infer(null)
        is Expr.Cons -> {
            //println(listOf("infer-cons",this.to_str(),tp.to_str()))
            val (s,_,xtp) = this.tp.walk_tpl()
            val e = this.e.infer(xtp)
            when {
                (this.tp.xtpls != null) -> {}
                (e == null) -> {}
                else -> this.tp.infer(this.tp.abs_con(s, e.template_con_abs(xtp)))
            }
        }

        is Expr.Uno -> this.e.infer(tpe)
        is Expr.Bin -> {
            val xtp = when (this.tk.str) {
                "+", "-", "*", "/", "%", "++" -> tpe
                else -> null
            }
            this.e1.infer(xtp)
            this.e2.infer(xtp)
        }
        is Expr.Call -> {
            val f = this.f.infer(null)
            if (f is Type.Proto) {
                this.args.forEachIndexed { i,e ->
                    e.infer(if (i < f.inps.size) f.inps[i] else null)
                }
            } else {
                this.args.forEach {
                    it.infer(null)
                }
            }
            this.xtpls = this.xtpls ?: emptyList()  // TODO: infer
        }

        is Expr.If -> {
            this.cnd.infer(null)
            val t = this.t.infer(tpe)
            val f = this.f.infer(tpe)
            if (t!=null && f!=null) {
                this.xtp = t.sup_vs(f)
            }
            this.xtp?.infer(null)
        }
        is Expr.MatchT -> {
            val tst = this.tst.infer(null)
            val cases = this.cases.map { (dat,e) ->
                e.infer(tpe)
                val fst = if (dat == null) Type.Any(this.tk) else dat
                Pair(fst, e.type())
            }
            if (tst!=null && !cases.any { (a,b) -> b==null }) {
                val es = cases.map { it.second } as List<Type>
                val fst: Type? = es.first()
                this.xtp = es.fold(fst) { a,b -> a?.sup_vs(b) }
            }
            this.xtp?.infer(null)
        }
        is Expr.MatchE -> {
            val tst = this.tst.infer(null)
            val cases = this.cases.map { (e1,e2) ->
                val fst = if (e1 == null) Type.Any(this.tk) else e1.infer(tst)
                e2.infer(tpe)
                Pair(fst, e2.type())
            }
            if (tst!=null && !cases.any { (a,b) -> a==null||b==null }) {
                val es = cases.map { it.second } as List<Type>
                val fst: Type? = es.first()
                this.xtp = es.fold(fst) { a,b -> a?.sup_vs(b) }
            }
            this.xtp?.infer(null)
        }
    }

    val xtp = this.type()
    //println(xtp)
    this.xnum = when {
        (xtp == null) -> null
        (tpe == null) -> null
        !xtp.is_num() -> null
        !tpe.is_num() -> null
        else -> tpe
    }
    return xtp
}

fun Type.infer (tpe: Type?): Type {
    when (this) {
        is Type.Any, is Type.Bot, is Type.Top,
        is Type.Tpl, is Type.Nat,
        is Type.Unit, is Type.Prim -> {}
        is Type.Pointer -> {
            if (tpe is Type.Pointer) {
                this.ptr.infer(tpe.ptr)
            }
        }
        is Type.Tuple -> {
            if (tpe is Type.Tuple) {
                this.ts.zip(tpe.ts).forEach { (a,b) -> a.second.infer(b.second) }
            }
        }
        is Type.Vector -> {
            if (tpe is Type.Vector) {
                this.tp.infer(tpe.tp)
            }
        }
        is Type.Union -> {
            if (tpe is Type.Union) {
                this.ts.zip(tpe.ts).forEach { (a,b) -> a.second.infer(b.second) }
            }
        }
        is Type.Proto.Func -> {
            if (tpe is Type.Proto.Func) {
                this.inps.zip(tpe.inps).forEach { (a,b) -> a.infer(b) }
                this.out.infer(tpe.out)
            }
        }
        is Type.Proto.Coro -> {
            if (tpe is Type.Proto.Coro) {
                this.inps.zip(tpe.inps).forEach { (a,b) -> a.infer(b) }
                this.res.infer(tpe.out)
                this.yld.infer(tpe.out)
                this.out.infer(tpe.out)
                if (this.xpro == null) {
                    this.xpro = tpe.xpro
                }
            }
        }
        is Type.Proto.Task -> {
            if (tpe is Type.Proto.Task) {
                this.inps.zip(tpe.inps).forEach { (a,b) -> a.infer(b) }
                this.out.infer(tpe.out)
                if (this.xpro == null) {
                    this.xpro = tpe.xpro
                }
            }
        }
        is Type.Exec.Coro -> {
            if (tpe is Type.Exec.Coro) {
                this.inps.zip(tpe.inps).forEach { (a,b) -> a.infer(b) }
                this.res.infer(tpe.out)
                this.yld.infer(tpe.out)
                this.out.infer(tpe.out)
                if (this.xpro == null) {
                    this.xpro = tpe.xpro
                }
            }
        }
        is Type.Exec.Task -> {
            if (tpe is Type.Exec.Task) {
                this.inps.zip(tpe.inps).forEach { (a,b) -> a.infer(b) }
                this.out.infer(tpe.out)
                if (this.xpro == null) {
                    this.xpro = tpe.xpro
                }
            }
        }
        is Type.Data -> {
            val (s,_,_) = this.walk()!!
            when {
                (this.xtpls != null) -> {}
                s.tpls.isEmpty() -> this.xtpls = emptyList()
                (tpe !is Type.Data) -> {}
                (tpe.xtpls != null) -> {
                    this.xtpls = tpe.xtpls
                    assert(this.is_same_of(tpe), {"TODO: unmatching infer"})
                }
                else -> TODO("infer tpls")
            }
        }
    }
    return this
}

fun infer_apply () {
    fun Any.set () {
        val dst = when (this) {
            is Stmt.SetS -> this.dst
            is Stmt.SetE -> this.dst
            else -> error("impossible case")
        }
        val xdst = dst.infer(null)
        val xsrc = when (this) {
            is Stmt.SetS -> this.src.infer(xdst)
            is Stmt.SetE -> this.src.infer(xdst)
            else -> error("impossible case")
        }
        //println(listOf("set", this.to_str(), dst.to_str(), xdst?.to_str(), xsrc?.to_str()))

        // any missing type information?
        val xxdst = when {
            (xdst == null) -> true
            (xdst is Type.Data && xdst.xtpls==null) -> true
            (xdst is Type.Proto.Coro && xdst.xpro==null) -> true
            (xdst is Type.Proto.Task && xdst.xpro==null) -> true
            (xdst is Type.Exec.Coro && xdst.xpro==null) -> true
            (xdst is Type.Exec.Task && xdst.xpro==null) -> true
            else -> false
        }

        if (xxdst && xsrc!=null) {
            if (dst is Expr.Acc) {
                val dcl = dst.to_xdcl()!!.first
                when (dcl) {
                    is Stmt.Dcl -> {
                        dcl.xtp = xsrc
                        dcl.xtp!!.infer(null)
                    }
                    is Stmt.Proto.Coro -> {
                        if (dcl.tp_.xpro == null) {
                            dcl.tp_.xpro = xsrc.xpro()!!
                        }
                    }
                    is Stmt.Proto.Task -> {
                        if (dcl.tp_.xpro == null) {
                            dcl.tp_.xpro = xsrc.xpro()!!
                        }
                    }
                    else -> {}
                }
           } else {
                dst.infer(null)
           }
        }
    }

    G.outer!!.dn_visit_pos({ me ->
       when (me) {
           is Stmt.Data -> {}
           is Stmt.Proto.Func -> me.tp_.infer(null)
           is Stmt.Proto.Coro -> {
               me.tp_.infer(null)
               if (me.tp_.xpro == null) {
                   me.tp_.xpro = me.id
               }
           }
           is Stmt.Proto.Task -> {
               me.tp_.infer(null)
               if (me.tp_.xpro == null) {
                   me.tp_.xpro = me.id
               }
           }

           is Stmt.Block -> {
               me.esc?.infer(null)
           }
           is Stmt.Dcl -> {
               me.xtp?.infer(null)
           }
           is Stmt.SetE -> me.set()
           is Stmt.SetS -> me.set()

           is Stmt.Escape -> me.e.infer(null)
           is Stmt.Defer -> {}
           is Stmt.Catch -> {
               me.tp?.infer(null)
               if (me.xup !is Stmt.SetS) {
                   me.infer(null)
               }
           }
           is Stmt.Throw -> me.e.infer(null)

           is Stmt.If -> me.cnd.infer(Type.Prim(Tk.Type("Bool",me.tk.pos)))
           is Stmt.Loop -> {}
           is Stmt.MatchT -> {
               me.tst.infer(null)
               me.cases.forEach { (t,_) -> t?.infer(null) }
           }
           is Stmt.MatchE -> {
               val tst = me.tst.infer(null)
               me.cases.forEach {
                   it.first?.infer(tst)
               }
           }

           is Stmt.Create -> {
               if (me.xup !is Stmt.SetS) {
                   me.infer(null)
               }
           }
           is Stmt.Start -> {
               if (me.xup !is Stmt.SetS) {
                   me.infer(null)
               }
           }
           is Stmt.Resume -> {
               if (me.xup !is Stmt.SetS) {
                   me.infer(null)
               }
           }
           is Stmt.Yield -> {
               if (me.xup !is Stmt.SetS) {
                   me.infer(null)
               }
           }
           is Stmt.Await -> {
               if (me.xup !is Stmt.SetS) {
                   me.infer(null)
               }
               if (me is Stmt.Await.Data) {
                    me.cnd.infer(Type.Prim(Tk.Type("Bool",me.tk.pos)))
               }
           }
           is Stmt.Emit -> me.e.infer(null)
           is Stmt.Pass -> me.e.infer(Type.Unit(me.tk))
       }
   }, {}, { me ->
        when (me) {
            is Type.Data -> {
                val (s,_,_) = me.walk()!!
                if (s.tpls.isEmpty()) {
                    me.xtpls = emptyList()
                }
            }
            else -> {}
        }
   })
}

fun infer_check () {
    G.outer!!.dn_visit_pos({ me ->
        me.dn_collect_pre({if (me==it) emptyList<Unit>() else null}, {
            val xtp = when (it) {
                is Expr.Table  -> it.xtp
                is Expr.If     -> it.xtp
                is Expr.MatchT -> it.xtp
                is Expr.MatchE -> it.xtp
                else -> Unit
            }
            /*
                println("-=-=-")
                println(me.to_str())
                println(it.to_str())
                println(xtp)
            */
            if (xtp == null) {
                err(it.tk, "inference error : unknown type")
            }
            emptyList()
        }, {null})
    }, {}, {})

    G.outer!!.dn_visit_pos({
        when (it) {
            is Stmt.Dcl -> {
                //println(listOf(it.to_str(), it.xtp?.to_str()))
                if (it.xtp==null || it.xtp is Type.Any) {
                    err(it.id, "inference error : unknown type")
                }
            }
            else -> {}
        }
    }, {}, {
        val ok = when (it) {
            is Type.Proto.Coro, is Type.Proto.Task, is Type.Exec.Coro, is Type.Exec.Task -> (it.xpro() != null)
            else -> true
        }
        if (!ok) {
            err(it.tk, "inference error : unknown size")
        }
    })
}
