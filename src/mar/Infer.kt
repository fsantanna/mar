package mar

fun Expr.typex (): Type? {
    return try {
        this.type()
    } catch (e: Exception ) {
        null
    }
}

fun infer_types () {
    fun fe (me: Expr) {
        fun infer (): Type? {
            val up = me.fupx()
            return when (up) {
                is Stmt.Set -> {
                    assert(up.src.n == me.n)
                    up.dst.typex()
                }
                is Expr.Cons -> up.tk_.to_data()!!.tp
                is Expr.Call -> {
                    val i = up.args.indexOfFirst { it.n == me.n }
                    (up.f.type() as Type.Proto.Func).inps[i]
                }
                is Stmt.Return -> {
                    assert(up.e.n == me.n)
                    (up.up_first { it is Stmt.Proto.Func } as Stmt.Proto.Func).tp.out
                }
                is Expr.Start -> {
                    val i = up.args.indexOfFirst { it.n == me.n }
                    (up.exe.type() as Type.Exec).inps[i]
                }
                is Expr.Resume -> {
                    assert(up.arg.n == me.n)
                    (up.exe.type() as Type.Exec).res
                }
                is Expr.Yield -> {
                    assert(up.arg.n == me.n)
                    (up.up_first { it is Stmt.Proto.Coro } as Stmt.Proto.Coro).tp_.yld
                }
                else -> null
            }
        }
        when (me) {
            is Expr.Tuple -> {
                if (me.xtp == null) {
                    me.xtp = infer().let {
                        when (it) {
                            null -> null
                            is Type.Tuple -> it
                            else -> err(me.tk, "inference error : incompatible types")
                        }
                    }
                    if (me.xtp == null) {
                        val tps = me.vs.map { it.type() }
                        me.xtp = Type.Tuple(me.tk, tps, me.ids)
                    }
                }
            }
            is Expr.Union -> {
                if (me.xtp == null) {
                    me.xtp = infer().let {
                        when (it) {
                            null -> err(me.tk, "inference error : unknown type")
                            !is Type.Union -> err(me.tk, "inference error : incompatible types")
                            else -> it
                        }
                    }
                }
            }
            else -> {}
        }
    }
    fun fs (me: Stmt) {
        when (me) {
            is Stmt.Set -> {
                if (me.dst is Expr.Acc) {
                    val dcl = me.dst.to_xdcl()!!.to_dcl()
                    if (dcl!=null && dcl.xtp==null) {
                        dcl.xtp = me.src.type()
                    }
                }
            }
            else -> {}
        }
    }
    G.outer!!.dn_visit_pos(::fs, ::fe, null)
    G.outer!!.dn_visit_pre({
        if (it is Stmt.Dcl) {
            if (it.xtp == null) {
                err(it.tk, "inference error : unknown type")
            }
        }
    }, null, null)
}