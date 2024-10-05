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

fun parser_expr_3_prim (): Expr {
    return when {
        accept_enu("Nat")  -> Expr.Nat(G.tk0 as Tk.Nat)
        accept_enu("Var")  -> Expr.Acc(G.tk0 as Tk.Var)
        accept_fix("false") -> Expr.Bool(G.tk0 as Tk.Fix)
        accept_fix("true")  -> Expr.Bool(G.tk0 as Tk.Fix)
        accept_enu("Chr")  -> Expr.Char(G.tk0 as Tk.Chr)
        accept_enu("Num")  -> Expr.Num(G.tk0 as Tk.Num)
        accept_fix("null")  -> Expr.Null(G.tk0 as Tk.Fix)
        accept_fix("(")     -> parser_expr().let { accept_fix_err(")") ; it }
        else                    -> err_expected(G.tk1!!, "expression")
    }
}

fun parser_expr_2_suf (xe: Expr? = null): Expr {
    val e = if (xe !== null) xe else parser_expr_3_prim()
    val ok = accept_fix("[") || accept_fix(".") || accept_fix("(")
    if (!ok) {
        return e
    }

    return parser_expr_2_suf(
        when (G.tk0!!.str) {
            "(" -> {
                val args = parser_list(",",")") { parser_expr() }
                Expr.Call(e.tk, e, args)
            }
            else -> error("impossible case")
        }
    )
}

fun parser_expr_1_bin (xop: String? = null, xe1: Expr? = null): Expr {
    val e1 = if (xe1 !== null) xe1 else parser_expr_2_suf()
    if (!accept_enu("Op")) {
        return e1
    }
    val op = G.tk0!! as Tk.Op
    if (xop!==null && xop!=op.str) {
        err(op, "binary operation error : expected surrounding parentheses")
    }
    val e2 = parser_expr_2_suf()
    return parser_expr_1_bin(op.str, Expr.Bin(op, e1, e2))
}

fun parser_expr (): Expr {
    return parser_expr_1_bin()
}

fun parser_var_type (): Var_Type {
    accept_enu_err("Var")
    val id = G.tk0 as Tk.Var
    accept_fix_err(":")
    accept_enu_err("Type")
    val tp = G.tk0 as Tk.Type
    return Pair(id, tp)
}

fun parser_stmt (): Stmt {
    return when {
        accept_fix("var") -> {
            val tk0 = G.tk0 as Tk.Fix
            val id_tp = parser_var_type()
            Stmt.Dcl(tk0, id_tp)
        }
        accept_fix("set") -> {
            val tk0 = G.tk0 as Tk.Fix
            val dst = parser_expr()
            accept_fix_err("=")
            val src = parser_expr()
            if (!dst.is_lval()) {
                err(tk0, "set error : expected assignable destination")
            }
            Stmt.Set(tk0, dst, src)
        }
        accept_enu("Nat") -> Stmt.Nat(G.tk0 as Tk.Nat)
        else -> {
            val tk1 = G.tk1!!
            val call = parser_expr_2_suf()
            if (call is Expr.Call) {
                Stmt.Call(call.tk, call)
            } else {
                err_expected(G.tk1!!, "statement")
            }
        }
    }
}

fun parser_stmts (eof: Boolean = false): List<Stmt> {
    val ret = mutableListOf<Stmt>()
    while (!check_fix("}") && !check_enu("Eof")) {
        ret.add(parser_stmt())
    }
    if (eof) {
        check_enu_err("Eof")
    }
    return ret
}
