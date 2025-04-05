package mar

fun cache_ups () {
    fun fs (me: Stmt) {
        when (me) {
            is Stmt.Data   -> {
                me.tp.xup = me
                me.subs?.forEach {
                    it.xup = me
                }
            }
            is Stmt.Proto  -> {
                me.tp.xup = me
                me.blk.xup = me
                when (me) {
                    is Stmt.Proto.Func -> me.tp_.xup = me
                    is Stmt.Proto.Coro -> me.tp_.xup = me
                    is Stmt.Proto.Task -> me.tp_.xup = me
                }
            }

            is Stmt.Block -> {
                me.esc?.xup = me
                me.ss.forEach { it.xup = me }
            }
            is Stmt.Dcl -> me.xtp?.xup = me
            is Stmt.SetE -> {
                me.dst.xup = me
                me.src.xup = me
            }
            is Stmt.SetS -> {
                me.dst.xup = me
                me.src.xup = me
            }

            is Stmt.Escape -> {
                me.e.xup = me
            }
            is Stmt.Defer -> {
                me.blk.xup = me
            }
            is Stmt.Catch -> {
                me.tp?.xup = me
                me.blk.xup = me
            }
            is Stmt.Throw -> me.e.xup = me

            is Stmt.If -> {
                me.cnd.xup = me
                me.t.xup = me
                me.f.xup = me
            }
            is Stmt.Loop -> {
                me.blk.xup = me
            }
            is Stmt.MatchT -> {
                me.tst.xup = me
                me.cases.forEach {
                    it.first?.xup = me
                    it.second.xup = me
                }
            }
            is Stmt.MatchE -> {
                me.tst.xup = me
                me.cases.forEach {
                    it.first?.xup = me
                    it.second.xup = me
                }
            }

            is Stmt.Create -> me.pro.xup = me
            is Stmt.Start -> {
                me.exe.xup = me
                me.args.forEach {
                    it.xup = me
                }
            }
            is Stmt.Resume -> {
                me.exe.xup = me
                me.arg.xup = me
            }
            is Stmt.Yield -> {
                me.arg.xup = me
            }
            is Stmt.Await -> {
                me.tp?.xup = me
                me.e?.xup = me
                me.es?.forEach {
                    it.xup = me
                }
            }
            is Stmt.Emit -> me.e.xup = me

            is Stmt.Print -> me.e.xup = me
            is Stmt.Pass -> me.e.xup = me
        }
    }
    fun fe (me: Expr) {
        when (me) {
            is Expr.Uno -> me.e.xup = me
            is Expr.Bin -> {
                me.e1.xup = me
                me.e2.xup = me
            }
            is Expr.Call -> {
                me.f.xup = me
                me.args.forEach { it.xup = me }
            }

            is Expr.Tuple -> {
                me.xtp?.xup = me
                me.vs.forEach { (_,e) -> e.xup = me }
            }
            is Expr.Vector -> {
                me.xtp?.xup = me
                me.vs.forEach { it.xup = me }
            }
            is Expr.Union -> {
                me.xtp?.xup = me
                me.v.xup = me
            }
            is Expr.Field -> me.col.xup = me
            is Expr.Index -> {
                me.col.xup = me
                me.idx.xup = me
            }
            is Expr.Disc  -> me.col.xup = me
            is Expr.Pred  -> me.col.xup = me
            is Expr.Cons  -> {
                me.tp.xup = me
                me.e.xup = me
            }
            is Expr.Nat -> {
                me.xtp?.xup = me
            }

            is Expr.Acc, is Expr.Bool, is Expr.Chr, is Expr.Str,
            is Expr.Null, is Expr.Num, is Expr.Unit, is Expr.Tpl -> {}

            is Expr.If -> {
                me.cnd.xup = me
                me.t.xup = me
                me.f.xup = me
            }
            is Expr.MatchT -> {
                me.tst.xup = me
                me.cases.forEach {
                    it.first?.xup = me
                    it.second.xup = me
                }
            }
            is Expr.MatchE -> {
                me.tst.xup = me
                me.cases.forEach {
                    it.first?.xup = me
                    it.second.xup = me
                }
            }
        }
    }
    fun ft (me: Type) {
        when (me) {
            is Type.Data -> {
                me.xtpls?.forEach { (tp,e) ->
                    tp?.xup = me
                    e?.xup = me
                }
            }
            is Type.Pointer -> me.ptr.xup = me
            is Type.Tuple -> me.ts.forEach { (_,tp) ->
                tp.xup = me
            }
            is Type.Vector -> {
                me.max?.xup = me
                me.tp.xup = me
            }
            is Type.Union -> {
                me.ts.forEach { (_,tp) ->
                    tp.xup = me
                }
            }
            is Type.Exec -> {
                me.inps.forEach {
                    it.xup = me
                }
                me.out.xup = me
                when (me) {
                    is Type.Exec.Coro -> {
                        me.res.xup = me
                        me.yld.xup = me
                    }
                    is Type.Exec.Task -> {}
                }
            }
            is Type.Proto -> {
                me.inps.forEach {
                    it.xup = me
                }
                me.out.xup = me
                when (me) {
                    is Type.Proto.Func.Vars -> {
                        me.inps_.forEach { (_,tp) ->
                            tp.xup = me
                        }
                    }
                    is Type.Proto.Coro -> {
                        me.res.xup = me
                        me.yld.xup = me
                        if (me is Type.Proto.Coro.Vars) {
                            me.inps_.forEach { (_, tp) ->
                                tp.xup = me
                            }
                        }
                    }
                    is Type.Proto.Task.Vars -> {
                        me.inps_.forEach { (_,tp) ->
                            tp.xup = me
                        }
                    }
                    else -> {}
                }
            }
            else -> {}
        }
    }
    G.outer!!.dn_visit_pre(::fs, ::fe, ::ft)
}

fun cache_tpls () {
    fun fe (me: Expr) {
        when (me) {
            is Expr.Call -> {
                if (!me.xtpls!!.isEmpty()) {
                    val f = me.f as Expr.Acc
                    val dcl = f.to_xdcl()!!.first as Stmt.Proto
                    if (G.tpls[dcl] == null) {
                        G.tpls[dcl] = mutableMapOf()
                    }
                    val id = me.coder(null,false)
                    G.tpls[dcl]!![id] = me.xtpls!!
                }
            }
            else -> {}
        }
    }
    G.outer!!.dn_visit_pre({}, ::fe, {})
}

fun cache_blks () {
    fun Stmt.f (sd: Int): Int {
        return when (this) {
            is Stmt.Block -> {
                G.tsks_ids[this] = sd
                this.ss.fold(sd) { i,s ->
                    s.f(i)
                }
                1
            }
            //is Stmt.Proto -> this.blk.f(0, 0)
            is Stmt.SetS -> this.src.f(sd)
            is Stmt.Catch -> this.blk.f(sd)
            //is Stmt.Defer -> TODO()
            is Stmt.Loop -> this.blk.f(sd)
            is Stmt.If -> this.t.f(sd) + this.f.f(sd+1)
            is Stmt.MatchE -> this.cases.map { it.second }.fold(sd) { i,s ->
                s.f(i)
            }
            is Stmt.MatchT -> this.cases.map { it.second }.fold(sd) { i,s ->
                s.f(i)
            }
            is Stmt.Await -> {
                G.tsks_ids[this] = sd
                sd+1
            }
            else -> sd
        }
    }
    G.outer!!.dn_visit_pre({
        if (it is Stmt.Proto.Task) {
            it.blk.f( 0)
            Unit
        }
    }, {}, {})
}