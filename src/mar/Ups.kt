package mar

fun Any.fup (): Any? {
    return when (this) {
        is Stmt -> this.xup
        is Expr -> this.xup
        is Type -> this.xup
        else -> error("impossible cast")
    }
}

fun Any.ups_until (cnd: (Any)->Boolean): List<Any> {
    return listOf(this) + when {
        cnd(this) -> emptyList()
        else -> this.fup()?.ups_until(cnd) ?: emptyList()
    }
}

fun Any.ups (): List<Any> {
    return this.ups_until { false }
}

fun Any.ups_depth (): Int {
    return this.ups().count()
}

fun Any.up_first (cnd: (Any)->Any?): Any? {
    val v = cnd(this)
    return when {
        (v == true) -> this
        (v!=false && v!=null) -> v
        else -> this.fup()?.up_first(cnd)
    }
}

fun Any.up_last (cnd: (Any)->Any?): Any? {
    var up = this.up_first(cnd)
    var tmp = up
    while (tmp != null) {
        up = tmp
        tmp = up.fup()?.up_first(cnd)
    }
    return up
}

fun Any.up_any (cnd: (Any)->Boolean): Boolean {
    return this.up_first(cnd) !== null
}

fun Any.up_none (cnd: (Any)->Boolean): Boolean {
    return this.up_first(cnd) === null
}

fun Any.up_data (id: String): Stmt.Data? {
    return this.up_first { blk ->
        if (blk !is Stmt.Block) null else {
            blk.dn_filter_pre(
                {
                    when (it) {
                        is Stmt.Data -> true
                        is Stmt.Block -> if (it == blk) false else null
                        else -> false
                    }
                },
                {null},
                {null}
            ).let {
                it as List<Stmt.Data>
            }.find {
                it.t.str == id
            }
        }
    } as Stmt.Data?
}

fun Any.up_exe (): Boolean {
    return this.up_first { it is Stmt.Proto }.let { it is Stmt.Proto.Coro || it is Stmt.Proto.Task }
}
