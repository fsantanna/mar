package mar

fun <A,B> Pair<A,B>?.nulls (): Pair<A?,B?> {
    return if (this == null) Pair(null, null) else this
}

fun <T> T.dump(): T {
    val s = when (this) {
        is Type -> this.to_str()
        is Expr -> this.to_str()
        is Stmt -> this.to_str()
        else -> this.toString()
    }
    println(s)
    return this
}

fun trap (f: ()->Unit): String? {
    try {
        f()
        return null
    } catch (e: Throwable) {
        if (THROW) {
            throw e
        }
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

fun Array<String>.cmds_opts () : Pair<List<String>,Map<String,List<String>>> {
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
        .groupBy { it.first }
        .map { (k,vs) -> Pair(k, vs.map { it.second }.filter { !it.isNullOrBlank() }.let { it as List<String> }) }
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
