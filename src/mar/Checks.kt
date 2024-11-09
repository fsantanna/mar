package mar

fun Expr.Acc.to_xdcl (): XDcl? {
    return this.up_first {
        if (it !is Stmt.Block) null else {
            it.to_dcls().find { (_,id,_) -> id.str == this.tk.str }
        }
    } as XDcl?
}

fun XDcl.to_dcl (): Stmt.Dcl? {
    val (n,_,_) = this
    return G.ns[n]!!.let {
        if (it is Stmt.Dcl) it else null
    }
}

fun Stmt.Block.to_dcls (): List<XDcl> {
    return this.fup().let {
        when {
            (it is Stmt.Proto.Func) -> it.tp_.inps_.map { Triple(this.n, it.first, it.second) }
            (it is Stmt.Proto.Coro) -> it.tp_.inps_.map { Triple(this.n, it.first, it.second) }
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
        {null},
        {null}
    )
        .let { it as List<Stmt> }
        .map {
            when (it) {
                is Stmt.Dcl -> Triple(it.n, it.id, it.xtp)
                is Stmt.Proto -> Triple(it.n, it.id, it.tp)
                else -> error("impossible case")
            }
        }
}

fun check_vars () {
    fun fs (me: Stmt) {
        when (me) {
            is Stmt.Extd -> {
                val top = me.ids.first().str
                val dat = top.to_data()
                if (dat == null) {
                    err(me.tk, "type error : data \"$top\" is not declared")
                }
                // all path, excluding last, exists
                var sup: Type.Union? = null
                for (n in 1..me.ids.size-1) {
                    val path = me.ids.take(n)
                    val idx_tps = dat.walk(path.map { it.str })
                    if (idx_tps == null) {
                        err(me.tk, "type error : data \"${path.map { it.str }.joinToString(".")}\" is invalid")
                    }
                    val (_,tps) = idx_tps
                    val tp = tps.last()
                    if (tp !is Type.Union) {
                        err(me.tk, "type error : data \"${path.map { it.str }.joinToString(".")}\" is not extendable")
                    }
                    if (n == me.ids.size-1) {
                        sup = tp
                    }
                }
                // last path does not exist
                if (dat.walk(me.ids.map { it.str }) != null) {
                    err(me.tk, "type error : data \"${me.ids.map { it.str }.joinToString(".")}\" is already declared")
                }
                sup!!.let {
                    it.ids!!.add(me.ids.last())
                    it.ts.add(me.tp)
                }
            }
            is Stmt.Block -> {
                val ids2 = me.to_dcls()
                me.ups().filter { it is Stmt.Block }.forEach {
                    it as Stmt.Block
                    val ids1 = it.to_dcls()
                    val err = ids2.find { (n2,id2,_) ->
                        ids1.find { (n1,id1,_) ->
                            (n1 != n2) && (id1.str == id2.str)
                        } != null
                    }
                    if (err != null) {
                        val (_,id,_) = err
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
                if (me.to_xdcl() == null) {
                    err(me.tk, "access error : variable \"${me.tk.str}\" is not declared")
                }
            }
            else -> {}
        }
    }
    fun ft (me: Type) {
        when (me) {
            is Type.Data -> {
                val dat = me.to_data()
                val fst = me.ts.first()
                if (dat == null) {
                    err(me.tk, "type error : data \"${fst.str}\" is not declared")
                }
                val ok = me.ts.filterIndexed { i,t -> i<me.ts.size-1 && t.str==me.ts[i+1].str }.let {
                    when {
                        (it.size >= 2) -> false
                        (it.size == 0) -> true
                        else -> it.first().str == me.ts[me.ts.size - 1].str
                    }
                }
                if (!ok || me.walk()==null) {
                    err(me.tk, "type error : data \"${me.ts.to_str()}\" is invalid")
                }
            }
            else -> {}
        }
    }
    G.outer!!.dn_visit_pre(::fs, ::fe, ::ft)
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
            else -> {}
        }
    }
    fun fe (me: Expr) {
        when (me) {
            is Expr.Tuple -> {
                val tp = Type.Tuple(me.tk, me.vs.map { it.type() }, me.ids)
                if (!me.xtp!!.is_sup_of(tp)) {
                    //println(me.xtp!!.to_str())
                    //println(tp.to_str())
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
                val idx_tp = me.xtp!!.sub__idx_id__to__idx_tp(null, me.idx)
                if (idx_tp==null || !idx_tp.second.is_sup_of(me.v.type())) {
                    err(me.tk, "union error : types mismatch")
                }
            }
            is Expr.Disc -> {
                val tp = me.col.type()
                val sup = if (tp !is Type.Data) null else tp.ts.last().str
                val ok = tp.no_data().sub__idx_id__to__idx_tp(sup, me.path.first())
                if (ok == null) {
                    err(me.tk, "discriminator error : types mismatch")
                }
            }
            is Expr.Pred -> {
                val ok = me.col.type().no_data().sub__idx_id__to__idx_tp(null, me.idx)
                if (ok == null) {
                    err(me.tk, "predicate error : types mismatch")
                }
            }
            is Expr.Cons -> {
                val ts = me.dat.walk()!!.second
                if (ts.filter { it != null }.size != me.es.size) {
                    //println(ts.map { it.to_str() })
                    //println(me.es.map { it.to_str() })
                    err(me.tk, "constructor error : arity mismatch")
                }
                val xts = ts.filter { it != null } as List<Type>
                val x = xts.zip(me.es).find { (tp,e) ->
                    !tp.is_sup_of(e.type())
                }
                if (x != null) {
                    //println(x.second.to_str())
                    err(x.second.tk, "constructor error : types mismatch")
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
            is Expr.Create -> {
                val co = me.co.type()
                if (co !is Type.Proto.Coro) {
                    err(me.tk, "create error : expected coroutine prototype")
                }
            }
            is Expr.Start -> {
                val exe = me.exe.type()
                if (exe !is Type.Exec) {
                    err(me.tk, "start error : expected active coroutine")
                }
                val ok = (exe.inps.size == me.args.size) && exe.inps.zip(me.args).all { (thi,oth) -> thi.is_sup_of(oth.type()) }
                if (!ok) {
                    err(me.tk, "start error : types mismatch")
                }
            }
            is Expr.Resume -> {
                val exe = me.exe.type()
                if (exe !is Type.Exec) {
                    err(me.tk, "resume error : expected active coroutine")
                }
                if (!exe.res.is_sup_of(me.arg.type())) {
                    err(me.tk, "resume error : types mismatch")
                }
            }
            is Expr.Yield -> {
                val up = me.up_first { it is Stmt.Proto }
                if (up !is Stmt.Proto.Coro) {
                    err(me.tk, "yield error : expected enclosing coro")
                }
                val exe = up.tp_
                if (!exe.yld.is_sup_of(me.arg.type())) {
                    err(me.tk, "yield error : types mismatch")
                }
            }
            else -> {}
        }
    }
    G.outer!!.dn_visit_pos(::fs, ::fe, {null})
}
