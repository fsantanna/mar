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
                val dcls  = me.vs.filter { it.second is Type.Proto }
                val impls = me.dn_filter (
                    {
                        when (it) {
                            is Stmt.Block -> if (it == me) false else null
                            is Stmt.Proto -> true
                            else -> false
                        }
                    },
                    {false},
                    {false}
                ) as List<Stmt.Proto>
                val dcl_wo_impl = dcls.find { (id,_) -> impls.none { impl -> impl.id.str == id.str } }
                if (dcl_wo_impl != null) {
                    err(dcl_wo_impl.first, "declaration error : missing implementation")
                }
                val impl_wo_dcl = impls.find { impl -> dcls.none { (id,_) -> impl.id.str == id.str } }
                if (impl_wo_dcl != null) {
                    err(impl_wo_dcl.tk, "implementation error : variable \"${impl_wo_dcl.id.str}\" is not declared")
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
                if (!me.tp.is_same_of(me.id.type(me)!!)) {
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
            is Stmt.Spawn -> {
                val co = me.co.type()
                if (co !is Type.Proto.Coro) {
                    err(me.tk, "invalid spawn : expected coroutine prototype")
                }

                val tp = me.dst.type()
                val xtp = Type.XCoro(co.tk_, co.res, co.out)
                val ok1 = xtp.is_sup_of(tp)

                val ok2 = when {
                    (co.inps.size != me.args.size) -> false
                    else -> co.inps.zip(me.args).all { (par, arg) ->
                        par.is_sup_of(arg.type())
                    }
                }
                if (!ok1 || !ok2) {
                    err(me.tk, "invalid spawn : types mismatch")
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
