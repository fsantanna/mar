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
fun Stmt.dump (): String {
    return if (!DUMP) "" else {
        this.tk.dump() + " | " + this.to_str().quote(15)
    }
}

fun Tk.fpre (pre: Boolean): String {
    return if (pre) this.pos.pre() else ""
}

@JvmName("List_Tk_Type_to_str")
fun List<Tk.Type>.to_str (pre: Boolean=false): String {
    return this.map { it.str }.joinToString(".")
}

fun Type.to_str (pre: Boolean = false): String {
    return when (this) {
        //is Type.Err,
        is Type.Any -> "*"
        is Type.Bot -> "-"
        is Type.Top -> "+"
        is Type.Tpl -> "{{${this.tk.str}}}"
        is Type.Nat -> "`${this.tk.str}`"
        is Type.Prim -> this.tk.str
        is Type.Data -> {
            val tpls = this.xtpls.let { xtpls ->
                if (xtpls==null || xtpls.isEmpty()) "" else {
                    val xs = xtpls.map { (t,e) -> if (t==null) e!!.to_str(pre) else ":"+t.to_str(pre) }
                    " {{${xs.joinToString(",")}}}"
                }
            }
            this.ts.first().str + tpls + this.ts.drop(1).map { "."+it.str }.joinToString("")
        }
        is Type.Unit -> "()"
        is Type.Pointer -> "\\" + this.ptr.to_str(pre)
        is Type.Tuple -> "[" + this.ts.map { (id,tp) -> id.cond { it.str+":" } + tp.to_str(pre) }.joinToString(",") + "]"
        is Type.Vector -> "#[" + this.tp.to_str(pre) + this.max.cond { e -> "*" + e.to_str(pre) } + "]"
        is Type.Union -> "<" + this.ts.map { (id,tp) -> id.cond { it.str+":" } + tp.to_str(pre) }.joinToString(",") + ">"
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
        is Expr.Tpl    -> "{{${this.tk.str}}}"
        is Expr.Nat    -> {
            val nat = this.tk.str.let { if (it.contains("\n")) "```"+it+"```" else "`"+it+"`" }
            if (this.xtp==null || this.xtp is Type.Any) {
                nat
            } else {
                "(" + nat + ": " + this.xtp!!.to_str(pre) + ")"
            }
        }
        is Expr.Acc    -> this.ign.cond { "__" } + this.tk.str
        is Expr.Bool   -> this.tk.str
        is Expr.Str    -> this.tk.str
        is Expr.Chr    -> this.tk.str
        is Expr.Num    -> this.tk.str
        is Expr.Null   -> this.tk.str
        is Expr.Unit   -> "()"

        is Expr.Tuple  -> {
            "([" + this.vs.map { (id,v) -> id.cond { "."+it.str+"=" } + v.to_str(pre) }.joinToString(",") + "]" + this.xtp.cond { ":${it.to_str()}" } + ")"
        }
        is Expr.Vector -> {
            "(#[" + this.vs.map { it.to_str(pre) }.joinToString(",") + "]" + this.xtp.cond { ":${it.to_str()}" } + ")"
        }
        is Expr.Field  -> "(" + this.col.to_str(pre) + "." + this.idx + ")"
        is Expr.Index  -> "(" + this.col.to_str(pre) + "[" + this.idx.to_str(pre) + "])"
        is Expr.Union  -> "<." + this.idx + "=" + this.v.to_str(pre) + ">" + this.xtp.cond { ":${it.to_str()}" }
        is Expr.Disc   -> "(${this.col.to_str(pre)}!${this.idx})"
        is Expr.Pred   -> "(${this.col.to_str(pre)}?${this.idx})"
        is Expr.Cons   -> "(${this.tp.to_str(pre)}(${this.e.to_str(pre)}))"

        is Expr.Uno    -> "(" + this.tk_.to_str(pre) + this.e.to_str(pre) + ")"
        is Expr.Bin    -> "(" + this.e1.to_str(pre) + " " + this.tk_.to_str(pre) + " " + this.e2.to_str(pre) + ")"
        is Expr.Call   -> {
            val tpls = this.xtpls.let { xtpls ->
                if (xtpls==null || xtpls.isEmpty()) "" else {
                    val xs = xtpls.map { (t,e) -> if (t==null) e!!.to_str(pre) else ":"+t.to_str(pre) }
                    " {{${xs.joinToString(",")}}} "
                }
            }
            "(" + this.f.to_str(pre) + tpls + "(" + this.args.map { it.to_str(pre) }.joinToString(",") + "))"
        }
        is Expr.Throw  -> "throw(" + this.e.to_str(pre) + ")"

        is Expr.If     -> "if ${this.cnd.to_str(pre)} => ${this.t.to_str(pre)} => ${this.f.to_str(pre)}"
        is Expr.MatchT -> {
            val tst = this.tst.to_str(pre)
            val cases = this.cases.map {
                val cnd = if (it.first == null) "else" else ":"+it.first!!.to_str(pre)
                val e = it.second.to_str(pre)
                "$cnd => $e\n"
            }.joinToString("")
            "match $tst {\n$cases}"
        }
        is Expr.MatchE -> {
            val tst = this.tst.to_str(pre)
            val cases = this.cases.map {
                val cnd = if (it.first == null) "else" else it.first!!.to_str(pre)
                val e = it.second.to_str(pre)
                "$cnd => $e\n"
            }.joinToString("")
            "match $tst {\n$cases}"
        }
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
        is Stmt.Data   -> {
            val tpls = (this.tpls.size>0).cond { " {{" + this.tpls.map { it.to_str(pre) }.joinToString(",") + "}}" }
            if (this.subs == null) {
                "data " + this.t.str + tpls + ": " + this.tp.to_str(pre)
            } else {
                fun f (l: List<Stmt.Data>): String {
                    return l.map { it.t.str + ": " + it.tp.to_str(pre) + " {\n" + f(it.subs!!) + "}\n" }.joinToString("")
                }
                "data " + this.t.str + tpls + ".*: " + this.tp.to_str(pre) + " {\n" + f(this.subs) + "}"
            }
        }
        is Stmt.Proto.Func -> {
            val tpls = (this.tpls.size>0).cond { " {{" + this.tpls.map { it.to_str(pre) }.joinToString(",") + "}}" }
            "func " + this.id.str + tpls + ": " + this.tp.to_str(pre).drop(5) + " {\n" + this.blk.ss.to_str(pre) + "}"
        }
        is Stmt.Proto.Coro -> {
            val tpls = (this.tpls.size>0).cond { " {{" + this.tpls.map { it.to_str(pre) }.joinToString(",") + "}}" }
            "coro " + this.id.str + tpls + ": " + this.tp.to_str(pre).drop(5) + " {\n" + this.blk.ss.to_str(pre) + "}"
        }
        is Stmt.Block  -> "do " + this.esc.cond { ":"+it.to_str(pre)+" " } + "{\n" + (this.ss.map { it.to_str(pre) + "\n" }.joinToString("")) + "}"
        is Stmt.Dcl    -> "var ${this.id.str}" + this.xtp.cond { ": ${it.to_str()}" }
        is Stmt.SetE   -> "set " + this.dst.to_str(pre) + " = " + this.src.to_str(pre)
        is Stmt.SetS   -> "set " + this.dst.to_str(pre) + " = " + this.src.to_str(pre)
        is Stmt.Escape -> "escape(" + this.e.to_str(pre) + ")"
        is Stmt.Defer  -> "defer {\n" + this.blk.ss.to_str(pre) + "}"
        is Stmt.Catch  -> "catch " + this.tp.cond { ":"+it.to_str(pre)+" " } + "{\n" + this.blk.ss.to_str(pre) + "}"
        is Stmt.If     -> "if " + this.cnd.to_str(pre) + " {\n" + this.t.ss.to_str(pre) + "} else {\n" + this.f.ss.to_str(pre) + "}"
        is Stmt.Loop   -> "loop {\n" + this.blk.ss.to_str(pre) + "}"
        is Stmt.MatchT -> {
            val tst = this.tst.to_str(pre)
            val cases = this.cases.map {
                val cnd = if (it.first == null) "else" else ":"+it.first!!.to_str(pre)
                val ss = it.second.ss.map { it.to_str(pre) }.joinToString("\n")
                "$cnd { $ss }\n"
            }.joinToString("")
            "match $tst {\n$cases}"
        }
        is Stmt.MatchE -> {
            val tst = this.tst.to_str(pre)
            val cases = this.cases.map {
                val cnd = if (it.first == null) "else" else it.first!!.to_str(pre)
                val ss = it.second.ss.map { it.to_str(pre) }.joinToString("\n")
                "$cnd { $ss }\n"
            }.joinToString("")
            "match $tst {\n$cases}"
        }

        is Stmt.Create -> "create(" + this.co.to_str(pre) + ")"
        is Stmt.Start  -> "start " + this.exe.to_str(pre) + "(" + this.args.map { it.to_str(pre) }.joinToString(",") + ")"
        is Stmt.Resume -> "resume " + this.exe.to_str(pre) + "(" + this.arg.let { if (it is Expr.Unit) "" else it.to_str(pre) } + ")"
        is Stmt.Yield  -> "yield(" + this.arg.let { if (it is Expr.Unit) "" else it.to_str(pre) } + ")"


        is Stmt.Print  -> "print(" + this.e.to_str(pre) + ")"
        is Stmt.Pass   -> "do(" + this.e.to_str(pre) + ")"
    }.let {
        when {
            !pre -> it
            else -> this.tk.pos.pre() + it
        }
    }
}
