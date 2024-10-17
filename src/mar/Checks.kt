package mar

fun Tk.Type.to_data (): Stmt.Data? {
    return G.outer!!.dn_filter_pre({ it is Stmt.Data }, null, null)
        .let { it as List<Stmt.Data> }
        .find { it.id.str == this.str }
}

fun Type.Data.to_data (): Stmt.Data? {
    return this.tk_.to_data()
}

fun Stmt.Block.to_dcls (): List<Pair<Node,Var_Type>> {
    return this.fup().let {
        when {
            (it is Stmt.Proto.Func) -> it.tp_.inps_.map { Pair(this.n, it) }
            (it is Stmt.Proto.Coro) -> if (it.tp_ !is Type.Proto.Coro.Vars) emptyList() else {
                it.tp_.inps_.map { Pair(this.n, it) }
            }
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
                val out = me.up_first { it is Stmt.Proto.Func || it is Stmt.Proto.Coro}.let {
                    when {
                        (it is Stmt.Proto) -> it.tp.out
                        else -> error("impossible case")
                    }
                }
                if (!out.is_sup_of(me.e.type())) {
                    err(me.tk, "return error : types mismatch")
                }
            }
            is Stmt.Dcl -> {
                val (_,tp) = me.var_type
                if (tp is Type.Data) {
                    if (tp.to_data() == null) {
                        err(me.tk, "declaration error : data \"${tp.tk_.str}\" is not declared")
                    }
                }
            }
            is Stmt.Set -> {
                if (!me.dst.type().is_sup_of(me.src.type())) {
                    err(me.tk, "set error : types mismatch")
                }
            }
            is Stmt.If -> {
                if (!me.cnd.type().is_sup_of(Type.Prim(Tk.Type("Bool",me.tk.pos.copy())))) {
                    err(me.tk, "if error : expected boolean condition")
                }
            }
            is Stmt.Create -> {
                val co = me.co.type()
                if (co !is Type.Proto.Coro) {
                    err(me.tk, "create error : expected coroutine prototype")
                }
                val tp = me.dst.type()
                val xtp = Type.Exec(co.tk, co.inps, co.res, co.yld, co.out)
                if (!xtp.is_sup_of(tp)) {
                    err(me.tk, "create error : types mismatch")
                }
            }
            is Stmt.Start -> {
                val exe = me.exe.type()
                if (exe !is Type.Exec) {
                    err(me.tk, "start error : expected active coroutine")
                }

                val ok1 = (me.dst == null) || me.dst.type().is_sup_of(Type.Union(exe.tk, false, listOf(exe.yld,exe.out), null))
                val ok2 = (exe.inps.size == me.args.size) && exe.inps.zip(me.args).all { (thi,oth) -> thi.is_sup_of(oth.type()) }
                if (!ok1 || !ok2) {
                    err(me.tk, "start error : types mismatch")
                }
            }
            is Stmt.Resume -> {
                val exe = me.exe.type()
                if (exe !is Type.Exec) {
                    err(me.tk, "resume error : expected active coroutine")
                }

                val ok1 = (me.dst == null) || me.dst.type().is_sup_of(Type.Union(exe.tk, false, listOf(exe.yld,exe.out), null))
                val ok2 = exe.res.is_sup_of(me.arg.type())
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
                val ok1 = (me.dst == null) || me.dst.type().is_sup_of(exe.yld)
                val ok2 = exe.yld.is_sup_of(me.arg.type())
                if (!ok1 || !ok2) {
                    err(me.tk, "yield error : types mismatch")
                }
            }
            else -> {}
        }
    }
    fun fe (me: Expr) {
        when (me) {
            is Expr.Tuple -> {
                val tp = Type.Tuple(me.tk, me.vs.map { it.type() }, me.ids)
                if (!me.tp.is_sup_of(tp)) {
                    err(me.tk, "tuple error : types mismatch")
                }
            }
            is Expr.Field -> {
                val tp = me.col.type().no_data()
                val i = me.idx.toIntOrNull()
                val ok = when {
                    (tp is Type.Any) -> true
                    (tp !is Type.Tuple) -> false
                    (i!=null && (i<=0 || i>tp.ts.size)) -> false
                    (i==null && (tp.ids==null || tp.ids.find { it.str==me.idx } == null)) -> false
                    else -> true
                }
                if (!ok) {
                    err(me.tk, "field error : types mismatch")
                }
            }
            is Expr.Union -> {
                val n = me.tp.disc_to_i(me.idx)
                if (n==null || !me.tp.ts[n-1].is_sup_of(me.v.type())) {
                    err(me.tk, "union error : types mismatch")
                }
            }
            is Expr.Disc -> {
                val n = me.col.type().no_data().let {
                    if (it !is Type.Union) null else it.disc_to_i(me.idx)
                }
                if (n == null) {
                    err(me.tk, "discriminator error : types mismatch")
                }
            }
            is Expr.Pred -> {
                val n = me.col.type().no_data().let {
                    if (it !is Type.Union) null else it.disc_to_i(me.idx)
                }
                if (n == null) {
                    err(me.tk, "predicate error : types mismatch")
                }
            }
            is Expr.Cons -> {
                val dat = me.tk_.to_data()
                if (dat == null) {
                    err(me.tk, "constructor error : data \"${me.tk_.str}\" is not declared")
                }
                if (!dat.tp.is_sup_of(me.e.type())) {
                    err(me.tk, "constructor error : types mismatch")
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
    G.outer!!.dn_visit_pos(::fs, ::fe, null)
}
