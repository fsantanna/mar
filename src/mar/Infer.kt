package mar

fun Expr.infer (tp: Type?): Type? {
    return when (this) {
        is Expr.Nat -> this.xtp ?: tp
        is Expr.Acc -> this.tk_.type(this)

        is Expr.Bool, is Expr.Str, is Expr.Chr,
        is Expr.Num, is Expr.Null, is Expr.Unit -> this.type()

        is Expr.Tuple -> {
            val up = this.xtp ?: tp
            val dn = this.vs.mapIndexed { i,v ->
                val xi = if (up !is Type.Tuple) null else {
                    up.ts[i].second
                }
                Pair(v.first, v.second.infer(xi))
            }.let {
                if (it.any { it.second == null }) null else {
                    it as List<Pair<Tk.Var?, Type>>
                    val tup = Type.Tuple(this.tk, it)
                    if (this.xtp == null) {
                        this.xtp = tup
                    }
                    tup
                }
            }
            dn ?: up
        }
        is Expr.Field -> TODO()
        is Expr.Union -> TODO()
        is Expr.Pred -> TODO()
        is Expr.Disc -> TODO()
        is Expr.Cons -> TODO()

        is Expr.Uno -> TODO()
        is Expr.Bin -> TODO()
        is Expr.Call -> TODO()

        is Expr.Create -> TODO()
        is Expr.Start -> TODO()
        is Expr.Resume -> TODO()
        is Expr.Yield -> TODO()

        is Expr.If -> TODO()
        is Expr.Match -> TODO()
    }
}

fun infer_types () {
    fun fs (me: Stmt) {
        val ok: Boolean = when (me) {
            is Stmt.Data -> true
            is Stmt.Proto -> true

            is Stmt.Block -> true
            is Stmt.Dcl -> true
            is Stmt.Set -> {
                var tp1 = me.dst.infer(null)
                val tp2 = me.src.infer(tp1)
                if (tp2!=null && tp1==null) {
                    if (me.dst is Expr.Acc) {
                        val dcl = me.dst.to_xdcl()!!.first
                        if (dcl is Stmt.Dcl) {
                            assert(dcl.xtp == null)
                            dcl.xtp = tp2
                        }
                        tp1 = tp2
                    } else {
                        tp1 = me.dst.infer(null)
                    }
                }
                (tp1!=null && tp2!=null)
            }

            is Stmt.Escape -> (me.e.infer(null) != null)
            is Stmt.Defer -> true
            is Stmt.Catch -> true
            is Stmt.Throw -> (me.e.infer(null) != null)

            is Stmt.If -> (me.cnd.infer(Type.Prim(Tk.Type("Bool",me.tk.pos.copy()))) != null)
            is Stmt.Loop -> true

            is Stmt.Print -> (me.e.infer(null) != null)
            is Stmt.Pass -> (me.e.infer(null) != null)
        }
        if (!ok) {
            err(me.tk, "inference error : unknown types")
        }
    }
    G.outer!!.dn_visit_pos(::fs, {}, {})

    G.outer!!.dn_visit_pre({
        if (it is Stmt.Dcl) {
            if (it.xtp == null) {
                err(it.tk, "inference error : unknown type")
            }
        }
    }, {null}, {null})
}