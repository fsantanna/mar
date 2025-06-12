package test

import mar.*
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Parser {
    @Test
    fun de_01_do_pass() {
        G.tks = "do(10)".lexer()
        parser_lexer()
        val s = parser_stmt().first()
        assert(s is Stmt.Pass)
        assert(s.to_str() == "do(10)") { s.to_str() }
    }

    // IF / LOOP / MATCH

    @Test
    fun ii_04_match () {
        G.tks = ("match x { :X.Y {} else {pass(x)} }").lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "match x {\n"+
                                ":X.Y {  }\n"+
                                "else { do((pass(x))) }\n"+
                                "}\n") { ss.to_str() }
    }
    @Test
    fun ii_04_match_err () {
        G.tks = ("match x { :X.Y {} ; 1 {} ; else {pass(x)} }").lexer()
        parser_lexer()
        assert(trap { parser_stmt() } == "anon : (lin 1, col 21) : expected type : have \"1\"")
    }
    @Test
    fun ii_05_match_err () {
        G.tks = ("match x { 1 {} ; X.Y {} ; else {pass(x)} }").lexer()
        parser_lexer()
        assert(trap { parser_stmt() } == "anon : (lin 1, col 22) : expected \"(\" : have \"{\"")
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
    fun kk_05_data_err () {
        G.tks = ("""
            Int(10)
        """).lexer()
        parser_lexer()
        assert(trap { parser_stmt() } == "anon : (lin 2, col 16) : constructor error : unexpected primitive type")
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
        assert(e is Expr.Cons && e.tp.ts.size==2)
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
        G.tks = ("catch :Int { }").lexer()
        parser_lexer()
        assert(trap { parser_stmt() } == "anon : (lin 1, col 8) : exception error : expected data type")
    }
    @Test
    fun ll_04_catch () {
        G.tks = ("catch :X.Y.Z { print(x) }").lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "catch :X.Y.Z {\n" +
                "print(x)\n" +
                "}\n") { ss.to_str() }
    }
    @Test
    fun ll_05_catch () {
        G.tks = ("catch :X { throw(X[]) }").lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "catch :X {\n" +
                "throw((X(([]))))\n" +
                "}\n") { ss.to_str() }
    }
    @Test
    fun ll_06_throw_as_expr () {
        G.tks = ("pass(throw()) }").lexer()
        parser_lexer()
        //val ss = parser_stmt()
        //assert(ss.to_str() == "do((pass(throw((Error(([])))))))\n") { ss.to_str() }
        assert(trap { parser_stmt() } == "anon : (lin 1, col 6) : expected expression : have \"throw\"")
    }

    // EXPR / IF / MATCH

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

    // WHERE / IT

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
    @Test
    fun qq_02_it () {
        G.tks = ("var x = it").lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "var x\n"+
            "set x = it\n"+
            "}\n") { ss.to_str() }
    }

    // TEMPLATE

    @Test
    fun tt_01_data_type () {
        G.tks = ("""
            data Maybe {{t:Type}}: <Nothing:(), Just:{{t}}>
        """).lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "data Maybe {{t: Type}}: <Nothing:(),Just:{{t}}>\n") { ss.to_str() }
    }
    @Test
    fun tt_02_data_type () {
        G.tks = ("""
            do {
                var x: Maybe {{:Int}} = Maybe {{:Int}}.Just(10)
                var x: Maybe {{:Int}} = Just(10)
                var x: Maybe = Just(10)
                var x = Maybe.Just(10)
            }
        """).lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "do {\n"+
            "var x: Maybe {{:Int}}\n"+
            "set x = (Maybe {{:Int}}.Just(10))\n"+
            "var x: Maybe {{:Int}}\n"+
            "set x = (Just(10))\n"+
            "var x: Maybe\n"+
            "set x = (Just(10))\n"+
            "var x\n"+
            "set x = (Maybe.Just(10))\n"+
            "}\n") { ss.to_str() }
    }
    @Test
    fun tt_03_data_num () {
        G.tks = ("""
            data Vec {{n:Int}}: #[Int * {{n}}]
        """).lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "data Vec {{n: Int}}: #[Int*{{n}}]\n") { ss.to_str() }
    }
    @Test
    fun tt_04_data_num () {
        G.tks = ("""
            var vs: Vec {{n}}
        """).lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "var vs: Vec {{n}}\n") { ss.to_str() }
    }
    @Test
    fun tt_05_func () {
        G.tks = ("""
            do {
                func f {{t:Type}}: (v:{{t}}) -> () {
                    print(v)
                }
                print(f {{:Int}} (10))
            }
        """).lexer()
        parser_lexer()
        val ss = parser_stmt()
        assert(ss.to_str() == "do {\n" +
            "func f {{t: Type}}: (v: {{t}}) -> () {\n" +
            "print(v)\n" +
            "}\n" +
            "print((f {{:Int}} (10)))\n" +
            "}\n") { ss.to_str() }
    }
    @Test
    fun TODO_tt_XX_func () {
        G.tks = ("""
            func {{n:Int, t:Type}} f: (v:{{t}}) -> [Int,Int] {
                var x: #[{{t}}*{{n}}] = #[v]
                return([##x, x[0]])
            }
            print(f {{10,Int}} (20))
            print(f {{10}} (20))

            func {{n:Int}} f: (v:#[Int*{{n}}]) -> () {
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
}
