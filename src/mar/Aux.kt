package mar

fun <V> Stmt.dn_collect (fs: (Stmt)->List<V>?, fe: (Expr)->List<V>?, ft: (Type)->List<V>?): List<V> {
    val v = fs(this)
    if (v === null) {
        return emptyList()
    }
    return v + when (this) {
        is Stmt.Proto -> this.blk.dn_collect(fs,fe,ft) + this.tp.dn_collect(ft)
        is Stmt.Return -> this.e.dn_collect(fe)
        is Stmt.Block -> this.vs.map { (_,tp) -> tp.dn_collect(ft) }.flatten() + this.ss.map { it.dn_collect(fs,fe,ft) }.flatten()
        is Stmt.Set -> this.dst.dn_collect(fe) + this.src.dn_collect(fe)
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
        is Expr.Spawn -> this.co.dn_collect(fe) + this.args.map { it.dn_collect(fe) }.flatten()
        is Expr.Resume -> this.xco.dn_collect(fe) + this.args.map { it.dn_collect(fe) }.flatten()
        is Expr.Yield -> this.arg.dn_collect(fe)
        is Expr.Uno -> this.e.dn_collect(fe)
        is Expr.Bin -> this.e1.dn_collect(fe) + this.e2.dn_collect(fe)
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
        is Type.Basic -> emptyList()
        is Type.Pointer -> this.ptr.dn_collect(ft)
        is Type.Proto.Coro -> this.inps_.map { it.dn_collect(ft) }.flatten() + this.res.dn_collect(ft) + this.out_.dn_collect(ft)
        is Type.Proto.Func -> this.inps_.map { it.dn_collect(ft) }.flatten() + this.out_.dn_collect(ft)
        is Type.Unit -> emptyList()
    }
}

fun Stmt.dn_visit (fs: (Stmt)->Unit, fe: (Expr)->Unit, ft: (Type)->Unit) {
    this.dn_collect({ fs(it) ; emptyList<Unit>() }, { fe(it) ; emptyList<Unit>() }, { ft(it) ; emptyList<Unit>() })
}
fun Expr.dn_visit (f: (Expr)->Unit) {
    this.dn_collect { f(it) ; emptyList<Unit>() }
}

fun trap (f: ()->Unit): String {
    try {
        f()
        error("impossible case")
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
