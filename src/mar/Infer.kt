package mar

fun Expr.infer (tp: Type?): Type? {
    return when (this) {
        is Expr.Nat -> {
            if (this.xtp == null) {
                this.xtp = tp ?: Type.Any(this.tk)
            }
            this.xtp
        }
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
                    Pair(tk, e.infer(if (up.ts.size<i+1) null else up.ts[i].second))
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
            if (dn == null) null else {
                if (this.xtp == null) {
                    this.xtp = dn
                }
                this.xtp
            }
        }
        is Expr.Field -> {
            val col = this.col.infer(null)
            val tup = when (col) {
                is Type.Tuple -> col
                is Type.Data -> col.walk()?.third
                else -> null
            }
            when {
                (col == null) -> null
                (tup !is Type.Tuple) -> Type.Err(this.tk)
                else -> tup.index(this.idx) ?: Type.Err(this.tk)
            }
        }
        is Expr.Union -> {
            val up = this.xtp ?: tp
            val sub = if (up !is Type.Union) null else {
                up.disc(this.idx).nulls().second
            }
            val dn = this.v.infer(sub)
            if (dn == null) null else {
                if (this.xtp == null) {
                    this.xtp = (if (up is Type.Union) up else null)
                }
                this.xtp
            }
        }
        is Expr.Pred -> {
            val col = this.col.infer(null)
            if (col == null) null else {
                if (col.discx(this.idx)?.second == null) {
                    Type.Err(this.tk)
                } else {
                    Type.Prim(Tk.Type("Bool",this.tk.pos.copy()))
                }
            }
        }
        is Expr.Disc -> {
            val col = this.col.infer(null)
            if (col == null) null else {
                col.discx(this.idx)?.second ?: Type.Err(this.tk)
            }
        }
        is Expr.Cons -> {
            val e = this.e.infer(this.walk(this.ts)!!.third)
            if (e == null) null else {
                this.type()
            }
        }

        is Expr.Uno -> {
            val e = this.e.infer(null)
            if (e == null) null else {
                this.type()
            }
        }
        is Expr.Bin -> {
            val e1 = this.e1.infer(null)
            val e2 = this.e2.infer(null)
            if (e1==null || e2==null) null else {
                this.type()
            }
        }
        is Expr.Call -> {
            val f = this.f.infer(null)
            val args = if (f is Type.Proto) {
                this.args.mapIndexed { i,e ->
                    e.infer(f.inps[i])
                }
            } else {
                this.args.map {
                    it.infer(null)
                }
            }
            when {
                (f==null || args.any { it==null }) -> null
                (f is Type.Nat || f is Type.Any || f is Type.Proto) -> this.type()
                else -> Type.Err(this.tk)
            }
        }
        is Expr.Throw -> {
            this.xtp = tp
            val e = this.e.infer(null)
            if (e == null) null else {
                this.xtp
            }
        }

        is Expr.Create -> {
            val co = this.co.infer(null)
            when (co) {
                null -> null
                !is Type.Proto.Coro -> Type.Err(this.tk)
                else -> this.type()
            }
        }
        is Expr.Start -> {
            val exe = this.exe.infer(null)
            val args = if (exe is Type.Exec) {
                this.args.mapIndexed { i,e ->
                    e.infer(exe.inps[i])
                }
            } else {
                this.args.map {
                    it.infer(null)
                }
            }
            when {
                (exe==null || args.any { it==null }) -> null
                (exe !is Type.Exec) -> Type.Err(this.tk)
                else -> this.type()
            }
        }
        is Expr.Resume -> {
            val exe = this.exe.infer(null)
            if (exe is Type.Exec) {
                this.arg.infer(exe.res)
            }
            when (exe) {
                null -> null
                !is Type.Exec -> Type.Err(this.tk)
                else -> this.type()
            }
        }
        is Expr.Yield -> {
            val coro = (this.up_first { it is Stmt.Proto.Coro } as Stmt.Proto.Coro).tp_
            val arg = this.arg.infer(coro.yld)
            if (arg == null) null else {
                coro.res
            }
        }

        is Expr.If -> {
            val cnd = this.cnd.infer(null)
            val t = this.t.infer(tp)
            val f = this.f.infer(tp)
            if (cnd==null || t==null || f==null) null else {
                t.sup_vs(f) ?: Type.Err(this.tk)
            }
        }
        is Expr.Match -> {
            val tst = this.tst.infer(tp)
            val cases = this.cases.map {
                val fst = if (it.first == null) Type.Err(this.tk) else it.first!!.infer(tst)
                Pair(fst, it.second.infer(tp))
            }
            if (tst==null || cases.any { (a,b) -> a==null||b==null }) null else {
                val es = cases.map { it.second } as List<Type>
                es.fold(es.first()) { a,b -> a.sup_vs(b) ?: Type.Err(this.tk) }
            }
        }
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

            is Stmt.If -> (me.cnd.infer(Type.Prim(Tk.Type("Bool",me.tk.pos.copy()))) != null)
            is Stmt.Loop -> true

            is Stmt.Print -> (me.e.infer(null) != null)
            is Stmt.Pass -> (me.e.infer(Type.Unit(me.tk)) != null)
        }
        if (!ok) {
            //println(me.to_str())
            err(me.tk, "inference error : unknown types")
        }
    }
    G.outer!!.dn_visit_pos(::fs, {}, {})

    G.outer!!.dn_visit_pre({
        if (it is Stmt.Dcl) {
            if (it.xtp==null || it.xtp is Type.Any || it.xtp is Type.Err) {
                err(it.tk, "inference error : unknown type")
            }
        }
    }, {null}, {null})
}