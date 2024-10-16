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
        "break", "do", "coro", "create", "data", "else",
        "false", "func", "if", "loop", "null", "resume",
        "return", "set", "start", "true", "var", "exec",
        "yield",
    ).toSortedSet()
)

val BASICS = setOf(
    "Bool", "Char", "Int"
)

typealias Var_Type = Pair<Tk.Var,Type>

sealed class Tk (var n: Int, val str: String, val pos: Pos) {
    class Eof  (pos: Pos): Tk(G.N++, "", pos)
    class Fix  (str: String, pos: Pos): Tk(G.N++, str, pos)
    class Type (str: String, pos: Pos): Tk(G.N++, str, pos)
    class Op   (str: String, pos: Pos): Tk(G.N++, str, pos)
    class Var  (str: String, pos: Pos): Tk(G.N++, str, pos)  // up: 0=var, 1=upvar, 2=upref
    class Num  (str: String, pos: Pos): Tk(G.N++, str, pos)
    class Chr  (str: String, pos: Pos): Tk(G.N++, str, pos)
    class Nat  (str: String, pos: Pos): Tk(G.N++, str, pos)
}

sealed class Type (var n: Int, val tk: Tk) {
    //data class Top   (val tk_: Tk): Type(G.N++, tk_)
    class Any     (tk: Tk): Type(G.N++, tk)
    class Unit    (tk: Tk): Type(G.N++, tk)
    class Basic   (val tk_: Tk.Type): Type(G.N++, tk_)
    class Pointer (tk: Tk, val ptr: Type): Type(G.N++, tk)
    class Tuple   (tk: Tk, val ts: List<Type>): Type(G.N++, tk)
    class Union   (tk: Tk, val tagged: Boolean, val ts: List<Type>): Type(G.N++, tk)

    sealed class Proto (tk: Tk.Fix, val inps: List<Type>, val out: Type): Type(G.N++, tk) {
        open class Func (tk: Tk.Fix, inps: List<Type>, out: Type): Proto(tk, inps, out) {
            class Vars (tk: Tk.Fix, val inps_: List<Var_Type>, out: Type) :
                Func(tk, inps_.map { (_, tp) -> tp }, out)
        }
        open class Coro (tk: Tk.Fix, inps: List<Type>, val res: Type, val yld: Type, out: Type): Proto(tk, inps, out) {
            class Vars (tk: Tk.Fix, val inps_: List<Var_Type>, res: Type, yld: Type, out: Type):
                Coro(tk, inps_.map { (_, tp) -> tp }, res, yld, out)
        }
    }
    class Exec (tk: Tk, val inps: List<Type>, val res: Type, val yld: Type, val out: Type): Type(G.N++, tk)
}

sealed class Expr (var n: Int, val tk: Tk) {
    class Nat  (val tk_: Tk.Nat): Expr(G.N++, tk_)
    class Acc  (val tk_: Tk.Var, val ign: Boolean=false): Expr(G.N++, tk_)
    class Bool (val tk_: Tk.Fix): Expr(G.N++, tk_)
    class Char (val tk_: Tk.Chr): Expr(G.N++, tk_)
    class Num  (val tk_: Tk.Num): Expr(G.N++, tk_)
    class Null (val tk_: Tk.Fix): Expr(G.N++, tk_)
    class Unit (val tk_: Tk.Fix): Expr(G.N++, tk_)

    class Tuple (tk: Tk, val vs: List<Expr>): Expr(G.N++, tk)
    class Field (tk: Tk, val col: Expr, val idx: String): Expr(G.N++, tk)
    class Union (tk: Tk, val tp: Type, val idx: String, val v: Expr): Expr(G.N++, tk)
    class Pred  (tk: Tk, val col: Expr, val idx: String): Expr(G.N++, tk)
    class Disc  (tk: Tk, val col: Expr, val idx: String): Expr(G.N++, tk)
    class Cons  (val tk_: Tk.Type, val e: Expr): Expr(G.N++, tk_)

    class Uno  (val tk_: Tk.Op, val e: Expr): Expr(G.N++, tk_)
    class Bin  (val tk_: Tk.Op, val e1: Expr, val e2: Expr): Expr(G.N++, tk_)
    class Call (tk: Tk, val f: Expr, val args: List<Expr>): Expr(G.N++, tk)
}

sealed class Stmt (var n: Int, val tk: Tk) {
    class Data     (tk: Tk, val id: Tk.Type, val tp: Type): Stmt(G.N++, tk)
    sealed class Proto (tk: Tk.Fix, val id: Tk.Var, val tp: Type.Proto, val blk: Stmt.Block) : Stmt(G.N++, tk) {
        class Func (tk: Tk.Fix, id: Tk.Var, val tp_: Type.Proto.Func.Vars, blk: Stmt.Block) : Stmt.Proto(tk, id, tp_, blk)
        class Coro (tk: Tk.Fix, id: Tk.Var, val tp_: Type.Proto.Coro.Vars, blk: Stmt.Block) : Stmt.Proto(tk, id, tp_, blk)
    }
    class Return  (tk: Tk, val e: Expr) : Stmt(G.N++, tk)

    class Block   (tk: Tk, val ss: List<Stmt>) : Stmt(G.N++, tk)
    class Dcl     (tk: Tk, val var_type: Var_Type) : Stmt(G.N++, tk)
    class Set     (tk: Tk, val dst: Expr, val src: Expr): Stmt(G.N++, tk)

    class If      (tk: Tk, val cnd: Expr, val t: Stmt.Block, val f: Stmt.Block): Stmt(G.N++, tk)
    class Loop    (tk: Tk, val blk: Stmt.Block): Stmt(G.N++, tk)
    class Break   (tk: Tk): Stmt(G.N++, tk)

    class Create  (tk: Tk, val dst: Expr, val co: Expr): Stmt(G.N++, tk)
    class Start   (tk: Tk, val dst: Expr?, val exe: Expr, val args: List<Expr>): Stmt(G.N++, tk)
    class Resume  (tk: Tk, val dst: Expr?, val exe: Expr, val arg: Expr): Stmt(G.N++, tk)
    class Yield   (tk: Tk, val dst: Expr?, val arg: Expr): Stmt(G.N++, tk)

    class Nat     (tk: Tk.Nat): Stmt(G.N++, tk)
    class Call    (tk: Tk, val call: Expr.Call): Stmt(G.N++, tk)
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

    val types = mutableSetOf<String>()

    /*
    var tags: MutableMap<String,Tk.Type> = mutableMapOf()
    val datas = mutableMapOf<String,List<Var_Type>>()
    val nats: MutableMap<Node,Pair<List<Node>,String>> = mutableMapOf()
    var nonlocs: MutableMap<Node,List<Node>> = mutableMapOf()
    val mems: MutableSet<Stmt>  = mutableSetOf()
     */

    fun reset () {
        N = 1

        tks = null
        tk0 = null
        tk1 = null

        outer = null
        ns.clear()
        ups.clear()

        types.clear()

        /*
        tags.clear()
        datas.clear()
        nats.clear()
        nonlocs.clear()
        mems.clear()
         */
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
