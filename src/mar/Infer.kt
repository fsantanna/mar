package mar

fun Expr.infer (tp: Type?): Type? {
    return when (this) {
        is Expr.Nat -> this.xtp ?: tp
        is Expr.Acc -> this.tk_.type(this)

        is Expr.Bool, is Expr.Str, is Expr.Chr,
        is Expr.Num, is Expr.Null, is Expr.Unit -> this.type()

        is Expr.Tuple -> {
            val up = this.xtp ?: tp
            val dn = if (up !is Type.Tuple) {
                val vs = this.vs.map { (tk,e) ->
                    Pair(tk, e.infer(null))
                }
                if (vs.any { it.second == null }) null else {
                    Type.Tuple(this.tk, vs as List<Pair<Tk.Var?, Type>>)
                }
            } else {
                val vs = this.vs.mapIndexed { i,(tk,e) ->
                    Pair(tk, e.infer(up.ts[i].second))
                }
                if (vs.any { it.second == null }) null else {
                    vs as List<Pair<Tk.Var?, Type>>
                    Type.Tuple(this.tk,
                        vs.zip(up.ts).map { (vs,ts) ->
                            Pair(vs.first ?: ts.first, vs.second)
                        }
                    )
                }
            }
            if (this.xtp == null) {
                this.xtp = dn
            }
            this.xtp
        }
        is Expr.Field -> {
            val col = this.col.infer(null)
            val tup = when (col) {
                is Type.Tuple -> col
                is Type.Data -> col.walk()?.third
                else -> null
            }
            if (tup !is Type.Tuple) null else {
                tup.index(this.idx)
            }
        }
        is Expr.Union -> TODO()
        is Expr.Pred -> TODO()
        is Expr.Disc -> TODO()
        is Expr.Cons -> {
            this.e.infer(this.walk(this.ts)!!.third)
            this.type()
        }

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
            //println(me.to_str())
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