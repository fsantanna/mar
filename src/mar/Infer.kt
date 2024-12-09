package mar

fun Expr.typex (): Type? {
    return try {
        this.type()
    } catch (e: Throwable) {
        try {
            this.infer()
        } catch (e: Throwable) {
            null
        }
    }
}

fun Expr.infer (): Type? {
    val up = this.xup!!
    return when (up) {
        is Stmt.Set -> {
            assert(up.src == this)
            up.dst.typex()
        }
        is Expr.Cons -> up.walk(up.ts)!!.third
        is Expr.Tuple -> up.typex().let {
            if (it == null) null else {
                it as Type.Tuple
                val i = up.vs.indexOfFirst { (_,e) -> e==this }
                it.ts[i].second
            }
        }
        is Expr.Union -> up.typex().let {
            if (it == null) null else {
                it as Type.Union
                val (_,tp) = it.disc(up.idx)!!
                tp
            }
        }
        is Expr.Call -> {
            val i = up.args.indexOfFirst { it == this }
            val tp = up.f.type()
            if (tp !is Type.Proto.Func) null else {
                tp.inps[i]
            }
        }
        is Expr.Start -> {
            val i = up.args.indexOfFirst { it == this }
            (up.exe.type() as Type.Exec).inps[i]
        }
        is Expr.Resume -> {
            assert(up.arg == this)
            (up.exe.type() as Type.Exec).res
        }
        is Expr.Yield -> {
            assert(up.arg == this)
            (up.up_first { it is Stmt.Proto.Coro } as Stmt.Proto.Coro).tp_.yld
        }
        is Expr.Bin -> {
            when (up.tk_.str) {
                in listOf(">", "<", ">=", "<=", "+", "-", "*", "/", "%") -> Type.Prim(Tk.Type("Int",up.tk.pos.copy()))
                in listOf("||", "&&") -> Type.Prim(Tk.Type("Bool",up.tk.pos.copy()))
                else -> null
            }
        }
        is Expr.Uno -> {
            when (up.tk_.str) {
                "-" -> Type.Prim(Tk.Type("Int",up.tk.pos.copy()))
                else -> null
            }
        }
        else -> null
    }
}

fun infer_types () {
    fun fe (me: Expr) {
        when (me) {
            is Expr.Tuple -> {
                if (me.xtp == null) {
                    me.xtp = me.infer().let {
                        when (it) {
                            null -> null
                            is Type.Tuple -> it
                            else -> err(me.tk, "inference error : incompatible types")
                        }
                    }
                    if (me.xtp == null) {
                        val tps = me.vs.map { (id,v) -> Pair(id,v.type()) }
                        me.xtp = Type.Tuple(me.tk, tps)
                        me.xtp!!.xup = me
                    }
                }
            }
            is Expr.Union -> {
                if (me.xtp == null) {
                    me.xtp = me.infer().let {
                        when (it) {
                            null -> err(me.tk, "inference error : unknown type")
                            !is Type.Union -> err(me.tk, "inference error : incompatible types")
                            else -> it
                        }
                    }
                }
            }
            is Expr.Nat -> {
                if (me.xtp == null) {
                    val up = me.xup!!
                    me.xtp = when {
                        (me.tk.str == "mar_ret") -> (me.up_first { it is Stmt.Proto } as Stmt.Proto).tp.out
                        (up is Stmt.XExpr) -> null
                        (up is Expr.Call && up.f==me)   -> null
                        (up is Stmt.Set  && up.dst==me) -> null
                        else -> me.infer().let {
                            when (it) {
                                null -> err(me.tk, "inference error : unknown type")
                                else -> it
                            }
                        }
                    }
                }
            }
            else -> {}
        }
    }
    fun fs (me: Stmt) {
        when (me) {
            is Stmt.Set -> {
                if (me.dst is Expr.Acc) {
                    val dcl = me.dst.to_xdcl()!!.first
                    if (dcl is Stmt.Dcl && dcl.xtp==null) {
                        dcl.xtp = me.src.typex()
                    }
                }
            }
            else -> {}
        }
    }
    G.outer!!.dn_visit_pos(::fs, ::fe, {})
    G.outer!!.dn_visit_pre({
        if (it is Stmt.Dcl) {
            if (it.xtp == null) {
                err(it.tk, "inference error : unknown type")
            }
        }
    }, {null}, {null})
}