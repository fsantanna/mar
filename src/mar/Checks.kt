package mar

fun Stmt.Block.to_dcls (): List<Pair<Node,Var_Type>> {
    return this.fup().let {
        when {
            (it is Stmt.Proto.Func) -> it.tp_.inp__.map { Pair(this.n, it) }
            (it is Stmt.Proto.Coro) -> listOf(Pair(this.n, it.tp_.inp__))
            else -> emptyList()
        }
    } + this.dn_filter_pre(
        {
            when (it) {
                is Stmt.Dcl -> true
                is Stmt.Proto -> true
                is Stmt.Block -> if (it === this) false else null
                else -> false
            }
        },
        null,
        null
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
    G.outer!!.dn_visit_pre(::fs, ::fe, null)
}

fun check_types () {
    fun fs (me: Stmt) {
        when (me) {
            is Stmt.Proto.Coro -> {
                //if (me.tp.in)) {
                    //err(me.tk, "declaration error : types mismatch")
                //}
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
                val xtp = Type.Exec(co.tk_, co.inp_, co.out_)
                if (!xtp.is_sup_of(tp)) {
                    err(me.tk, "create error : types mismatch")
                }
            }
            is Stmt.Resume -> {
                val exe = me.exe.type()
                if (exe !is Type.Exec) {
                    err(me.tk, "resume error : expected active coroutine")
                }

                val ok1 = (me.dst == null) || me.dst.type().is_sup_of(exe.out)
                val ok2 = exe.inp.is_sup_of(me.arg.type())
                if (!ok1 || !ok2) {
                    err(me.tk, "resume error : types mismatch")
                }
            }
            is Stmt.Yield -> {
                val up = me.up_first { it is Stmt.Proto }
                if (up !is Stmt.Proto.Coro) {
                    err(me.tk, "yield error : expected enclosing coro")
                }
                val exe = up.tp_
                val ok1 = (me.dst == null) || me.dst.type().is_sup_of(exe.inp_)
                val ok2 = exe.out.is_sup_of(me.arg.type())
                if (!ok1 || !ok2) {
                    err(me.tk, "yield error : types mismatch")
                }
            }
            else -> {}
        }
    }
    fun fe (me: Expr) {
        when (me) {
            is Expr.Index -> {
                val tp = me.col.type()
                val i = me.idx.toInt()
                val ok = when {
                    (tp is Type.Any) -> true
                    (tp !is Type.Tuple) -> false
                    (i<=0 || i>tp.ts.size) -> false
                    else -> true
                }
                if (!ok) {
                    err(me.tk, "index error : types mismatch")
                }
            }
            is Expr.Union -> {
                val i = me.idx.toInt()
                val ok = when {
                    (me.tp is Type.Any) -> true
                    (me.tp !is Type.Union) -> false
                    (i<=0 || i>me.tp.ts.size) -> false
                    else -> me.tp.ts[i-1].is_sup_of(me.v.type())
                }
                if (!ok) {
                    err(me.tk, "union error : types mismatch")
                }
            }
            is Expr.Disc -> {
                val tp = me.col.type()
                val i = me.idx.toInt()
                val ok = when {
                    (tp is Type.Any) -> true
                    (tp !is Type.Union) -> false
                    (i<=0 || i>tp.ts.size) -> false
                    else -> true
                }
                if (!ok) {
                    err(me.tk, "discriminator error : types mismatch")
                }
            }
            is Expr.Bin -> if (!me.args(me.e1.type(), me.e2.type())) {
                err(me.tk, "operation error : types mismatch")
            }
            is Expr.Call -> {
                val tp = me.f.type()
                val ok = when {
                    (tp is Type.Any) -> true
                    (tp !is Type.Proto.Func) -> false
                    (tp.inp_.size != me.args.size) -> false
                    else -> tp.inp_.zip(me.args).all { (par, arg) ->
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
    G.outer!!.dn_visit_pos(::fs, ::fe, null)
}
