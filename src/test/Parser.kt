package test

import mar.*
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Parser {
    // VAR

    @Test
    fun aa_01_var () {
        G.tks = (" x ").lexer()
        parser_lexer()
        val e = parser_expr_3_prim()
        assert(e is Expr.Acc && e.tk.str == "x")
    }
    @Test
    fun aa_02_var_err () {
        G.tks = (" { ").lexer()
        parser_lexer()
        assert(trap { parser_expr_3_prim() } == "anon : (lin 1, col 2) : expected expression : have \"{\"")
    }
    @Test
    fun aa_03_var_err () {
        G.tks = ("  ").lexer()
        parser_lexer()
        assert(trap { parser_expr_3_prim() } == "anon : (lin 1, col 3) : expected expression : have end of file")
    }
    @Test
    fun aa_04_evt () {
        G.tks = (" evt ").lexer()
        parser_lexer()
        val e = parser_expr_3_prim()
        assert(e is Expr.Acc && e.tk.str == "evt")
    }
    @Test
    fun aa_05_err () {
        G.tks = (" err ").lexer()
        parser_lexer()
        val e = parser_expr_3_prim()
        assert(e is Expr.Acc && e.tk.str == "err")
    }

    // TYPES

    @Test
    fun aj_01_type () {
        G.tks = ("Int").lexer()
        parser_lexer()
        val tp = parser_type()
        assert(tp is Type.Basic && tp.tk.str=="Int")
    }
    @Test
    fun aj_02_type () {
        G.tks = ("func (Int,Int) -> ()").lexer()
        parser_lexer()
        val tp = parser_type()
        assert(tp is Type.Func && tp.tk.str=="func" && tp.inps.size==2 && tp.out is Type.Unit)
    }
    @Test
    fun aj_03_type () {
        G.tks = ("func () -> Int").lexer()
        parser_lexer()
        val tp = parser_type()
        assert(tp is Type.Func && (tp.out as Type.Basic).tk.str=="Int")
    }

    // PARENS

    @Test
    fun bb_01_expr_parens() {
        G.tks = (" ( a ) ").lexer()
        parser_lexer()
        val e = parser_expr_3_prim()
        assert(e is Expr.Acc && e.tk.str == "a")
    }
    @Test
    fun bb_02_expr_parens_err() {
        G.tks = (" ( a  ").lexer()
        parser_lexer()
        assert(trap { parser_expr_3_prim() } == "anon : (lin 1, col 7) : expected \")\" : have end of file")
    }
    @Test
    fun bb_03_op_prec_err() {
        G.tks = ("println(2 * 3 - 1)").lexer()
        parser_lexer()
        assert(trap { parser_expr() } == "anon : (lin 1, col 15) : binary operation error : expected surrounding parentheses")
    }
    @Test
    fun bb_04_op_prec_ok() {
        G.tks = ("println(2 * (3 - 1))").lexer()
        parser_lexer()
        val e = parser_expr()
        assert(e.to_str() == "println((2 * (3 - 1)))") { e.to_str() }
    }

    // BINARY

    @Test
    fun cc_01_bin_err() {
        G.tks = ("2 * 3 - 1").lexer()
        parser_lexer()
        //parser_expr_1_bin()
        assert(trap { parser_expr_1_bin() } == "anon : (lin 1, col 7) : binary operation error : expected surrounding parentheses")
    }
    @Test
    fun cc_02_bin() {
        G.tks = ("2 * (3 - 1)").lexer()
        parser_lexer()
        val e = parser_expr_1_bin()
        assert(e.to_str() == "(2 * (3 - 1))")
    }

    // DO

    @Test
    fun dd_01_do() {
        G.tks = ("do [] {}").lexer()
        parser_lexer()
        val s = parser_stmt()
        assert(s is Stmt.Block && s.ss.isEmpty())
    }
    @Test
    fun dd_02_do() {
        G.tks = """
            do [
                i: Int
            ] {
                set i=1
            }
        """.lexer()
        parser_lexer()
        val s = parser_stmt()
        assert(s is Stmt.Block && s.vs.size==1 && s.ss.size==1)
        assert(s.to_str() == "do [i: Int" + "] {\nset i = 1\n}") { s.to_str() }
    }
    @Test
    fun dd_03_do() {
        G.tks = ("do [] ;;;(a);;; { print(a) }").lexer()
        parser_lexer()
        val s = parser_stmt()
        assert(s is Stmt.Block)
        assert(s.to_str() == "do [] {\nprint(a)\n}") { s.to_str() }
    }
    @Test
    fun dd_04_do() {
        G.tks = "do [x:X] { do [y:Y] {  }}".lexer()
        parser_lexer()
        val s = parser_stmt()
        assert(s is Stmt.Block)
        assert(s.to_str() == "do [x: X] {\n" +
                "do [y: Y] {\n" +
                "}\n" +
                "}") { s.to_str() }
    }

    // DCL / SET

    @Test
    fun ee_01_dcl () {
        G.tks = ("do [x: Int] {}").lexer()
        parser_lexer()
        val e = parser_stmt()
        assert(e.to_str() == "do [x: Int] {\n}") { e.to_str() }
    }
    @Test
    fun ee_02_set () {
        G.tks = ("set x = 10").lexer()
        parser_lexer()
        val e = parser_stmt()
        assert(e.to_str() == "set x = 10")
    }

    // NUM / NIL / BOOL

    @Test
    fun ff_01_num() {
        G.tks = (" 1.5F ").lexer()
        parser_lexer()
        val e = parser_expr_3_prim()
        assert(e is Expr.Num && e.tk.str == "1.5F")
    }
    @Test
    fun ff_02_nil() {
        G.tks = ("null").lexer()
        parser_lexer()
        val e = parser_expr_3_prim()
        assert(e is Expr.Null && e.tk.str == "null")
    }
    @Test
    fun ff_03_true() {
        G.tks = ("true").lexer()
        parser_lexer()
        val e = parser_expr_3_prim()
        assert(e is Expr.Bool && e.tk.str == "true")
    }
    @Test
    fun ff_04_false() {
        G.tks = ("false").lexer()
        parser_lexer()
        val e = parser_expr_3_prim()
        assert(e is Expr.Bool && e.tk.str == "false")
    }
    @Test
    fun ff_05_char() {
        G.tks = ("'x'").lexer()
        parser_lexer()
        val e = parser_expr_3_prim()
        assert(e is Expr.Char && e.tk.str == "'x'")
    }

    // CALL

    @Test
    fun gg_01_call() {
        G.tks = (" f (1.5F, x) ").lexer()
        parser_lexer()
        val e = parser_expr_2_suf()
        assert(e is Expr.Call && e.tk.str=="f" && e.f is Expr.Acc && e.args.size==2)
    }
    @Test
    fun gg_02_call() {
        G.tks = (" f() ").lexer()
        parser_lexer()
        val e = parser_expr_2_suf()
        assert(e is Expr.Call && e.f.tk.str=="f" && e.f is Expr.Acc && e.args.size==0)
    }
    @Test
    fun gg_03_call() {
        G.tks = (" f(x,8)() ").lexer()
        parser_lexer()
        val e = parser_expr_2_suf()
        assert(e is Expr.Call && e.f is Expr.Call && e.args.size==0)
        assert(e.to_str() == "f(x,8)()")
    }
    @Test
    fun gg_04_call_err() {
        G.tks = ("f (999 ").lexer()
        parser_lexer()
        assert(trap { parser_expr_2_suf() } == "anon : (lin 1, col 8) : expected \",\" : have end of file")
    }
    @Test
    fun gg_05_call_err() {
        G.tks = (" f ({ ").lexer()
        parser_lexer()
        assert(trap { parser_expr_2_suf() } == "anon : (lin 1, col 5) : expected expression : have \"{\"")
    }
}
