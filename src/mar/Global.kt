package mar

import java.io.File
import java.io.Reader
import java.util.*

val VALGRIND = ""
val THROW = false
var DUMP = true
val PATH = File(File(System.getProperty("java.class.path")).absolutePath).parent
val D = "\$"

val OPERATORS = Pair (
    setOf('+', '-', '*', '/', '%', '>', '<', '=', '|', '&'),
    setOf(
        "==", "!=",
        ">", "<", ">=", "<=",
        "||", "&&",
        "+", "-", "*", "/", "%",
    )
)

val KEYWORDS: SortedSet<String> = (
    setOf (
        "data", "do", "else",
        "false", "func'", "group", "if",
        "set", "true",
        "null",
    ).toSortedSet()
)

typealias Var_Type  = Pair<Tk.Var,Tk.Type>

sealed class Tk (val str: String, val pos: Pos) {
    data class Eof (val pos_: Pos, val n_: Int=G.N++): Tk("", pos_)
    data class Fix (val str_: String, val pos_: Pos, val n_: Int=G.N++): Tk(str_, pos_)
    data class Type (val str_: String, val pos_: Pos, val n_: Int=G.N++): Tk(str_, pos_)
    data class Op  (val str_: String, val pos_: Pos, val n_: Int=G.N++): Tk(str_, pos_)
    data class Var  (val str_: String, val pos_: Pos, val n_: Int=G.N++): Tk(str_, pos_)  // up: 0=var, 1=upvar, 2=upref
    data class Num (val str_: String, val pos_: Pos, val n_: Int=G.N++): Tk(str_, pos_)
    data class Chr (val str_: String, val pos_: Pos, val n_: Int=G.N++): Tk(str_, pos_)
    data class Nat (val str_: String, val pos_: Pos, val n_: Int=G.N++): Tk(str_, pos_)
}

sealed class Expr (var n: Int, val tk: Tk) {
    data class Nat  (val tk_: Tk.Nat): Expr(G.N++, tk_)
    data class Acc  (val tk_: Tk.Var, val ign: Boolean=false): Expr(G.N++, tk_)
    data class Bool (val tk_: Tk.Fix): Expr(G.N++, tk_)
    data class Char (val tk_: Tk.Chr): Expr(G.N++, tk_)
    data class Num  (val tk_: Tk.Num): Expr(G.N++, tk_)
    data class Null (val tk_: Tk.Fix): Expr(G.N++, tk_)

    data class Bin  (val tk_: Tk.Op, val e1: Expr, val e2: Expr): Expr(G.N++, tk_)
    data class Call (val tk_: Tk, val f: Expr, val args: List<Expr>): Expr(G.N++, tk_)
}

sealed class Stmt (var n: Int, val tk: Tk) {
    data class Block   (val tk_: Tk, val vs: List<Var_Type>, val ss: List<Stmt>) : Stmt(G.N++, tk_)
    data class Set     (val tk_: Tk.Fix, val dst: Expr, val src: Expr): Stmt(G.N++, tk_)

    data class Nat     (val tk_: Tk.Nat): Stmt(G.N++, tk_)
    data class Call    (val tk_: Tk, val call: Expr.Call): Stmt(G.N++, tk_)
}

typealias Node = Int

object G {
    var N: Int = 1

    var tks: Iterator<Tk>? = null
    var tk0: Tk? = null
    var tk1: Tk? = null

    var outer: Stmt? = null
    var ns: MutableMap<Node,Any> = mutableMapOf()
    var ups: MutableMap<Node,Node> = mutableMapOf()
    var tags: MutableMap<String,Tk.Type> = mutableMapOf()
    val datas = mutableMapOf<String,List<Var_Type>>()
    val nats: MutableMap<Node,Pair<List<Node>,String>> = mutableMapOf()
    var nonlocs: MutableMap<Node,List<Node>> = mutableMapOf()
    val mems: MutableSet<Stmt>  = mutableSetOf()

    fun reset () {
        N = 1

        tks = null
        tk0 = null
        tk1 = null

        outer = null
        ns.clear()
        ups.clear()
        tags.clear()
        datas.clear()
        nats.clear()
        nonlocs.clear()
        mems.clear()
    }
}

fun exec (hold: Boolean, cmds: List<String>): Pair<Boolean,String> {
    //System.err.println(cmds.joinToString(" "))
    val (x,y) = if (hold) {
        Pair(ProcessBuilder.Redirect.PIPE, true)
    } else {
        Pair(ProcessBuilder.Redirect.INHERIT, false)
    }
    val p = ProcessBuilder(cmds)
        .redirectOutput(x)
        .redirectError(x)
        .redirectErrorStream(y)
        .start()
    val ret = p.waitFor()
    val str = p.inputStream.bufferedReader().readText()
    return Pair(ret==0, str)
}
fun exec (hold: Boolean, cmd: String): Pair<Boolean,String> {
    return exec(hold, cmd.split(' '))
}

fun all (tst: Boolean, verbose: Boolean, inps: List<Pair<Triple<String, Int, Int>, Reader>>, out: String, args: List<String>): String {
    G.reset()
    G.tks = inps.lexer()
    parser_lexer()

    if (verbose) {
        System.err.println("... parsing ...")
    }
    try {
        check_fix("do")
        G.outer = parser_stmt() as Stmt.Block
        check_enu_err("Eof")
        cache_ns()
        cache_ups()
        check_vars()
        check_types()
    } catch (e: Throwable) {
        if (THROW) {
            throw e
        }
        return e.message!! + "\n"
    }


    if (verbose) {
        System.err.println("... mar -> c ...")
    }
    val c = coder_main(false)

    if (verbose) {
        System.err.println("... c -> exe ...")
    }
    File("$out.c").writeText(c)
    val cmd = listOf("gcc", "-Werror", "$out.c", "-l", "m", "-o", "$out.exe") + args
    if (verbose) {
        System.err.println("\t" + cmd.joinToString(" "))
    }
    val (ok2, out2) = exec(true, cmd)
    if (!ok2) {
        return out2
    }
    if (verbose) {
        System.err.println("... executing ...")
    }
    val (_, out3) = exec(tst, "$VALGRIND./$out.exe")
    //println(out3)
    return out3
}

fun test (inp: String, pre: Boolean=false): String {
    //println(inp)
    val prelude = "build/prelude.mar"
    val inps = listOf(Pair(Triple("anon",1,1), inp.reader())) + if (!pre) emptyList() else {
        listOf(Pair(Triple(prelude,1,1), File(prelude).reader()))
    }
    return all(true, false, inps, "out", emptyList())
}
