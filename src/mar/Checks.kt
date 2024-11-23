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
                val top = me.ts.first().str
                val dat = top.to_data()
                when {
                    (dat == null) -> err(me.tk, "type error : data \"$top\" is not declared")
                    (dat.tp !is Type.Union) -> err(me.tk, "type error : data \"$top\" is not extendable")
                }
                val uni = dat!!.tp as Type.Union
                if (me.ts.size >= 3) {
                    val tp1 = uni.indexes(me.ts.drop(1).dropLast(1).map { it.str })
                    if (tp1 == null) {
                        err(me.tk,"type error : data \"${me.ts.dropLast(1).map { it.str }.joinToString(".")}\" is invalid")
                        //err(me.tk, "type error : data \"${path.map { it.str }.joinToString(".")}\" is not extendable")
                    }
                    println(tp1.third.to_str())
                }
                val tp2 = uni.indexes(me.ts.drop(1).map { it.str })
                if (tp2 != null) {
                    err(me.tk, "type error : data \"${me.ts.map { it.str }.joinToString(".")}\" is already declared")
                }
                /*
                sup!!.let {
                    it.ids!!.add(me.ts.last())
                    it.ts.add(me.tp)
                }
                 */
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
                val stmt = me.to_stmt()
                if (stmt == null) {
                    err(me.tk, "type error : data \"${me.to_str()}\" is not declared")
                }
                /*
                val ok = me.ts.filterIndexed { i,t -> i<me.ts.size-1 && t.str==me.ts[i+1].str }.let {
                    when {
                        (it.size >= 2) -> false
                        (it.size == 0) -> true
                        else -> it.first().str == me.ts[me.ts.size - 1].str
                    }
                }
                 */
                when {
                    (me.ts.size == 1) -> {}
                    (dat.tp !is Type.Union) ->
                        err(me.tk, "type error : data \"${me.ts.to_str()}\" is invalid")
                    (dat.tp.indexes(me) == null) ->
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
                val tp = me.col.type()
                val tpx = me.col.type().no_data()
                if (tpx is Type.Any) {
                    return
                }
                val tup = when {
                    (tp !is Type.Data) -> tpx
                    (tp.ts.size == 1) -> tpx
                    else -> (tpx as Type.Union).indexes(tp)?.third
                }
                val i = me.idx.toIntOrNull()
                val ok = when {
                    (tup !is Type.Tuple) -> false
                    (i!=null && (i<=0 || i>tup.ts.size)) -> false
                    (i==null && (tup.ids==null || tup.ids.find { it.str==me.idx } == null)) -> false
                    else -> true
                }
                if (!ok) {
                    err(me.tk, "field error : types mismatch")
                }
            }
            is Expr.Union -> {
                val (_,sub) = me.xtp!!.disc(me.idx).nulls()
                if (sub==null || !sub.is_sup_of(me.v.type())) {
                    err(me.tk, "union error : types mismatch")
                }
            }
            is Expr.Disc -> {
                if (me.col.type().discx(me.idx) == null) {
                    err(me.tk, "discriminator error : types mismatch")
                }
            }
            is Expr.Pred -> {
                if (me.col.type().discx(me.idx) == null) {
                    err(me.tk, "predicate error : types mismatch")
                }
            }
            is Expr.Cons -> {
                val tp = me.dat.no_data()
                val te = me.e.type()
                if (!tp.is_sup_of(te)) {
                    err(me.e.tk, "constructor error : types mismatch")
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
