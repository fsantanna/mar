package mar

import java.io.File
import java.io.PushbackReader
import java.io.Reader
import java.io.StringReader

data class Lex (
    var file: String?,
    var lin: Int, var col: Int, var brks: Int,
    var prv: Int, // previous col before \n to restore on unread
    val reader: PushbackReader
)
data class Pos (val file: String, val lin: Int, val col: Int, val brks: Int)

fun Lex.toPos (): Pos {
    return Pos(this.file ?: "anon", this.lin, this.col, this.brks)
}

fun FileX (name: String, cur: String?): File? {
    val c = name.firstOrNull()
    val path = when {
        (c == null) -> return null
        (c == '/') -> name
        (c == '@') -> PATH + "/" + name.drop(1)
        (cur != null) -> cur + "/" + name
        else -> return null
    }
    //println(path)
    val h = File(path)
    if (h.exists() && h.isFile) {
        return h
    }
    return null
}

// TODO: reads 65535 after unreading -1
fun iseof (n: Int): Boolean {
    return (n==-1 || n==65535)
}

typealias Lexer = List<Pair<Triple<String?,Int,Int>,Reader>>

fun String.lexer (): Iterator<Tk> {
    val lex: Lexer = listOf(Pair(Triple(null,1,1), this.reader()))
    return lex.lexer()
}

fun Lexer.lexer (): Iterator<Tk> = sequence {
    val stack = ArrayDeque<Lex>()
    val comms = ArrayDeque<String>()

    for (inp in this@lexer) {
        stack.addFirst(Lex(inp.first.first, inp.first.second, inp.first.third, 0, 0, PushbackReader(inp.second,2)))
    }

    fun read2 (): Pair<Int,Char> {
        val pos = stack.first()
        val n = pos.reader.read()
        val x = n.toChar()
        pos.prv = pos.col
        when {
            (x == '\n') -> { pos.lin++; pos.col=1}
            (x == ';')  -> { pos.col++ ; pos.brks++ }
            !iseof(n)   -> pos.col++
        }
        return Pair(n,x)
    }
    fun unread2 (n: Int) {
        val pos = stack.first()
        val x = n.toChar()
        pos.reader.unread(n)
        when {
            iseof(n) -> {}
            (x == ';') -> pos.brks--
            (x == '\n') -> { pos.lin--; pos.col=pos.prv }
            else -> pos.col = pos.col-1
        }
    }

    fun read2Until (f: (n: Int, x: Char)->Boolean): String? {
        var ret = ""
        while (true) {
            val (n,x) = read2()
            when {
                iseof(n) -> return null
                f(n,x) -> {
                    //ret += x
                    break
                }
                else -> ret += x
            }
            assert(n != -1)
        }
        return ret
    }
    fun read2Until (x: Char): String? {
        return read2Until { _,c -> c == x }
    }
    fun read2While (f: (x: Char)->Boolean): String {
        var ret = ""
        while (true) {
            val (n,x) = read2()
            when {
                f(x) -> ret += x
                else -> {
                    unread2(n)
                    break
                }
            }
            assert(!iseof(n))
        }
        return ret
    }
    fun read2While (x: Char): String {
        return read2While { it == x }
    }

    fun next (): Pair<Char?, Pos> {
        while (true) {
            val lex = stack.first()
            val pos = lex.toPos()
            val (n1,x1) = read2()
            when (x1) {
                ' ', '\t', '\n' -> {}
                ';' -> {
                    val (n2,x2) = read2()
                    if (x2 == ';') {
                        val x3 = ";;" + read2While(';')
                        if (x3 == ";;") {
                            read2Until { n,c -> iseof(n) || c=='\n' }
                        } else {
                            var x4 = x3
                            outer@ while (true) {
                                if (comms.firstOrNull() == x4) {
                                    comms.removeFirst()
                                    if (comms.size == 0) {
                                        break
                                    }
                                } else {
                                    comms.addFirst(x4)
                                }
                                do {
                                    if (read2Until(';') == null) {
                                        break@outer
                                    }
                                    x4 = ";" + read2While(';')
                                } while (x4.length<=2 || x4.length<comms.first().length)
                            }
                        }
                    } else {
                        unread2(n2)
                    }
                }
                else -> {
                    return if (iseof(n1)) {
                        Pair(null, pos)
                    } else {
                        Pair(x1, pos)
                    }
                }
            }
        }
    }

    var prv: Char? = null
    while (true) {
        val (x,pos) = next()
        when {
            (x == null) -> {
                if (stack.size > 1) {
                    stack.removeFirst()
                } else {
                    yield(Tk.Eof(pos))
                    break
                }
            }
            (x in listOf('{','}','(',')','[',']',',','\$','.',':')) -> yield(Tk.Fix(x.toString(), pos))
            (x == '^') -> {
                val id = read2While { a -> a.isLetter() }
                when (id) {
                     "include" -> {
                        val (_, x2) = read2()
                        if (x2 != '(') {
                            err(pos, "include error : expected \"(\"")
                        }
                        val f = read2Until(')')
                        if (f == null) {
                            err(pos, "include error : exoected \")\"")
                        }
                        val h = FileX(f, stack.first().file.let { if (it==null) null else File(it).parentFile?.toString() })
                        if (h == null) {
                            err(pos, "include error : file not found : $f")
                        }
                        stack.addFirst(Lex(f, 1, 1, 0, 0, PushbackReader(StringReader(h.readText()), 2)))
                    }
                    "compile" -> {
                        val (_, x2) = read2()
                        if (x2 != '(') {
                            err(pos, "compile error : expected \"(\"")
                        }
                        val f = read2Until(')')
                        if (f == null) {
                            err(pos, "compile error : exoected \")\"")
                        }
                        G.ccs.addAll(f.split(" "))
                    }
                    else -> err(pos, "preprocessor error : unexpected \"$id\"")
                }
            }
            (x == '#') -> {
                val (n1,x1) = read2()
                when {
                    (x1 == '[') -> yield(Tk.Fix("$x[", pos))
                    (x1 == '#') -> yield(Tk.Op("##", pos))
                    else -> {
                        unread2(n1)
                        yield(Tk.Op(x.toString(), pos))
                    }
                }
            }
            (x in OPERATORS.first) -> {
                val op = x + read2While { it in OPERATORS.first }
                when {
                    (op == "=") -> yield(Tk.Fix(op, pos))
                    (op == "=>") -> yield(Tk.Fix(op, pos))
                    (op == "->") -> yield(Tk.Fix(op, pos))
                    (op in OPERATORS.second) -> yield(Tk.Op(op, pos))
                    (op == "<>") -> {   // special case union <>
                        yield(Tk.Op("<", pos))
                        yield(Tk.Op(">", pos.copy(lin=pos.col+1)))
                    }
                    else -> {
                        for (i in 0..op.length-1) {
                            yield(Tk.Op(op[i].toString(), pos.copy(col=pos.col+i)))
                        }
                    }
                }
            }
            (x.isLetter() || x=='_') -> {
                val id = x + read2While { a -> a.isLetterOrDigit() || a=='_' }
                when {
                    KEYWORDS.contains(id) -> yield(Tk.Fix(id, pos))
                    x.isUpperCase() -> yield(Tk.Type(id, pos))
                    else -> yield(Tk.Var(id, pos))
                }
            }
            x.isDigit() -> {
                val num = x + (if (prv in listOf('.','!','?')) {
                    read2While { it.isDigit() }
                } else {
                    read2While { it == '.' || it.isLetterOrDigit() }
                })
                yield(Tk.Num(num, pos))
            }
            (x == '`') -> {
                val open = x + read2While('`')
                var close = ""
                val nat = read2Until { _,c ->
                    if (c == '`') {
                        close = close + c
                    } else {
                        close = ""
                    }
                    (close == open)
                }
                if (nat == null) {
                    err(pos, "token error : unterminated \"$open\"")
                }
                yield(Tk.Nat(nat.dropLast(close.length-1), pos))
            }
            (x == '\'') -> {
                val (n2,x2) = read2()
                if (iseof(n2)) {
                    err(stack.first().toPos(), "char error : expected '")
                }
                val c = if (x2 != '\\') x2.toString() else {
                    val (n3,x3) = read2()
                    if (iseof(n3)) {
                        err(stack.first().toPos(), "char error : expected '")
                    }
                    x2.toString()+x3
                }
                val (n3,x3) = read2()
                if (iseof(n3) || x3!='\'') {
                    err(stack.first().toPos(), "char error : expected '")
                }
                yield(Tk.Chr("'$c'", pos))
            }
            (x == '"') -> {
                var n = 0
                var brk = false
                val v = read2Until { _,c ->
                    brk = (c=='"' && n%2==0)
                    n = if (c == '\\') n+1 else 0
                    brk
                }
                if (v == null) {
                    err(pos, "string error : unterminated \"")
                }
                var str = "\""
                var i = 0
                while (i < v.length) {
                    val z = v[i]
                    val zz = when {
                        (z == '\'') -> "\\'"
                        (z != '\\') -> z.toString()
                        else -> {
                            i++
                            z.toString() + v[i]
                        }
                    }
                    str += zz
                    i++
                }
                yield(Tk.Str(str+'"', pos))
            }
            else -> {
                err(pos, "token error : unexpected $x")
            }
        }
        prv = x
    }
}.iterator()
