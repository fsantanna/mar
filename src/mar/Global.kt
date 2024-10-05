package mar

import java.io.File
import java.io.Reader
import java.util.*

val VALGRIND = ""
val THROW = false
var DUMP = true
val PATH = File(File(System.getProperty("java.class.path")).absolutePath).parent

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
        "nil", "set", "true",
        "val", "var",
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
    data class Nat (val str_: String, val pos_: Pos, val tag: String?, val n_: Int=G.N++): Tk(str_, pos_)
}

sealed class Expr (var n: Int, val tk: Tk) {
    data class Nat  (val tk_: Tk.Nat): Expr(G.N++, tk_)
    data class Acc  (val tk_: Tk.Var, val ign: Boolean=false): Expr(G.N++, tk_)
    data class Bool (val tk_: Tk.Fix): Expr(G.N++, tk_)
    data class Char (val tk_: Tk.Chr): Expr(G.N++, tk_)
    data class Num  (val tk_: Tk.Num): Expr(G.N++, tk_)

    data class Bin  (val tk_: Tk.Op, val e1: Expr, val e2: Expr): Expr(G.N++, tk_)
}

sealed class Stmt (var n: Int, val tk: Tk) {
    data class Proto   (val tk_: Tk.Fix, val nst: Boolean, val fake: Boolean, val tag: Tk.Type?, val pars: List<Stmt.Dcl>, val blk: Do): Stmt(G.N++, tk_)
    data class Do      (val tk_: Tk, val es: List<Stmt>) : Stmt(G.N++, tk_)
    data class Group   (val tk_: Tk.Fix, val es: List<Stmt>) : Stmt(G.N++, tk_)
    data class Enclose (val tk_: Tk.Fix, val tag: Tk.Type, val es: List<Stmt>): Stmt(G.N++, tk_)
    data class Escape  (val tk_: Tk.Fix, val tag: Tk.Type, val e: Stmt?): Stmt(G.N++, tk_)
    data class Dcl     (val tk_: Tk.Fix, val lex: Boolean, /*val poly: Boolean,*/ val idtag: Var_Type, val src: Stmt?):  Stmt(G.N++, tk_)
    data class Set     (val tk_: Tk.Fix, val dst: Stmt, /*val poly: Tk.Tag?,*/ val src: Stmt): Stmt(G.N++, tk_)
    data class If      (val tk_: Tk.Fix, val cnd: Stmt, val t: Stmt, val f: Stmt): Stmt(G.N++, tk_)
    data class Loop    (val tk_: Tk.Fix, val blk: Stmt): Stmt(G.N++, tk_)
    data class Data    (val tk_: Tk.Type, val ids: List<Var_Type>): Stmt(G.N++, tk_)
    data class Drop    (val tk_: Tk.Fix, val e: Stmt): Stmt(G.N++, tk_)

    data class Catch   (val tk_: Tk.Fix, val tag: Tk.Type?, val blk: Stmt.Do): Stmt(G.N++, tk_)
    data class Defer   (val tk_: Tk.Fix, val blk: Stmt.Do): Stmt(G.N++, tk_)

    data class Yield   (val tk_: Tk.Fix, val e: Stmt): Stmt(G.N++, tk_)
    data class Resume  (val tk_: Tk.Fix, val co: Stmt, val args: List<Stmt>): Stmt(G.N++, tk_)

    data class Spawn   (val tk_: Tk.Fix, val tsks: Stmt?, val tsk: Stmt, val args: List<Stmt>): Stmt(G.N++, tk_)
    data class Delay   (val tk_: Tk.Fix): Stmt(G.N++, tk_)
    data class Pub     (val tk_: Tk, val tsk: Stmt?): Stmt(G.N++, tk_)
    data class Toggle  (val tk_: Tk.Fix, val tsk: Stmt, val on: Stmt): Stmt(G.N++, tk_)
    data class Tasks   (val tk_: Tk.Fix, val max: Stmt): Stmt(G.N++, tk_)
}

typealias Node = Int

object G {
    var N: Int = 1

    var tks: Iterator<Tk>? = null
    var tk0: Tk? = null
    var tk1: Tk? = null

    var outer: Expr? = null
    var ns: MutableMap<Node,Stmt> = mutableMapOf()
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
    if (verbose) {
        System.err.println("... parsing ...")
    }
    G.reset()
    G.tks = inps.lexer()
    parser_lexer()
    G.outer = try {
        parser_expr()
    } catch (e: Throwable) {
        if (THROW) {
            throw e
        }
        return e.message!! + "\n"
    }
    //println(es.to_str())
    val c = try {
        if (verbose) {
            System.err.println("... mar -> c ...")
        }
        coder_main()
    } catch (e: Throwable) {
        if (THROW) {
            throw e
        }
        return e.message!! + "\n"
    }
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
