package mar

fun Any.ups_until (cnd: (Any)->Boolean): List<Any> {
    return listOf(this) + when {
        cnd(this) -> emptyList()
        (this is Stmt) -> this.fup()?.ups_until(cnd) ?: emptyList()
        (this is Expr) -> this.fup()?.ups_until(cnd) ?: emptyList()
        else -> error("impossible case")
    }
}

fun Any.ups (): List<Any> {
    return this.ups_until { false }
}

fun Any.up_first (cnd: (Any)->Boolean): Any? {
    return when {
        cnd(this) -> this
        (this is Stmt) -> this.fup()?.up_first(cnd)
        (this is Expr) -> this.fup()?.up_first(cnd)
        else -> error("impossible case")
    }
}

fun Any.up_none (cnd: (Any)->Boolean): Boolean {
    return this.up_first(cnd) === null
}
