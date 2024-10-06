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
    G.outer!!.dn_visit (
        {G.ns[it.n] = it},
        {G.ns[it.n] = it}
    )
}

fun cache_ups () {
    fun fs (me: Stmt) {
        when (me) {
            is Stmt.Block -> me.ss.forEach { G.ups[it.n] = me.n }
            is Stmt.Set -> {
                G.ups[me.dst.n] = me.n
                G.ups[me.src.n] = me.n
            }
            is Stmt.Call -> G.ups[me.call.n] = me.n
            else -> {}
        }
    }
    fun fe (me: Expr) {
        when (me) {
            is Expr.Bin -> {
                G.ups[me.e1.n] = me.n
                G.ups[me.e2.n] = me.n
            }
            is Expr.Call -> {
                G.ups[me.f.n] = me.n
                me.args.forEach { G.ups[it.n] = me.n }
            }
            else -> {}
        }
    }
    G.outer!!.dn_visit(::fs, ::fe)
}