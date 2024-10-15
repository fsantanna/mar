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

fun Stmt.fupx (): Stmt {
    return this.fup()!!
}
fun Expr.fupx (): Any {
    return this.fup()!!
}

fun cache_ns () {
    G.outer!!.dn_visit_pre (
        {G.ns[it.n] = it ; Unit},
        {G.ns[it.n] = it ; Unit},
        null
    )
}

fun cache_ups () {
    fun fs (me: Stmt) {
        when (me) {
            is Stmt.Proto -> G.ups[me.blk.n] = me.n
            is Stmt.Return -> G.ups[me.e.n] = me.n

            is Stmt.Block -> me.ss.forEach { G.ups[it.n] = me.n }
            is Stmt.Dcl -> {}
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

            is Stmt.Create -> {
                G.ups[me.dst.n] = me.n
                G.ups[me.co.n] = me.n
            }
            is Stmt.Start -> {
                if (me.dst != null) {
                    G.ups[me.dst.n] = me.n
                }
                G.ups[me.exe.n] = me.n
                me.args.forEach {
                    G.ups[it.n] = me.n
                }
            }
            is Stmt.Resume -> {
                if (me.dst != null) {
                    G.ups[me.dst.n] = me.n
                }
                G.ups[me.exe.n] = me.n
                G.ups[me.arg.n] = me.n
            }
            is Stmt.Yield -> {
                if (me.dst != null) {
                    G.ups[me.dst.n] = me.n
                }
                G.ups[me.arg.n] = me.n
            }

            is Stmt.Call -> G.ups[me.call.n] = me.n
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

            is Expr.Tuple -> me.vs.forEach { G.ups[it.n] = me.n }
            is Expr.Union -> G.ups[me.v.n] = me.n
            is Expr.Index -> G.ups[me.col.n] = me.n
            is Expr.Disc  -> G.ups[me.col.n] = me.n
            is Expr.Pred  -> G.ups[me.col.n] = me.n

            is Expr.Acc, is Expr.Bool, is Expr.Char, is Expr.Nat,
            is Expr.Null, is Expr.Num, is Expr.Unit -> {}
        }
    }
    G.outer!!.dn_visit_pre(::fs, ::fe, null)
}
