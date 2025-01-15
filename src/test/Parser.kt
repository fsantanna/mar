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
        val e = parser_expr_4_prim()
        assert(e is Expr.Acc && e.tk.str == "x")
    }
    @Test
    fun aa_02_var_err () {
        G.tks = (" { ").lexer()
        parser_lexer()
        assert(trap { parser_expr_4_prim() } == "anon : (lin 1, col 2) : expected expression : have \"{\"")
    }
    @Test
    fun aa_03_var_err () {
        G.tks = ("  ").lexer()
        parser_lexer()
        assert(trap { parser_expr_4_prim() } == "anon : (lin 1, col 3) : expected expression : have end of file")
    }
    @Test
    fun aa_04_evt () {
        G.tks = (" evt ").lexer()
        parser_lexer()
        val e = parser_expr_4_prim()
        assert(e is Expr.Acc && e.tk.str == "evt")
    }
    @Test
    fun aa_05_err () {
        G.tks = (" err ").lexer()
        parser_lexer()
        val e = parser_expr_4_prim()
        assert(e is Expr.Acc && e.tk.str == "err")
    }

    // TYPE

    @Test
    fun aj_01_type () {
        G.tks = ("Int").lexer()
        parser_lexer()
        val tp = parser_type(null, false, false)
        assert(tp is Type.Prim && tp.tk.str=="Int")
    }
    @Test
    fun aj_02_type () {
        G.tks = ("func (Int,Int) -> ()").lexer()
        parser_lexer()
        val tp = parser_type(null, false, false)
        assert(tp is Type.Proto.Func && tp.tk.str=="func" && tp.inps.size==2 && tp.out is Type.Unit)
    }
    @Test
    fun aj_03_type () {
        G.tks = ("func () -> Int").lexer()
        parser_lexer()
        val tp = parser_type(null, false, false)
        assert(tp is Type.Proto.Func && (tp.out as Type.Prim).tk.str=="Int")
        assert(tp.to_str() == "func () -> Int")
    }
    @Test
    fun aj_04_type () {
        G.tks = ("func (x: Int) -> Int").lexer()
        parser_lexer()
        val tp = parser_type(null, false, false)
        assert(tp is Type.Proto.Func && (tp.out as Type.Prim).tk.str=="Int")
        assert(tp.to_str() == "func (x: Int) -> Int")
    }
    @Test
    fun aj_05_type () {
        G.tks = ("func (Int, x: Int) -> ()").lexer()
        parser_lexer()
        assert(trap { parser_type(null, false, false) } == "anon : (lin 1, col 12) : expected type : have \"x\"")
    }
    @Test
    fun aj_06_type () {
        G.tks = ("func (x: Int, Int) -> ()").lexer()
        parser_lexer()
        assert(trap { parser_type(null, false, false) } == "anon : (lin 1, col 15) : expected variable : have \"Int\"")
    }
    @Test
    fun aj_07_type_ptr () {
        G.tks = ("\\Int").lexer()
        parser_lexer()
        val tp = parser_type(null, false, false)
        assert(tp is Type.Pointer && tp.ptr is Type.Prim)
        assert(tp.to_str() == "\\Int")
    }
    @Test
    fun aj_08_type_coro () {
        G.tks = ("coro () -> () -> () -> ()").lexer()
        parser_lexer()
        val tp = parser_type(null, false, false)
        assert(tp is Type.Proto.Coro && tp.inps.size==0 && tp.res is Type.Unit && tp.yld is Type.Unit && tp.out is Type.Unit)
        assert(tp.to_str() == "coro () -> () -> () -> ()")
    }
    @Test
    fun aj_09_type_exec () {
        G.tks = ("exec (Int) -> () -> () -> Int").lexer()
        parser_lexer()
        val tp = parser_type(null, false, false)
        assert(tp is Type.Exec && tp.inps.size==1 && tp.res is Type.Unit && tp.yld is Type.Unit && tp.out is Type.Prim)
        assert(tp.to_str() == "exec (Int) -> () -> () -> Int") { tp.to_str() }
    }
    @Test
    fun aj_10_type_coro_err () {
        G.tks = ("coro (Int,Int) -> ()").lexer()
        parser_lexer()
        assert(trap { parser_type(null, false, false) } == "anon : (lin 1, col 21) : expected \"->\" : have end of file")
        //assert(trap { parser_type(false) } == "anon : (lin 1, col 1) : coro error : unexpected second argument")
        //assert(trap { parser_type(false) } == "anon : (lin 1, col 7) : expected \"<\" : have \"Int\"")
    }
    @Test
    fun aj_11_type_exec_err () {
        G.tks = ("exec (Int,Int) -> Int").lexer()
        parser_lexer()
        assert(trap { parser_type(null, false, false) } == "anon : (lin 1, col 22) : expected \"->\" : have end of file")
        //assert(trap { parser_type(false) } == "anon : (lin 1, col 7) : expected \"<\" : have \"Int\"")
        //assert(trap { parser_type(false) } == "anon : (lin 1, col 1) : exec error : unexpected second argument")
    }
    @Test
    fun aj_12_data_hier () {
        G.tks = ("X.Y.Z").lexer()
        parser_lexer()
        val tp = parser_type(null, false, false)
        assert(tp is Type.Data && tp.ts.size==3)
        assert(tp.to_str() == "X.Y.Z")
    }
    @Test
    fun aj_13_nat () {
        G.tks = ("`10`").lexer()
        parser_lexer()
        val tp = parser_type(null, false, false)
        assert(tp is Type.Nat && tp.tk.str=="10")
        assert(tp.to_str() == "`10`")
    }

    // TUPLE / UNION

    @Test
    fun ak_01_type_tuple () {
        G.tks = ("[]").lexer()
        parser_lexer()
        val tp = parser_type(null, false, false)
        assert(tp is Type.Tuple && tp.ts.size==0)
        assert(tp.to_str() == "[]") { tp.to_str() }
    }
    @Test
    fun ak_02_type_tuple () {
        G.tks = ("[Int,[Bool],()]").lexer()
        parser_lexer()
        val tp = parser_type(null, false, false)
        assert(tp is Type.Tuple && tp.ts.size==3)
        assert(tp.to_str() == "[Int,[Bool],()]") { tp.to_str() }
    }
    @Test
    fun ak_03_type_tuple () {  // tuple field names
        G.tks = ("[x:Int,y:[Bool],z:()]").lexer()
        parser_lexer()
        val tp = parser_type(null, false, false)
        assert(tp is Type.Tuple && tp.ts.size==3)
        assert(tp.to_str() == "[x:Int,y:[Bool],z:()]") { tp.to_str() }
    }
    @Test
    fun ak_04_type_union () {
        G.tks = ("<>").lexer()  // lex sees <> as a single token
        parser_lexer()
        val tp = parser_type(null, false, false)
        assert(tp is Type.Union && tp.ts.size==0)
        assert(tp.to_str() == "<>") { tp.to_str() }
    }
    @Test
    fun ak_05_type_union () {
        G.tks = ("<Int,<Bool>,()>").lexer()
        parser_lexer()
        val tp = parser_type(null, false, false)
        //println(tp)
        assert(tp is Type.Union && tp.ts.size==3)
        assert(tp.to_str() == "<Int,<Bool>,()>") { tp.to_str() }
    }
    @Test
    fun ak_06_type_union () {
        G.tks = ("<X:Int,Y:[Bool],Z:()>").lexer()
        parser_lexer()
        val tp = parser_type(null, false, false)
        assert(tp is Type.Union && tp.ts.size==3)
        assert(tp.to_str() == "<X:Int,Y:[Bool],Z:()>") { tp.to_str() }
    }
    @Test
    fun ak_07_type_union_err () {
        G.tks = ("var x: Int = <.1=10>: Int").lexer()
        parser_lexer()
        assert(trap { parser_stmt() } == "anon : (lin 1, col 23) : expected \"<\" : have \"Int\"")
    }

    // TYPE / VECTOR

    @Test
    fun al_01_vec () {
        G.tks = ("#[Int*10]").lexer()
        parser_lexer()
        val tp = parser_type(null, false, false)
        assert(tp is Type.Vector && tp.max is Expr.Num && tp.max.tk.str=="10" && tp.tp is Type.Prim)
        assert(tp.to_str() == "#[Int*10]")
    }
    @Test
    fun al_02_vec () {
        G.tks = ("#[#[Int*1] * 10]").lexer()
        parser_lexer()
        val tp = parser_type(null, false, false)
        assert(tp is Type.Vector && tp.max is Expr.Num && tp.max.tk.str=="10" && tp.tp is Type.Vector)
        assert(tp.to_str() == "#[#[Int*1]*10]")
    }
    @Test
    fun al_03_vec_err () {
        G.tks = ("#[Int*1.1]").lexer()
        parser_lexer()
        val tp = parser_type(null, false, false)
        assert(tp.to_str() == "#[Int*1.1]") { tp.to_str() }
        //assert(trap { } == "anon : (lin 1, col 3) : vector error : expected number")
    }
    @Test
    fun al_04_vec_err () {
        G.tks = ("#[Int 1]").lexer()
        parser_lexer()
        assert(trap { parser_type(null, false, false) } == "anon : (lin 1, col 7) : expected \"*\" : have \"1\"")
    }
    @Test
    fun al_05_vec_undef_err () {
        G.tks = ("#[Int]").lexer()
        parser_lexer()
        assert(trap { parser_type(null, false, false) } == "anon : (lin 1, col 6) : expected \"*\" : have \"]\"")
    }
    @Test
    fun al_06_vec_ptr () {
        G.tks = ("\\#[Int]").lexer()
        parser_lexer()
        val tp = parser_type(null, false, false)
        assert(tp is Type.Pointer && tp.ptr.let { it is Type.Vector && it.max==null })
        assert(tp.to_str() == "\\#[Int]") { tp.to_str() }
    }
    @Test
    fun al_07_vec_vec_ptr () {
        G.tks = ("\\#[#[Int]]").lexer()
        parser_lexer()
        val tp = parser_type(null, false, false)
        assert(tp is Type.Pointer && tp.ptr.let { it is Type.Vector && it.max==null })
        assert(tp.to_str() == "\\#[#[Int]]") { tp.to_str() }
    }

    // PARENS

    @Test
    fun bb_01_expr_parens() {
        G.tks = (" ( a ) ").lexer()
        parser_lexer()
        val e = parser_expr_4_prim()
        assert(e is Expr.Acc && e.tk.str == "a")
    }
    @Test
    fun bb_02_expr_parens_err() {
        G.tks = (" ( a  ").lexer()
        parser_lexer()
        assert(trap { parser_expr_4_prim() } == "anon : (lin 1, col 7) : expected \")\" : have end of file")
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
        assert(e.to_str() == "(println((2 * (3 - 1))))") { e.to_str() }
    }

    // STRING

    @Test
    fun bc_01_str () {
        G.tks = ("\"f\"").lexer()
        parser_lexer()
        val e = parser_expr()
        assert(e.to_str() == "\"f\"") { e.to_str() }
    }

    // BINARY / UNARY

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
    @Test
    fun cc_03_uno() {
        G.tks = ("!-x").lexer()
        parser_lexer()
        val e = parser_expr()
        assert(e.to_str() == "(!(-x))")
    }
    @Test
    fun cc_04_concat() {
        G.tks = ("a ++ b").lexer()
        parser_lexer()
        val e = parser_expr_1_bin()
        assert(e.to_str() == "(a ++ b)") { e.to_str() }
    }

    // DO

    @Test
    fun dd_01_do() {
        G.tks = ("do {}").lexer()
        parser_lexer()
        val s = parser_stmt().first()
        assert(s is Stmt.Block && s.ss.isEmpty())
    }
    @Test
    fun dd_02_do() {
        G.tks = """
            do {
                var i: Int=1
            }
        """.lexer()
        parser_lexer()
        val s = parser_stmt().first()
        assert(s is Stmt.Block && s.ss.size==2)
        assert(s.to_str() == "do {\nvar i: Int\nset i = 1\n}") { s.to_str() }
    }
    @Test
    fun dd_03_do() {
        G.tks = ("do { print(a) }").lexer()
        parser_lexer()
        val s = parser_stmt().first()
        assert(s is Stmt.Block)
        assert(s.to_str() == "do {\nprint(a)\n}") { s.to_str() }
    }
    @Test
    fun dd_04_do() {
        G.tks = "do { var x:X do { var y:Y }}".lexer()
        parser_lexer()
        val s = parser_stmt().first()
        assert(s is Stmt.Block)
        assert(s.to_str() == "do {\n" +
                "var x: X\n" +
                "do {\n" +
                "var y: Y\n" +
                "}\n" +
                "}") { s.to_str() }
    }

    // PASS

    @Test
    fun de_01_do_pass() {
        G.tks = "do(10)".lexer()
        parser_lexer()
        val s = parser_stmt().first()
        assert(s is Stmt.Pass)
        assert(s.to_str() == "do(10)") { s.to_str() }
    }

    // DCL / SET

    @Test
    fun ee_01_dcl () {
        G.tks = ("do { var x: Int }").lexer()
        parser_lexer()
        val e = parser_stmt().first()
        assert(e.to_str() == "do {\nvar x: Int\n}") { e.to_str() }
    }
    @Test
    fun ee_02_set () {
        G.tks = ("set x = 10").lexer()
        parser_lexer()
        val e = parser_stmt().first()
        assert(e.to_str() == "set x = 10")
    }
    @Test
    fun ee_03_func() {
        G.tks = ("""
            func f: (Int) -> Int {
            }
        """).lexer()
        parser_lexer()
        assert(trap { parser_stmt() } == "anon : (lin 2, col 22) : expected variable : have \"Int\"")
    }
    @Test
    fun ee_04_func() {
        G.tks = ("""
            do {
                func f: (a:Int) -> Int {
                }
            }
        """).lexer()
        parser_lexer()
        val e = parser_stmt().first()
        assert(e.to_str() == "do {\n" +
                "func f: (a: Int) -> Int {\n" +
                "}\n" +
                "}") { e.to_str() }
    }
    @Test
    fun ee_05_return() {
        G.tks = ("return(10)").lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "set `mar_ret` = 10\n" +
                "escape((Return(([]))))\n") { ss.to_str() }
    }
    @Test
    fun ee_06_return_Err() {
        G.tks = ("return 10").lexer()
        parser_lexer()
        assert(trap { parser_stmt() } == "anon : (lin 1, col 8) : expected \"(\" : have \"10\"")
    }
    @Test
    fun ee_07_dcl_infer () {
        G.tks = ("do { var x = 10 }").lexer()
        parser_lexer()
        val e = parser_stmt().first()
        assert(e.to_str() == "do {\n" +
                "var x\n" +
                "set x = 10\n" +
                "}") { e.to_str() }
    }

    // NUM / NIL / BOOL

    @Test
    fun ff_01_num() {
        G.tks = (" 1.5F ").lexer()
        parser_lexer()
        val e = parser_expr_4_prim()
        assert(e is Expr.Num && e.tk.str == "1.5F")
    }
    @Test
    fun ff_02_nil() {
        G.tks = ("null").lexer()
        parser_lexer()
        val e = parser_expr_4_prim()
        assert(e is Expr.Null && e.tk.str == "null")
    }
    @Test
    fun ff_03_true() {
        G.tks = ("true").lexer()
        parser_lexer()
        val e = parser_expr_4_prim()
        assert(e is Expr.Bool && e.tk.str == "true")
    }
    @Test
    fun ff_04_false() {
        G.tks = ("false").lexer()
        parser_lexer()
        val e = parser_expr_4_prim()
        assert(e is Expr.Bool && e.tk.str == "false")
    }
    @Test
    fun ff_05_char() {
        G.tks = ("'x'").lexer()
        parser_lexer()
        val e = parser_expr_4_prim()
        assert(e is Expr.Chr && e.tk.str == "'x'")
    }

    // CALL

    @Test
    fun gg_01_call() {
        G.tks = (" f (1.5F, x) ").lexer()
        parser_lexer()
        val e = parser_expr_3_suf()
        assert(e is Expr.Call && e.tk.str=="f" && e.f is Expr.Acc && e.args.size==2)
    }
    @Test
    fun gg_02_call() {
        G.tks = (" f() ").lexer()
        parser_lexer()
        val e = parser_expr_3_suf()
        assert(e is Expr.Call && e.f.tk.str=="f" && e.f is Expr.Acc && e.args.size==0)
    }
    @Test
    fun gg_03_call() {
        G.tks = (" f(x,8)() ").lexer()
        parser_lexer()
        val e = parser_expr_3_suf()
        assert(e is Expr.Call && e.f is Expr.Call && e.args.size==0)
        assert(e.to_str() == "((f(x,8))())") { e.to_str() }
    }
    @Test
    fun gg_04_call_err() {
        G.tks = ("f (999 ").lexer()
        parser_lexer()
        assert(trap { parser_expr_3_suf() } == "anon : (lin 1, col 8) : expected \",\" : have end of file")
    }
    @Test
    fun gg_05_call_err() {
        G.tks = (" f ({ ").lexer()
        parser_lexer()
        assert(trap { parser_expr_3_suf() } == "anon : (lin 1, col 5) : expected expression : have \"{\"")
    }

    // SPAWN / RESUME / YIELD / CORO

    @Test
    fun hh_01_spawn() {
        G.tks = ("create(f)").lexer()
        parser_lexer()
        val s = parser_stmt().first()
        assert(s is Stmt.Create && s.co is Expr.Acc)
        //assert(trap { parser_stmt() } == "anon : (lin 1, col 1) : expected expression : have \"create\"")
    }
    @Test
    fun hh_02_resume() {
        G.tks = ("resume xf()").lexer()
        parser_lexer()
        val s = parser_stmt().first()
        assert(s is Stmt.Resume && s.exe is Expr.Acc && s.arg is Expr.Unit)
        assert(s.to_str() == "resume xf()") { s.to_str() }
    }
    @Test
    fun hh_03_yield() {
        G.tks = ("yield()").lexer()
        parser_lexer()
        val s = parser_stmt().first()
        assert(s is Stmt.Yield && s.arg is Expr.Unit)
        assert(s.to_str() == "yield()") { s.to_str() }
    }
    @Test
    fun hh_04_spawn() {
        G.tks = ("set x = create(f)").lexer()
        parser_lexer()
        val s = parser_stmt().first()
        assert(s is Stmt.SetS && s.src is Stmt.Create && s.src.co is Expr.Acc)
        assert(s.to_str() == "set x = create(f)")
    }
    @Test
    fun hh_05_resume() {
        G.tks = ("set x = resume xf(false)").lexer()
        parser_lexer()
        val s = parser_stmt().first()
        assert(s is Stmt.SetS && s.src is Stmt.Resume && s.src.exe is Expr.Acc && s.src.arg is Expr.Bool)
        assert(s.to_str() == "set x = resume xf(false)")
    }
    @Test
    fun hh_06_yield() {
        G.tks = ("set y = yield(null)").lexer()
        parser_lexer()
        val s = parser_stmt().first()
        assert(s is Stmt.SetS && s.src is Stmt.Yield && s.src.arg is Expr.Null)
        assert(s.to_str() == "set y = yield(null)") { s.to_str() }
    }
    @Test
    fun hh_07_coro() {
        G.tks = ("coro co: () -> () -> () -> () {}").lexer()
        parser_lexer()
        val s = parser_stmt().first()
        assert(s is Stmt.Proto.Coro && s.tp.inps.size==0)
        assert(s.to_str() == "coro co: () -> () -> () -> () {\n}") { s.to_str() }
    }
    @Test
    fun hh_08_coro() {
        G.tks = ("coro co: (x: ()) -> () -> () -> () {}").lexer()
        parser_lexer()
        val s = parser_stmt().first()
        assert(s is Stmt.Proto.Coro && s.tp.out is Type.Unit)
        assert(s.to_str() == "coro co: (x: ()) -> () -> () -> () {\n}") { s.to_str() }
    }

    // IF / LOOP / MATCH

    @Test
    fun ii_01_if() {
        G.tks = ("if x {`.`} else {}").lexer()
        parser_lexer()
        val s = parser_stmt().first()
        assert(s is Stmt.If && s.cnd is Expr.Acc && s.t.ss.size==1 && s.f.ss.size==0)
        assert(s.to_str() == "if x {\n" +
                "do(`.`)\n" +
                "} else {\n" +
                "}") { s.to_str() }
    }
    @Test
    fun ii_02_loop() {
        G.tks = ("loop { break }").lexer()
        parser_lexer()
        val s = parser_stmt().first()
        assert(s is Stmt.Loop && s.blk.ss.size==1 && s.blk.ss.first() is Stmt.Escape)
        assert(s.to_str() == "loop {\nescape((Break(([]))))\n}") { s.to_str() }
    }
    @Test
    fun ii_03_match () {
        G.tks = ("match x { 1 {} else {pass(x)} }").lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "match x {\n"+
            "1 {  }\n"+
            "else { do((pass(x))) }\n"+
            "}\n") { ss.to_str() }
    }
    @Test
    fun ii_04_match () {
        G.tks = ("match x { :X.Y: {} else {pass(x)} }").lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "match x {\n"+
                                ":X.Y: {  }\n"+
                                "else { do((pass(x))) }\n"+
                                "}\n") { ss.to_str() }
    }
    @Test
    fun ii_04_match_err () {
        G.tks = ("match x { :X.Y: {} ; 1 {} ; else {pass(x)} }").lexer()
        parser_lexer()
        assert(trap { parser_stmt() } == "anon : (lin 1, col 22) : expected type : have \"1\"")
    }
    @Test
    fun ii_05_match_err () {
        G.tks = ("match x { 1 {} ; X.Y {} ; else {pass(x)} }").lexer()
        parser_lexer()
        assert(trap { parser_stmt() } == "anon : (lin 1, col 22) : expected \"(\" : have \"{\"")
    }
    @Test
    fun ii_06_loop_n () {
        G.N = 1
        G.tks = ("loop i in 10 {}").lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "var i: Int\n"+
            "set i = 0\n"+
            "var mar_lim_3: Int\n"+
            "set mar_lim_3 = 10\n"+
            "loop {\n"+
            "set i = (i + 1)\n"+
            "if (i == mar_lim_3) {\n"+
            "escape((Break(([]))))\n"+
            "} else {\n"+
            "}\n"+
            "}\n") { ss.to_str() }
    }
    @Test
    fun ii_07_loop_until_err () {
        G.N = 1
        G.tks = ("loop { until 10 }").lexer()
        parser_lexer()
        assert(trap { parser_stmt() } == "anon : (lin 1, col 14) : expected \"(\" : have \"10\"")
    }

    // TUPLE

    @Test
    fun jj_00_tuple_err () {
        G.tks = ("[10,20]").lexer()
        parser_lexer()
        val e = parser_expr()
        assert(e.to_str() == "([10,20])") { e.to_str() }
        //assert(trap { parser_expr() } == "anon : (lin 1, col 8) : expected \":\" : have end of file")
    }
    @Test
    fun jj_01_tuple () {
        G.tks = ("""
            do {
                var v: [Int,Int] = [10,20]: [Int,Int]
            }
        """).lexer()
        parser_lexer()
        val s = parser_stmt().first()
        assert(s.to_str() == "do {\n" +
                "var v: [Int,Int]\n" +
                "set v = ([10,20]:[Int,Int])\n" +
                "}") { s.to_str() }
    }
    @Test
    fun jj_02_tuple () {
        G.tks = ("""
            do {
                var v: [Int,Int] = [10,20]:[Int,Int]
                var x: Int = v.1
            }
        """).lexer()
        parser_lexer()
        val s = parser_stmt().first()
        assert(s.to_str() == "do {\n" +
                "var v: [Int,Int]\n" +
                "set v = ([10,20]:[Int,Int])\n" +
                "var x: Int\n" +
                "set x = (v.1)\n" +
                "}") { s.to_str() }
    }
    @Test
    fun jj_03_expr() {
        G.tks = ("[10,[1]:[Int],20]: [Int,[Int],Int]").lexer()
        parser_lexer()
        val e = parser_expr()
        assert(e.to_str() == "([10,([1]:[Int]),20]:[Int,[Int],Int])") { e.to_str() }
    }
    @Test
    fun jj_04_expr() {
        G.tks = ("x.1.2").lexer()   // BUG: ((x.1).2)
        parser_lexer()
        val e = parser_expr()
        assert(e.to_str() == "((x.1).2)") { e.to_str() }
    }
    @Test
    fun jj_05_tuple_id_err () {
        G.tks = ("""
            var pos: [x:Int, Int]
        """).lexer()
        parser_lexer()
        //assert(trap { parser_stmt() } == "anon : (lin 2, col 22) : tuple error : missing field identifier")
        val s = parser_stmt().first()
        assert(s.to_str() == "var pos: [x:Int,Int]") { s.to_str() }
    }
    @Test
    fun jj_06_tuple_id () {
        G.tks = ("""
            do {
                var pos: [x:Int, y:Int] = [.x=10,.y=20]: [x:Int, y:Int]
                var x: Int = v.x
            }
        """).lexer()
        parser_lexer()
        val s = parser_stmt().first()
        assert(s.to_str() == "do {\n" +
                "var pos: [x:Int,y:Int]\n" +
                "set pos = ([.x=10,.y=20]:[x:Int,y:Int])\n" +
                "var x: Int\n" +
                "set x = (v.x)\n" +
                "}") { s.to_str() }
    }
    @Test
    fun jj_07_tuple_id_err () {
        G.tks = ("""
            set `x` = [.x=10,20]
        """).lexer()
        parser_lexer()
        //assert(trap { parser_stmt() } == "anon : (lin 2, col 23) : tuple error : missing field identifier")
        val s = parser_stmt().first()
        assert(s.to_str() == "set `x` = ([.x=10,20])") { s.to_str() }
    }

    // UNION

    @Test
    fun jk_01_union_cons () {
        G.tks = ("""
            var v: <Int,Int> = <.1=20> :<Int,Int>
        """).lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "var v: <Int,Int>\n" +
                "set v = <.1=20>:<Int,Int>\n") { ss.to_str() }
    }
    @Test
    fun jk_02_union_disc () {
        G.tks = ("v!1").lexer()
        parser_lexer()
        val e = parser_expr()
        assert(e is Expr.Disc && e.col is Expr.Acc && e.idx=="1")
        assert(e.to_str() == "(v!1)") { e.to_str() }
    }
    @Test
    fun jk_03_union_pred () {
        G.tks = ("v?1").lexer()
        parser_lexer()
        val e = parser_expr()
        assert(e is Expr.Pred && e.col is Expr.Acc && e.idx=="1")
        assert(e.to_str() == "(v?1)") { e.to_str() }
    }
    @Test
    fun jk_04_union_cons_id () {
        G.tks = ("""
            var v: <Err:(), Ok:Int> = <.Ok=20>: <Err:(),Ok:Int>
        """).lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "var v: <Err:(),Ok:Int>\n" +
                "set v = <.Ok=20>:<Err:(),Ok:Int>\n") { ss.to_str() }
    }

    // VECTOR

    @Test
    fun jl_01_vector () {
        G.tks = ("#[10,20]").lexer()
        parser_lexer()
        val e = parser_expr()
        assert(e.to_str() == "(#[10,20])") { e.to_str() }
        //assert(trap { parser_expr() } == "anon : (lin 1, col 8) : expected \":\" : have end of file")
    }
    @Test
    fun jl_02_vector () {
        G.tks = ("x[1]()[2]").lexer()
        parser_lexer()
        val e = parser_expr()
        assert(e.to_str() == "(((x[1])())[2])") { e.to_str() }
        //assert(trap { parser_expr() } == "anon : (lin 1, col 8) : expected \":\" : have end of file")
    }
    @Test
    fun jl_03_vector_size () {
        G.tks = ("#x").lexer()
        parser_lexer()
        val e = parser_expr()
        assert(e.to_str() == "(#x)") { e.to_str() }
        //assert(trap { parser_expr() } == "anon : (lin 1, col 8) : expected \":\" : have end of file")
    }
    @Test
    fun jl_04_vector_size () {
        G.tks = ("set #x = 1").lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "set (#x) = 1\n") { ss.to_str() }
    }
    @Test
    fun jl_05_vector_size_err () {
        G.tks = ("set ##x = 1").lexer()
        parser_lexer()
        assert(trap { parser_stmt() } == "anon : (lin 1, col 5) : set error : expected assignable destination")
    }

    // DATA

    @Test
    fun kk_01_data () {
        G.tks = ("""
            data Pos: [Int, Int]
        """).lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "data Pos: [Int,Int]\n") { ss.to_str() }
    }
    @Test
    fun kk_02_data () {
        G.tks = ("""
            var p: Pos = Pos [10, 10]:[]
        """).lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "var p: Pos\nset p = (Pos(([10,10]:[])))\n") { ss.to_str() }
    }
    @Test
    fun kk_03_data () {
        G.tks = ("""
            do {
                data Result: <Error: (), Success: Int>
                var r: Result = Result <.Success=10>: <Error:(),Success:Int>
                var i: Int = r!Success
            }
        """).lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "do {\n" +
                "data Result: <Error:(),Success:Int>\n" +
                "var r: Result\n" +
                "set r = (Result(<.Success=10>:<Error:(),Success:Int>))\n" +
                "var i: Int\n" +
                "set i = (r!Success)\n" +
                "}\n") { ss.to_str() }
    }
    @Test
    fun kk_04_data () {
        G.tks = ("""
            do {
                data Result: <Error: (), Success: Int>
                var r: Result = Result.Success(10)
            }
        """).lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "do {\n" +
                "data Result: <Error:(),Success:Int>\n" +
                "var r: Result\n" +
                "set r = (Result.Success(10))\n" +
                "}\n") { ss.to_str() }
    }

    // DATA / HIER

    @Test
    fun kj_04_data_hier () {
        G.tks = ("""
            data Event: [Int] + <>
        """).lexer()
        parser_lexer()
        val s = parser_stmt().first()
        assert(G.tk1?.str == "+")
        //assert(s.to_str() == "data Event.*: [x:Int,*:<>]") { s.to_str() }
        //assert(s.to_str() == "data Event: [Int] + <>") { s.to_str() }
        assert(s.to_str() == "data Event: [Int]") { s.to_str() }
    }
    @Test
    fun kj_05_data_hier_err () {
        G.tks = ("""
            data Event.*: [a:Int] {}
        """).lexer()
        parser_lexer()
        val s = parser_stmt().first()
        assert(s.to_str() == "data Event.*: [a:Int] {\n}") { s.to_str() }
        //assert(trap { parser_stmt() } == "anon : (lin 4, col 20) : expected \"<\" : have \"Int\"")
    }
    @Test
    fun kj_06_data_hier_err () {
        G.tks = ("""
            data Event.*: [] {
                Dn: ()
            }
        """).lexer()
        parser_lexer()
        assert(trap { parser_stmt() } == "anon : (lin 3, col 21) : expected \"[\" : have \"(\"")
    }
    @Test
    fun kj_07_data_hier_err () {
        G.tks = ("""
            data Event.*: [ts: Int] {
                Dn: [Int]   ;; x:Int
            }
        """).lexer()
        parser_lexer()
        val s = parser_stmt().first()
        assert(s.to_str() == "data Event.*: [ts:Int] {\n" +
                "Dn: [Int] {\n" +
                "}\n" +
                "}") { s.to_str() }
        //assert(trap { parser_stmt() } == "anon : (lin 3, col 21) : data error : missing field identifier")
        //assert(trap { parser_stmt() } == "anon : (lin 3, col 21) : union error : missing type identifiers")
    }
    @Test
    fun kj_08_data_hier () {
        G.tks = ("""
            data Event.*: [ts: Int] {
                Dn: []
                Up: []
            }
        """).lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "data Event.*: [ts:Int] {\n" +
                "Dn: [] {\n" +
                "}\n" +
                "Up: [] {\n" +
                "}\n" +
                "}\n") { ss.to_str() }
    }
    @Test
    fun kj_09_data_hier () {
        G.tks = ("""
            data A.*: [] {
                B: []
            }
        """).lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "data A.*: [] {\n" +
                "B: [] {\n" +
                "}\n" +
                "}\n") { ss.to_str() }
    }
    @Test
    fun kj_10_data_hier () {
        G.tks = ("""
            do {
                data Event1.*: [ts:Int] {
                    Quit: []
                    Frame: [ms:Int]
                    Key: [key:Int] {
                        Dn: []
                        Up: []
                    }
                }
                    
                data Event2.*: [] {
                    Quit: [ts:Int]
                    Frame: [ts:Int, ms:Int]
                    Key: [] {
                        Dn: [ts:Int, key:Int]
                        Up: [ts:Int, key:Int]
                    }
                }
                
                data Mouse.*: [] {
                    Left: []
                    Middle: []
                    Right: []
                }
            }
        """).lexer()
        parser_lexer()
        parser_stmt()
    }
    @Test
    fun kj_11_cons () {
        G.tks = ("B.F ()").lexer()
        parser_lexer()
        val e = parser_expr()
        assert(e is Expr.Cons && e.ts.size==2)
        assert(e.to_str() == "(B.F(()))") { e.to_str() }
    }
    @Test
    fun kj_12_data_hier_err () {
        G.tks = ("""
            data E.*: [ts:Int] {
                Quit: [Int]
            }
        """).lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "data E.*: [ts:Int] {\n" +
                "Quit: [Int] {\n" +
                "}\n" +
                "}\n") { ss.to_str() }
        //assert(trap { parser_stmt() } == "anon : (lin 3, col 25) : union error : missing type identifiers")
    }

    // DATA / HIER / EXTD

    @Test
    fun kl_00_data_hier_extd_err () {
        G.tks = ("""
            data X.Y: ()
        """).lexer()
        parser_lexer()
        assert(trap { parser_stmt() } == "anon : (lin 2, col 20) : expected \"*\" : have \"Y\"")
        //assert(trap { parser_stmt() } == "anon : (lin 2, col 21) : expected \".\" : have \":\"")
    }
    @Test
    fun kl_01_data_hier_extd_err () {
        G.tks = ("""
            data X.*: ()
        """).lexer()
        parser_lexer()
        assert(trap { parser_stmt() } == "anon : (lin 2, col 23) : expected \"[\" : have \"(\"")
    }
    @Test
    fun kl_02_data_hier_extd_err () {
        G.tks = ("data X.*: [Int]").lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "data X.*: [Int] {\n}\n") { ss.to_str() }
        //assert(trap { parser_stmt() } == "anon : (lin 1, col 11) : tuple error : missing field identifier")
    }
    @Test
    fun kl_03_data_hier_extd () {
        G.tks = ("data X.*: [y:Int]").lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "data X.*: [y:Int] {\n}\n") { ss.to_str() }
    }

    // CATCH / THROW

    @Test
    fun ll_01_catch () {
        G.tks = ("catch { }").lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "catch {\n" +
                "}\n") { ss.to_str() }
    }
    @Test
    fun ll_02_catch_err () {
        G.tks = ("catch x { }").lexer()
        parser_lexer()
        assert(trap { parser_stmt() } == "anon : (lin 1, col 7) : expected \":\" : have \"x\"")
        //assert(trap { parser_stmt() } == "anon : (lin 1, col 7) : expected type : have \"x\"")
    }
    @Test
    fun ll_03_catch_err () {
        G.tks = ("catch :Int: { }").lexer()
        parser_lexer()
        assert(trap { parser_stmt() } == "anon : (lin 1, col 8) : exception error : expected data type")
    }
    @Test
    fun ll_04_catch () {
        G.tks = ("catch :X.Y.Z: { print(x) }").lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "catch :X.Y.Z: {\n" +
                "print(x)\n" +
                "}\n") { ss.to_str() }
    }
    @Test
    fun ll_05_catch () {
        G.tks = ("catch :X: { throw(X[]) }").lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "catch :X: {\n" +
                "do(throw((X(([])))))\n" +
                "}\n") { ss.to_str() }
    }
    @Test
    fun ll_06_throw_as_expr () {
        G.tks = ("pass(throw()) }").lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "do((pass(throw((Error(([])))))))\n") { ss.to_str() }
    }

    // DEFER

    @Test
    fun mm_01_defer () {
        G.tks = ("defer { print(1) }").lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "defer {\n" +
                "print(1)\n" +
                "}\n") { ss.to_str() }
    }

    // ESCAPE

    @Test
    fun nn_01_escaoe () {
        G.tks = ("do :X: { escape(X[]) }").lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "do :X: {\n" +
                "escape((X(([]))))\n" +
                "}\n") { ss.to_str() }
    }

    // NAT

    @Test
    fun oo_01_nat () {
        G.tks = ("`f`").lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "do(`f`)\n") { ss.to_str() }
    }
    @Test
    fun oo_02_nat () {
        G.tks = ("`f`([10])").lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "do((`f`(([10]))))\n") { ss.to_str() }
    }

    // EXPR / IF / MATCH

    @Test
    fun pp_01_if () {
        G.tks = ("do(if x => t => f)").lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "do(if x => t => f)\n") { ss.to_str() }
    }
    @Test
    fun pp_02_match () {
        G.tks = ("var x = match x {}").lexer()
        parser_lexer()
        assert(trap { parser_stmt() } == "anon : (lin 1, col 18) : match error : unexpected \"}\"")
    }
    @Test
    fun pp_04_match () {
        G.tks = ("set y = match x { 1=>1 else=>x }").lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "set y = match x {\n" +
                "1 => 1\n" +
                "else => x\n" +
                "}\n") { ss.to_str() }
    }

    // WHERE

    @Test
    fun qq_01_where () {
        G.tks = ("var x = y where { y = 10 }").lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "var x\n"+
            "do {\n"+
            "var y\n"+
            "set y = 10\n"+
            "set x = y\n"+
            "}\n") { ss.to_str() }
    }

    // TEMPLATE

    @Test
    fun tt_01_data () {
        G.tks = ("""
            data Maybe {t:Type}: <Nothing:(), Just:{t}>
            var x: Maybe {Int} = Maybe {Int}.Just(10)
            var x: Maybe {Int} = Just(10)
            var x: Maybe = Just(10)
            var x = Maybe.Just(10)
        """).lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "data Pos: [Int,Int]\n") { ss.to_str() }
    }
    @Test
    fun tt_02_func () {
        G.tks = ("""
            func {n:Int, t:Type} f: (v:{t}) -> [Int,Int] {
                var x: #[{t}*{n}] = #[v]
                return([##x, x[0]])
            }
            print(f {10,Int} (20))
            print(f {10} (20))

            func {n:Int} f: (v:#[Int*{n}]) -> () {
                return(##v)
            }
            print(f(#[1,2,3]))
        """).lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "data Pos: [Int,Int]\n") { ss.to_str() }
    }

    @Test
    fun NEW_tt_xx_data () {     // template
        G.tks = ("""
            data Exec {a,b,n}: <Yield: a, Return: b, X: #[n*Int]>
            var x: Exec {(),Int} = Exec.Return(10)
            var i = x!Return
            var y = x?Yield
            `printf("%d %d\n", i, y);`
        """).lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "data Pos: [Int,Int]\n") { ss.to_str() }
    }

    // MISC

    @Test
    fun zz_01_lin () {
        G.tks = ("""
            do(1 /= 1)
        """).lexer()
        parser_lexer()
        assert(trap { parser_stmt() } == "anon : (lin 2, col 19) : expected expression : have \"=\"")
    }
}
