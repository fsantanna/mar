package mar

fun Stmt.Block.to_dcls (): List<Pair<Node,Var_Type>> {
    return this.fup().let {
        when {
            (it is Stmt.Proto.Func) -> it.tp_.inps__.map { Pair(this.n, it) }
            (it is Stmt.Proto.Coro) -> it.tp_.inps__.map { Pair(this.n, it) }
            else -> emptyList()
        }
    } + this.dn_filter(
        {
            when (it) {
                is Stmt.Dcl -> true
                is Stmt.Proto -> true
                is Stmt.Block -> if (it === this) false else null
                else -> false
            }
        },
        {null},
        {null}
    )
        .let { it as List<Stmt> }
        .map {
            val vt = when (it) {
                is Stmt.Dcl -> it.var_type
                is Stmt.Proto -> Pair(it.id, it.tp)
                else -> error("impossible case")
            }
            Pair(it.n, vt)
        }
}

fun check_vars () {
    fun fs (me: Stmt) {
        when (me) {
            is Stmt.Block -> {
                val ids2 = me.to_dcls()
                me.ups().filter { it is Stmt.Block }.forEach {
                    it as Stmt.Block
                    val ids1 = it.to_dcls()
                    val err = ids2.find { (n2,id2) ->
                        ids1.find { (n1,id1) ->
                            (n1 != n2) && (id1.first.str == id2.first.str)
                        } != null
                    }
                    if (err != null) {
                        val (_,vt) = err
                        val (id,_) = vt
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
                val ok = me.up_any {
                    it is Stmt.Block && it.to_dcls().any { (_,vt) -> vt.first.str == me.tk.str }
                }
                if (!ok) {
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
                    err(me.tk, "create error : expected coroutine prototype")
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
