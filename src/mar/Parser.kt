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

fun parser_var_type (): Var_Type {
    accept_enu_err("Var")
    val id = G.tk0 as Tk.Var
    accept_fix_err(":")
    if (check_fix("func") || check_fix("coro")) {
        err(G.tk1!!, "type error : unexpected \"${G.tk1!!.str}\"")
    }
    val tp = parser_type()
    return Pair(id, tp)
}

fun parser_type (req_vars: Boolean = false, pre: Tk.Fix? = null): Type {
    return when {
        (pre?.str=="func" || accept_fix("func")) -> {
            val tk0 = pre ?: (G.tk0 as Tk.Fix)
            accept_fix_err("(")
            val vars = req_vars || check_enu("Var")
            val inp = parser_list(",", ")") {
                if (vars) {
                    parser_var_type()
                } else {
                    parser_type(req_vars)
                }
            }
            accept_fix_err("->")
            val out = parser_type(req_vars)
            if (vars) {
                Type.Proto.Func.Vars(tk0, inp as List<Var_Type>, out)
            } else {
                Type.Proto.Func(tk0, inp as List<Type>, out)
            }
        }
        (pre?.str=="coro" || accept_fix("coro")) -> {
            val tk0 = pre ?: (G.tk0 as Tk.Fix)
            accept_fix_err("(")
            val vars = req_vars || check_enu("Var")
            check_op_err("<")
            val inp = if (vars) {
                val x = parser_var_type()
                assert(x.second is Type.Union)
                x
            } else {
                parser_type(req_vars) as Type.Union
            }
            accept_fix_err(")")
            accept_fix_err("->")
            check_op_err("<")
            val out = parser_type(req_vars) as Type.Union
            if (vars) {
                Type.Proto.Coro.Vars(tk0, inp as Var_Type, out)
            } else {
                Type.Proto.Coro(tk0, inp as Type.Union, out)
            }
        }
        accept_fix("xcoro") -> {
            val tk0 = pre ?: (G.tk0 as Tk.Fix)
            accept_fix_err("(")
            check_op_err("<")
            val inp = parser_type() as Type.Union
            check_op_err(")")
            accept_fix_err("->")
            check_op_err("<")
            val out = parser_type() as Type.Union
            Type.XCoro(tk0, inp, out)
        }
        accept_fix("(") -> {
            val tp = if (check_fix(")")) {
                Type.Unit(G.tk0 as Tk.Fix)
            } else {
                parser_type(req_vars)
            }
            accept_fix_err(")")
            tp
        }
        accept_enu("Type") -> Type.Basic(G.tk0 as Tk.Type)
        accept_op("\\") -> {
            val tk0 = G.tk0 as Tk.Op
            val ptr = parser_type(req_vars)
            Type.Pointer(tk0, ptr)
        }
        accept_fix("[") -> {
            val tk0 = G.tk0 as Tk.Fix
            val ts = parser_list(",", "]") {
                parser_type(req_vars, pre)
            }
            Type.Tuple(tk0, ts)
        }
        accept_op("<") -> {
            val tk0 = G.tk0 as Tk.Op
            val ts = parser_list(",", ">") {
                parser_type(req_vars, pre)
            }
            Type.Union(tk0, ts)
        }
        else -> err_expected(G.tk1!!, "type")
    }
}

fun parser_expr_4_prim (): Expr {
    return when {
        accept_enu("Nat")  -> Expr.Nat(G.tk0 as Tk.Nat)
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
            val vs = parser_list(",", "]") {
                parser_expr()
            }
            Expr.Tuple(tk0, vs)
        }
        accept_op("<")     -> {
            val tk0 = G.tk0 as Tk.Op
            accept_fix_err(".")
            accept_enu_err("Num")
            val idx = G.tk0!!.str
            val v = parser_expr_2_pre() // avoid bin `>` (x>10)
            accept_op_err(">")
            accept_fix_err(":")
            val tp = parser_type()
            Expr.Union(tk0, tp, idx, v)
        }

        else                    -> err_expected(G.tk1!!, "expression")
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
                accept_enu_err("Num")
                Expr.Index(dot, e, G.tk0!!.str)
            }
            "!" -> {
                val dot = G.tk0 as Tk.Op
                accept_enu_err("Num")
                Expr.Disc(dot, e, G.tk0!!.str)
            }
            "?" -> {
                val dot = G.tk0 as Tk.Op
                accept_enu_err("Num")
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
    return (check_fix("create") || check_fix("resume") || check_fix("yield"))
}

fun parser_stmt (set: Expr? = null): List<Stmt> {
    return when {
        (accept_fix("func") || accept_fix("coro")) -> {
            val tk0 = G.tk0 as Tk.Fix
            accept_enu_err("Var")
            val id = G.tk0 as Tk.Var
            val tp = parser_type(true, tk0)
            accept_fix_err("{")
            val ss = parser_list(null, "}") {
                parser_stmt()
            }.flatten()
            when (tp) {
                is Type.Proto.Func.Vars ->
                    Stmt.Proto.Func(tk0, id, tp, Stmt.Block(tp.tk_, ss))
                is Type.Proto.Coro.Vars ->
                    Stmt.Proto.Coro(tk0, id, tp, Stmt.Block(tp.tk_, ss))
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
            accept_fix_err("{")
            val ss = parser_list(null, "}") {
                parser_stmt()
            }.flatten()
            listOf(Stmt.Block(tk0, ss))
        }
        accept_fix("set") -> {
            val tk0 = G.tk0 as Tk.Fix
            val dst = parser_expr()
            if (!dst.is_lval()) {
                err(tk0, "set error : expected assignable destination")
            }
            accept_fix_err("=")
            if (check_stmt_is_expr()) {
                parser_stmt(dst)
            } else {
                val src = parser_expr()
                listOf(Stmt.Set(tk0, dst, src))
            }
        }
        accept_fix("var") -> {
            val tk0 = G.tk0 as Tk.Fix
            val vt = parser_var_type()
            when {
                !accept_fix("=") -> listOf(Stmt.Dcl(tk0, vt))
                !check_stmt_is_expr() -> listOf(Stmt.Dcl(tk0, vt), Stmt.Set(tk0, Expr.Acc(vt.first), parser_expr()))
                else -> {
                    listOf(Stmt.Dcl(tk0, vt)) + parser_stmt(Expr.Acc(vt.first))
                }
            }
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
            listOf(Stmt.If(tk0, cnd, Stmt.Block(tk0, t), Stmt.Block(tk0, f)))
        }
        accept_fix("loop") -> {
            val tk0 = G.tk0 as Tk.Fix
            accept_fix_err("{")
            val blk = parser_list(null, "}") {
                parser_stmt()
            }.flatten()
            listOf(Stmt.Loop(tk0, Stmt.Block(tk0, blk)))
        }
        accept_fix("break") -> listOf(Stmt.Break(G.tk0 as Tk.Fix))

        (set!=null && accept_fix("create")) -> {
            val tk0 = G.tk0 as Tk.Fix
            accept_fix_err("(")
            val co = parser_expr()
            accept_fix_err(")")
            listOf(Stmt.Create(tk0, set, co))
        }
        accept_fix("resume") -> {
            val tk0 = G.tk0 as Tk.Fix
            val xco = parser_expr_4_prim()
            accept_fix_err("(")
            val arg = if (check_fix(")")) {
                Expr.Unit(G.tk0 as Tk.Fix)
            } else {
                parser_expr()
            }
            accept_fix_err(")")
            listOf(Stmt.Resume(tk0, set, xco, arg))
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
            listOf(Stmt.Yield(tk0, set, arg))
        }

        accept_enu("Nat") -> listOf(Stmt.Nat(G.tk0 as Tk.Nat))
        else -> {
            val tk1 = G.tk1!!
            val call = parser_expr_3_suf()
            if (call is Expr.Call) {
                listOf(Stmt.Call(call.tk, call))
            } else {
                err_expected(tk1, "statement")
            }
        }
    }
}
