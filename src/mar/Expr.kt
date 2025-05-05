package mar

fun Expr.is_lval (): Boolean {
    return when (this) {
        is Expr.Acc   -> true
        is Expr.Nat   -> true
        is Expr.Field -> true
        is Expr.Index -> true
        is Expr.Uno   -> (this.tk.str == "#")
        else          -> false
    }
}

fun Expr.static_int_is (): Boolean {
    return when (this) {
        is Expr.Tpl -> true
        is Expr.Num -> (this.tk.str.toIntOrNull() != null)
        is Expr.Uno -> this.e.static_int_is()
        is Expr.Bin -> this.e1.static_int_is() && this.e2.static_int_is()
        else        -> false
    }
}

fun Expr.static_int_eval (tpls: Tpl_Map?): Int {
    assert(this.static_int_is())
    return when (this) {
        is Expr.Tpl -> {
            println("static_int_eval: ${this.tk.str}")
            println(listOf(tpls))
            tpls!![this.tk.str]!!.second!!.static_int_eval(tpls)
        }
        is Expr.Num -> this.tk.str.toInt()
        is Expr.Uno -> when (this.tk.str) {
            "-"  -> - this.e.static_int_eval(tpls)
            else -> TODO("5")
        }
        is Expr.Bin -> when (this.tk.str) {
            "+" -> this.e1.static_int_eval(tpls) + this.e2.static_int_eval(tpls)
            "-" -> this.e1.static_int_eval(tpls) - this.e2.static_int_eval(tpls)
            "*" -> this.e1.static_int_eval(tpls) * this.e2.static_int_eval(tpls)
            else -> TODO("5")
        }
        else -> TODO("5")
    }
}
