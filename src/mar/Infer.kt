package mar

class InferException () : Exception("inference error")

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
        is Expr.Null, is Expr.Unit -> this.type()
        is Expr.Num -> this.type() //.let { it.num_cast(tp).sup_vs(it)!! }

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
                    this.xtp = if (tp is Type.Tuple) tp else dn // b/c of int/float
                }
                this.xtp
            }
        }
        is Expr.Vector -> {
            val up = this.xtp ?: tp
            val xup = if (up !is Type.Vector) null else up.tp
            val dn = if (this.vs.size == 0) null else {
                val vs = this.vs.map { it.infer(xup) }
                val v = vs.fold(vs.first()) { a,b ->
                    if (a==null||b==null) null else a.sup_vs(b)
                }
                //println(v?.to_str())
                if (v == null) null else {
                    Type.Vector(this.tk, vs.size, v)
                }
            }
            if (dn == null) null else {
                if (this.xtp == null) {
                    this.xtp = dn
                }
                this.xtp
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
            when {
                (col == null) -> null
                (tup !is Type.Tuple) -> throw InferException()
                else -> tup.index(this.idx) ?: throw InferException()
            }
        }
        is Expr.Index -> {
            this.col.infer(Type.Prim(Tk.Type("Int",this.tk.pos)))
            val col = this.col.infer(null)
            if (col !is Type.Vector) null else {
                col.tp
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
                    throw InferException()
                } else {
                    Type.Prim(Tk.Type("Bool",this.tk.pos))
                }
            }
        }
        is Expr.Disc -> {
            val col = this.col.infer(null)
            if (col == null) null else {
                col.discx(this.idx)?.second ?: throw InferException()
            }
        }
        is Expr.Cons -> {
            val e = this.e.infer(this.walk(this.ts)!!.third)
            if (e == null) null else {
                this.type()
            }
        }

        is Expr.Uno -> {
            val e = this.e.infer(tp)
            if (e == null) null else {
                this.type()
            }
        }
        is Expr.Bin -> {
            val e1 = this.e1.infer(tp)
            val e2 = this.e2.infer(tp)
            if (e1==null || e2==null) null else {
                this.type()
            }
        }
        is Expr.Call -> {
            val f = this.f.infer(null)
            val args = if (f is Type.Proto) {
                this.args.mapIndexed { i,e ->
                    if (i < f.inps.size) {
                        e.infer(f.inps[i])
                    }
                }
            } else {
                this.args.map {
                    it.infer(null)
                }
            }
            when {
                (f==null || args.any { it==null }) -> null
                (f is Type.Nat || f is Type.Any || f is Type.Proto) -> this.type()
                else -> throw InferException()
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
                !is Type.Proto.Coro -> throw InferException()
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
                (exe !is Type.Exec) -> throw InferException()
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
                !is Type.Exec -> throw InferException()
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
                this.xtp = t.sup_vs(f)
                if (this.xtp == null) {
                    throw InferException()
                }
                this.xtp
            }
        }
        is Expr.MatchT -> {
            val tst = this.tst.infer(null)
            val cases = this.cases.map {
                val fst = if (it.first == null) Type.Any(this.tk) else it.first!!
                Pair(fst, it.second.infer(tp))
            }
            if (tst==null || cases.any { (a,b) -> a==null||b==null }) null else {
                val es = cases.map { it.second } as List<Type>
                val fst: Type? = es.first()
                this.xtp = es.fold(fst) { a,b -> a?.sup_vs(b) }
                if (this.xtp == null) {
                    throw InferException()
                }
                this.xtp
            }
        }
        is Expr.MatchE -> {
            val tst = this.tst.infer(null)
            val cases = this.cases.map {
                val fst = if (it.first == null) Type.Any(this.tk) else it.first!!.infer(tst)
                Pair(fst, it.second.infer(tp))
            }
            if (tst==null || cases.any { (a,b) -> a==null||b==null }) null else {
                val es = cases.map { it.second } as List<Type>
                val fst: Type? = es.first()
                this.xtp = es.fold(fst) { a,b -> a?.sup_vs(b) }
                if (this.xtp == null) {
                    throw InferException()
                }
                this.xtp
            }
        }
    }.let {
        this.xnum = when {
            (it == null) -> null
            (tp == null) -> null
            !it.is_num() -> null
            !tp.is_num() -> null
            else -> tp
        }
        it
    }
}

fun infer_types () {
    fun fs (me: Stmt) {
        when (me) {
            is Stmt.Data, -> {}
            is Stmt.Proto -> {}

            is Stmt.Block -> {}
            is Stmt.Dcl -> {}
            is Stmt.Set -> {
                val tp1 = me.dst.infer(null)
                val tp2 = me.src.infer(tp1)
                if (tp2!=null && tp1==null) {
                    if (me.dst is Expr.Acc) {
                        val dcl = me.dst.to_xdcl()!!.first
                        if (dcl is Stmt.Dcl) {
                            assert(dcl.xtp == null)
                            dcl.xtp = tp2
                        }
                    } else {
                        me.dst.infer(null)
                    }
                }
            }

            is Stmt.Escape -> me.e.infer(null)
            is Stmt.Defer -> {}
            is Stmt.Catch -> {}

            is Stmt.If -> me.cnd.infer(Type.Prim(Tk.Type("Bool",me.tk.pos)))
            is Stmt.Loop -> {}
            is Stmt.MatchT -> me.tst.infer(null)
            is Stmt.MatchE -> {
                val tst = me.tst.infer(null)
                me.cases.forEach {
                    it.first?.infer(tst)
                }
            }

            is Stmt.Print -> me.e.infer(null)
            is Stmt.Pass -> me.e.infer(Type.Unit(me.tk))
        }
        me.dn_collect_pre({if (me==it) emptyList<Unit>() else null}, {
            val xtp = when (it) {
                is Expr.Tuple  -> it.xtp
                is Expr.Vector -> it.xtp
                is Expr.Union  -> it.xtp
                is Expr.Throw  -> it.xtp
                is Expr.If     -> it.xtp
                is Expr.MatchT -> it.xtp
                is Expr.MatchE -> it.xtp
                else -> Unit
            }
            /*
                println("-=-=-")
                println(me.to_str())
                println(it.to_str())
                println(xtp)
             */
            if (xtp == null) {
                err(it.tk, "inference error : unknown type")
            }
            emptyList()
        }, {null})
    }

    val ok = try {
        G.outer!!.dn_visit_pos(::fs, {}, {})
        true
    } catch (x: InferException) {
        false
    }

    if (ok) {
        G.outer!!.dn_visit_pos({
            when (it) {
                is Stmt.Dcl -> {
                    //println(listOf(it.to_str(), it.xtp?.to_str()))
                    if (it.xtp==null || it.xtp is Type.Any) {
                        err(it.id, "inference error : unknown type")
                    }
                }
                else -> {}
            }
        }, {}, {})
    }
}