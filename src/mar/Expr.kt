package mar

fun Expr.is_lval (): Boolean {
    return when (this) {
        is Expr.Acc -> true
        is Expr.Nat -> true
        //is Expr.Index -> true
        else -> false
    }
}
