package mar

import java.io.File
import java.io.Reader
import java.util.*

val VALGRIND = ""
val THROW = false
var DUMP = true
val PATH = File(File(System.getProperty("java.class.path")).absolutePath).parent
val D = "\$"

// VERSION
const val MAJOR    = 0
const val MINOR    = 1
const val REVISION = 0
const val VERSION  = "v$MAJOR.$MINOR.$REVISION"

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
        "break", "do", "catch", "coro", "create", "defer",
        "data", "else", "escape", "exec", "false", "func", "if",
        "include", "loop", "null", "print", "resume", "return", "set",
        "start", "throw", "true", "var", "yield",
    ).toSortedSet()
)

val PRIMS = setOf(
    "Bool", "Char", "Int"
)

typealias XDcl = Triple<Stmt,Tk.Var,Type?>
typealias Var_Type = Pair<Tk.Var,Type>

sealed class Tk (val str: String, val pos: Pos) {
    class Eof  (pos: Pos): Tk("", pos)
    class Fix  (str: String, pos: Pos): Tk(str, pos)
    class Type (str: String, pos: Pos): Tk(str, pos)
    class Op   (str: String, pos: Pos): Tk(str, pos)
    class Var  (str: String, pos: Pos): Tk(str, pos)  // up: 0=var, 1=upvar, 2=upref
    class Num  (str: String, pos: Pos): Tk(str, pos)
    class Chr  (str: String, pos: Pos): Tk(str, pos)
    class Nat  (str: String, pos: Pos): Tk(str, pos)
}

sealed class Type (var n: Int, var xup: kotlin.Any?, val tk: Tk) {
    //data class Top   (val tk_: Tk): Type(G.N++, tk_)
    //class Any     (tk: Tk): Type(G.N++, null, tk)
    class Unit    (tk: Tk): Type(G.N++, null, tk)
    class Prim    (val tk_: Tk.Type): Type(G.N++, null, tk_)
    class Data    (tk: Tk, val ts: List<Tk.Type>): Type(G.N++, null, tk)
    class Pointer (tk: Tk, val ptr: Type?): Type(G.N++, null, tk)
    class Tuple   (tk: Tk, val ts: List<Pair<Tk.Var?,Type>>): Type(G.N++, null, tk)
    class Union   (tk: Tk, val tagged: Boolean, val ts: List<Pair<Tk.Type?,Type>>): Type(G.N++, null, tk)

    sealed class Proto (tk: Tk, val inps: List<Type>, val out: Type): Type(G.N++, null, tk) {
        open class Func (tk: Tk, inps: List<Type>, out: Type): Proto(tk, inps, out) {
            class Vars (tk: Tk, val inps_: List<Var_Type>, out: Type) :
                Func(tk, inps_.map { (_, tp) -> tp }, out)
        }
        open class Coro (tk: Tk, inps: List<Type>, val res: Type, val yld: Type, out: Type): Proto(tk, inps, out) {
            class Vars (tk: Tk, val inps_: List<Var_Type>, res: Type, yld: Type, out: Type):
                Coro(tk, inps_.map { (_, tp) -> tp }, res, yld, out)
        }
    }
    class Exec (tk: Tk, val inps: List<Type>, val res: Type, val yld: Type, val out: Type): Type(G.N++, null, tk)
}

sealed class Expr (var n: Int, var xup: Any?, val tk: Tk) {
    class Nat  (val tk_: Tk.Nat, var xtp: Type?): Expr(G.N++, null, tk_)
    class Acc  (val tk_: Tk.Var, val ign: Boolean=false): Expr(G.N++, null, tk_)
    class Bool (val tk_: Tk.Fix): Expr(G.N++, null, tk_)
    class Char (val tk_: Tk.Chr): Expr(G.N++, null, tk_)
    class Num  (val tk_: Tk.Num): Expr(G.N++, null, tk_)
    class Null (tk_: Tk): Expr(G.N++, null, tk_)
    class Unit (tk_: Tk): Expr(G.N++, null, tk_)

    class Tuple (tk: Tk, var xtp: Type.Tuple?, val vs: List<Pair<Tk.Var?,Expr>>): Expr(G.N++, null, tk)
    class Field (tk: Tk, val col: Expr, val idx: String): Expr(G.N++, null, tk)
    class Union (tk: Tk, var xtp: Type.Union?, val idx: String, val v: Expr): Expr(G.N++, null, tk)
    class Pred  (tk: Tk, val col: Expr, val idx: String): Expr(G.N++, null, tk)
    class Disc  (tk: Tk, val col: Expr, val idx: String): Expr(G.N++, null, tk)
    class Cons  (tk: Tk, val ts: List<Tk.Type>, val e: Expr): Expr(G.N++, null, tk)

    class Uno  (val tk_: Tk.Op, val e: Expr): Expr(G.N++, null, tk_)
    class Bin  (val tk_: Tk.Op, val e1: Expr, val e2: Expr): Expr(G.N++, null, tk_)
    class Call (tk: Tk, val f: Expr, val args: List<Expr>): Expr(G.N++, null, tk)

    class Create  (tk: Tk, val co: Expr): Expr(G.N++, null, tk)
    class Start   (tk: Tk, val exe: Expr, val args: List<Expr>): Expr(G.N++, null, tk)
    class Resume  (tk: Tk, val exe: Expr, val arg: Expr): Expr(G.N++, null, tk)
    class Yield   (tk: Tk, val arg: Expr): Expr(G.N++, null, tk)
}

sealed class Stmt (var n: Int, var xup: Stmt?, val tk: Tk) {
    class Data   (tk: Tk, val t: Tk.Type, val tp: Type, val subs: List<Stmt.Data>?): Stmt(G.N++, null, tk)
    sealed class Proto (tk: Tk.Fix, val id: Tk.Var, val tp: Type.Proto, val blk: Stmt.Block) : Stmt(G.N++, null, tk) {
        class Func (tk: Tk.Fix, id: Tk.Var, val tp_: Type.Proto.Func.Vars, blk: Stmt.Block) : Stmt.Proto(tk, id, tp_, blk)
        class Coro (tk: Tk.Fix, id: Tk.Var, val tp_: Type.Proto.Coro.Vars, blk: Stmt.Block) : Stmt.Proto(tk, id, tp_, blk)
    }

    class Block  (tk: Tk, val esc: Type.Data?, val ss: List<Stmt>) : Stmt(G.N++, null, tk)
    class Dcl    (tk: Tk, val id: Tk.Var, var xtp: Type?) : Stmt(G.N++, null, tk)
    class Set    (tk: Tk, val dst: Expr, val src: Expr): Stmt(G.N++, null, tk)

    class Escape (tk: Tk, val e: Expr.Cons): Stmt(G.N++, null, tk)
    class Defer  (tk: Tk, val blk: Stmt.Block): Stmt(G.N++, null, tk)
    class Catch  (tk: Tk, val tp: Type.Data?, val blk: Stmt.Block): Stmt(G.N++, null, tk)
    class Throw  (tk: Tk, val e: Expr): Stmt(G.N++, null, tk)

    class If     (tk: Tk, val cnd: Expr, val t: Stmt.Block, val f: Stmt.Block): Stmt(G.N++, null, tk)
    class Loop   (tk: Tk, val blk: Stmt.Block): Stmt(G.N++, null, tk)

    class Print  (tk: Tk, val e: Expr): Stmt(G.N++, null, tk)
    class XExpr  (tk: Tk, val e: Expr): Stmt(G.N++, null, tk)
}

object G {
    var N: Int = 1

    var tks: Iterator<Tk>? = null
    var tk0: Tk? = null
    var tk1: Tk? = null

    var outer: Stmt.Block? = null

    //val cons  = mutableMapOf<Node,Type.Tuple>()     // resolve sub types
    val types = mutableSetOf<String>()              // for C generation

    val defers: MutableMap<Any, Triple<MutableList<Int>,String,String>> = mutableMapOf()

    var datas = 1

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

        //cons.clear()
        types.clear()
        defers.clear()

        datas = 1

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
        G.outer = Stmt.Block(tk0, null, listOf(
            Stmt.Data(tk0, Tk.Type("Return", tk0.pos.copy()), Type.Tuple(tk0, emptyList()), emptyList()),
            Stmt.Data(tk0, Tk.Type("Break", tk0.pos.copy()), Type.Tuple(tk0, emptyList()), emptyList()),
        ) + ss)
        cache_ups()
        check_vars()
        infer_types()
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
    val cmd = listOf("gcc", /*"-Werror",*/ "$out.c", "-l", "m", "-o", "$out.exe") + args
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

fun test (inp: String): String {
    //println(inp)
    val prelude = "build/prelude.mar"
    val inps = listOf (
        Pair(Triple("anon",1,1), inp.reader()),
        Pair(Triple(prelude,1,1), File(prelude).reader()),
    )
    return all(true, false, inps, "out", emptyList())
}
