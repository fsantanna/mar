package mar

fun <V> Stmt.dn_collect_pos (fs: ((Stmt)->List<V>)?, fe: ((Expr)->List<V>)?, ft: ((Type)->List<V>)?): List<V> {
    if (fs == null) {
        return emptyList()
    }
    return when (this) {
        is Stmt.Data   -> this.tp.dn_collect_pos(ft)
        is Stmt.Proto  -> this.blk.dn_collect_pos(fs,fe,ft) + this.tp.dn_collect_pos(ft)
        is Stmt.Return -> this.e.dn_collect_pos(fe)

        is Stmt.Block -> this.ss.map { it.dn_collect_pos(fs,fe,ft) }.flatten()
        is Stmt.Dcl -> this.xtp?.dn_collect_pos(ft) ?: emptyList()
        is Stmt.Set -> this.src.dn_collect_pos(fe) + this.dst.dn_collect_pos(fe)

        is Stmt.If    -> this.cnd.dn_collect_pos(fe) + this.t.dn_collect_pos(fs,fe,ft) + this.f.dn_collect_pos(fs,fe,ft)
        is Stmt.Loop  -> this.blk.dn_collect_pos(fs,fe,ft)
        is Stmt.Break -> emptyList()

        is Stmt.Print -> this.e.dn_collect_pos(fe)
        is Stmt.XExpr -> this.e.dn_collect_pos(fe)
        is Stmt.Nat -> emptyList()
    } + fs(this)
}
fun <V> Expr.dn_collect_pos (fe: ((Expr)->List<V>)?): List<V> {
    if (fe == null) {
        return emptyList()
    }
    return when (this) {
        is Expr.Uno   -> this.e.dn_collect_pos(fe)
        is Expr.Bin   -> this.e1.dn_collect_pos(fe) + this.e2.dn_collect_pos(fe)
        is Expr.Tuple -> this.vs.map { it.dn_collect_pos(fe) }.flatten()
        is Expr.Union -> this.v.dn_collect_pos(fe)
        is Expr.Field -> this.col.dn_collect_pos(fe)
        is Expr.Disc  -> this.col.dn_collect_pos(fe)
        is Expr.Pred  -> this.col.dn_collect_pos(fe)
        is Expr.Cons  -> this.e.dn_collect_pos(fe)
        is Expr.Call  -> this.f.dn_collect_pos(fe) + this.args.map { it.dn_collect_pos(fe) }.flatten()
        is Expr.Acc, is Expr.Nat, is Expr.Null, is Expr.Unit,
        is Expr.Bool, is Expr.Char, is Expr.Num -> emptyList()
        is Expr.Create -> this.co.dn_collect_pos(fe)
        is Expr.Start  -> this.args.map { it.dn_collect_pos(fe) }.flatten() + this.exe.dn_collect_pos(fe)
        is Expr.Resume -> this.arg.dn_collect_pos(fe) + this.exe.dn_collect_pos(fe)
        is Expr.Yield  -> this.arg.dn_collect_pos(fe)

    } + fe(this)
}
fun <V> Type.dn_collect_pos (ft: ((Type)->List<V>)?): List<V> {
    if (ft == null) {
        return emptyList()
    }
    return when (this) {
        is Type.Any -> emptyList()
        is Type.Unit -> emptyList()
        is Type.Prim -> emptyList()
        is Type.Data -> emptyList()
        is Type.Pointer -> this.ptr.dn_collect_pos(ft)
        is Type.Tuple -> this.ts.map { it.dn_collect_pos(ft) }.flatten()
        is Type.Union -> this.ts.map { it.dn_collect_pos(ft) }.flatten()
        is Type.Proto.Func -> (this.inps + listOf(this.out)).map { it.dn_collect_pos(ft) }.flatten()
        is Type.Proto.Coro -> (this.inps + listOf(this.res, this.yld, this.out)).map { it.dn_collect_pos(ft) }.flatten()
        is Type.Exec -> (this.inps + listOf(this.res, this.yld, this.out)).map { it.dn_collect_pos(ft) }.flatten()
    } + ft(this)
}

fun Stmt.dn_visit_pos (fs: ((Stmt)->Unit)?, fe: ((Expr)->Unit)?, ft: ((Type)->Unit)?) {
    this.dn_collect_pos (
        if (fs == null) null else ({ fs(it) ; emptyList<Unit>() }),
        if (fe == null) null else ({ fe(it) ; emptyList() }),
        if (ft == null) null else ({ ft(it) ; emptyList() })
    )
}
fun Expr.dn_visit_pos (fe: ((Expr)->Unit)?) {
    this.dn_collect_pos (
        if (fe == null) null else ({ fe(it) ; emptyList<Unit>() })
    )
}

fun <V> Stmt.dn_collect_pre (fs: ((Stmt)->List<V>?)?, fe: ((Expr)->List<V>?)?, ft: ((Type)->List<V>?)?): List<V> {
    if (fs == null) {
        return emptyList()
    }
    val v = fs(this)
    if (v === null) {
        return emptyList()
    }
    return v + when (this) {
        is Stmt.Data   -> this.tp.dn_collect_pre(ft)
        is Stmt.Proto  -> this.blk.dn_collect_pre(fs,fe,ft) + this.tp.dn_collect_pre(ft)
        is Stmt.Return -> this.e.dn_collect_pre(fe)

        is Stmt.Block -> this.ss.map { it.dn_collect_pre(fs,fe,ft) }.flatten()
        is Stmt.Dcl -> this.xtp?.dn_collect_pre(ft) ?: emptyList()
        is Stmt.Set -> this.dst.dn_collect_pre(fe) + this.src.dn_collect_pre(fe)

        is Stmt.If    -> this.cnd.dn_collect_pre(fe) + this.t.dn_collect_pre(fs,fe,ft) + this.f.dn_collect_pre(fs,fe,ft)
        is Stmt.Loop  -> this.blk.dn_collect_pre(fs,fe,ft)
        is Stmt.Break -> emptyList()

        is Stmt.Print -> this.e.dn_collect_pre(fe)
        is Stmt.XExpr -> this.e.dn_collect_pre(fe)
        is Stmt.Nat -> emptyList()
    }
}
fun <V> Expr.dn_collect_pre (fe: ((Expr)->List<V>?)?): List<V> {
    if (fe == null) {
        return emptyList()
    }
    val v = fe(this)
    if (v === null) {
        return emptyList()
    }
    return v + when (this) {
        is Expr.Uno   -> this.e.dn_collect_pre(fe)
        is Expr.Bin   -> this.e1.dn_collect_pre(fe) + this.e2.dn_collect_pre(fe)
        is Expr.Tuple -> this.vs.map { it.dn_collect_pre(fe) }.flatten()
        is Expr.Union -> this.v.dn_collect_pre(fe)
        is Expr.Field -> this.col.dn_collect_pre(fe)
        is Expr.Disc  -> this.col.dn_collect_pre(fe)
        is Expr.Pred  -> this.col.dn_collect_pre(fe)
        is Expr.Cons  -> this.e.dn_collect_pre(fe)
        is Expr.Call  -> this.f.dn_collect_pre(fe) + this.args.map { it.dn_collect_pre(fe) }.flatten()
        is Expr.Acc, is Expr.Nat, is Expr.Null, is Expr.Unit,
        is Expr.Bool, is Expr.Char, is Expr.Num -> emptyList()
        is Expr.Create -> this.co.dn_collect_pre(fe)
        is Expr.Start  -> this.exe.dn_collect_pre(fe) + this.args.map { it.dn_collect_pre(fe) }.flatten()
        is Expr.Resume -> this.exe.dn_collect_pre(fe) + this.arg.dn_collect_pre(fe)
        is Expr.Yield  -> this.arg.dn_collect_pre(fe)
    }
}
fun <V> Type.dn_collect_pre (ft: ((Type)->List<V>?)?): List<V> {
    if (ft == null) {
        return emptyList()
    }
    val v = ft(this)
    if (v === null) {
        return emptyList()
    }
    return v + when (this) {
        is Type.Any -> emptyList()
        is Type.Unit -> emptyList()
        is Type.Prim -> emptyList()
        is Type.Data -> emptyList()
        is Type.Pointer -> this.ptr.dn_collect_pre(ft)
        is Type.Tuple -> this.ts.map { it.dn_collect_pre(ft) }.flatten()
        is Type.Union -> this.ts.map { it.dn_collect_pre(ft) }.flatten()
        is Type.Proto.Func -> (this.inps + listOf(this.out)).map { it.dn_collect_pre(ft) }.flatten()
        is Type.Proto.Coro -> (this.inps + listOf(this.res,this.yld,this.out)).map { it.dn_collect_pre(ft) }.flatten()
        is Type.Exec -> (this.inps + listOf(this.res,this.yld,this.out)).map { it.dn_collect_pre(ft) }.flatten()
    }
}

fun Stmt.dn_visit_pre (fs: ((Stmt)->Unit?)?, fe: ((Expr)->Unit?)?, ft: ((Type)->Unit?)?) {
    this.dn_collect_pre (
        if (fs == null) null else ({ if (fs(it) == null) null else emptyList<Unit>() }),
        if (fe == null) null else ({ if (fe(it) == null) null else emptyList() }),
        if (ft == null) null else ({ if (ft(it) == null) null else emptyList() })
    )
}
fun Expr.dn_visit_pre (fe: ((Expr)->Unit?)?) {
    this.dn_collect_pre (
        if (fe == null) null else ({ if (fe(it) == null) null else emptyList<Unit>() })
    )
}

fun Stmt.dn_filter_pre (fs: ((Stmt)->Boolean?)?, fe: ((Expr)->Boolean?)?, ft: ((Type)->Boolean?)?): List<Any> {
    return this.dn_collect_pre (
        if (fs == null) null else (
            {
                when (fs(it)) {
                    null -> null
                    false -> emptyList()
                    true -> listOf(it)
                }
            }
        ),
        if (fe == null) null else (
            {
                when (fe(it)) {
                    null -> null
                    false -> emptyList()
                    true -> listOf(it)
                }
            }
        ),
        if (ft == null) null else (
            {
                when (ft(it)) {
                    null -> null
                    false -> emptyList()
                    true -> listOf(it)
                }
            }
        ),
    )
}

fun Stmt.dn_first_pre (fs: ((Stmt)->Boolean?)?, fe: ((Expr)->Boolean?)?, ft: ((Type)->Boolean?)?): Any? {
    var ret: Any? = null
    this.dn_collect_pre (
        if (fs == null) null else ({
            val v: Boolean? = fs(it)
            when (v) {
                null  -> null
                false -> emptyList<Unit>()
                true  -> { ret=it ; null }
            }
        }),
        if (fe == null) null else ({
            val v: Boolean? = fe(it)
            when (v) {
                null  -> null
                false -> emptyList<Unit>()
                true  -> { ret=it ; null }
            }
        }),
        if (ft == null) null else ({
            val v: Boolean? = ft(it)
            when (v) {
                null  -> null
                false -> emptyList<Unit>()
                true  -> { ret=it ; null }
            }
        })
    )
    return ret
}
