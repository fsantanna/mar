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

fun Type.to_str (pre: Boolean = false): String {
    return when (this) {
        is Type.Any -> TODO()
        is Type.Basic -> this.tk.str
        is Type.Unit -> "()"
        is Type.Pointer -> "\\" + this.ptr.to_str(pre)
        is Type.Proto -> {
            val inps = when (this) {
                is Type.Proto.Func.Vars -> this.inps__.map { it.to_str(pre) }.joinToString(",")
                is Type.Proto.Coro.Vars -> this.inps__.map { it.to_str(pre) }.joinToString(",")
                else -> this.inps.map { it.to_str(pre) }.joinToString(",")
            }
            when (this) {
                is Type.Proto.Func -> "func (" + inps + ") -> " + this.out.to_str(pre)
                is Type.Proto.Coro -> "coro (" + inps + ") -> " + this.out.to_str(pre)
            }
        }
        is Type.XCoro -> "xcoro (" + this.inps.map { it.to_str(pre) }.joinToString(",") + ") -> " + this.out.to_str(pre)
    }.let {
        when {
            !pre -> it
            else -> this.tk.pos.pre() + it
        }
    }
}

fun Expr.to_str (pre: Boolean = false): String {
    return when (this) {
        is Expr.Nat    -> "```" + this.tk.str + "```"
        is Expr.Acc    -> this.ign.cond { "__" } + this.tk.str
        is Expr.Bool   -> this.tk.str
        is Expr.Char   -> this.tk.str
        is Expr.Num    -> this.tk.str
        is Expr.Null   -> this.tk.str
        is Expr.Unit   -> ""

        is Expr.Uno    -> "(" + this.tk.str + this.e.to_str(pre) + ")"
        is Expr.Bin    -> "(" + this.e1.to_str(pre) + " " + this.tk.str + " " + this.e2.to_str(pre) + ")"
        is Expr.Call   -> this.f.to_str(pre) + "(" + this.args.map { it.to_str(pre) }.joinToString(",") + ")"
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
    return id.fpre(pre) + id.str + ": " + tp.to_str(pre)
}

fun Stmt.to_str (pre: Boolean = false): String {
    return when (this) {
        is Stmt.Proto.Func -> "func " + this.id.str + " " + this.tp.to_str(pre).drop(5) + " {\n" + this.blk.ss.map { it.to_str(pre) }.joinToString("\n") + "}"
        is Stmt.Proto.Coro -> TODO()
        is Stmt.Return -> "return(" + this.e.to_str(pre) + ")"
        is Stmt.Block  -> "do [" + (this.vs.map { (id,tp) -> id.str + ": " + tp.to_str(pre) }.joinToString(",")) + "] {\n" + (this.ss.map { it.to_str(pre) + "\n" }.joinToString("")) + "}"
        is Stmt.Set    -> "set " + this.dst.to_str(pre) + " = " + this.src.to_str(pre)
        is Stmt.Create -> this.dst.cond { "set ${it.to_str(pre)} = " } + "create(" + this.co.to_str(pre) + ")"
        is Stmt.Resume -> this.dst.cond { "set ${it.to_str(pre)} = " } + "resume " + this.xco.to_str(pre) + "(" + this.arg.to_str(pre) + ")"
        is Stmt.Yield  -> this.dst.cond { "set ${it.to_str(pre)} = " } + "yield(" + this.arg.to_str(pre) + ")"
        is Stmt.Call   -> this.call.to_str(pre)
        is Stmt.Nat    -> TODO()
    }.let {
        when {
            !pre -> it
            else -> this.tk.pos.pre() + it
        }
    }
}
