package mar

fun <V> Stmt.dn_collect (fs: (Stmt)->List<V>?, fe: (Expr)->List<V>?, ft: (Type)->List<V>?): List<V> {
    val v = fs(this)
    if (v === null) {
        return emptyList()
    }
    return v + when (this) {
        is Stmt.Proto -> this.blk.dn_collect(fs,fe,ft) + this.tp.dn_collect(ft)
        is Stmt.Return -> this.e.dn_collect(fe)
        is Stmt.Block -> this.ss.map { it.dn_collect(fs,fe,ft) }.flatten()
        is Stmt.Dcl -> emptyList()
        is Stmt.Set -> this.dst.dn_collect(fe) + this.src.dn_collect(fe)
        is Stmt.If -> this.cnd.dn_collect(fe) + this.t.dn_collect(fs,fe,ft) + this.f.dn_collect(fs,fe,ft)

        is Stmt.Create -> this.dst.dn_collect(fe) + this.co.dn_collect(fe)
        is Stmt.Resume -> (this.dst?.dn_collect(fe) ?: emptyList()) + this.xco.dn_collect(fe) + this.arg.dn_collect(fe)
        is Stmt.Yield  -> (this.dst?.dn_collect(fe) ?: emptyList()) + this.arg.dn_collect(fe)

        is Stmt.Call -> this.call.dn_collect(fe)
        is Stmt.Nat -> emptyList()
    }
}
fun <V> Expr.dn_collect (fe: (Expr)->List<V>?): List<V> {
    val v = fe(this)
    if (v === null) {
        return emptyList()
    }
    return v + when (this) {
        is Expr.Uno -> this.e.dn_collect(fe)
        is Expr.Bin -> this.e1.dn_collect(fe) + this.e2.dn_collect(fe)
        is Expr.Tuple -> this.vs.map { it.dn_collect(fe) }.flatten()
        is Expr.Index -> this.col.dn_collect(fe)
        is Expr.Call -> this.f.dn_collect(fe) + this.args.map { it.dn_collect(fe) }.flatten()
        is Expr.Acc, is Expr.Nat, is Expr.Null, is Expr.Unit,
        is Expr.Bool, is Expr.Char, is Expr.Num -> emptyList()
    }
}
fun <V> Type.dn_collect (ft: (Type)->List<V>?): List<V> {
    val v = ft(this)
    if (v === null) {
        return emptyList()
    }
    return v + when (this) {
        is Type.Any -> emptyList()
        is Type.Unit -> emptyList()
        is Type.Basic -> emptyList()
        is Type.Pointer -> this.ptr.dn_collect(ft)
        is Type.Tuple -> this.ts.map { it.dn_collect(ft) }.flatten()
        is Type.Proto -> (this.inps + listOf(this.out)).map { it.dn_collect(ft) }.flatten()
        is Type.XCoro -> (this.inps + listOf(this.out)).map { it.dn_collect(ft) }.flatten()
    }
}

fun Stmt.dn_visit (fs: (Stmt)->Unit?, fe: (Expr)->Unit?, ft: (Type)->Unit?) {
    this.dn_collect (
        { if (fs(it) == null) null else emptyList<Unit>() },
        { if (fe(it) == null) null else emptyList() },
        { if (ft(it) == null) null else emptyList() }
    )
}
fun Expr.dn_visit (fe: (Expr)->Unit?) {
    this.dn_collect { if (fe(it) == null) null else emptyList<Unit>() }
}

fun Stmt.dn_filter (fs: (Stmt)->Boolean?, fe: (Expr)->Boolean?, ft: (Type)->Boolean?): List<Any> {
    return this.dn_collect (
        {
            when (fs(it)) {
                null -> null
                false -> emptyList()
                true -> listOf(it)
            }
        },
        {
            when (fe(it)) {
                null -> null
                false -> emptyList()
                true -> listOf(it)
            }
        },
        {
            when (ft(it)) {
                null -> null
                false -> emptyList()
                true -> listOf(it)
            }
        },
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
