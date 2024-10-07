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
    G.outer!!.dn_visit(::fs, ::fe, {})
}

fun check_types () {
    fun fs (me: Stmt) {
        when (me) {
            is Stmt.Proto -> {
                if (!me.tp.is_same_of(me.tk_.type(me)!!)) {
                    err(me.tk, "invalid declaration : types mismatch")
                }
            }
            is Stmt.Set -> {
                if (!me.dst.type().is_sup_of(me.src.type())) {
                    err(me.tk, "invalid set : types mismatch")
                }
            }
            is Stmt.Return -> {
                val func = me.fupx().up_first { it is Stmt.Proto } as Stmt.Proto
                if (!func.tp.out.is_sup_of(me.e.type())) {
                    err(me.tk, "invalid return : types mismatch")
                }
            }
            else -> {}
        }
    }
    fun fe (me: Expr) {
        when (me) {
            is Expr.Bin -> if (!me.args(me.e1.type(), me.e2.type())) {
                err(me.tk, "invalid operation : types mismatch")
            }
            is Expr.Call -> {
                val tp = me.f.type()
                val ok = when {
                    (tp is Type.Any) -> true
                    (tp !is Type.Proto.Func) -> false
                    (tp.inps.size != me.args.size) -> false
                    else -> tp.inps.zip(me.args).all { (par, arg) ->
                        par.is_sup_of(arg.type())
                    }
                }
                if (!ok) {
                    err(me.tk, "invalid call : types mismatch")
                }
            }
            else -> {}
        }
    }
    G.outer!!.dn_visit(::fs, ::fe, {})
}
