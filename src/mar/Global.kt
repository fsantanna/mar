package mar

import java.io.File
import java.util.*

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

    data class Nat     (val tk_: Tk.Nat): Stmt(G.N++, tk_)
    data class Acc     (val tk_: Tk.Var, val ign: Boolean=false): Stmt(G.N++, tk_)
    data class Nil     (val tk_: Tk.Fix): Stmt(G.N++, tk_)
    data class Tag     (val tk_: Tk.Type): Stmt(G.N++, tk_)
    data class Bool    (val tk_: Tk.Fix): Stmt(G.N++, tk_)
    data class Char    (val tk_: Tk.Chr): Stmt(G.N++, tk_)
    data class Num     (val tk_: Tk.Num): Stmt(G.N++, tk_)
    data class Tuple   (val tk_: Tk.Fix, val args: List<Stmt>): Stmt(G.N++, tk_)
    data class Vector  (val tk_: Tk.Fix, val args: List<Stmt>): Stmt(G.N++, tk_)
    data class Dict    (val tk_: Tk.Fix, val args: List<Pair<Stmt,Stmt>>): Stmt(G.N++, tk_)
    data class Index   (val tk_: Tk, val col: Stmt, val idx: Stmt): Stmt(G.N++, tk_)
    data class Call    (val tk_: Tk, val clo: Stmt, val args: List<Stmt>): Stmt(G.N++, tk_)
}

typealias Node = Int

object G {
    var N: Int = 1
    var outer: Stmt.Do? = null
    var ns: MutableMap<Node,Stmt> = mutableMapOf()
    var ups: MutableMap<Node,Node> = mutableMapOf()
    var tags: MutableMap<String,Tk.Type> = mutableMapOf()
    val datas = mutableMapOf<String,List<Var_Type>>()
    val nats: MutableMap<Node,Pair<List<Node>,String>> = mutableMapOf()
    var nonlocs: MutableMap<Node,List<Node>> = mutableMapOf()
    val mems: MutableSet<Stmt>  = mutableSetOf()

    fun reset () {
        N = 1
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
