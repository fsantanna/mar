package mar

fun Stmt.infer (tp: Type?): Type? {
    return when (this) {
        is Stmt.Create -> {
            val xtp = if (tp !is Type.Exec) null else {
                this.co.infer(Type.Proto.Coro(tp.tk, tp.inps, tp.res, tp.yld, tp.out))
            }
            this.co.infer(xtp).let {
                if (it !is Type.Proto.Coro) null else {
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
                exe.yld
            } else {
                this.args.map {
                    it.infer(null)
                }
                null
            }
        }
       else -> error("impossible case")
   }
}

fun Expr.infer (tp: Type?): Type? {
    when (this) {
        is Expr.Acc, is Expr.Bool, is Expr.Str, is Expr.Chr,
        is Expr.Null, is Expr.Unit, is Expr.Num -> {}

        is Expr.Nat -> {
            if (this.xtp == null) {
                this.xtp = tp ?: Type.Any(this.tk)
            }
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
                    Type.Vector(this.tk, vs.size, v)
                }
            }
            if (dn == null) null else {
                if (this.xtp == null) {
                    this.xtp = dn
                }
                this.xtp
            }
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
        }
        is Expr.Pred -> this.col.infer(null)
        is Expr.Disc -> this.col.infer(null)
        is Expr.Cons -> this.e.infer(this.walk(this.ts)!!.third)

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
        }

        is Expr.Resume -> {
            val exe = this.exe.infer(null)
            if (exe is Type.Exec) {
                this.arg.infer(exe.res)
            }
        }
        is Expr.Yield -> {
            val coro = (this.up_first { it is Stmt.Proto.Coro } as Stmt.Proto.Coro).tp_
            this.arg.infer(coro.yld)
        }

        is Expr.If -> {
            val cnd = this.cnd.infer(null)
            val t = this.t.infer(tp)
            val f = this.f.infer(tp)
            if (cnd!=null && t!=null && f!=null) {
                this.xtp = t.sup_vs(f)
            }
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

fun infer_apply () {
    G.outer!!.dn_visit_pos({ me ->
       when (me) {
           is Stmt.Data, -> {}
           is Stmt.Proto -> {}

           is Stmt.Block -> {}
           is Stmt.Dcl -> {}
           is Stmt.SetE -> {
               val tp1 = me.dst.infer(null)
               val tp2 = me.src.infer(tp1)
               if (tp2!=null && tp1==null) {
                   if (me.dst is Expr.Acc) {
                       val dcl = me.dst.to_xdcl()!!.first
                       if (dcl is Stmt.Dcl) {
                           assert(dcl.xtp == null)
                           dcl.xtp = tp2
                       }
                   } else {
                       me.dst.infer(null)
                   }
               }
           }
           is Stmt.SetS -> {
               val tp1 = me.dst.infer(null)
               val tp2 = me.src.infer(tp1)
               if (tp2!=null && tp1==null) {
                   if (me.dst is Expr.Acc) {
                       val dcl = me.dst.to_xdcl()!!.first
                       if (dcl is Stmt.Dcl) {
                           assert(dcl.xtp == null)
                           dcl.xtp = tp2
                       }
                   } else {
                       me.dst.infer(null)
                   }
               }
           }

           is Stmt.Escape -> me.e.infer(null)
           is Stmt.Defer -> {}
           is Stmt.Catch -> {}

           is Stmt.If -> me.cnd.infer(Type.Prim(Tk.Type("Bool",me.tk.pos)))
           is Stmt.Loop -> {}
           is Stmt.MatchT -> me.tst.infer(null)
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

           is Stmt.Print -> me.e.infer(null)
           is Stmt.Pass -> me.e.infer(Type.Unit(me.tk))
       }
   }, {}, {})
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