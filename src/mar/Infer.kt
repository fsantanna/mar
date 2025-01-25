package mar

fun Stmt.infer (tp: Type?): Type? {
    return when (this) {
        is Stmt.Catch -> if (this.tp == null) null else {
            Type.Union(this.tk, true, listOf(
                Pair(Tk.Type("Ok",this.tk.pos), Type.Unit(this.tk)),
                Pair(Tk.Type("Err",this.tk.pos), this.tp)
            ))
        }

        is Stmt.Create -> {
            val xtp = if (tp !is Type.Exec) null else {
                Type.Proto.Coro(tp.tk, tp.inps, tp.res, tp.yld, tp.out)
            }
            this.co.infer(xtp).let {
                if (it !is Type.Proto.Coro) tp else {
                    Type.Exec(co.tk, it.inps, it.res, it.yld, it.out)
                }
            }
        }
        is Stmt.Start -> {
            val exe = this.exe.infer(null)
            if (exe is Type.Exec) {
                this.args.mapIndexed { i,e ->
                    e.infer(exe.inps[i])
                }
                Type.Union(this.tk, true, listOf(exe.yld,exe.out).map { Pair(null,it) })
            } else {
                this.args.map {
                    it.infer(null)
                }
                null
            }
        }
        is Stmt.Resume -> {
           val exe = this.exe.infer(null)
           if (exe is Type.Exec) {
               this.arg.infer(exe.res)
                Type.Union(this.tk, true, listOf(exe.yld,exe.out).map { Pair(null,it) })
           } else {
               this.arg.infer(null)
               null
           }
        }
        is Stmt.Yield -> {
            val coro = (this.up_first { it is Stmt.Proto.Coro } as Stmt.Proto.Coro).tp_
            this.arg.infer(coro.yld)
            coro.res
        }

       else -> error("impossible case")
   }
}

fun Expr.infer (tpx: Type?): Type? {
    val tp = when {
        (tpx == null) -> null
        (tpx.has_tpls_dn()) -> null
        else -> tpx
    }

    //tp?.assert_no_tpls()
    when (this) {
        is Expr.Acc, is Expr.Bool, is Expr.Str, is Expr.Chr,
        is Expr.Null, is Expr.Unit, is Expr.Num -> {}

        is Expr.Nat -> {
            if (this.xtp == null) {
                this.xtp = tp ?: Type.Any(this.tk)
            }
            this.xtp?.infer(null)
        }

        is Expr.Tuple -> {
            val up = this.xtp ?: tp
            val dn = if (up !is Type.Tuple) {
                val vs = this.vs.map { (tk,e) ->
                    Pair(tk, e.infer(null))
                }
                if (vs.any { it.second == null }) null else {
                    Type.Tuple(this.tk, vs as List<Pair<Tk.Var?, Type>>)
                }
            } else {
                val vs = this.vs.mapIndexed { i,(tk,e) ->
                    Pair(tk, e.infer(if (up.ts.size<i+1) null else up.ts[i].second))
                }
                if (vs.any { it.second == null }) null else {
                    vs as List<Pair<Tk.Var?, Type>>
                    Type.Tuple(this.tk,
                        vs.zip(up.ts).map { (vs,ts) ->
                            Pair(vs.first ?: ts.first, vs.second)
                        }
                    )
                }
            }
            if (dn == null) null else {
                if (this.xtp == null) {
                    this.xtp = if (tp is Type.Tuple) tp else dn // b/c of int/float
                }
            }
            this.xtp?.infer(null)
        }
        is Expr.Vector -> {
            val up = this.xtp ?: tp
            val xup = if (up !is Type.Vector) null else up.tp
            val dn = if (this.vs.size == 0) null else {
                val vs = this.vs.map { it.infer(xup) }
                val v = vs.fold(vs.first()) { a,b ->
                    if (a==null||b==null) null else a.sup_vs(b)
                }
                //println(v?.to_str())
                if (v == null) null else {
                    Type.Vector(this.tk, Expr.Num(Tk.Num(vs.size.toString(),this.tk.pos)), v)
                }
            }
            if (dn != null) {
                if (this.xtp == null) {
                    this.xtp = dn
                }
            }
            this.xtp?.infer(null)
        }
        is Expr.Field -> this.col.infer(null)
        is Expr.Index -> {
            this.col.infer(Type.Prim(Tk.Type("Int",this.tk.pos)))
            this.col.infer(null)
        }
        is Expr.Union -> {
            val up = this.xtp ?: tp
            val sub = if (up !is Type.Union) null else {
                up.disc(this.idx).nulls().second
            }
            val dn = this.v.infer(sub)
            if (dn!=null && this.xtp==null) {
                this.xtp = (if (up is Type.Union) up else null)
            }
            this.xtp?.infer(null)
        }
        is Expr.Pred -> this.col.infer(null)
        is Expr.Disc -> this.col.infer(null)
        is Expr.Cons -> {
            val (s,_,xtp) = this.walk(null,this.tp.ts)!!
            val e = this.e.infer(xtp)
            //println(listOf(this.tp.to_str(), e?.to_str()))
            val t = if (this.tp.xtpls != null) this.tp else {
                val tpl = if (e == null) null else {
                    println(listOf(s.tp.to_str(),e.to_str()))
                    val m = s.tp.template_unresolve(e)
                    println(m)
                    s.tpls.map {
                        m[it.first.str] ?: Pair(null,null)
                    }
                }
                Type.Data(this.tp.tk, tpl, this.tp.ts)
            }
            println(listOf(e?.to_str(), t.to_str()))
            this.tp.infer(t)
        }

        is Expr.Uno -> this.e.infer(tp)
        is Expr.Bin -> {
            val xtp = when (this.tk.str) {
                "+", "-", "*", "/", "%", "++" -> tp
                else -> null
            }
            this.e1.infer(xtp)
            this.e2.infer(xtp)
        }
        is Expr.Call -> {
            val f = this.f.infer(null)
            if (f is Type.Proto) {
                this.args.mapIndexed { i,e ->
                    if (i < f.inps.size) {
                        e.infer(f.inps[i])
                    }
                }
            } else {
                this.args.map {
                    it.infer(null)
                }
            }
        }
        is Expr.Throw -> {
            this.xtp = tp
            this.e.infer(null)
            this.xtp?.infer(null)
        }

        is Expr.If -> {
            val cnd = this.cnd.infer(null)
            val t = this.t.infer(tp)
            val f = this.f.infer(tp)
            if (cnd!=null && t!=null && f!=null) {
                this.xtp = t.sup_vs(f)
            }
            this.xtp?.infer(null)
        }
        is Expr.MatchT -> {
            val tst = this.tst.infer(null)
            val cases = this.cases.map { (dat,e) ->
                e.infer(tp)
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
                e2.infer(tp)
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
        (tp == null) -> null
        !xtp.is_num() -> null
        !tp.is_num() -> null
        else -> tp
    }
    return xtp
}

fun Type.infer (tp: Type?): Type {
    when (this) {
        is Type.Any, is Type.Tpl, is Type.Nat,
        is Type.Unit, is Type.Prim -> {}
        is Type.Pointer -> {
            if (tp is Type.Pointer) {
                this.ptr.infer(tp.ptr)
            }
        }
        is Type.Tuple -> {
            if (tp is Type.Tuple) {
                this.ts.zip(tp.ts).forEach { (a,b) -> a.second.infer(b.second) }
            }
        }
        is Type.Vector -> {
            if (tp is Type.Vector) {
                this.tp.infer(tp.tp)
            }
        }
        is Type.Union -> {
            if (tp is Type.Union) {
                this.ts.zip(tp.ts).forEach { (a,b) -> a.second.infer(b.second) }
            }
        }
        is Type.Proto.Func -> {
            if (tp is Type.Proto.Func) {
                this.inps.zip(tp.inps).forEach { (a,b) -> a.infer(b) }
                this.out.infer(tp.out)
            }
        }
        is Type.Proto.Coro -> {
            if (tp is Type.Proto.Coro) {
                this.inps.zip(tp.inps).forEach { (a,b) -> a.infer(b) }
                this.res.infer(tp.out)
                this.yld.infer(tp.out)
                this.out.infer(tp.out)
            }
        }
        is Type.Exec -> {
            if (tp is Type.Exec) {
                this.inps.zip(tp.inps).forEach { (a,b) -> a.infer(b) }
                this.res.infer(tp.out)
                this.yld.infer(tp.out)
                this.out.infer(tp.out)
            }
        }
        is Type.Data -> {
            val (s,_,_) = this.walk(false)!!
            when {
                (this.xtpls != null) -> {}
                s.tpls.isEmpty() -> this.xtpls = emptyList()
                (tp !is Type.Data) -> {}
                (tp.xtpls != null) -> {
                    this.xtpls = tp.xtpls
                    assert(this.is_same_of(tp), {"TODO: unmatching infer"})
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
        //println(listOf("set",xdst?.to_str(),xsrc?.to_str()))
        val xxdst = (xdst==null || (xdst is Type.Data && xdst.xtpls==null))
        if (xxdst && xsrc!=null) {
            if (dst is Expr.Acc) {
                val dcl = dst.to_xdcl()!!.first
                if (dcl is Stmt.Dcl) {
                    //assert(dcl.xtp == null)
                    dcl.xtp = xsrc
                    //println(listOf("xxx",dcl.xtp?.to_str()))
                    dcl.xtp!!.infer(null)
                }
           } else {
                dst.infer(null)
           }
        }
    }

    G.outer!!.dn_visit_pos({ me ->
       when (me) {
           is Stmt.Data, -> {}
           is Stmt.Proto.Func -> me.tp_.infer(null)
           is Stmt.Proto.Coro -> me.tp_.infer(null)

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

           is Stmt.Print -> me.e.infer(null)
           is Stmt.Pass -> me.e.infer(Type.Unit(me.tk))
       }
   }, {}, { me ->
        when (me) {
            is Type.Data -> {
                val (s,_,_) = me.walk(false)!!
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
                is Expr.Tuple  -> it.xtp
                is Expr.Vector -> it.xtp
                is Expr.Union  -> it.xtp
                is Expr.Throw  -> it.xtp
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
    }, {}, {})
}
