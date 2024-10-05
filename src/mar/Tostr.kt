package mar

fun Pos.pre (): String {
    assert(this.lin>=0 && this.col>=0)
    return "^[${this.lin},${this.col}]"
}

fun Tk.dump (): String {
    return if (!DUMP) "" else {
        "(${this.pos.file} : lin ${this.pos.lin} : col ${this.pos.col})"
    }
}
fun Expr.dump (): String {
    return if (!DUMP) "" else {
        this.tk.dump() + " | " + this.to_str().quote(15)
    }
}

fun Tk.fpre (pre: Boolean): String {
    return if (pre) this.pos.pre() else ""
}

fun Expr.to_str (pre: Boolean = false): String {
    return when (this) {
        is Expr.Nat    -> "```" + this.tk_.tag.cond { it+" " } + this.tk.str + "```"
        is Expr.Acc    -> this.ign.cond { "__" } + this.tk.str
        is Expr.Bool   -> this.tk.str
        is Expr.Char   -> this.tk.str
        is Expr.Num    -> this.tk.str
        is Expr.Bin     -> "(" + this.e1.to_str(pre) + " " + this.tk.str + " " + this.e2.to_str(pre) + ")"
    }.let {
        when {
            !pre -> it
            (it.length>0 && it[0]=='(') -> '(' + this.tk.pos.pre() + it.drop(1)
            else -> this.tk.pos.pre() + it
        }
    }
}

fun Var_Type.to_str (pre: Boolean = false): String {
    val (id,tp) = this
    return id.fpre(pre) + id.str + ": " + tp.fpre(pre) + tp.str
}

fun Stmt.to_str (pre: Boolean = false): String {
    return when (this) {
        is Stmt.Dcl -> "var " + this.var_type.to_str(pre)
        is Stmt.Set -> "set " + this.dst.to_str(pre) + " = " + this.src.to_str(pre)
        else -> TODO()
    }
}

fun List<Stmt>.to_str (pre: Boolean=false): String {
    return this.map { it.to_str(pre) }.joinToString(";\n") + ";\n"
}
