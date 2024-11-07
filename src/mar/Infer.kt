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

fun Stmt.Data.hier_to_types (tp: Type.Data): List<Type>? {
    assert(this.id.str==tp.ts.first().str)
    val base = tp.base()
    val lst = mutableListOf<Type>()
    var cur: Type = this.tp
    if (base != null) {
        val sub = tp.ts.dropLast(2).find { it.str==base }
        if (sub != null) {
            return null // X.Y.A.Y.Y
        }
    }
    for (i in 1 .. tp.ts.size-1) {
        val sub = tp.ts[i]
        when {
            (cur !is Type.Union) -> return null
            (cur.ids == null) -> return null
        }
        val uni = cur as Type.Union
        if (uni._0 != null) {
            lst.add(uni._0)
        }
        if (base!=null && i==tp.ts.size-1) {
            return lst  // X.A.A
        }
        val k = uni.sub_to_idx(sub.str)
        if (k == null) {
            return null
        }
        cur = uni.ts[k]
    }
    lst.add(cur)
    return lst
}

fun Expr.infer (): Type? {
    val up = this.fupx()
    return when (up) {
        is Stmt.Set -> {
            assert(up.src.n == this.n)
            up.dst.typex()
        }
        is Expr.Cons -> {
            val dat = up.dat.to_data()!!
            val i = up.es.indexOfFirst { it.n==this.n }
            val tp = dat.hier_to_types(up.dat)!![i]
            //println(this.to_str())
            //println(up.to_str())
            //println(listOf(i,tp.to_str()))
            tp
        }
        is Expr.Tuple -> up.typex().let {
            if (it == null) null else {
                it as Type.Tuple
                val i = up.vs.indexOfFirst { it.n==this.n }
                it.ts[i]
            }
        }
        is Expr.Union -> up.typex().let {
            if (it == null) null else {
                it as Type.Union
                it.ts[it.sub_to_idx(up.idx)!!]
            }
        }
        is Expr.Call -> {
            val i = up.args.indexOfFirst { it.n == this.n }
            (up.f.type() as Type.Proto.Func).inps[i]
        }
        is Stmt.Return -> {
            assert(up.e.n == this.n)
            (up.up_first { it is Stmt.Proto.Func } as Stmt.Proto.Func).tp.out
        }
        is Expr.Start -> {
            val i = up.args.indexOfFirst { it.n == this.n }
            (up.exe.type() as Type.Exec).inps[i]
        }
        is Expr.Resume -> {
            assert(up.arg.n == this.n)
            (up.exe.type() as Type.Exec).res
        }
        is Expr.Yield -> {
            assert(up.arg.n == this.n)
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
                        val tps = me.vs.map { it.type() }
                        me.xtp = Type.Tuple(me.tk, tps, me.ids)
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
                    val up = me.fupx()
                    me.xtp = when {
                        (up is Expr.Call && up.f.n==me.n)   -> null
                        (up is Stmt.Set  && up.dst.n==me.n) -> null
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
                    val dcl = me.dst.to_xdcl()!!.to_dcl()
                    if (dcl!=null && dcl.xtp==null) {
                        dcl.xtp = me.src.typex()
                    }
                }
            }
            else -> {}
        }
    }
    G.outer!!.dn_visit_pos(::fs, ::fe, {null})
    G.outer!!.dn_visit_pre({
        if (it is Stmt.Dcl) {
            if (it.xtp == null) {
                err(it.tk, "inference error : unknown type")
            }
        }
    }, {null}, {null})
}