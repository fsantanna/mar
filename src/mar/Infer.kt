package mar

fun infer_types () {
    fun fs (me: Stmt) {
        when (me) {
            is Stmt.Dcl -> {
                if (me.xtp == null) {
                    val set = me.fupx().dn_first_pre({
                        it is Stmt.Set && it.dst is Expr.Acc && it.dst.tk_.str==me.id.str
                     }, null, null) as Stmt.Set
                    me.xtp = set.src.type()
                }
            }
            else -> {}
        }
    }
    fun fe (me: Expr) {
        when (me) {
            is Expr.Tuple -> {
                if (me.xtp == null) {

                }
            }
            is Expr.Union -> {
                if (me.xtp == null) {

                }
            }
            else -> {}
        }
    }
    G.outer!!.dn_visit_pos(::fs, ::fe, null)
}