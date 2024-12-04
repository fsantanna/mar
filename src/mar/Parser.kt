package mar

fun check_fix (str: String): Boolean {
    return (G.tk1.let { it is Tk.Fix && it.str == str })
}
fun check_fix_err (str: String): Boolean {
    val ret = check_fix(str)
    if (!ret) {
        err_expected(G.tk1!!, '"'+str+'"')
    }
    return ret
}
fun accept_fix (str: String): Boolean {
    val ret = check_fix(str)
    if (ret) {
        parser_lexer()
    }
    return ret
}
fun accept_fix_err (str: String): Boolean {
    check_fix_err(str)
    accept_fix(str)
    return true
}

fun check_enu (enu: String): Boolean {
    return when (enu) {
        "Eof"  -> G.tk1 is Tk.Eof
        "Fix"  -> G.tk1 is Tk.Fix
        "Type" -> G.tk1 is Tk.Type
        "Op"   -> G.tk1 is Tk.Op
        "Var"  -> G.tk1 is Tk.Var
        "Chr"  -> G.tk1 is Tk.Chr
        "Num"  -> G.tk1 is Tk.Num
        "Nat"  -> G.tk1 is Tk.Nat
        else   -> error("bug found")
    }
}
fun check_enu_err (str: String): Boolean {
    val ret = check_enu(str)
    val err = when (str) {
        "Eof"  -> "end of file"
        "Var"  -> "variable"
        "Type" -> "type"
        "Num"  -> "number"
        "Nat"  -> "native"
        "Op"   -> "operator"
        else   -> TODO(str)
    }

    if (!ret) {
        err_expected(G.tk1!!, err)
    }
    return ret
}
fun accept_enu (enu: String): Boolean {
    val ret = check_enu(enu)
    if (ret && G.tk1 !is Tk.Eof) {
        parser_lexer()
    }
    return ret
}
fun accept_enu_err (str: String): Boolean {
    check_enu_err(str)
    accept_enu(str)
    return true
}

fun check_op (str: String): Boolean {
    return (G.tk1.let { it is Tk.Op && it.str == str })
}
fun check_op_err (str: String): Boolean {
    val ret = check_op(str)
    if (!ret) {
        err_expected(G.tk1!!, '"'+str+'"')
    }
    return ret
}
fun accept_op (str: String): Boolean {
    val ret = check_op(str)
    if (ret) {
        parser_lexer()
    }
    return ret
}
fun accept_op_err (str: String): Boolean {
    check_op_err(str)
    accept_op(str)
    return true
}

fun parser_lexer () {
    G.tk0 = G.tk1
    G.tk1 = G.tks!!.next()
}

fun <T> parser_list (sep: String?, close: String, func: () -> T): List<T> {
    return parser_list(sep, { accept_fix(close)||accept_op(close) }, func)

}
fun <T> parser_list (sep: String?, close: ()->Boolean, func: () -> T): List<T> {
    val l = mutableListOf<T>()
    if (!close()) {
        l.add(func())
        while (true) {
            if (close()) {
                break
            }
            if (sep != null) {
                accept_fix_err(sep)
                if (close()) {
                    break
                }
            }
            l.add(func())
        }
    }
    return l
}

fun parser_var_type (pre: Tk.Var?): Var_Type {
    val id = if (pre != null) pre else {
        accept_enu_err("Var")
        val x = G.tk0 as Tk.Var
        accept_fix_err(":")
        x
    }
    if (check_fix("func") || check_fix("coro")) {
        err(G.tk1!!, "type error : unexpected \"${G.tk1!!.str}\"")
    }
    val tp = parser_type(null, false)
    return Pair(id, tp)
}

fun parser_type (pre: Tk?, fr_proto: Boolean): Type {
    return when {
        (pre is Tk.Fix || accept_fix("func") || accept_fix("coro")) -> {
            val tk0 = pre ?: (G.tk0 as Tk.Fix)
            accept_fix_err("(")
            val req = fr_proto || check_enu("Var")
            val inps = parser_list(",", ")") {
                if (req) {
                    parser_var_type(null)
                } else {
                    parser_type(null, false)
                }
            }
            accept_fix_err("->")
            val (res,yld) = if (tk0.str != "coro") Pair(null,null) else {
                val res = parser_type(null, false)
                accept_fix_err("->")
                val yld = parser_type(null, false)
                accept_fix_err("->")
                Pair(res, yld)
            }
            val out = parser_type(null, false)
            when {
                (tk0.str=="func" &&  req) ->
                    Type.Proto.Func.Vars(tk0, inps as List<Var_Type>, out)
                (tk0.str=="func" && !req) ->
                    Type.Proto.Func(tk0, inps as List<Type>, out)
                (tk0.str=="coro" &&  req) ->
                    Type.Proto.Coro.Vars(tk0, inps as List<Var_Type>, res!!, yld!!, out)
                (tk0.str=="coro" && !req) ->
                    Type.Proto.Coro(tk0, inps as List<Type>, res!!, yld!!, out)
                else -> error("impossible case")
            }
        }
        accept_fix("exec") -> {
            val tk0 = pre ?: (G.tk0 as Tk.Fix)
            accept_fix_err("(")
            val inps = parser_list(",", ")") {
                parser_type(null, false)
            }
            accept_fix_err("->")
            val res = parser_type(null, false)
            accept_fix_err("->")
            val yld = parser_type(null, false)
            accept_fix_err("->")
            val out = parser_type(null, false)
            Type.Exec(tk0, inps, res, yld, out)
        }
        accept_fix("(") -> {
            val tp = if (check_fix(")")) {
                Type.Unit(G.tk0 as Tk.Fix)
            } else {
                parser_type(null, false)
            }
            accept_fix_err(")")
            tp
        }
        (pre is Tk.Type) || accept_enu("Type") -> {
            val tp = if (pre is Tk.Type) pre else G.tk0 as Tk.Type
            if (PRIMS.contains(tp.str)) {
                Type.Prim(tp)
            } else {
                val l = mutableListOf(tp)
                while (accept_fix(".")) {
                    accept_enu_err("Type")
                    l.add(G.tk0 as Tk.Type)
                }
                Type.Data(tp, l)
            }
        }
        accept_op("\\") -> {
            val tk0 = G.tk0 as Tk.Op
            val ptr = parser_type(null, false)
            Type.Pointer(tk0, ptr)
        }
        accept_fix("[") -> {
            val tk0 = G.tk0 as Tk.Fix
            val ts = parser_list(",", "]") {
                val id = if (!accept_enu("Var")) null else {
                    val xid = G.tk0 as Tk.Var
                    accept_fix_err(":")
                    xid
                }
                Pair(id, parser_type(null, false))
            }
            Type.Tuple(tk0, ts)
        }
        accept_op("<") -> {
            val tk0 = G.tk0 as Tk.Op
            val ts = parser_list(",", ">") {
                val tk1 = G.tk1
                val has_id = accept_enu("Type") && accept_fix(":")
                when {
                    has_id -> Pair(tk1 as Tk.Type, parser_type(null, false))
                    (tk1 is Tk.Type) -> Pair(null, parser_type(tk1, false))
                    else -> Pair(null, parser_type(null, false))
                }
            }
            Type.Union(tk0, true, ts)
        }
        else -> err_expected(G.tk1!!, "type")
    }
}

fun parser_expr_4_prim (): Expr {
    return when {
        accept_enu("Nat")  -> {
            val tk0 = G.tk0 as Tk.Nat
            val tp = if (!accept_fix(":")) null else {
                parser_type(null, false)
            }
            Expr.Nat(tk0, tp)
        }
        accept_enu("Var")  -> Expr.Acc(G.tk0 as Tk.Var)
        accept_fix("false") -> Expr.Bool(G.tk0 as Tk.Fix)
        accept_fix("true")  -> Expr.Bool(G.tk0 as Tk.Fix)
        accept_enu("Chr")  -> Expr.Char(G.tk0 as Tk.Chr)
        accept_enu("Num")  -> Expr.Num(G.tk0 as Tk.Num)
        accept_fix("null")  -> Expr.Null(G.tk0 as Tk.Fix)
        accept_fix("(")     -> {
            val tk0 = G.tk0 as Tk.Fix
            if (accept_fix(")")) {
                Expr.Unit(tk0)
            } else {
                parser_expr().let { accept_fix_err(")") ; it }
            }
        }

        accept_fix("[")     -> {
            val tk0 = G.tk0 as Tk.Fix
            val l = parser_list(",", "]") {
                val x = if (!accept_fix(".")) null else {
                    (accept_enu("Var") || accept_enu_err("Num"))
                    val idx = G.tk0!! as Tk.Var
                    accept_fix_err("=")
                    idx
                }
                val e = parser_expr()
                Pair(x, e)
            }
            val tp = if (!accept_fix(":")) null else {
                check_fix_err("[")
                parser_type(null, false) as Type.Tuple
            }
            Expr.Tuple(tk0, tp, l)
        }
        accept_op("<")      -> {
            val tk0 = G.tk0 as Tk.Op
            accept_fix_err(".")
            (accept_enu("Type") || accept_enu_err("Num"))
            val idx = G.tk0!!.str
            accept_fix_err("=")
            val v = parser_expr_2_pre() // avoid bin `>` (x>10)
            accept_op_err(">")
            val tp = if (!accept_fix(":")) null else {
                check_op_err("<")
                parser_type(null, false) as Type.Union
            }
            Expr.Union(tk0, tp, idx, v)
        }

        accept_enu("Type") -> {
            val tp = G.tk0 as Tk.Type
            val l = mutableListOf(tp)
            while (accept_fix(".")) {
                accept_enu_err("Type")
                l.add(G.tk0 as Tk.Type)
            }
            val e = when {
                check_fix("[") -> parser_expr()
                check_op("<") -> parser_expr()
                else -> {
                    accept_fix_err("(")
                    val x = if (check_fix(")")) {
                        Expr.Unit(G.tk0!!)
                    } else {
                        parser_expr()
                    }
                    accept_fix_err(")")
                    x
                }
            }
            Expr.Cons(tp, l, e)
        }

        else -> err_expected(G.tk1!!, "expression")
    }
}

fun parser_expr_3_suf (xe: Expr? = null): Expr {
    val e = if (xe !== null) xe else parser_expr_4_prim()
    val ok = listOf("[",".","(").any { accept_fix(it) } ||
             listOf("\\","?","!").any { accept_op(it) }
    if (!ok) {
        return e
    }

    return parser_expr_3_suf(
        when (G.tk0!!.str) {
            "(" -> {
                val args = parser_list(",",")") { parser_expr() }
                Expr.Call(e.tk, e, args)
            }
            "\\" -> Expr.Uno(Tk.Op("deref", G.tk0!!.pos.copy()), e)
            "." -> {
                val dot = G.tk0 as Tk.Fix
                (accept_enu("Var") || accept_enu_err("Num"))
                Expr.Field(dot, e, G.tk0!!.str)
            }
            "!" -> {
                val dot = G.tk0 as Tk.Op
                (accept_enu("Type") || accept_enu_err("Num"))
                Expr.Disc(dot, e, G.tk0!!.str)
            }
            "?" -> {
                val dot = G.tk0 as Tk.Op
                (accept_enu("Type") || accept_enu_err("Num"))
                Expr.Pred(dot, e, G.tk0!!.str)
            }
            else -> error("impossible case")
        }
    )
}

fun parser_expr_2_pre (): Expr {
    return when {
        accept_op("-") || accept_op("\\") -> {
            val op = (G.tk0 as Tk.Op).let {
                if (it.str != "\\") it else {
                    Tk.Op("ref", it.pos.copy())
                }
            }
            val e = parser_expr_2_pre()
            Expr.Uno(op, e)
        }
        else -> parser_expr_3_suf()
    }
}

fun parser_expr_1_bin (xop: String? = null, xe1: Expr? = null): Expr {
    val e1 = if (xe1 !== null) xe1 else parser_expr_2_pre()
    val ok = check_enu("Op") && G.tk1!!.str in BINS
    if (!ok) {
        return e1
    }
    accept_enu_err("Op")
    val op = G.tk0!! as Tk.Op
    if (xop!==null && xop!=op.str) {
        err(op, "binary operation error : expected surrounding parentheses")
    }
    val e2 = parser_expr_2_pre()
    return parser_expr_1_bin(op.str, Expr.Bin(op, e1, e2))
}

fun parser_expr (): Expr {
    return parser_expr_1_bin()
}

fun check_stmt_is_expr (): Boolean {
    return (check_fix("create") || check_fix("start") || check_fix("resume") || check_fix("yield"))
}

fun parser_stmt (set: Pair<Tk,Expr>? = null): List<Stmt> {
    return when {
        (accept_fix("func") || accept_fix("coro")) -> {
            val tk0 = G.tk0 as Tk.Fix
            accept_enu_err("Var")
            val id = G.tk0 as Tk.Var
            accept_fix_err(":")
            val tp = parser_type(tk0, true)
            accept_fix_err("{")
            val ss = parser_list(null, "}") {
                parser_stmt()
            }.flatten()
            when (tp) {
                is Type.Proto.Func.Vars ->
                    Stmt.Proto.Func(tk0, id, tp, Stmt.Block(tp.tk, null, ss))
                is Type.Proto.Coro.Vars ->
                    Stmt.Proto.Coro(tk0, id, tp, Stmt.Block(tp.tk, null, ss))
                else -> error("impossible case")
            }.let { listOf(it) }
        }
        accept_fix("return") -> {
            val tk0 = G.tk0 as Tk.Fix
            check_fix_err("(")
            val e = parser_expr()
            listOf(Stmt.Return(tk0, e))
        }

        accept_fix("do") -> {
            val tk0 = G.tk0 as Tk.Fix
            val esc = if (check_fix("{")) {
                null
            } else {
                check_enu_err("Type")
                val tp = parser_type(null, false)
                if (tp !is Type.Data) {
                    err(tp.tk, "exception error : expected data type")
                }
                tp
            }
            accept_fix_err("{")
            val ss = parser_list(null, "}") {
                parser_stmt()
            }.flatten()
            listOf(Stmt.Block(tk0, esc, ss))
        }
        accept_fix("set") -> {
            val dst = parser_expr()
            if (!dst.is_lval()) {
                err(dst.tk, "set error : expected assignable destination")
            }
            accept_fix_err("=")
            val tk0 = G.tk0 as Tk.Fix
            if (check_stmt_is_expr()) {
                parser_stmt(Pair(tk0,dst))
            } else {
                val src = parser_expr()
                listOf(Stmt.Set(tk0, dst, src))
            }
        }
        accept_fix("var") -> {
            val tk0 = G.tk0 as Tk.Fix
            accept_enu_err("Var")
            val id = G.tk0 as Tk.Var
            val (_,tp) = if (accept_fix(":")) parser_var_type(id) else {
                Pair(id, null)
            }
            when {
                !accept_fix("=") -> listOf(Stmt.Dcl(tk0, id, tp))
                !check_stmt_is_expr() -> listOf(Stmt.Dcl(tk0, id, tp), Stmt.Set(tk0, Expr.Acc(id), parser_expr()))
                else -> {
                    listOf(Stmt.Dcl(tk0,id,tp)) + parser_stmt(Pair(G.tk0!!,Expr.Acc(id)))
                }
            }
        }

        accept_fix("escape") -> {
            val tk0 = G.tk0!!
            accept_fix_err("(")
            check_enu_err("Type")
            val e = parser_expr() as Expr.Cons
            accept_fix_err(")")
            listOf(Stmt.Escape(tk0, e))
        }
        accept_fix("defer") -> {
            val tk0 = G.tk0!!
            accept_fix_err("{")
            val blk = parser_list(null, "}") {
                parser_stmt()
            }.flatten()
            listOf(Stmt.Defer(tk0, Stmt.Block(tk0, null, blk)))
        }
        accept_fix("catch") -> {
            val tk0 = G.tk0!!
            val tp = if (check_fix("{")) {
                null
            } else {
                check_enu_err("Type")
                val tp = parser_type(null, false)
                if (tp !is Type.Data) {
                    err(tp.tk, "exception error : expected data type")
                }
                tp
            }
            accept_fix_err("{")
            val blk = parser_list(null, "}") {
                parser_stmt()
            }.flatten()
            listOf(Stmt.Catch(tk0, tp, Stmt.Block(tk0, null, blk)))
        }
        accept_fix("throw") -> {
            val tk0 = G.tk0!!
            accept_fix_err("(")
            val e = parser_expr()
            accept_fix_err(")")
            listOf(Stmt.Throw(tk0, e))
        }

        accept_fix("if") -> {
            val tk0 = G.tk0 as Tk.Fix
            val cnd = parser_expr()
            accept_fix_err("{")
            val t = parser_list(null, "}") {
                parser_stmt()
            }.flatten()
            val f = if (accept_fix("else")) {
                accept_fix_err("{")
                parser_list(null, "}") {
                    parser_stmt()
                }.flatten()
            } else {
                emptyList()
            }
            listOf(Stmt.If(tk0, cnd, Stmt.Block(tk0, null, t), Stmt.Block(tk0, null, f)))
        }
        accept_fix("loop") -> {
            val tk0 = G.tk0!!
            accept_fix_err("{")
            val blk = parser_list(null, "}") {
                parser_stmt()
            }.flatten()
            listOf(Stmt.Loop(tk0, Stmt.Block(tk0, Type.Data(tk0, listOf(Tk.Type("_Break_",tk0.pos.copy()))), blk)))
        }
        accept_fix("break") -> listOf(Stmt.Break(G.tk0 as Tk.Fix))

        (set!=null && accept_fix("create")) -> {
            val tk0 = G.tk0 as Tk.Fix
            accept_fix_err("(")
            val co = parser_expr()
            accept_fix_err(")")
            listOf(Stmt.Set(set.first, set.second, Expr.Create(tk0, co)))
        }
        accept_fix("start") -> {
            val tk0 = G.tk0 as Tk.Fix
            val exe = parser_expr_4_prim()
            accept_fix_err("(")
            val args = parser_list(",",")") { parser_expr() }
            if (set == null) {
                listOf(Stmt.XExpr(tk0, Expr.Start(tk0, exe, args)))
            } else {
                listOf(Stmt.Set(set.first, set.second, Expr.Start(tk0, exe, args)))

            }
        }
        accept_fix("resume") -> {
            val tk0 = G.tk0 as Tk.Fix
            val exe = parser_expr_4_prim()
            accept_fix_err("(")
            val arg = if (check_fix(")")) {
                Expr.Unit(G.tk0 as Tk.Fix)
            } else {
                parser_expr()
            }
            accept_fix_err(")")
            if (set == null) {
                listOf(Stmt.XExpr(tk0, Expr.Resume(tk0, exe, arg)))
            } else {
                listOf(Stmt.Set(set.first, set.second, Expr.Resume(tk0, exe, arg)))

            }
        }
        accept_fix("yield") -> {
            val tk0 = G.tk0 as Tk.Fix
            accept_fix_err("(")
            val arg = if (check_fix(")")) {
                Expr.Unit(G.tk0 as Tk.Fix)
            } else {
                parser_expr()
            }
            accept_fix_err(")")
            if (set == null) {
                listOf(Stmt.XExpr(tk0, Expr.Yield(tk0, arg)))
            } else {
                listOf(Stmt.Set(set.first, set.second, Expr.Yield(tk0, arg)))

            }
        }

        accept_fix("data") -> {
            val tk0 = G.tk0!!
            accept_enu_err("Type")
            val t = G.tk0 as Tk.Type
            if (!accept_fix(".")) {
                accept_fix_err(":")
                val tp = parser_type(null, false)
                listOf(Stmt.Data(tk0, t, tp, null))
            } else {
                accept_op_err("*")
                accept_fix_err(":")
                val tp = if (!check_fix_err("[")) {
                    Type.Tuple(G.tk0!!, emptyList())
                } else {
                    parser_type(null, false) as Type.Tuple
                }
                fun f (): List<Stmt.Data> {
                    return if (!accept_fix("{")) emptyList() else {
                        parser_list(null, "}") {
                            accept_enu_err("Type")
                            val xt = G.tk0 as Tk.Type
                            accept_fix_err(":")
                            val xtp = if (!check_fix_err("[")) {
                                Type.Tuple(G.tk0!!, emptyList())
                            } else {
                                parser_type(null, false) as Type.Tuple
                            }
                            Stmt.Data(xt, xt, xtp, f())
                        }
                    }
                }
                listOf(Stmt.Data(tk0, t, tp, f()))
            }
        }
        accept_fix("print") -> {
            val tk0 = G.tk0 as Tk.Fix
            accept_fix_err("(")
            val e = parser_expr()
            accept_fix_err(")")
            listOf(Stmt.Print(tk0, e))
        }

        accept_enu("Nat") -> listOf(Stmt.Nat(G.tk0 as Tk.Nat))
        else -> {
            val tk1 = G.tk1!!
            val call = parser_expr_3_suf()
            if (call is Expr.Call) {
                listOf(Stmt.XExpr(call.tk, call))
            } else {
                err_expected(tk1, "statement")
            }
        }
    }
}
