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
        else   -> TODO(str)
    }

    if (!ret) {
        err_expected(G.tk1!!, err)
    }
    return ret
}
fun accept_enu (enu: String): Boolean {
    val ret = check_enu(enu)
    if (ret) {
        parser_lexer()
    }
    return ret
}
fun accept_enu_err (str: String): Boolean {
    check_enu_err(str)
    accept_enu(str)
    return true
}

fun parser_lexer () {
    G.tk0 = G.tk1
    G.tk1 = G.tks!!.next()
}

fun <T> parser_list (sep: String?, close: String, func: () -> T): List<T> {
    return parser_list(sep, { accept_fix(close) }, func)

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
    val tp = parser_type()
    return Pair(id, tp)
}

fun parser_type (req_vars: Boolean = false, pre: Tk.Fix? = null): Type {
    return when {
        (pre!=null || accept_fix("func") || accept_fix("coro")) -> {
            val tk0 = pre ?: (G.tk0 as Tk.Fix)
            accept_fix_err("(")
            val vars = req_vars || check_enu("Var")
            val inps = parser_list(",", ")") {
                if (vars) {
                    parser_var_type()
                } else {
                    parser_type(req_vars)
                }
            }
            val mid = if (tk0.str == "func") null else {
                accept_fix_err("->")
                parser_type(req_vars)
            }
            accept_fix_err("->")
            val out = parser_type(req_vars)
            when {
                (mid==null &&  vars) -> Type.Proto.Func.Vars(tk0, inps as List<Var_Type>, out)
                (mid==null && !vars) -> Type.Proto.Func(tk0, inps as List<Type>, out)
                (mid!=null &&  vars) -> Type.Proto.Coro.Vars(tk0, inps as List<Var_Type>, mid, out)
                (mid!=null && !vars) -> Type.Proto.Coro(tk0, inps as List<Type>, mid, out)
                else -> error("impossible case")
            }
        }
        accept_fix("xcoro") -> {
            val tk0 = pre ?: (G.tk0 as Tk.Fix)
            accept_fix_err("(")
            val inp = if (check_fix(")")) {
                Type.Unit(G.tk0 as Tk.Fix)
            } else {
                parser_type(req_vars)
            }
            accept_fix_err(")")
            accept_fix_err("->")
            val out = parser_type(req_vars)
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
        accept_enu("Op") -> {
            val tk0 = G.tk0 as Tk.Op
            val ptr = parser_type(req_vars)
            Type.Pointer(tk0, ptr)
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

        else                    -> err_expected(G.tk1!!, "expression")
    }
}

fun parser_expr_3_suf (xe: Expr? = null): Expr {
    val e = if (xe !== null) xe else parser_expr_4_prim()
    val ok = accept_fix("[") || accept_fix(".") || accept_fix("(")
    if (!ok) {
        return e
    }

    return parser_expr_3_suf(
        when (G.tk0!!.str) {
            "(" -> {
                val args = parser_list(",",")") { parser_expr() }
                Expr.Call(e.tk, e, args)
            }
            else -> error("impossible case")
        }
    )
}

fun parser_expr_2_pre (): Expr {
    return when {
        accept_enu("Op") -> {
            val op = G.tk0 as Tk.Op
            val e = parser_expr_2_pre()
            Expr.Uno(op, e)
        }
        else -> parser_expr_3_suf()
    }
}

fun parser_expr_1_bin (xop: String? = null, xe1: Expr? = null): Expr {
    val e1 = if (xe1 !== null) xe1 else parser_expr_2_pre()
    if (!accept_enu("Op")) {
        return e1
    }
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

fun parser_stmt (set: Expr? = null): Stmt {
    return when {
        accept_fix("do") -> {
            val tk0 = G.tk0 as Tk.Fix
            accept_fix_err("[")
            val vs = parser_list(",","]") {
                parser_var_type()
            }
            accept_fix_err("{")
            val ss = parser_list(null, "}") {
                parser_stmt()
            }
            Stmt.Block(tk0, vs, ss)
        }
        accept_fix("set") -> {
            val tk0 = G.tk0 as Tk.Fix
            val dst = parser_expr()
            accept_fix_err("=")
            if (check_fix("spawn") || check_fix("resume") || check_fix("yield")) {
                parser_stmt(dst)
            } else {
                val src = parser_expr()
                if (!dst.is_lval()) {
                    err(tk0, "set error : expected assignable destination")
                }
                Stmt.Set(tk0, dst, src)
            }
        }
        (accept_fix("func") || accept_fix("coro")) -> {
            val tk0 = G.tk0 as Tk.Fix
            accept_enu_err("Var")
            val id = G.tk0 as Tk.Var
            val tp = parser_type(true, tk0)
            accept_fix_err("{")
            val ss = parser_list(null, "}") {
                parser_stmt()
            }
            when (tp) {
                is Type.Proto.Func.Vars ->
                    Stmt.Proto.Func(tk0, id, tp, Stmt.Block(tp.tk_, tp.inps__, ss))
                is Type.Proto.Coro.Vars ->
                    Stmt.Proto.Coro(tk0, id, tp, Stmt.Block(tp.tk_, tp.inps__, ss))
                else -> error("impossible case")
            }
        }
        accept_fix("return") -> {
            val tk0 = G.tk0 as Tk.Fix
            check_fix_err("(")
            val e = parser_expr()
            Stmt.Return(tk0, e)
        }

        accept_fix("spawn") -> {
            val tk0 = G.tk0 as Tk.Fix
            val co = parser_expr_4_prim()
            accept_fix_err("(")
            val args = parser_list(",",")") { parser_expr() }
            Stmt.Spawn(tk0, set, co, args)
        }
        accept_fix("resume") -> {
            val tk0 = G.tk0 as Tk.Fix
            val xco = parser_expr_4_prim()
            accept_fix_err("(")
            val args = parser_list(",",")") { parser_expr() }
            Stmt.Resume(tk0, set, xco, args)
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
            Stmt.Yield(tk0, set, arg)
        }

        accept_enu("Nat") -> Stmt.Nat(G.tk0 as Tk.Nat)
        else -> {
            val tk1 = G.tk1!!
            val call = parser_expr_3_suf()
            if (call is Expr.Call) {
                Stmt.Call(call.tk, call)
            } else {
                err_expected(tk1, "statement")
            }
        }
    }
}
