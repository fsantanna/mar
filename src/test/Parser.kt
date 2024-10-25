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
        val tp = parser_type()
        assert(tp is Type.Prim && tp.tk.str=="Int")
    }
    @Test
    fun aj_02_type () {
        G.tks = ("func (Int,Int) -> ()").lexer()
        parser_lexer()
        val tp = parser_type()
        assert(tp is Type.Proto.Func && tp.tk.str=="func" && tp.inps.size==2 && tp.out is Type.Unit)
    }
    @Test
    fun aj_03_type () {
        G.tks = ("func () -> Int").lexer()
        parser_lexer()
        val tp = parser_type()
        assert(tp is Type.Proto.Func && (tp.out as Type.Prim).tk.str=="Int")
        assert(tp.to_str() == "func () -> Int")
    }
    @Test
    fun aj_04_type () {
        G.tks = ("func (x: Int) -> Int").lexer()
        parser_lexer()
        val tp = parser_type()
        assert(tp is Type.Proto.Func && (tp.out as Type.Prim).tk.str=="Int")
        assert(tp.to_str() == "func (x: Int) -> Int")
    }
    @Test
    fun aj_05_type () {
        G.tks = ("func (Int, x: Int) -> ()").lexer()
        parser_lexer()
        assert(trap { parser_type() } == "anon : (lin 1, col 12) : expected type : have \"x\"")
    }
    @Test
    fun aj_06_type () {
        G.tks = ("func (x: Int, Int) -> ()").lexer()
        parser_lexer()
        assert(trap { parser_type() } == "anon : (lin 1, col 15) : expected variable : have \"Int\"")
    }
    @Test
    fun aj_07_type_ptr () {
        G.tks = ("\\Int").lexer()
        parser_lexer()
        val tp = parser_type()
        assert(tp is Type.Pointer && tp.ptr is Type.Prim)
        assert(tp.to_str() == "\\Int")
    }
    @Test
    fun aj_08_type_coro () {
        G.tks = ("coro () -> () -> () -> ()").lexer()
        parser_lexer()
        val tp = parser_type()
        assert(tp is Type.Proto.Coro && tp.inps.size==0 && tp.res is Type.Unit && tp.yld is Type.Unit && tp.out is Type.Unit)
        assert(tp.to_str() == "coro () -> () -> () -> ()")
    }
    @Test
    fun aj_09_type_exec () {
        G.tks = ("exec (Int) -> () -> () -> Int").lexer()
        parser_lexer()
        val tp = parser_type()
        assert(tp is Type.Exec && tp.inps.size==1 && tp.res is Type.Unit && tp.yld is Type.Unit && tp.out is Type.Prim)
        assert(tp.to_str() == "exec (Int) -> () -> () -> Int") { tp.to_str() }
    }
    @Test
    fun aj_10_type_coro_err () {
        G.tks = ("coro (Int,Int) -> ()").lexer()
        parser_lexer()
        assert(trap { parser_type() } == "anon : (lin 1, col 21) : expected \"->\" : have end of file")
        //assert(trap { parser_type() } == "anon : (lin 1, col 1) : coro error : unexpected second argument")
        //assert(trap { parser_type() } == "anon : (lin 1, col 7) : expected \"<\" : have \"Int\"")
    }
    @Test
    fun aj_11_type_exec_err () {
        G.tks = ("exec (Int,Int) -> Int").lexer()
        parser_lexer()
        assert(trap { parser_type() } == "anon : (lin 1, col 22) : expected \"->\" : have end of file")
        //assert(trap { parser_type() } == "anon : (lin 1, col 7) : expected \"<\" : have \"Int\"")
        //assert(trap { parser_type() } == "anon : (lin 1, col 1) : exec error : unexpected second argument")
    }
    @Test
    fun aj_12_data_hier () {
        G.tks = ("X.Y.Z").lexer()
        parser_lexer()
        val tp = parser_type()
        assert(tp is Type.Data && tp.ts.size==3)
        assert(tp.to_str() == "X.Y.Z")
    }

    // TUPLE / UNION

    @Test
    fun ak_01_type_tuple () {
        G.tks = ("[]").lexer()
        parser_lexer()
        val tp = parser_type()
        assert(tp is Type.Tuple && tp.ts.size==0)
        assert(tp.to_str() == "[]") { tp.to_str() }
    }
    @Test
    fun ak_02_type_tuple () {
        G.tks = ("[Int,[Bool],()]").lexer()
        parser_lexer()
        val tp = parser_type()
        assert(tp is Type.Tuple && tp.ts.size==3)
        assert(tp.to_str() == "[Int,[Bool],()]") { tp.to_str() }
    }
    @Test
    fun ak_03_type_tuple () {  // tuple field names
        G.tks = ("[x:Int,y:[Bool],z:()]").lexer()
        parser_lexer()
        val tp = parser_type()
        assert(tp is Type.Tuple && tp.ts.size==3)
        assert(tp.to_str() == "[x:Int,y:[Bool],z:()]") { tp.to_str() }
    }
    @Test
    fun ak_04_type_union () {
        G.tks = ("<>").lexer()  // lex sees <> as a single token
        parser_lexer()
        val tp = parser_type()
        assert(tp is Type.Union && tp.ts.size==0)
        assert(tp.to_str() == "<>") { tp.to_str() }
    }
    @Test
    fun ak_05_type_union () {
        G.tks = ("<Int,<Bool>,()>").lexer()
        parser_lexer()
        val tp = parser_type()
        //println(tp)
        assert(tp is Type.Union && tp.ts.size==3)
        assert(tp.to_str() == "<Int,<Bool>,()>") { tp.to_str() }
    }
    @Test
    fun TODO_ak_06_type_union () {  // tuple field names
        G.tks = ("<X:Int,Y:[Bool],Z:()>").lexer()
        parser_lexer()
        val tp = parser_type()
        assert(tp is Type.Tuple && tp.ts.size==3)
        assert(tp.to_str() == "[X:Int,Y:[Bool],Z:()]") { tp.to_str() }
    }
    @Test
    fun ak_07_type_union_err () {
        G.tks = ("var x: Int = <.1=10>: Int").lexer()
        parser_lexer()
        assert(trap { parser_stmt() } == "anon : (lin 1, col 23) : expected \"<\" : have \"Int\"")
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
        assert(s.to_str() == "do {\n(print(a))\n}") { s.to_str() }
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
        val e = parser_stmt().first()
        assert(e.to_str() == "return(10)") { e.to_str() }
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
        assert(e is Expr.Char && e.tk.str == "'x'")
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
        assert(trap { parser_stmt() } == "anon : (lin 1, col 1) : expected expression : have \"create\"")
    }
    @Test
    fun hh_02_resume() {
        G.tks = ("resume xf()").lexer()
        parser_lexer()
        val s = parser_stmt().first()
        assert(s is Stmt.XExpr && s.e is Expr.Resume && s.e.exe is Expr.Acc && s.e.arg is Expr.Unit)
        assert(s.to_str() == "resume xf()") { s.to_str() }
    }
    @Test
    fun hh_03_yield() {
        G.tks = ("yield()").lexer()
        parser_lexer()
        val s = parser_stmt().first()
        assert(s is Stmt.XExpr && s.e is Expr.Yield && s.e.arg is Expr.Unit)
        assert(s.to_str() == "yield()") { s.to_str() }
    }
    @Test
    fun hh_04_spawn() {
        G.tks = ("set x = create(f)").lexer()
        parser_lexer()
        val s = parser_stmt().first()
        assert(s is Stmt.Set && s.src is Expr.Create && s.src.co is Expr.Acc)
        assert(s.to_str() == "set x = create(f)")
    }
    @Test
    fun hh_05_resume() {
        G.tks = ("set x = resume xf(false)").lexer()
        parser_lexer()
        val s = parser_stmt().first()
        assert(s is Stmt.Set && s.src is Expr.Resume && s.src.exe is Expr.Acc && s.src.arg is Expr.Bool)
        assert(s.to_str() == "set x = resume xf(false)")
    }
    @Test
    fun hh_06_yield() {
        G.tks = ("set y = yield(null)").lexer()
        parser_lexer()
        val s = parser_stmt().first()
        assert(s is Stmt.Set && s.src is Expr.Yield && s.src.arg is Expr.Null)
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

    // IF / LOOP

    @Test
    fun ii_01_if() {
        G.tks = ("if x {`.`} else {}").lexer()
        parser_lexer()
        val s = parser_stmt().first()
        assert(s is Stmt.If && s.cnd is Expr.Acc && s.t.ss.size==1 && s.f.ss.size==0)
        assert(s.to_str() == "if x {\n" +
                "`.`\n" +
                "} else {\n" +
                "}") { s.to_str() }
    }
    @Test
    fun ii_02_loop() {
        G.tks = ("loop { break }").lexer()
        parser_lexer()
        val s = parser_stmt().first()
        assert(s is Stmt.Loop && s.blk.ss.size==1 && s.blk.ss.first() is Stmt.Break)
        assert(s.to_str() == "loop {\nbreak\n}") { s.to_str() }
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
        assert(trap { parser_stmt() } == "anon : (lin 2, col 22) : tuple error : missing field identifier")
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
        assert(trap { parser_stmt() } == "anon : (lin 2, col 23) : tuple error : missing field identifier")
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
    @Test
    fun kk_05_sub_sub () {     // Event.*
        G.tks = ("""
            struct Event {
                int tag;
                int ts;
                union {
                    struct {
                        int ms;
                    } Frame;
                    struct {
                        in tag;
                        int key;
                        union {
                            struct {
                            } Dn;
                            struct {
                            } Up;
                        }
                    } Key;
                }
            }
            struct Frame {
                
            data Event.*: [
                ts: Int,
                *: <
                    Quit:   (),
                    Frame:  Int,
                    Key:    [
                        key: Int,
                        *: <Dn:(), Up:()>
                    ],
                >,
            ]
            data Event.Pause.*: [
            
            data Event: <
                Quit:  [ts:Int],
                Frame: [ts:Int, ms:Int],
                Key: <
                    Dn: [ts:Int, key:Int],
                    Up: [ts:Int, key:Int],
                >
            >
                
            
            data Event: [ts:Int] {
                Quit:   []
                Frame:  [ms:Int]
                Key:    [key:Int] {
                    Dn: []
                    Up: []
                }
            }
            data Mouse: <
                Left:   (),
                Middle: (),
                Right:  (),
            >
            
            data Error.*: [msg: String]
            data Error.Runtime: []
        """).lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "var v: <Int,Int>\n" +
                "set v = <.1 20>:<Int,Int>\n") { ss.to_str() }
    }
    @Test
    fun NEW_kk_XX_data () {     // Event.*
        G.tks = ("""
            data Event: ()
            data Event.Keys
            var p: Pos = [10,10]
            
            data Mouse_Button: <
                Left   = (),
                Middle = (),
                Right  = (),
            >
            
            data Error.*: [msg: String]
            data Error.Runtime: []
        """).lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "var v: <Int,Int>\n" +
                "set v = <.1 20>:<Int,Int>\n") { ss.to_str() }
    }

    // CONS

    @Test
    fun ll_01_cons () {
        G.tks = ("B.F ()").lexer()
        parser_lexer()
        val e = parser_expr()
        assert(e is Expr.Cons && e.ts.size==2)
        assert(e.to_str() == "(B.F(()))") { e.to_str() }
    }

    // TEMPLATE

    @Test
    fun NEW_tt_01_data () {     // template
        G.tks = ("""
            data Exec {a,b}: <Yield: a, Return: b>
            var x: Exec {(),Int} = Exec.Return(10)
            var i = x!Return
            var y = x?Yield
            `printf("%d %d\n", i, y);`
        """).lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "data Pos: [Int,Int]\n") { ss.to_str() }
    }
}
