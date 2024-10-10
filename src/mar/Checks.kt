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
    G.outer!!.dn_visit(::fs, ::fe, {null})
}

fun check_types () {
    fun fs (me: Stmt) {
        when (me) {
            is Stmt.Proto -> {
                if (!me.tp.is_same_of(me.id.type(me)!!)) {
                    err(me.tk, "declaration error : types mismatch")
                }
            }
            is Stmt.Return -> {
                val func = me.fupx().up_first { it is Stmt.Proto } as Stmt.Proto
                if (!func.tp.out.is_sup_of(me.e.type())) {
                    err(me.tk, "return error : types mismatch")
                }
            }
            is Stmt.Set -> {
                if (!me.dst.type().is_sup_of(me.src.type())) {
                    err(me.tk, "set error : types mismatch")
                }
            }
            is Stmt.If -> {
                if (!me.cnd.type().is_sup_of(Type.Basic(Tk.Type("Bool",me.tk.pos.copy())))) {
                    err(me.tk, "if error : expected boolean condition")
                }
            }
            is Stmt.Create -> {
                val co = me.co.type()
                if (co !is Type.Proto.Coro) {
                    err(me.tk, "spawn error : expected coroutine prototype")
                }
                val tp = me.dst.type()
                val xtp = Type.XCoro(co.tk_, co.out, co.inps)
                if (!xtp.is_sup_of(tp)) {
                    err(me.tk, "create error : types mismatch")
                }
            }
            is Stmt.Resume -> {
                val xco = me.xco.type()
                if (xco !is Type.XCoro) {
                    err(me.tk, "resume error : expected active coroutine")
                }

                val ok1 = (me.dst == null) || me.dst.type().is_sup_of(xco.out)
                val ok2 = me.arg.type().let {
                    when {
                        (xco.inps.size == 0) -> it is Type.Unit
                        else -> xco.inps.first().is_sup_of(it)
                    }
                }
                if (!ok1 || !ok2) {
                    err(me.tk, "resume error : types mismatch")
                }
            }
            is Stmt.Yield -> {
                val up = me.up_first { it is Stmt.Proto }
                if (up !is Stmt.Proto.Coro) {
                    err(me.tk, "yield error : expected enclosing coro")
                }
                val xco = up.tp_
                val ok1 = when {
                    (me.dst == null) -> true
                    (xco.inps.size == 0) -> me.dst.type() is Type.Unit
                    else -> me.dst.type().is_sup_of(xco.inps.first())
                }
                val ok2 = xco.out.is_sup_of(me.arg.type())
                if (!ok1 || !ok2) {
                    err(me.tk, "yield error : types mismatch")
                }
            }
            else -> {}
        }
    }
    fun fe (me: Expr) {
        when (me) {
            is Expr.Bin -> if (!me.args(me.e1.type(), me.e2.type())) {
                err(me.tk, "operation error : types mismatch")
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
                    err(me.tk, "call error : types mismatch")
                }
            }
            else -> {}
        }
    }
    G.outer!!.dn_visit(::fs, ::fe, {null})
}
