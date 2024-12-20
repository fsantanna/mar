package mar

fun Any.ups_until (cnd: (Any)->Boolean): List<Any> {
    return listOf(this) + when {
        cnd(this) -> emptyList()
        (this is Stmt) -> this.xup?.ups_until(cnd) ?: emptyList()
        (this is Expr) -> this.xup?.ups_until(cnd) ?: emptyList()
        else -> error("impossible case")
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
        (this is Stmt) -> this.xup?.up_first(cnd)
        (this is Expr) -> this.xup?.up_first(cnd)
        (this is Type) -> this.xup?.up_first(cnd)
        else -> error("impossible case")
    }
}

fun Any.up_any (cnd: (Any)->Boolean): Boolean {
    return this.up_first(cnd) !== null
}

fun Any.up_none (cnd: (Any)->Boolean): Boolean {
    return this.up_first(cnd) === null
}
