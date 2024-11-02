package mar

fun Node.fnex (): Any {
    return G.ns[this]!!
}

fun Stmt.fup (): Stmt? {
    return G.ups[this.n]?.fnex() as Stmt?
}
fun Expr.fup (): Any? {
    return G.ups[this.n]?.fnex()
}
fun Type.fup (): Any? {
    return G.ups[this.n]?.fnex()
}

fun Stmt.fupx (): Stmt {
    return this.fup()!!
}
fun Expr.fupx (): Any {
    return this.fup()!!
}
fun Type.fupx (): Any {
    return this.fup()!!
}

fun cache_ns () {
    G.outer!!.dn_visit_pre (
        {G.ns[it.n] = it ; Unit},
        {G.ns[it.n] = it ; Unit},
        {null}
    )
}

fun cache_ups () {
    fun fs (me: Stmt) {
        when (me) {
            is Stmt.Data   -> {
                G.ups[me.id.n] = me.n
                G.ups[me.tp.n] = me.n
            }
            is Stmt.Proto  -> {
                G.ups[me.tp.n] = me.n
                G.ups[me.blk.n] = me.n
                when (me) {
                    is Stmt.Proto.Func -> G.ups[me.tp_.n] = me.n
                    is Stmt.Proto.Coro -> G.ups[me.tp_.n] = me.n
                }
            }
            is Stmt.Return -> G.ups[me.e.n] = me.n

            is Stmt.Block -> me.ss.forEach { G.ups[it.n] = me.n }
            is Stmt.Dcl -> {
                if (me.xtp != null) {
                    G.ups[me.xtp!!.n] = me.n
                }
            }
            is Stmt.Set -> {
                G.ups[me.dst.n] = me.n
                G.ups[me.src.n] = me.n
            }

            is Stmt.If -> {
                G.ups[me.cnd.n] = me.n
                G.ups[me.t.n] = me.n
                G.ups[me.f.n] = me.n
            }
            is Stmt.Loop -> {
                G.ups[me.blk.n] = me.n
            }
            is Stmt.Break -> {}

            is Stmt.Print -> G.ups[me.e.n] = me.n
            is Stmt.XExpr -> G.ups[me.e.n] = me.n
            is Stmt.Nat -> {}
        }
    }
    fun fe (me: Expr) {
        when (me) {
            is Expr.Uno -> G.ups[me.e.n] = me.n
            is Expr.Bin -> {
                G.ups[me.e1.n] = me.n
                G.ups[me.e2.n] = me.n
            }
            is Expr.Call -> {
                G.ups[me.f.n] = me.n
                me.args.forEach { G.ups[it.n] = me.n }
            }

            is Expr.Tuple -> {
                if (me.xtp != null) {
                    G.ups[me.xtp!!.n] = me.n
                }
                me.vs.forEach { G.ups[it.n] = me.n }
            }
            is Expr.Union -> {
                if (me.xtp != null) {
                    G.ups[me.xtp!!.n] = me.n
                }
                G.ups[me.v.n] = me.n
            }
            is Expr.Field -> G.ups[me.col.n] = me.n
            is Expr.Disc  -> G.ups[me.col.n] = me.n
            is Expr.Pred  -> G.ups[me.col.n] = me.n
            is Expr.Cons  -> {
                G.ups[me.dat.n] = me.n
                me.es.forEach {
                    G.ups[it.n] = me.n
                }
            }

            is Expr.Acc, is Expr.Bool, is Expr.Char, is Expr.Nat,
            is Expr.Null, is Expr.Num, is Expr.Unit -> {}

            is Expr.Create -> G.ups[me.co.n] = me.n
            is Expr.Start -> {
                G.ups[me.exe.n] = me.n
                me.args.forEach {
                    G.ups[it.n] = me.n
                }
            }
            is Expr.Resume -> {
                G.ups[me.exe.n] = me.n
                G.ups[me.arg.n] = me.n
            }
            is Expr.Yield -> {
                G.ups[me.arg.n] = me.n
            }
        }
    }
    fun ft (me: Type) {
        when (me) {
            is Type.Pointer -> G.ups[me.ptr.n] = me.n
            is Type.Tuple -> me.ts.forEach {
                G.ups[it.n] = me.n
            }
            is Type.Union -> {
                if (me.o != null) {
                    G.ups[me.o.n] = me.n
                }
                me.ts.forEach {
                    G.ups[it.n] = me.n
                }
            }
            is Type.Exec -> {
                me.inps.forEach {
                    G.ups[it.n] = me.n
                }
                G.ups[me.res.n] = me.n
                G.ups[me.yld.n] = me.n
                G.ups[me.out.n] = me.n
            }
            is Type.Proto -> {
                me.inps.forEach {
                    G.ups[it.n] = me.n
                }
                G.ups[me.out.n] = me.n
                when (me) {
                    is Type.Proto.Func -> {
                        if (me is Type.Proto.Func.Vars) {
                            me.inps_.forEach { (_,tp) ->
                                G.ups[tp.n] = me.n
                            }
                        }
                    }
                    is Type.Proto.Coro -> {
                        G.ups[me.res.n] = me.n
                        G.ups[me.yld.n] = me.n
                        if (me is Type.Proto.Coro.Vars) {
                            me.inps_.forEach { (_,tp) ->
                                G.ups[tp.n] = me.n
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
