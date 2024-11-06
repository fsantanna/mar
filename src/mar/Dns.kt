package mar

fun <V> Stmt.dn_collect_pos (fs: (Stmt)->List<V>, fe: (Expr)->List<V>, ft: (Type)->List<V>): List<V> {
    return when (this) {
        is Stmt.Data   -> this.tp.dn_collect_pos(ft)
        is Stmt.Proto  -> this.blk.dn_collect_pos(fs,fe,ft) + this.tp.dn_collect_pos(ft)
        is Stmt.Return -> this.e.dn_collect_pos(fe,ft)

        is Stmt.Block -> this.ss.map { it.dn_collect_pos(fs,fe,ft) }.flatten()
        is Stmt.Dcl -> this.xtp?.dn_collect_pos(ft) ?: emptyList()
        is Stmt.Set -> this.src.dn_collect_pos(fe,ft) + this.dst.dn_collect_pos(fe,ft)

        is Stmt.If    -> this.cnd.dn_collect_pos(fe,ft) + this.t.dn_collect_pos(fs,fe,ft) + this.f.dn_collect_pos(fs,fe,ft)
        is Stmt.Loop  -> this.blk.dn_collect_pos(fs,fe,ft)
        is Stmt.Break -> emptyList()

        is Stmt.Print -> this.e.dn_collect_pos(fe,ft)
        is Stmt.XExpr -> this.e.dn_collect_pos(fe,ft)
        is Stmt.Nat -> emptyList()
    } + fs(this)
}
fun <V> Expr.dn_collect_pos (fe: (Expr)->List<V>, ft: (Type)->List<V>): List<V> {
    return when (this) {
        is Expr.Uno   -> this.e.dn_collect_pos(fe,ft)
        is Expr.Bin   -> this.e1.dn_collect_pos(fe,ft) + this.e2.dn_collect_pos(fe,ft)
        is Expr.Tuple -> (this.xtp?.dn_collect_pos(ft) ?: emptyList()) + this.vs.map { it.dn_collect_pos(fe,ft) }.flatten()
        is Expr.Union -> (this.xtp?.dn_collect_pos(ft) ?: emptyList()) + this.v.dn_collect_pos(fe,ft)
        is Expr.Field -> this.col.dn_collect_pos(fe,ft)
        is Expr.Disc  -> this.col.dn_collect_pos(fe,ft)
        is Expr.Pred  -> this.col.dn_collect_pos(fe,ft)
        is Expr.Cons  -> this.dat.dn_collect_pos(ft) + this.es.map { it.dn_collect_pos(fe,ft) }.flatten()
        is Expr.Call  -> this.f.dn_collect_pos(fe,ft) + this.args.map { it.dn_collect_pos(fe,ft) }.flatten()
        is Expr.Create -> this.co.dn_collect_pos(fe,ft)
        is Expr.Start  -> this.args.map { it.dn_collect_pos(fe,ft) }.flatten() + this.exe.dn_collect_pos(fe,ft)
        is Expr.Resume -> this.arg.dn_collect_pos(fe,ft) + this.exe.dn_collect_pos(fe,ft)
        is Expr.Yield  -> this.arg.dn_collect_pos(fe,ft)
        is Expr.Nat    -> (this.xtp?.dn_collect_pos(ft) ?: emptyList())
        is Expr.Acc, is Expr.Null, is Expr.Unit,
        is Expr.Bool, is Expr.Char, is Expr.Num -> emptyList()
    } + fe(this)
}
fun <V> Type.dn_collect_pos (ft: (Type)->List<V>): List<V> {
    return when (this) {
        is Type.Any -> emptyList()
        is Type.Unit -> emptyList()
        is Type.Prim -> emptyList()
        is Type.Data -> emptyList()
        is Type.Pointer -> this.ptr.dn_collect_pos(ft)
        is Type.Tuple -> this.ts.map { it.dn_collect_pos(ft) }.flatten()
        is Type.Union -> (this._0?.dn_collect_pos(ft) ?: emptyList()) + this.ts.map { it.dn_collect_pos(ft) }.flatten()
        is Type.Proto.Func -> (this.inps + listOf(this.out)).map { it.dn_collect_pos(ft) }.flatten()
        is Type.Proto.Coro -> (this.inps + listOf(this.res, this.yld, this.out)).map { it.dn_collect_pos(ft) }.flatten()
        is Type.Exec -> (this.inps + listOf(this.res, this.yld, this.out)).map { it.dn_collect_pos(ft) }.flatten()
    } + ft(this)
}

fun Stmt.dn_visit_pos (fs: (Stmt)->Unit, fe: (Expr)->Unit, ft: (Type)->Unit) {
    this.dn_collect_pos (
        { fs(it) ; emptyList<Unit>() },
        { fe(it) ; emptyList() },
        { ft(it) ; emptyList() }
    )
}
fun Expr.dn_visit_pos (fe: (Expr)->Unit, ft: (Type)->Unit) {
    this.dn_collect_pos (
        { fe(it) ; emptyList<Unit>() },
        { ft(it) ; emptyList<Unit>() },
    )
}

fun <V> Stmt.dn_collect_pre (fs: (Stmt)->List<V>?, fe: (Expr)->List<V>?, ft: (Type)->List<V>?): List<V> {
    val v = fs(this)
    if (v === null) {
        return emptyList()
    }
    return v + when (this) {
        is Stmt.Data   -> this.tp.dn_collect_pre(ft)
        is Stmt.Proto  -> this.blk.dn_collect_pre(fs,fe,ft) + this.tp.dn_collect_pre(ft)
        is Stmt.Return -> this.e.dn_collect_pre(fe,ft)

        is Stmt.Block -> this.ss.map { it.dn_collect_pre(fs,fe,ft) }.flatten()
        is Stmt.Dcl -> this.xtp?.dn_collect_pre(ft) ?: emptyList()
        is Stmt.Set -> this.dst.dn_collect_pre(fe,ft) + this.src.dn_collect_pre(fe,ft)

        is Stmt.If    -> this.cnd.dn_collect_pre(fe,ft) + this.t.dn_collect_pre(fs,fe,ft) + this.f.dn_collect_pre(fs,fe,ft)
        is Stmt.Loop  -> this.blk.dn_collect_pre(fs,fe,ft)
        is Stmt.Break -> emptyList()

        is Stmt.Print -> this.e.dn_collect_pre(fe,ft)
        is Stmt.XExpr -> this.e.dn_collect_pre(fe,ft)
        is Stmt.Nat -> emptyList()
    }
}
fun <V> Expr.dn_collect_pre (fe: (Expr)->List<V>?, ft: (Type)->List<V>?): List<V> {
    val v = fe(this)
    if (v === null) {
        return emptyList()
    }
    return v + when (this) {
        is Expr.Uno   -> this.e.dn_collect_pre(fe,ft)
        is Expr.Bin   -> this.e1.dn_collect_pre(fe,ft) + this.e2.dn_collect_pre(fe,ft)
        is Expr.Tuple -> (this.xtp?.dn_collect_pre(ft) ?: emptyList()) + this.vs.map { it.dn_collect_pre(fe,ft) }.flatten()
        is Expr.Union -> (this.xtp?.dn_collect_pre(ft) ?: emptyList()) + this.v.dn_collect_pre(fe,ft)
        is Expr.Field -> this.col.dn_collect_pre(fe,ft)
        is Expr.Disc  -> this.col.dn_collect_pre(fe,ft)
        is Expr.Pred  -> this.col.dn_collect_pre(fe,ft)
        is Expr.Cons  -> this.dat.dn_collect_pre(ft) + this.es.map { it.dn_collect_pre(fe,ft) }.flatten()
        is Expr.Call  -> this.f.dn_collect_pre(fe,ft) + this.args.map { it.dn_collect_pre(fe,ft) }.flatten()
        is Expr.Create -> this.co.dn_collect_pre(fe,ft)
        is Expr.Start  -> this.exe.dn_collect_pre(fe,ft) + this.args.map { it.dn_collect_pre(fe,ft) }.flatten()
        is Expr.Resume -> this.exe.dn_collect_pre(fe,ft) + this.arg.dn_collect_pre(fe,ft)
        is Expr.Yield  -> this.arg.dn_collect_pre(fe,ft)
        is Expr.Nat    -> (this.xtp?.dn_collect_pre(ft) ?: emptyList())
        is Expr.Acc, is Expr.Null, is Expr.Unit,
        is Expr.Bool, is Expr.Char, is Expr.Num -> emptyList()
    }
}
fun <V> Type.dn_collect_pre (ft: (Type)->List<V>?): List<V> {
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
        is Type.Union -> (this._0?.dn_collect_pre(ft) ?: emptyList()) + this.ts.map { it.dn_collect_pre(ft) }.flatten()
        is Type.Proto.Func -> (this.inps + listOf(this.out)).map { it.dn_collect_pre(ft) }.flatten()
        is Type.Proto.Coro -> (this.inps + listOf(this.res,this.yld,this.out)).map { it.dn_collect_pre(ft) }.flatten()
        is Type.Exec -> (this.inps + listOf(this.res,this.yld,this.out)).map { it.dn_collect_pre(ft) }.flatten()
    }
}

fun Stmt.dn_visit_pre (fs: (Stmt)->Unit?, fe: (Expr)->Unit?, ft: (Type)->Unit?) {
    this.dn_collect_pre (
        { if (fs(it) == null) null else emptyList<Unit>() },
        { if (fe(it) == null) null else emptyList() },
        { if (ft(it) == null) null else emptyList() }
    )
}
fun Expr.dn_visit_pre (fe: (Expr)->Unit?, ft: (Type)->Unit?) {
    this.dn_collect_pre(
        { if (fe(it) == null) null else emptyList<Unit>() },
        { if (ft(it) == null) null else emptyList<Unit>() }
    )
}

fun Stmt.dn_filter_pre (fs: (Stmt)->Boolean?, fe: (Expr)->Boolean?, ft: (Type)->Boolean?): List<Any> {
    return this.dn_collect_pre (
        {
            when (fs(it)) {
                null -> null
                false -> emptyList()
                true -> listOf(it)
            }
        },
        {
            when (fe(it)) {
                null -> null
                false -> emptyList()
                true -> listOf(it)
            }
        },
        {
            when (ft(it)) {
                null -> null
                false -> emptyList()
                true -> listOf(it)
            }
        },
    )
}

fun Stmt.dn_first_pre (fs: (Stmt)->Boolean?, fe: (Expr)->Boolean?, ft: (Type)->Boolean?): Any? {
    var ret: Any? = null
    this.dn_collect_pre (
        {
            val v: Boolean? = fs(it)
            when (v) {
                null  -> null
                false -> emptyList<Unit>()
                true  -> { ret=it ; null }
            }
        },
        {
            val v: Boolean? = fe(it)
            when (v) {
                null  -> null
                false -> emptyList<Unit>()
                true  -> { ret=it ; null }
            }
        },
        {
            val v: Boolean? = ft(it)
            when (v) {
                null  -> null
                false -> emptyList<Unit>()
                true  -> { ret=it ; null }
            }
        }
    )
    return ret
}
