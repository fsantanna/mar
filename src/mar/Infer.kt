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

fun Stmt.Data.flat_to_type (tp: Type.Data): Type? {
    assert(!this.hier && this.id.str==tp.ts.first().str)
    var cur: Type? = this.tp
    for (sub in tp.ts.drop(1)) {
        val uni = cur
        cur = when {
            (uni !is Type.Union) -> null
            (uni.ids == null) -> null
            else -> {
                val i = uni.ids.indexOfFirst { it.str == sub.str }
                if (i == -1) null else {
                    uni.ts[i]
                }
            }
        }
        if (cur == null) {
            break
        }
    }
    return cur
}

fun Stmt.Data.hier_to_tuple (hier: Type.Data): Type.Tuple {
    assert(this.hier)

    val fst = hier.ts.first()
    var cur: Type? = this.tp
    val tps: MutableList<Type> = mutableListOf()
    val ids: MutableList<Tk.Var?> = mutableListOf()
    var n = hier.ts.size - 1

    fun xxx () {
        val tp = cur!!
        when (tp) {
            is Type.Unit -> {}
            is Type.Tuple -> {
                val ts = if (n == 0) tp.ts else tp.ts.dropLast(1)
                tps.addAll(ts)
                ids.addAll(ts.mapIndexed { i,_ -> if (tp.ids == null) null else tp.ids[i] })
            }
            else -> {
                if (tp is Type.Union && n>0) {
                    // this union is the next subtyoe
                } else {
                    tps.add(tp)
                    ids.add(null)
                }
            }
        }
    }

    xxx()
    for (sub in hier.ts.drop(1)) {
        n--
        val uni = when (cur) {
            is Type.Tuple -> cur.ts.lastOrNull()    // data X: [x, <K:...,A: ...>]
            is Type.Union -> cur                    // data X: <K:...,A: ...>
            else -> null
        }
        cur = when {
            (uni !is Type.Union) -> null
            (uni.ids == null) -> null
            else -> {
                val i = uni.ids.indexOfFirst { it.str == sub.str }
                if (i == -1) null else {
                    uni.ts[i]
                }
            }
        }
        if (cur == null) {
            err(fst, "constructor error : invalid subtype \"${sub.str}\"")
        }
        xxx()
        //println(listOf(sub.str, cur.to_str()))
    }

    val ids1 = ids.filter { it != null }
    val ids2 = ids.filter { it == null }
    if (ids1.size!=0 && ids2.size!=0) {
        error("TODO - mixing ids and nulls")
    }

    return Type.Tuple(fst, tps, if (ids1.size==0) null else ids1 as List<Tk.Var>)
}

fun Expr.infer (): Type? {
    val up = this.fupx()
    return when (up) {
        is Stmt.Set -> {
            assert(up.src.n == this.n)
            up.dst.typex()
        }
        is Expr.Cons -> {
            val dat = up.ts.to_data()!!
            if (!dat.hier) {
                dat.flat_to_type(up.ts)
            } else {
                // always expands to tuple, but depending on this.typex() context,
                // we may change from tup -> one
                val tup = dat.hier_to_tuple(up.ts)!!
                val one = when {
                    (tup.ts.size == 0) -> Type.Unit(tup.tk)
                    (tup.ts.size >= 2) -> tup
                    else -> tup.ts.first()
                }
                //println(listOf("ins", tup.to_str(), one.to_str()))
                when (this) {
                    is Expr.Tuple -> tup
                    is Expr.Unit -> one
                    else -> one

                }
            }
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
                it.ts[it.disc_to_i(up.idx)!! - 1]
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