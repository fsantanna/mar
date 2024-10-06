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

fun Expr.type (): String {
    return when (this) {
        is Expr.Acc -> {
            this.up_first {
                if (it !is Stmt.Block) false else {
                    it.vs.find { it.first.str == this.tk.str }?.second?.str
                }
            } as String
        }
        is Expr.Bin -> TODO()
        is Expr.Bool -> "Bool"
        is Expr.Call -> TODO()
        is Expr.Char -> "Char"
        is Expr.Nat -> "?"
        is Expr.Null -> TODO()
        is Expr.Num -> "Int"
    }
}

fun check_types () {
    fun fs (me: Stmt) {
        when (me) {
            is Stmt.Set -> {
                if (me.dst.type() != me.src.type()) {
                    err(me.tk, "invalid set : types mismatch")
                }
            }
            else -> {}
        }
    }
    fun fe (me: Expr) {
        when (me) {
            is Expr.Call -> {

            }
            else -> {}
        }
    }
    G.outer!!.dn_visit(::fs, ::fe)
}
