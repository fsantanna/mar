package mar

fun Tk.Var.to_xdcl (fr: Any): XDcl? {
    return fr.up_first {
        if (it !is Stmt.Block) null else {
            it.to_dcls().find { (_,id,_) -> id.str == this.str }
        }
    } as XDcl?
}

fun Expr.Acc.to_xdcl (): XDcl? {
    return this.tk_.to_xdcl(this)
}

fun Stmt.Block.to_dcls (): List<XDcl> {
    return this.xup.let {
        when {
            (it is Stmt.Proto.Func) -> it.tp_.inps_.map { Triple(this, it.first, it.second) }
            (it is Stmt.Proto.Coro) -> it.tp_.inps_.map { Triple(this, it.first, it.second) }
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
                is Stmt.Dcl -> Triple(it, it.id, it.xtp)
                is Stmt.Proto -> Triple(it, it.id, it.tp)
                else -> error("impossible case")
            }
        }
}

fun check_vars () {
    fun fs (me: Stmt) {
        when (me) {
            is Stmt.Data -> {
                val up = me.xup!!
                if (up !is Stmt.Data) {
                    val s = up.walk(listOf(me.t))?.first
                    if (s!=null && s!=me) {
                        err(me.tk, "type error : data \"${me.t.str}\" is already declared")
                    }
                }
                if (me.subs != null) {
                    for (sub in me.subs) {
                        val x = me.subs.find { sub.t.str==it.t.str && sub!=it }
                        if (x != null) {
                            err(x.tk, "type error : data \"${x.t.str}\" is already declared")
                        }
                    }
                }
            }
            is Stmt.Block -> {
                val ids1 = me.to_dcls()
                me.ups().filter { it is Stmt.Block }.forEach {
                    it as Stmt.Block
                    val ids2 = it.to_dcls()
                    val err = ids1.find { (n1,id1,_) ->
                        ids2.find { (n2,id2,_) ->
                            (n2 != n1) && (id2.str == id1.str) && (
                                n2.ups_depth() == n1.ups_depth() ||
                                n2.n < n1.n
                            )
                        } != null
                    }
                    if (err != null) {
                        val (_,id,_) = err
                        err(id, "declaration error : variable \"${id.str}\" is already declared")
                    }
                }
            }
            is Stmt.SetE -> {
                if (me.dst is Expr.Nat && me.dst.tk.str=="mar_ret") {
                    me.dst.xtp = (me.up_first { it is Stmt.Proto } as Stmt.Proto?)?.tp?.out
                }
            }
            is Stmt.Escape -> {
                val dos = me.ups_until { it is Stmt.Proto }
                    .filter {
                        it is Stmt.Block && it.esc!=null && it.esc.ts.let {
                            it.map { it.str }.zip(me.e.tp.ts.map { it.str }).all { (a,b) ->
                                a == b
                            }
                        }
                    }
                if (dos.size == 0) {
                    err(me.tk, "escape error : expected matching enclosing block")
                }
            }
            is Stmt.Yield -> {
                val up = me.up_first { it is Stmt.Proto }
                if (up !is Stmt.Proto.Coro) {
                    err(me.tk, "yield error : expected enclosing coro")
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
            is Expr.Cons -> {
                val v = me.walk(me.tp.ts)
                if (v == null) {
                    err(me.tk, "constructor error : data \"${me.tp.ts.to_str()}\" is not declared")
                }
            }
            else -> {}
        }
    }
    fun ft (me: Type) {
        when (me) {
            is Type.Data -> {
                val v = me.walk()
                if (v == null) {
                    err(me.tk, "type error : data \"${me.to_str()}\" is not declared")
                }
                val (s,_,_) = v
                if (s.subs==null && me.ts.size>1) {
                    err(me.tk, "type error : data \"${me.to_str()}\" is not declared")
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
            is Stmt.Block -> {
                if (me.esc != null) {
                    val xxx = me.esc.walk()
                    if (xxx==null || xxx.first.subs==null) {
                        err(me.esc.tk, "block error : expected hierarchical data type")
                    }
                }
            }
            is Stmt.SetE -> {
                val dst = me.dst.type()
                val src = me.src.type()
                if (dst!=null && src!=null) {
                    if (!dst.is_sup_of(src)) {
                        err(me.tk, "set error : types mismatch")
                    }
                }
            }
            is Stmt.SetS -> {
                val dst = me.dst.type()
                val src = me.src.type()
                if (dst!=null && src!=null) {
                    if (!dst.is_sup_of(src)) {
                        err(me.tk, "set error : types mismatch")
                    }
                }
            }
            is Stmt.If -> {
                if (!me.cnd.typex().is_sup_of(Type.Prim(Tk.Type("Bool",me.tk.pos)))) {
                    err(me.tk, "if error : expected boolean condition")
                }
            }
            is Stmt.MatchT -> {
                val fsts = listOf(me.tst.typex()) + me.cases.map { it.first }.filterNotNull()
                val x: Type? = fsts.first()
                val xs = fsts.fold(x, { a,b -> a?.sup_vs(b) })
                if (xs == null) {
                    err(me.tk, "match error : types mismatch")
                }
            }
            is Stmt.MatchE -> {
                val fsts = listOf(me.tst.typex()) + me.cases.map { it.first }.filterNotNull().map { it.typex() }
                val x: Type? = fsts.first()
                val xs = fsts.fold(x, { a,b -> a?.sup_vs(b) })
                if (xs == null) {
                    err(me.tk, "match error : types mismatch")
                }
            }
            is Stmt.Catch -> {
                if (me.tp != null) {
                    val xxx = me.tp.walk()
                    if (xxx==null || xxx.first.subs==null) {
                        err(me.tp.tk, "catch error : expected hierarchical data type")
                    }
                }
            }
            is Stmt.Create -> {
                val co = me.co.type()
                if (co !is Type.Proto.Coro) {
                    err(me.tk, "create error : expected coroutine prototype")
                }
            }
            is Stmt.Start -> {
                val exe = me.exe.type()
                if (exe !is Type.Exec) {
                    err(me.tk, "start error : expected active coroutine")
                }
                val ok = (exe.inps.size == me.args.size) && exe.inps.zip(me.args).all { (thi,oth) -> thi.is_sup_of(oth.typex()) }
                if (!ok) {
                    err(me.tk, "start error : types mismatch")
                }
            }
            is Stmt.Resume -> {
                val exe = me.exe.typex()
                if (exe !is Type.Exec) {
                    err(me.tk, "resume error : expected active coroutine")
                }
                if (!exe.res.is_sup_of(me.arg.typex())) {
                    err(me.tk, "resume error : types mismatch")
                }
            }
            is Stmt.Yield -> {
                val up = me.up_first { it is Stmt.Proto } as Stmt.Proto.Coro
                val exe = up.tp_
                if (!exe.yld.is_sup_of(me.arg.typex())) {
                    err(me.tk, "yield error : types mismatch")
                }
            }
            else -> {}
        }
    }
    fun fe (me: Expr) {
        when (me) {
            is Expr.Uno -> {
                when (me.tk.str) {
                    "deref" -> if (me.e.type() !is Type.Pointer) {
                        err(me.tk, "operation error : expected pointer")
                    }
                    "#" -> if (me.e.type() !is Type.Vector) {
                        err(me.tk, "operation error : expected vector")
                    }
                }
            }
            is Expr.Tuple -> {
                val tp = Type.Tuple(me.tk, me.vs.map { (id,v) -> Pair(id, v.typex()) })
                if (!tp.is_sup_of(me.xtp!!)) {  // tp=[10]: xtp=[a:Int], correct is xtp.sup(tp), but not for tuple cons
                    err(me.tk, "tuple error : types mismatch")
                }
            }
            is Expr.Field -> {
                val tp = me.col.type()
                val tup = when (tp) {
                    //is Type.Any -> tp
                    is Type.Tuple -> tp
                    is Type.Data -> tp.walk()?.third
                    else -> null
                }
                when (tup) {
                    //is Type.Any -> {}
                    !is Type.Tuple -> {
                        err(me.tk, "field error : types mismatch")
                    }
                    else -> if (tup.index(me.idx) == null) {
                        err(me.tk, "field error : invalid index")
                    }
                }
            }
            is Expr.Index -> {
                when {
                    (me.col.typex() !is Type.Vector) -> err(me.col.tk, "index error : expected vector")
                    (!me.idx.typex().is_num()) -> err(me.idx.tk, "index error : expected number")
                }
            }
            is Expr.Union -> {
                val (_,sub) = me.xtp!!.disc(me.idx).nulls()
                if (sub==null || !sub.is_sup_of(me.v.typex())) {
                    err(me.tk, "union error : types mismatch")
                }
            }
            is Expr.Disc -> {
                if (me.col.typex().discx(me.idx) == null) {
                    err(me.tk, "discriminator error : types mismatch")
                }
            }
            is Expr.Pred -> {
                if (me.col.typex().discx(me.idx) == null) {
                    err(me.tk, "predicate error : types mismatch")
                }
            }
            is Expr.Cons -> {
                val tp = me.walk(me.tp.ts)!!.third
                val te = me.e.typex()
                if (!tp.is_sup_of(te)) {
                    err(me.e.tk, "constructor error : types mismatch")
                }
            }
            is Expr.Bin -> {
                val tp1 = me.e1.typex()
                val tp2 = me.e2.typex()
                val ok = when (me.tk_.str) {
                    "==", "!=" -> tp1.is_sup_sub_of(tp2)
                    ">", "<", ">=", "<=",
                    "+", "-", "*", "/", "%" -> (tp1.is_num() && tp2.is_num())
                    "||", "&&" -> {
                        tp1.is_sup_of(Type.Prim(Tk.Type( "Bool", me.tk.pos))) &&
                        tp2.is_sup_of(Type.Prim(Tk.Type( "Bool", me.tk.pos)))
                    }
                    "++" -> {
                        when {
                            (tp1 is Type.Vector && tp2 is Type.Vector) -> {
                                (tp1.max!=null && tp2.max!=null && tp1.tp.is_same_of(tp2.tp))
                            }
                            (tp1 is Type.Vector && me.e2 is Expr.Str) -> {
                                (tp1.max!=null && tp1.is_str())
                            }
                            (tp2 is Type.Vector && me.e1 is Expr.Str) -> {
                                (tp2.max!=null && tp2.is_str())
                            }
                            (me.e1 is Expr.Str && me.e2 is Expr.Str) -> true
                            else -> false
                        }
                    }
                    else -> error("impossible case")
                }
                if (!ok) {
                    err(me.tk, "operation error : types mismatch")
                }
            }
            is Expr.Call -> {
                val tp = me.f.type()
                val ok = when {
                    (tp is Type.Any) -> true
                    (tp is Type.Nat) -> true
                    (tp !is Type.Proto.Func) -> false
                    (tp.inps.size != me.args.size) -> false
                    else -> tp.inps.zip(me.args).all { (par, arg) ->
                        par.is_sup_of(arg.typex())
                    }
                }
                if (!ok) {
                    err(me.tk, "call error : types mismatch")
                }
            }
            is Expr.If -> {
                if (!me.cnd.typex().is_sup_of(Type.Prim(Tk.Type("Bool",me.tk.pos)))) {
                    err(me.cnd.tk, "if error : expected boolean condition")
                }
                if (me.t.typex().sup_vs(me.f.typex()) == null) {
                    err(me.tk, "if error : types mismatch")
                }
            }
            is Expr.MatchT -> {
                val fsts = listOf(me.tst.typex()) + me.cases.map { it.first }.filterNotNull()
                val x: Type? = fsts.first()
                val xs = fsts.fold(x, { a,b -> a?.sup_vs(b) })
                if (xs == null) {
                    err(me.tk, "match error : types mismatch")
                }

                val snds = me.cases.map { it.second.typex() }
                val y: Type? = me.cases.first().second.typex()
                val ys = snds.fold(y) {a,b -> a?.sup_vs(b) }
                if (ys == null) {
                    err(me.tk, "match error : types mismatch")
                }
            }
            is Expr.MatchE -> {
                val fsts = listOf(me.tst.typex()) + me.cases.map { it.first }.filterNotNull().map { it.typex() }
                val x: Type? = fsts.first()
                val xs = fsts.fold(x, { a,b -> a?.sup_vs(b) })
                if (xs == null) {
                    err(me.tk, "match error : types mismatch")
                }

                val snds = me.cases.map { it.second.typex() }
                val y: Type? = me.cases.first().second.typex()
                val ys = snds.fold(y) {a,b -> a?.sup_vs(b) }
                if (ys == null) {
                    err(me.tk, "match error : types mismatch")
                }
            }
            else -> {}
        }
    }
    G.outer!!.dn_visit_pos(::fs, ::fe, {null})
}
