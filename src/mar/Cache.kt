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
                }
            }

            is Stmt.Block -> {
                if (me.esc != null) {
                    me.esc.xup = me
                }
                me.ss.forEach { it.xup = me }
            }
            is Stmt.Dcl -> {
                if (me.xtp != null) {
                    me.xtp!!.xup = me
                }
            }
            is Stmt.Set -> {
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
                if (me.tp != null) {
                    me.tp.xup = me
                }
                me.blk.xup = me
            }

            is Stmt.If -> {
                me.cnd.xup = me
                me.t.xup = me
                me.f.xup = me
            }
            is Stmt.Loop -> {
                me.blk.xup = me
            }
            is Stmt.Match -> {
                me.tst.xup = me
                me.cases.forEach {
                    it.first?.xup = me
                    it.second.xup = me
                }
            }

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
                if (me.xtp != null) {
                    me.xtp!!.xup = me
                }
                me.vs.forEach { (_,e) -> e.xup = me }
            }
            is Expr.Union -> {
                if (me.xtp != null) {
                    me.xtp!!.xup = me
                }
                me.v.xup = me
            }
            is Expr.Field -> me.col.xup = me
            is Expr.Disc  -> me.col.xup = me
            is Expr.Pred  -> me.col.xup = me
            is Expr.Cons  -> me.e.xup = me
            is Expr.Nat -> {
                if (me.xtp != null) {
                    me.xtp!!.xup = me
                }
            }

            is Expr.Acc, is Expr.Bool, is Expr.Chr, is Expr.Str,
            is Expr.Null, is Expr.Num, is Expr.Unit -> {}

            is Expr.Throw -> {
                me.e.xup = me
            }
            is Expr.Create -> me.co.xup = me
            is Expr.Start -> {
                me.exe.xup = me
                me.args.forEach {
                    it.xup = me
                }
            }
            is Expr.Resume -> {
                me.exe.xup = me
                me.arg.xup = me
            }
            is Expr.Yield -> {
                me.arg.xup = me
            }

            is Expr.If -> {
                me.cnd.xup = me
                me.t.xup = me
                me.f.xup = me
            }
            is Expr.Match -> {
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
            is Type.Pointer -> me.ptr?.xup = me
            is Type.Tuple -> me.ts.forEach { (_,tp) ->
                tp.xup = me
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
                me.res.xup = me
                me.yld.xup = me
                me.out.xup = me
            }
            is Type.Proto -> {
                me.inps.forEach {
                    it.xup = me
                }
                me.out.xup = me
                when (me) {
                    is Type.Proto.Func -> {
                        if (me is Type.Proto.Func.Vars) {
                            me.inps_.forEach { (_,tp) ->
                                tp.xup = me
                            }
                        }
                    }
                    is Type.Proto.Coro -> {
                        me.res.xup = me
                        me.yld.xup = me
                        if (me is Type.Proto.Coro.Vars) {
                            me.inps_.forEach { (_,tp) ->
                                tp.xup = me
                            }
                        }
                    }
                }
            }
            else -> {}
        }
    }
    G.outer!!.dn_visit_pre(::fs, ::fe, ::ft)
}
