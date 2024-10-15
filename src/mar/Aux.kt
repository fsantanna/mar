package mar

fun <V> Stmt.dn_collect_pos (fs: ((Stmt)->List<V>)?, fe: ((Expr)->List<V>)?, ft: ((Type)->List<V>)?): List<V> {
    if (fs == null) {
        return emptyList()
    }
    return when (this) {
        is Stmt.Proto -> this.blk.dn_collect_pos(fs,fe,ft) + this.tp.dn_collect_pos(ft)
        is Stmt.Return -> this.e.dn_collect_pos(fe)

        is Stmt.Block -> this.ss.map { it.dn_collect_pos(fs,fe,ft) }.flatten()
        is Stmt.Dcl -> this.var_type.second.dn_collect_pos(ft)
        is Stmt.Set -> this.dst.dn_collect_pos(fe) + this.src.dn_collect_pos(fe)

        is Stmt.If    -> this.cnd.dn_collect_pos(fe) + this.t.dn_collect_pos(fs,fe,ft) + this.f.dn_collect_pos(fs,fe,ft)
        is Stmt.Loop  -> this.blk.dn_collect_pos(fs,fe,ft)
        is Stmt.Break -> emptyList()

        is Stmt.Create -> this.dst.dn_collect_pos(fe) + this.co.dn_collect_pos(fe)
        is Stmt.Start  -> (this.dst?.dn_collect_pos(fe) ?: emptyList()) + this.exe.dn_collect_pos(fe) + this.args.map { it.dn_collect_pos(fe) }.flatten()
        is Stmt.Resume -> (this.dst?.dn_collect_pos(fe) ?: emptyList()) + this.exe.dn_collect_pos(fe) + this.arg.dn_collect_pos(fe)
        is Stmt.Yield  -> (this.dst?.dn_collect_pos(fe) ?: emptyList()) + this.arg.dn_collect_pos(fe)

        is Stmt.Call -> this.call.dn_collect_pos(fe)
        is Stmt.Nat -> emptyList()
    } + fs(this)
}

fun <V> Expr.dn_collect_pos (fe: ((Expr)->List<V>)?): List<V> {
    if (fe == null) {
        return emptyList()
    }
    return when (this) {
        is Expr.Uno   -> this.e.dn_collect_pos(fe)
        is Expr.Bin   -> this.e1.dn_collect_pos(fe) + this.e2.dn_collect_pos(fe)
        is Expr.Tuple -> this.vs.map { it.dn_collect_pos(fe) }.flatten()
        is Expr.Union -> this.v.dn_collect_pos(fe)
        is Expr.Index -> this.col.dn_collect_pos(fe)
        is Expr.Disc  -> this.col.dn_collect_pos(fe)
        is Expr.Pred  -> this.col.dn_collect_pos(fe)
        is Expr.Call  -> this.f.dn_collect_pos(fe) + this.args.map { it.dn_collect_pos(fe) }.flatten()
        is Expr.Acc, is Expr.Nat, is Expr.Null, is Expr.Unit,
        is Expr.Bool, is Expr.Char, is Expr.Num -> emptyList()
    } + fe(this)
}

fun <V> Type.dn_collect_pos (ft: ((Type)->List<V>)?): List<V> {
    if (ft == null) {
        return emptyList()
    }
    return when (this) {
        is Type.Any -> emptyList()
        is Type.Unit -> emptyList()
        is Type.Basic -> emptyList()
        is Type.Pointer -> this.ptr.dn_collect_pos(ft)
        is Type.Tuple -> this.ts.map { it.dn_collect_pos(ft) }.flatten()
        is Type.Union -> this.ts.map { it.dn_collect_pos(ft) }.flatten()
        is Type.Proto.Func -> (this.inps + listOf(this.out)).map { it.dn_collect_pos(ft) }.flatten()
        is Type.Proto.Coro -> (this.inps + listOf(this.res, this.yld, this.out)).map { it.dn_collect_pos(ft) }.flatten()
        is Type.Exec -> (this.inps + listOf(this.res, this.yld, this.out)).map { it.dn_collect_pos(ft) }.flatten()
    } + ft(this)
}

fun Stmt.dn_visit_pos (fs: ((Stmt)->Unit)?, fe: ((Expr)->Unit)?, ft: ((Type)->Unit)?) {
    this.dn_collect_pos (
        if (fs == null) null else ({ fs(it) ; emptyList<Unit>() }),
        if (fe == null) null else ({ fe(it) ; emptyList() }),
        if (ft == null) null else ({ ft(it) ; emptyList() })
    )
}
fun Expr.dn_visit_pos (fe: ((Expr)->Unit)?) {
    this.dn_collect_pos (
        if (fe == null) null else ({ fe(it) ; emptyList<Unit>() })
    )
}

fun <V> Stmt.dn_collect_pre (fs: ((Stmt)->List<V>?)?, fe: ((Expr)->List<V>?)?, ft: ((Type)->List<V>?)?): List<V> {
    if (fs == null) {
        return emptyList()
    }
    val v = fs(this)
    if (v === null) {
        return emptyList()
    }
    return v + when (this) {
        is Stmt.Proto -> this.blk.dn_collect_pre(fs,fe,ft) + this.tp.dn_collect_pre(ft)
        is Stmt.Return -> this.e.dn_collect_pre(fe)

        is Stmt.Block -> this.ss.map { it.dn_collect_pre(fs,fe,ft) }.flatten()
        is Stmt.Dcl -> this.var_type.second.dn_collect_pre(ft)
        is Stmt.Set -> this.dst.dn_collect_pre(fe) + this.src.dn_collect_pre(fe)

        is Stmt.If    -> this.cnd.dn_collect_pre(fe) + this.t.dn_collect_pre(fs,fe,ft) + this.f.dn_collect_pre(fs,fe,ft)
        is Stmt.Loop  -> this.blk.dn_collect_pre(fs,fe,ft)
        is Stmt.Break -> emptyList()

        is Stmt.Create -> this.dst.dn_collect_pre(fe) + this.co.dn_collect_pre(fe)
        is Stmt.Start  -> (this.dst?.dn_collect_pre(fe) ?: emptyList()) + this.exe.dn_collect_pre(fe) + this.args.map { it.dn_collect_pre(fe) }.flatten()
        is Stmt.Resume -> (this.dst?.dn_collect_pre(fe) ?: emptyList()) + this.exe.dn_collect_pre(fe) + this.arg.dn_collect_pre(fe)
        is Stmt.Yield  -> (this.dst?.dn_collect_pre(fe) ?: emptyList()) + this.arg.dn_collect_pre(fe)

        is Stmt.Call -> this.call.dn_collect_pre(fe)
        is Stmt.Nat -> emptyList()
    }
}

fun <V> Expr.dn_collect_pre (fe: ((Expr)->List<V>?)?): List<V> {
    if (fe == null) {
        return emptyList()
    }
    val v = fe(this)
    if (v === null) {
        return emptyList()
    }
    return v + when (this) {
        is Expr.Uno   -> this.e.dn_collect_pre(fe)
        is Expr.Bin   -> this.e1.dn_collect_pre(fe) + this.e2.dn_collect_pre(fe)
        is Expr.Tuple -> this.vs.map { it.dn_collect_pre(fe) }.flatten()
        is Expr.Union -> this.v.dn_collect_pre(fe)
        is Expr.Index -> this.col.dn_collect_pre(fe)
        is Expr.Disc  -> this.col.dn_collect_pre(fe)
        is Expr.Pred  -> this.col.dn_collect_pre(fe)
        is Expr.Call  -> this.f.dn_collect_pre(fe) + this.args.map { it.dn_collect_pre(fe) }.flatten()
        is Expr.Acc, is Expr.Nat, is Expr.Null, is Expr.Unit,
        is Expr.Bool, is Expr.Char, is Expr.Num -> emptyList()
    }
}

fun <V> Type.dn_collect_pre (ft: ((Type)->List<V>?)?): List<V> {
    if (ft == null) {
        return emptyList()
    }
    val v = ft(this)
    if (v === null) {
        return emptyList()
    }
    return v + when (this) {
        is Type.Any -> emptyList()
        is Type.Unit -> emptyList()
        is Type.Basic -> emptyList()
        is Type.Pointer -> this.ptr.dn_collect_pre(ft)
        is Type.Tuple -> this.ts.map { it.dn_collect_pre(ft) }.flatten()
        is Type.Union -> this.ts.map { it.dn_collect_pre(ft) }.flatten()
        is Type.Proto.Func -> (this.inps + listOf(this.out)).map { it.dn_collect_pre(ft) }.flatten()
        is Type.Proto.Coro -> (this.inps + listOf(this.res,this.yld,this.out)).map { it.dn_collect_pre(ft) }.flatten()
        is Type.Exec -> (this.inps + listOf(this.res,this.yld,this.out)).map { it.dn_collect_pre(ft) }.flatten()
    }
}

fun Stmt.dn_visit_pre (fs: ((Stmt)->Unit?)?, fe: ((Expr)->Unit?)?, ft: ((Type)->Unit?)?) {
    this.dn_collect_pre (
        if (fs == null) null else ({ if (fs(it) == null) null else emptyList<Unit>() }),
        if (fe == null) null else ({ if (fe(it) == null) null else emptyList() }),
        if (ft == null) null else ({ if (ft(it) == null) null else emptyList() })
    )
}
fun Expr.dn_visit_pre (fe: ((Expr)->Unit?)?) {
    this.dn_collect_pre (
        if (fe == null) null else ({ if (fe(it) == null) null else emptyList<Unit>() })
    )
}

fun Stmt.dn_filter_pre (fs: ((Stmt)->Boolean?)?, fe: ((Expr)->Boolean?)?, ft: ((Type)->Boolean?)?): List<Any> {
    return this.dn_collect_pre (
        if (fs == null) null else (
            {
                when (fs(it)) {
                    null -> null
                    false -> emptyList()
                    true -> listOf(it)
                }
            }
        ),
        if (fe == null) null else (
            {
                when (fe(it)) {
                    null -> null
                    false -> emptyList()
                    true -> listOf(it)
                }
            }
        ),
        if (ft == null) null else (
            {
                when (ft(it)) {
                    null -> null
                    false -> emptyList()
                    true -> listOf(it)
                }
            }
        ),
    )
}

fun trap (f: ()->Unit): String? {
    try {
        f()
        return null
    } catch (e: Throwable) {
        return e.message!!
    }
}

fun Pos.is_same_line (oth: Pos): Boolean {
    return (this.file==oth.file && this.lin==oth.lin && this.brks==oth.brks)
}

fun <T> T?.cond2 (f: (v:T)->String, g: (()->String)?): String {
    return when (this) {
        false, null -> if (g !== null) g() else ""
        else  -> f(this)
    }
}

fun <T> T?.cond (f: (v:T)->String): String {
    return this.cond2(f) {""}
}
fun String.quote (n: Int): String {
    return this
        .replace('\n',' ')
        .replace('"','.')
        .replace('\\','.')
        .let {
            if (it.length<=n) it else it.take(n-3)+"..."
        }

}

fun err (pos: Pos, str: String): Nothing {
    error(pos.file + " : (lin ${pos.lin}, col ${pos.col}) : $str")
}
fun err (tk: Tk, str: String): Nothing {
    err(tk.pos, str)
}
fun err_expected (tk: Tk, str: String): Nothing {
    val have = when {
        (tk is Tk.Eof) -> "end of file"
        else -> '"' + tk.str + '"'
    }
    err(tk, "expected $str : have $have")
}

fun Array<String>.cmds_opts () : Pair<List<String>,Map<String,String?>> {
    val cmds = this.filter { !it.startsWith("--") }
    val opts = this
        .filter { it.startsWith("--") }
        .map {
            if (it.contains('=')) {
                val (k,v) = Regex("(--.*)=(.*)").find(it)!!.destructured
                Pair(k,v)
            } else {
                Pair(it, null)
            }
        }
        .toMap()
    return Pair(cmds,opts)
}

fun String.idc (): String {
    return when {
        (this[0] == '{') -> {
            val MAP = mapOf(
                Pair('+', "plus"),
                Pair('-', "minus"),
                Pair('*', "asterisk"),
                Pair('/', "slash"),
                Pair('>', "greater"),
                Pair('<', "less"),
                Pair('=', "equals"),
                Pair('!', "exclaim"),
                Pair('|', "bar"),
                Pair('&', "ampersand"),
                Pair('#', "hash"),
            )
            this.drop(2).dropLast(2).toList().map { MAP[it] }.joinToString("_")
        }
        else -> {
            val MAP = mapOf(
                Pair(':', ""),
                Pair('.', "_"),
                Pair('-', "_dash_"),
                Pair('\'', "_plic_"),
                Pair('?', "_question_"),
                Pair('!', "_bang_"),
            )
            this.toList().map { MAP[it] ?: it }.joinToString("")
        }
    }
}
