package mar

fun check_vars () {
    fun fs (me: Stmt) {
        when (me) {
            is Stmt.Block -> {
                val vs1 = me.vs.map { it.first.str }.toSet()
                me.fup()?.ups()?.filter { it is Stmt.Block }?.forEach {
                    it as Stmt.Block
                    val vs2 = it.vs.map { it.first.str }.toSet()
                    val vs = vs1.intersect(vs2)
                    if (vs.size > 0) {
                        val (id,_) = me.vs.find { it.first.str == vs.first() }!!
                        err(id, "declaration error : variable \"${id.str}\" is already declared")
                    }
                }
            }
            else -> {}
        }
    }
    fun fe (me: Expr) {
        when (me) {
            is Expr.Acc -> {
                if (me.up_none { it is Stmt.Block && it.vs.any { it.first.str==me.tk.str } }) {
                    err(me.tk, "access error : variable \"${me.tk.str}\" is not declared")
                }
            }
            else -> {}
        }
    }
    G.outer!!.dn_visit(::fs, ::fe)
}
