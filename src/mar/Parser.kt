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
        "Str"  -> G.tk1 is Tk.Str
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
    val tp = parser_type(null, false, false)
    return Pair(id, tp)
}

fun parser_tpls_abs (): List<Tpl_Abs> {
    return if (!accept_fix("{{")) emptyList() else {
        val l = parser_list(",", "}") {
            parser_var_type(null)
        }
        accept_fix_err("}")
        l
    }
}

fun parser_tpls_con (): List<Tpl_Con>? {
    return if (!accept_fix("{{")) null else {
        val x = parser_list(",", "}") {
            if (accept_fix(":")) {
                Pair(parser_type(null, false, false), null)
            } else {
                Pair(null, parser_expr())
            }
        }
        accept_fix_err("}")
        x
    }
}

fun parser_type (pre: Tk?, fr_proto: Boolean, fr_pointer: Boolean): Type {
    return when {
        (pre is Tk.Fix || accept_fix("func") || accept_fix("coro") || accept_fix("task")) -> {
            val tk0 = pre ?: (G.tk0 as Tk.Fix)

            val xpro = when {
                (tk0.str == "func") -> null
                !accept_fix("[") -> null
                else -> {
                    accept_enu_err("Var")
                    val x = G.tk0 as Tk.Var
                    accept_fix_err("]")
                    x
                }
            }

            accept_fix_err("(")
            val req = fr_proto || check_enu("Var")
            val inps = parser_list(",", ")") {
                if (req) {
                    parser_var_type(null)
                } else {
                    parser_type(null, false, fr_pointer)
                }
            }
            accept_op_err("->")

            val (res,yld) = if (tk0.str != "coro") Pair(null,null) else {
                val res = parser_type(null, false, fr_pointer)
                accept_op_err("->")
                val yld = parser_type(null, false, fr_pointer)
                accept_op_err("->")
                Pair(res, yld)
            }
            val out = parser_type(null, false, fr_pointer)
            when {
                (tk0.str=="func" &&  req) ->
                    Type.Proto.Func.Vars(tk0, null, inps as List<Var_Type>, out)
                (tk0.str=="func" && !req) ->
                    Type.Proto.Func(tk0, null, inps as List<Type>, out)
                (tk0.str=="coro" &&  req) ->
                    Type.Proto.Coro.Vars(tk0, xpro, null, inps as List<Var_Type>, res!!, yld!!, out)
                (tk0.str=="coro" && !req) ->
                    Type.Proto.Coro(tk0, xpro, null, inps as List<Type>, res!!, yld!!, out)
                (tk0.str=="task" &&  req) ->
                    Type.Proto.Task.Vars(tk0, xpro, null, inps as List<Var_Type>, out)
                (tk0.str=="task" && !req) ->
                    Type.Proto.Task(tk0, xpro, null, inps as List<Type>, out)
                else -> error("impossible case")
            }
        }
        accept_fix("exec") -> {
            val tk0 = G.tk0 as Tk.Fix
            accept_fix("coro") || accept_fix("task") || err(G.tk1!!, "exec error : expected coro or task")
            val is_coro = (G.tk0!!.str == "coro")

            val xpro = when {
                !accept_fix("[") -> null
                else -> {
                    accept_enu_err("Var")
                    val x = G.tk0 as Tk.Var
                    accept_fix_err("]")
                    x
                }
            }

            accept_fix_err("(")
            val inps = parser_list(",", ")") {
                parser_type(null, false, fr_pointer)
            }
            accept_op_err("->")
            if (is_coro) {
                val res = parser_type(null, false, fr_pointer)
                accept_op_err("->")
                val yld = parser_type(null, false, fr_pointer)
                accept_op_err("->")
                val out = parser_type(null, false, fr_pointer)
                Type.Exec.Coro(tk0, xpro, inps, res, yld, out)
            } else {
                val out = parser_type(null, false, fr_pointer)
                Type.Exec.Task(tk0, xpro, inps, out)
            }
        }
        accept_fix("(") -> {
            val tp = if (check_fix(")")) {
                Type.Unit(G.tk0 as Tk.Fix)
            } else {
                parser_type(null, false, fr_pointer)
            }
            accept_fix_err(")")
            tp
        }
        (pre is Tk.Type) || accept_enu("Type") -> {
            val tp = if (pre is Tk.Type) pre else G.tk0 as Tk.Type
            if (PRIMS.contains(tp.str)) {
                Type.Prim(tp)
            } else {
                val tpls = parser_tpls_con()
                val l = mutableListOf(tp)
                while (accept_fix(".")) {
                    accept_enu_err("Type")
                    l.add(G.tk0 as Tk.Type)
                }
                Type.Data(tp, tpls, l)
            }
        }
        accept_op("\\") -> {
            val tk0 = G.tk0 as Tk.Op
            val ptr = parser_type(null, false, true)
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
                Pair(id, parser_type(null, false, fr_pointer))
            }
            Type.Tuple(tk0, ts)
        }
        accept_op("<") -> {
            val tk0 = G.tk0 as Tk.Op
            val ts = parser_list(",", ">") {
                val tk1 = G.tk1
                val has_id = accept_enu("Type") && accept_fix(":")
                when {
                    has_id -> Pair(tk1 as Tk.Type, parser_type(null, false, fr_pointer))
                    (tk1 is Tk.Type) -> Pair(null, parser_type(tk1, false, fr_pointer))
                    else -> Pair(null, parser_type(null, false, fr_pointer))
                }
            }
            Type.Union(tk0, true, ts)
        }
        accept_fix("#[") -> {
            val tk0 = G.tk0 as Tk.Fix
            val tp = parser_type(null, false, fr_pointer)
            val size = if (fr_pointer && check_fix("]")) null else {
                accept_op_err("*")
                parser_expr()
            }
            accept_fix_err("]")
            when {
                (size == null) -> {}
                size.static_int_is() -> {}
                else -> err(size.tk, "type error : expected constant integer expression")
            }
            Type.Vector(tk0, size, tp)
        }
        accept_enu("Nat")  -> Type.Nat(G.tk0 as Tk.Nat)
        accept_fix("{{") -> {
            accept_enu_err("Var")
            val t = Type.Tpl(G.tk0 as Tk.Var)
            accept_fix_err("}")
            accept_fix_err("}")
            t
        }
        else -> err_expected(G.tk1!!, "type")
    }
}

fun parser_expr_4_prim (): Expr {
    return when {
        accept_fix("{{") -> {
            accept_enu_err("Var")
            val e = Expr.Tpl(G.tk0 as Tk.Var)
            accept_fix_err("}")
            accept_fix_err("}")
            e
        }
        accept_enu("Nat")  -> {
            val tk0 = G.tk0 as Tk.Nat
            val tp = if (!accept_fix(":")) null else {
                parser_type(null, false, false)
            }
            Expr.Nat(tk0, tp)
        }
        accept_enu("Var")  -> Expr.Acc(G.tk0 as Tk.Var)
        accept_fix("it")    -> Expr.It(G.tk0 as Tk.Fix, null)
        accept_fix("false") -> Expr.Bool(G.tk0 as Tk.Fix)
        accept_fix("true")  -> Expr.Bool(G.tk0 as Tk.Fix)
        accept_enu("Str")  -> Expr.Str(G.tk0 as Tk.Str)
        accept_enu("Chr")  -> Expr.Chr(G.tk0 as Tk.Chr)
        accept_enu("Num")  -> Expr.Num(G.tk0 as Tk.Num)
        accept_fix("null")  -> Expr.Null(G.tk0!!)
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
                    accept_op_err("=")
                    idx
                }
                val e = parser_expr()
                Pair(x, e)
            }
            val tp = if (!accept_fix(":")) null else {
                check_fix_err("[")
                parser_type(null, false, false) as Type.Tuple
            }
            Expr.Tuple(tk0, tp, l)
        }
        accept_fix("#[")    -> {
            val tk0 = G.tk0 as Tk.Fix
            val l = parser_list(",", "]") {
                parser_expr()
            }
            val tp = if (!accept_fix(":")) null else {
                check_fix_err("#[")
                parser_type(null, false, false) as Type.Vector
            }
            Expr.Vector(tk0, tp, l)
        }
        accept_op("<")      -> {
            val tk0 = G.tk0 as Tk.Op
            accept_fix_err(".")
            (accept_enu("Type") || accept_enu_err("Num"))
            val idx = G.tk0!!.str
            accept_op_err("=")
            val v = parser_expr_2_pre() // avoid bin `>` (x>10)
            accept_op_err(">")
            val tp = if (!accept_fix(":")) null else {
                check_op_err("<")
                parser_type(null, false, false) as Type.Union
            }
            Expr.Union(tk0, tp, idx, v)
        }

        check_enu("Type") -> {
            val tp = parser_type(null, false, false)
            if (tp !is Type.Data) {
                err(G.tk1!!, "constructor error : unexpected primitive type")
            }
            val e = when {
                check_fix("[") -> parser_expr_4_prim()
                check_op("<") -> parser_expr_4_prim()
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
            Expr.Cons(tp.tk, tp, e)
        }

        accept_fix("if")    -> {
            val tk0 = G.tk0!!
            val cnd = parser_expr()
            accept_op_err("=>")
            val t = parser_expr()
            accept_op_err("=>")
            val f = parser_expr()
            Expr.If(tk0, null, cnd, t, f)
        }
        accept_fix("match") -> {
            val tk0 = G.tk0!!
            val tst = parser_expr()
            accept_fix_err("{")
            if (check_fix("}")) {
                err(G.tk1!!, "match error : unexpected \"}\"")
            }
            var tt: Boolean? = null
            val cases = parser_list(null, "}") {
                val cnd = if (accept_fix("else")) null else {
                    if (tt == true) {
                        check_enu_err("Type")
                    }
                    if (tt!=false && accept_fix(":")) {
                        val tp = parser_type(null, false, false)
                        if (tp !is Type.Data) {
                            err(tp.tk, "exception error : expected data type")
                        }
                        tt = true
                        tp
                    } else {
                        tt = false
                        parser_expr()
                    }
                }
                accept_op_err("=>")
                val se = parser_expr()
                Pair(cnd, se)
            }
            if (tt == true) {
                Expr.MatchT(tk0, null, tst, cases as List<Pair<Type.Data?,Expr>>)
            } else {
                Expr.MatchE(tk0, null, tst, cases as List<Pair<Expr?,Expr>>)
            }
        }

        else -> err_expected(G.tk1!!, "expression")
    }
}

fun parser_expr_3_suf (xe: Expr? = null): Expr {
    val e = if (xe !== null) xe else parser_expr_4_prim()
    val ok = G.tk0!!.pos.is_same_line(G.tk1!!.pos) && (
        listOf("[",".","(","{{").any { check_fix(it) } ||
        listOf("\\","?","!").any { check_op(it) }
    )
    if (!ok) {
        return e
    }

    return parser_expr_3_suf(
        when {
            (check_fix("{{") || check_fix("(")) -> {
                val tpls = parser_tpls_con()
                accept_fix_err("(")
                val args = parser_list(",",")") { parser_expr() }
                Expr.Call(e.tk, e, tpls, args)
            }
            accept_fix("[") -> {
                val idx = parser_expr()
                accept_fix_err("]")
                Expr.Index(e.tk, e, idx)
            }
            accept_op("\\") -> Expr.Uno(Tk.Op("deref", G.tk0!!.pos), e)
            accept_fix(".") -> {
                val dot = G.tk0 as Tk.Fix
                (accept_enu("Var") || accept_enu_err("Num"))
                Expr.Field(dot, e, G.tk0!!.str)
            }
            accept_op("!") -> {
                val dot = G.tk0 as Tk.Op
                (accept_enu("Type") || accept_enu_err("Num"))
                Expr.Disc(dot, e, G.tk0!!.str)
            }
            accept_op("?") -> {
                val dot = G.tk0 as Tk.Op
                (accept_enu("Type") || accept_enu_err("Num"))
                Expr.Pred(dot, e, G.tk0!!.str)
            }
            else -> error("impossible case")
        }
    )
}

fun parser_expr_2_pre (): Expr {
    val unos = listOf("-", "\\", "#", "##", "!")
    return when {
        unos.any { accept_op(it) } -> {
            val op = (G.tk0 as Tk.Op).let {
                if (it.str != "\\") it else {
                    Tk.Op("ref", it.pos)
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

fun parser_stmt_block (): List<Stmt> {
    accept_fix_err("{")
    val ss = parser_list(null, "}") {
        parser_stmt()
    }.flatten()
    return ss
}

fun gen_spawn (tk: Tk, N: Int, pro: Expr, args: List<Expr>): List<Stmt> {
    return listOf(
        //Stmt.Dcl(tk, Tk.Var("mar_exe_$N", tk.pos), Type.Nat(Tk.Nat("TODO", tk.pos))),
        Stmt.Dcl(tk, Tk.Var("mar_exe_$N", tk.pos), null),
        Stmt.SetS(
            tk,
            Expr.Acc(Tk.Var("mar_exe_$N", tk.pos)),
            Stmt.Create(tk, pro),
        ),
        Stmt.Start(tk, Expr.Acc(Tk.Var("mar_exe_$N", tk.pos)), args)
    )
}

fun gen_proto_spawn (tk: Tk, N: Int, blk: List<Stmt>): List<Stmt> {
    return listOf(
        Stmt.Proto.Task(
            Tk.Fix("task", tk.pos),
            Tk.Var("mar_pro_$N", tk.pos),
            emptyList(),
            Type.Proto.Task.Vars(tk, null, emptyList(), emptyList(), Type.Unit(tk)),
            Stmt.Block(tk, null, blk)
        )
    ) + gen_spawn(tk, N, Expr.Acc(Tk.Var("mar_pro_$N", tk.pos)), emptyList())
}

fun parser_stmt (set: Expr?=null): List<Stmt> {
    return when {
        accept_fix("await") -> {
            val tk0 = G.tk0 as Tk.Fix
            val awt: Stmt.Await = when {
                // await pro(...)
                !accept_fix("(") -> {
                    val n = G.N
                    val id = Tk.Var("mar_$n", tk0.pos)
                    val call = parser_expr()
                    if (call !is Expr.Call) {
                        err(call.tk, "await error : expected task call")
                    }
                    return listOf(
                        Stmt.Dcl(tk0, id, null),
                        Stmt.SetS(tk0, Expr.Acc(id), Stmt.Create(tk0, call.f)),
                        Stmt.Start(tk0, Expr.Acc(id), call.args),
                        Stmt.Await.Task(tk0, Expr.Acc(id))
                    )
                }
                // await(true), await(false)
                accept_fix("true") || accept_fix("false") -> {
                    val tk = G.tk0!!
                    accept_fix_err(")")
                    Stmt.Await.Bool(tk)
                }
                // await(%10min)
                accept_op("%") -> {
                    val ms = parser_expr()
                    accept_fix_err(")")
                    Stmt.Await.Clock(tk0, ms)
                }
                // await(:X)
                accept_fix(":") -> {
                    val tp = parser_type(null, false, false) as Type.Data
                    val cnd = if (!accept_fix(",")) {
                        Expr.Bool(Tk.Fix("true", G.tk0!!.pos))
                    } else {
                        parser_expr()
                    }
                    accept_fix_err(")")
                    Stmt.Await.Data(tk0, tp, cnd)
                }
                // await(exe)
                else -> {
                    val exe = parser_expr()
                    accept_fix_err(")")
                    Stmt.Await.Task(tk0, exe)
                }
            }
            val xawt = if (set == null) awt else {
                Stmt.SetS(set.tk, set, awt)
            }
            listOf(xawt)
        }
        accept_fix("emit") -> {
            val tk0 = G.tk0!!
            accept_fix_err("(")
            val e = if (check_fix(")")) {
                Expr.Cons(tk0, Type.Data(tk0, null, listOf(Tk.Type("Error",tk0.pos))), Expr.Tuple(tk0, null, emptyList()))
            } else {
                parser_expr()
            }
            accept_fix_err(")")
            listOf(Stmt.Emit(tk0, e))
        }
        accept_fix("spawn") -> {
            val n = G.N++
            if (check_fix("{")) {
                gen_proto_spawn(G.tk1!!, n, parser_stmt_block())
            } else {
                val tk = G.tk1!!
                val pro = parser_expr_4_prim()
                accept_fix_err("(")
                val args = parser_list(",",")") { parser_expr() }
                if (set == null) {
                    gen_spawn(tk, n, pro, args)
                } else {
                    listOf(
                        Stmt.SetS(set.tk, set, Stmt.Create(tk, pro)),
                        Stmt.Start(tk, set, args)
                    )
                }
            }
        }
        accept_fix("par") -> {
            val tk0 = G.tk0!!
            val l = mutableListOf(parser_stmt_block())
            check_fix_err("with")
            while (accept_fix("with")) {
                l.add(parser_stmt_block())
            }
            l.mapIndexed { i, ss ->
                gen_proto_spawn(tk0, G.N+i, ss)
            }.flatten() + listOf(
                Stmt.Await.Bool(Tk.Fix("false", tk0.pos)),
            )
        }
        accept_fix("par_and") -> {
            val tk0 = G.tk0!!
            val l = mutableListOf(parser_stmt_block())
            check_fix_err("with")
            val N = G.N
            while (accept_fix("with")) {
                l.add(parser_stmt_block())
            }
            (l.mapIndexed { i, ss ->
                gen_proto_spawn(tk0, N+i, ss)
            } +
            l.mapIndexed { i, ss ->
                listOf(Stmt.Await.Task(tk0, Expr.Acc(Tk.Var("mar_exe_${N+i}", tk0.pos))))
            }).flatten()
        }
        accept_fix("par_or") -> {
            val tk0 = G.tk0!!
            val l = mutableListOf(parser_stmt_block())
            check_fix_err("with")
            val N = G.N
            while (accept_fix("with")) {
                l.add(parser_stmt_block())
            }
            listOf(Stmt.Block(tk0, null,
                (l.mapIndexed { i, ss ->
                    gen_proto_spawn(tk0, N+i, ss)
                }).flatten() + listOf(
                    Stmt.Await.Any(tk0, l.mapIndexed { i, ss ->
                        Expr.Acc(Tk.Var("mar_exe_${N+i}", tk0.pos))
                    })
                )
            ))
        }
        accept_fix("every") -> {
            val tk0 = G.tk0!!
            val par = accept_fix("(")
            val awt = when {
                // await(%10min)
                accept_op("%") -> {
                    val ms = parser_expr()
                    Stmt.Await.Clock(tk0, ms)
                }
                // await(:X)
                accept_fix_err(":") -> {
                    val tp = parser_type(null, false, false) as Type.Data
                    val cnd = if (!accept_fix(",")) {
                        Expr.Bool(Tk.Fix("true", G.tk0!!.pos))
                    } else {
                        parser_expr()
                    }
                    Stmt.Await.Data(tk0, tp, cnd)
                }
                else -> error("impossible case")
            }
            if (par) {
                accept_fix_err(")")
            }
            val ss = parser_stmt_block()
            listOf(Stmt.Loop(tk0,
                Stmt.Block(tk0, null,
                    listOf(awt) + ss)
            ))
        }
        accept_fix("match") -> {
            val tk0 = G.tk0!!
            val tst = parser_expr()
            accept_fix_err("{")
            if (check_fix("}")) {
                err(G.tk1!!, "match error : unexpected \"}\"")
            }
            var tt: Boolean? = null
            val cases = parser_list(null, "}") {
                val cnd = if (accept_fix("else")) null else {
                    if (tt == true) {
                        check_enu_err("Type")
                    }
                    if (tt!=false && accept_fix(":")) {
                        val tp = parser_type(null, false, false)
                        if (tp !is Type.Data) {
                            err(tp.tk, "exception error : expected data type")
                        }
                        tt = true
                        tp
                    } else {
                        tt = false
                        parser_expr()
                    }
                }
                accept_fix_err("{")
                val se = Stmt.Block(G.tk0!!, null, parser_list(null, "}") {
                    parser_stmt()
                }.flatten())
                Pair(cnd, se)
            }
            if (tt == true) {
                listOf(Stmt.MatchT(tk0, tst, cases as List<Pair<Type.Data?,Stmt.Block>>))
            } else {
                listOf(Stmt.MatchE(tk0, tst, cases as List<Pair<Expr?,Stmt.Block>>))
            }
        }
        accept_fix("data") -> {
            val tk0 = G.tk0!!
            accept_enu_err("Type")
            val t = G.tk0 as Tk.Type
            if (!accept_fix(".")) {
                val xs = parser_tpls_abs()
                accept_fix_err(":")
                val tp = parser_type(null, false, false)
                listOf(Stmt.Data(tk0, t, xs, tp, null))
            } else {
                accept_op_err("*")
                val xs = parser_tpls_abs()
                accept_fix_err(":")
                val tp = if (!check_fix_err("[")) {
                    Type.Tuple(G.tk0!!, emptyList())
                } else {
                    parser_type(null, false, false) as Type.Tuple
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
                                parser_type(null, false, false) as Type.Tuple
                            }
                            Stmt.Data(xt, xt, emptyList(), xtp, f())
                        }
                    }
                }
                listOf(Stmt.Data(tk0, t, xs, tp, f()))
            }
        }
        accept_fix("test") -> {
            val tk0 = G.tk0!!
            val blk = parser_stmt_block()
            if (G.test) {
                listOf(Stmt.Block(tk0, null, blk))
            } else {
                emptyList()
            }
        }
        else -> TODO()
    }.let {
        if (!accept_fix("where")) it else {
            when {
                (it.size == 1) -> assert(it[0] !is Stmt.Dcl)
                (it.size == 2) -> assert(it[0] is Stmt.Dcl && it[1] !is Stmt.Dcl)
                else -> TODO("4")
            }

            val tk0 = G.tk0!!
            accept_fix_err("{")
            val ss = parser_list(null, "}") {
                accept_enu_err("Var")
                val id = G.tk0 as Tk.Var
                accept_op_err("=")
                val e = parser_expr()
                listOf (
                    Stmt.Dcl(tk0, id, null),
                    Stmt.SetE(G.tk0!!, Expr.Acc(id), e)
                )
            }.flatten()

            when {
                (it.size == 1) -> listOf(Stmt.Block(tk0, null, ss + it))
                (it.size == 2) -> listOf(it[0], Stmt.Block(tk0, null, ss + it.drop(1)))
                else -> error("impossible case")
            }
        }
     }
}
