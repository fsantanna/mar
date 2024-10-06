package mar

fun Type.is_sup_of (other: Type): Boolean {
    return when {
        //(this is Type.Top) -> true
        (this is Type.Any) -> true
        (this is Type.Unit  && other is Type.Unit)  -> true
        (this is Type.Basic && other is Type.Basic) -> (this.tk.str == other.tk.str)
        (this is Type.Func  && other is Type.Func)  -> TODO()
        else -> false
    }
}

fun Expr.Bin.args (tp1: Type, tp2: Type): Boolean {
    return when (this.tk_.str) {
        "==", "!=" -> tp1.is_sup_of(tp2) && tp2.is_sup_of(tp1)
        ">", "<", ">=", "<=",
        "+", "-", "*", "/", "%" -> {
            tp1.is_sup_of(Type.Basic(Tk.Type( "Int", this.tk.pos.copy()))) &&
            tp2.is_sup_of(Type.Basic(Tk.Type( "Int", this.tk.pos.copy())))
        }
        "||", "&&" -> {
            tp1.is_sup_of(Type.Basic(Tk.Type( "Bool", this.tk.pos.copy()))) &&
            tp2.is_sup_of(Type.Basic(Tk.Type( "Bool", this.tk.pos.copy())))
        }
        else -> error("impossible case")
    }
}

fun Expr.type (): Type {
    return when (this) {
        is Expr.Acc -> {
            this.up_first {
                if (it !is Stmt.Block) false else {
                    it.vs.find { it.first.str == this.tk.str }?.second
                }
            } as Type
        }
        is Expr.Bin -> when (this.tk_.str) {
            "==", "!=",
            ">", "<", ">=", "<=",
            "||", "&&" -> Type.Basic(Tk.Type( "Bool", this.tk.pos.copy()))
            "+", "-", "*", "/", "%" -> Type.Basic(Tk.Type( "Int", this.tk.pos.copy()))
            else -> error("impossible case")
        }
        is Expr.Bool -> Type.Basic(Tk.Type( "Bool", this.tk.pos.copy()))
        is Expr.Call -> this.f.type().let {
            if (it is Type.Any) it else (it as Type.Func).out
        }
        is Expr.Char -> Type.Basic(Tk.Type( "Char", this.tk.pos.copy()))
        is Expr.Nat -> Type.Any(this.tk)
        is Expr.Null -> TODO()
        is Expr.Num -> Type.Basic(Tk.Type( "Int", this.tk.pos.copy()))
    }
}
