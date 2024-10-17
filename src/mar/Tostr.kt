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
        is Type.Prim -> this.tk.str
        is Type.Data -> this.tk.str
        is Type.Unit -> "()"
        is Type.Pointer -> "\\" + this.ptr.to_str(pre)
        is Type.Tuple -> {
            if (this.ids == null) {
                "[" + this.ts.map { it.to_str(pre) }.joinToString(",") + "]"
            } else {
                "[" + this.ids.zip(this.ts).map { (id,tp) -> id.str + ":" + tp.to_str(pre) }.joinToString(",") + "]"
            }
        }
        is Type.Union -> {
            if (this.ids == null) {
                "<" + this.ts.map { it.to_str(pre) }.joinToString(",") + ">"
            } else {
                "<" + this.ids.zip(this.ts).map { (id,tp) -> id.str + ":" + tp.to_str(pre) }.joinToString(",") + ">"
            }
        }
        is Type.Proto -> {
            val inps = when (this) {
                is Type.Proto.Func.Vars -> this.inps_.map { it.to_str(pre) }.joinToString(",")
                is Type.Proto.Coro.Vars -> this.inps_.map { it.to_str(pre) }.joinToString(",")
                is Type.Proto.Func -> this.inps.map { it.to_str(pre) }.joinToString(",")
                is Type.Proto.Coro -> this.inps.map { it.to_str(pre) }.joinToString(",")
            }
            when (this) {
                is Type.Proto.Func -> "func ($inps) -> ${this.out.to_str(pre)}"
                is Type.Proto.Coro -> "coro ($inps) -> ${this.res.to_str(pre)} -> ${this.yld.to_str(pre)} -> ${this.out.to_str(pre)}"
            }
        }
        is Type.Exec -> "exec (${this.inps.map { it.to_str(pre) }.joinToString(",")}) -> ${this.res.to_str(pre)} -> ${this.yld.to_str(pre)} -> ${this.out.to_str(pre)}"
    }.let {
        when {
            !pre -> it
            else -> this.tk.pos.pre() + it
        }
    }
}

fun Tk.Op.to_str (pre: Boolean): String {
    return when (this.str) {
        "ref" -> "\\"
        "deref" -> "\\"
        else -> this.str
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

        is Expr.Tuple  -> {
            "(" + if (this.ids == null) {
                "[" + this.vs.map { it.to_str(pre) }.joinToString(",") + "]"
            } else {
                "[" + this.ids.zip(this.vs).map { (id,v) -> "."+id.str+"="+v.to_str(pre) }.joinToString(",") + "]"

            } + ":" + this.tp.to_str(pre) + ")"
        }
        is Expr.Field  -> "(" + this.col.to_str(pre) + "." + this.idx + ")"
        is Expr.Union  -> "<." + this.idx + "=" + this.v.to_str(pre) + ">:" + this.tp.to_str(pre)
        is Expr.Disc  -> "(${this.col.to_str(pre)}!${this.idx})"
        is Expr.Pred  -> "(${this.col.to_str(pre)}?${this.idx})"
        is Expr.Cons  -> "(${this.tk.str} ${this.e.to_str(pre)})"

        is Expr.Uno    -> "(" + this.tk_.to_str(pre) + this.e.to_str(pre) + ")"
        is Expr.Bin    -> "(" + this.e1.to_str(pre) + " " + this.tk_.to_str(pre) + " " + this.e2.to_str(pre) + ")"
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

fun List<Stmt>.to_str (pre: Boolean = false): String {
    return this.map { it.to_str(pre) + "\n" }.joinToString("")
}

fun Stmt.to_str (pre: Boolean = false): String {
    return when (this) {
        is Stmt.Data   -> "data " + this.id.str + ": " + this.tp.to_str(pre)
        is Stmt.Proto.Func -> "func " + this.id.str + " " + this.tp.to_str(pre).drop(5) + " {\n" + this.blk.ss.to_str(pre) + "}"
        is Stmt.Proto.Coro -> "coro " + this.id.str + " " + this.tp.to_str(pre).drop(5) + " {\n" + this.blk.ss.to_str(pre) + "}"
        is Stmt.Return -> "return(" + this.e.to_str(pre) + ")"
        is Stmt.Block  -> "do {\n" + (this.ss.map { it.to_str(pre) + "\n" }.joinToString("")) + "}"
        is Stmt.Dcl    -> "var " + this.var_type.to_str(pre)
        is Stmt.Set    -> "set " + this.dst.to_str(pre) + " = " + this.src.to_str(pre)
        is Stmt.If     -> "if " + this.cnd.to_str(pre) + " {\n" + this.t.ss.to_str(pre) + "} else {\n" + this.f.ss.to_str(pre) + "}"
        is Stmt.Loop   -> "loop {\n" + this.blk.ss.to_str(pre) + "}"
        is Stmt.Break  -> "break"
        is Stmt.Create -> this.dst.cond { "set ${it.to_str(pre)} = " } + "create(" + this.co.to_str(pre) + ")"
        is Stmt.Start  -> this.dst.cond { "set ${it.to_str(pre)} = " } + "start " + this.exe.to_str(pre) + "(" + this.args.map { it.to_str(pre) }.joinToString(",") + ")"
        is Stmt.Resume -> this.dst.cond { "set ${it.to_str(pre)} = " } + "resume " + this.exe.to_str(pre) + "(" + this.arg.to_str(pre) + ")"
        is Stmt.Yield  -> this.dst.cond { "set ${it.to_str(pre)} = " } + "yield(" + this.arg.to_str(pre) + ")"
        is Stmt.Call   -> this.call.to_str(pre)
        is Stmt.Nat    -> this.tk.str.let { if (it.contains("\n")) "```"+it+"```" else "`"+it+"`" }
    }.let {
        when {
            !pre -> it
            else -> this.tk.pos.pre() + it
        }
    }
}
