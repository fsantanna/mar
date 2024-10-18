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

                }
            }
            else -> {}
        }
    }
    fun fs (me: Stmt) {
        when (me) {
            is Stmt.Dcl -> {
                if (me.xtp == null) {
                    val set = me.fupx().dn_first_pre({
                        it is Stmt.Set && it.dst is Expr.Acc && it.dst.tk_.str==me.id.str
                    }, null, null) as Stmt.Set?
                    if (set != null) {
                        set.dn_visit_pos(::fs,::fe,null) // because set is collected after dcl
                        me.xtp = set.src.type()
                    } else {
                        err(me.tk, "inference error : unknown type")
                    }
                }
            }
            else -> {}
        }
    }
    G.outer!!.dn_visit_pos(::fs, ::fe, null)
}