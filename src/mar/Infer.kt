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
        when (me) {
            is Expr.Tuple -> {
                if (me.xtp == null) {
                    val up = me.fupx()
                    if (up is Stmt.Set) {
                        assert(up.src.n == me.n)
                        me.xtp = up.dst.typex() as Type.Tuple?
                    }
                    if (me.xtp == null) {
                        val tps = me.vs.map { it.type() }
                        me.xtp = Type.Tuple(me.tk, tps, me.ids)
                    }
                }
            }
            is Expr.Union -> {
                if (me.xtp == null) {
                    val up = me.fupx()
                    if (up is Stmt.Set) {
                        assert(up.src.n == me.n)
                        me.xtp = up.dst.typex() as Type.Union?
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