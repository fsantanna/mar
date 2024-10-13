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
    setOf('+', '-', '*', '/', '%', '>', '<', '=', '|', '&', '\\', '?', '!'),
    setOf(
        "==", "!=",
        ">", "<", ">=", "<=",
        "||", "&&",
        "+", "-", "*", "/", "%",
        "\\", "?", "!",
    )
)

val BINS = listOf (
    "==", "!=",
    ">", "<", ">=", "<=",
    "||", "&&",
    "+", "-", "*", "/", "%",
)

val KEYWORDS: SortedSet<String> = (
    setOf (
        "break", "do", "coro", "create", "else", "false", "func",
        "if", "loop", "null", "resume", "return", "set",
        "true", "var", "xcoro", "yield",
    ).toSortedSet()
)

typealias Var_Type = Pair<Tk.Var,Type>

sealed class Tk (val str: String, val pos: Pos) {
    data class Eof  (val pos_: Pos, val n_: Int=G.N++): Tk("", pos_)
    data class Fix  (val str_: String, val pos_: Pos, val n_: Int=G.N++): Tk(str_, pos_)
    data class Type (val str_: String, val pos_: Pos, val n_: Int=G.N++): Tk(str_, pos_)
    data class Op   (val str_: String, val pos_: Pos, val n_: Int=G.N++): Tk(str_, pos_)
    data class Var  (val str_: String, val pos_: Pos, val n_: Int=G.N++): Tk(str_, pos_)  // up: 0=var, 1=upvar, 2=upref
    data class Num  (val str_: String, val pos_: Pos, val n_: Int=G.N++): Tk(str_, pos_)
    data class Chr  (val str_: String, val pos_: Pos, val n_: Int=G.N++): Tk(str_, pos_)
    data class Nat  (val str_: String, val pos_: Pos, val n_: Int=G.N++): Tk(str_, pos_)
}

sealed class Type (var n: Int, val tk: Tk) {
    //data class Top   (val tk_: Tk): Type(G.N++, tk_)
    data class Any     (val tk_: Tk): Type(G.N++, tk_)
    data class Unit    (val tk_: Tk.Fix): Type(G.N++, tk_)
    data class Basic   (val tk_: Tk.Type): Type(G.N++, tk_)
    data class Pointer (val tk_: Tk.Op, val ptr: Type): Type(G.N++, tk_)
    data class Tuple   (val tk_: Tk.Fix, val ts: List<Type>): Type(G.N++, tk_)
    data class Union   (val tk_: Tk.Op, val ts: List<Type>): Type(G.N++, tk_)

    sealed class Proto (val tk_: Tk.Fix, val inp: kotlin.Any, val out: Type): Type(G.N++, tk_) {
        open class Func (val tk__: Tk.Fix, val inp_: List<Type>, val out_: Type): Proto(tk__, inp_, out_) {
            data class Vars (val tk___: Tk.Fix, val inp__: List<Var_Type>, val out__: Type) :
                Func(tk___, inp__.map { (_, tp) -> tp }, out__)
        }
        open class Coro (val tk__: Tk.Fix, val inp_: Type.Union, val out_: Type.Union): Proto(tk__, inp_, out_) {
            data class Vars (val tk___: Tk.Fix, val inp__: Var_Type, val out__: Type.Union) :
                Coro(tk___, inp__.second as Union, out__)
        }
    }
    data class XCoro (val tk_: Tk.Fix, val inp: Type.Union, val out: Type.Union): Type(G.N++, tk_)
}

sealed class Expr (var n: Int, val tk: Tk) {
    data class Nat  (val tk_: Tk.Nat): Expr(G.N++, tk_)
    data class Acc  (val tk_: Tk.Var, val ign: Boolean=false): Expr(G.N++, tk_)
    data class Bool (val tk_: Tk.Fix): Expr(G.N++, tk_)
    data class Char (val tk_: Tk.Chr): Expr(G.N++, tk_)
    data class Num  (val tk_: Tk.Num): Expr(G.N++, tk_)
    data class Null (val tk_: Tk.Fix): Expr(G.N++, tk_)
    data class Unit (val tk_: Tk.Fix): Expr(G.N++, tk_)

    data class Tuple (val tk_: Tk.Fix, val vs: List<Expr>): Expr(G.N++, tk_)
    data class Index (val tk_: Tk.Fix, val col: Expr, val idx: String): Expr(G.N++, tk_)
    data class Union (val tk_: Tk.Op, val tp: Type, val idx: String, val v: Expr): Expr(G.N++, tk_)
    data class Pred  (val tk_: Tk.Op, val col: Expr, val idx: String): Expr(G.N++, tk_)
    data class Disc  (val tk_: Tk.Op, val col: Expr, val idx: String): Expr(G.N++, tk_)

    data class Uno  (val tk_: Tk.Op, val e: Expr): Expr(G.N++, tk_)
    data class Bin  (val tk_: Tk.Op, val e1: Expr, val e2: Expr): Expr(G.N++, tk_)
    data class Call (val tk_: Tk, val f: Expr, val args: List<Expr>): Expr(G.N++, tk_)
}

sealed class Stmt (var n: Int, val tk: Tk) {
    sealed class Proto (val tk_: Tk.Fix, val id: Tk.Var, val tp: Type.Proto, val blk: Stmt.Block) : Stmt(G.N++, tk_) {
        data class Func (val tk__: Tk.Fix, val id_: Tk.Var, val tp_: Type.Proto.Func.Vars, val blk_: Stmt.Block) : Stmt.Proto(tk__, id_, tp_, blk_)
        data class Coro (val tk__: Tk.Fix, val id_: Tk.Var, val tp_: Type.Proto.Coro.Vars, val blk_: Stmt.Block) : Stmt.Proto(tk__, id_, tp_, blk_)
    }
    data class Return  (val tk_: Tk.Fix, val e: Expr) : Stmt(G.N++, tk_)

    data class Block   (val tk_: Tk, val ss: List<Stmt>) : Stmt(G.N++, tk_)
    data class Dcl     (val tk_: Tk.Fix, val var_type: Var_Type) : Stmt(G.N++, tk_)
    data class Set     (val tk_: Tk.Fix, val dst: Expr, val src: Expr): Stmt(G.N++, tk_)

    data class If      (val tk_: Tk.Fix, val cnd: Expr, val t: Stmt.Block, val f: Stmt.Block): Stmt(G.N++, tk_)
    data class Loop    (val tk_: Tk.Fix, val blk: Stmt.Block): Stmt(G.N++, tk_)
    data class Break   (val tk_: Tk.Fix): Stmt(G.N++, tk_)

    data class Create (val tk_: Tk.Fix, val dst: Expr, val co: Expr): Stmt(G.N++, tk_)
    data class Resume (val tk_: Tk.Fix, val dst: Expr?, val xco: Expr, val arg: Expr): Stmt(G.N++, tk_)
    data class Yield  (val tk_: Tk.Fix, val dst: Expr?, val arg: Expr): Stmt(G.N++, tk_)

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
        val tk0 = G.tk1!!
        val ss = parser_list(null, { accept_enu("Eof") }, {
            parser_stmt()
        }).flatten()
        G.outer = Stmt.Block(tk0, ss)
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
