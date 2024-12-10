package mar.test

import mar.*
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import test.static

fun infer (me: String): String? {
    return trap {
        G.reset()
        G.tks = me.lexer()
        parser_lexer()
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
    }
    //return null
}

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Infer {

    // DCL

    @Test
    fun aa_01_infer_dcl () {
        val out = infer("""
                var r = 10
            """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "var r: Int\n" +
                "set r = 10\n" +
                "}") { G.outer!!.to_str() }
    }
    @Test
    fun aa_02_infer_dcl () {
        val out = infer("""
            var x
        """)
        assert(out == "anon : (lin 2, col 13) : inference error : unknown type") { out!! }
    }
    @Test
    fun aa_03_infer_dcl () {
        val out = infer("""
                var x = [10, 10]
            """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "var x: [Int,Int]\n" +
                "set x = ([10,10]:[Int,Int])\n" +
                "}") { G.outer!!.to_str() }
    }
    @Test
    fun aa_04_infer_dcl () {
        val out = infer("""
                var x: <Int,()> = <.1=10>
            """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "var x: <Int,()>\n" +
                "set x = <.1=10>:<Int,()>\n" +
                "}") { G.outer!!.to_str() }
    }

    // PROTO / CALL

    @Test
    fun bb_01_infer_exec () {
        val out = infer("""
                coro genFunc: () -> () -> () -> () {}
                var genObj = create(genFunc)
            """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "coro genFunc: () -> () -> () -> () {\n" +
                "}\n" +
                "var genObj: exec () -> () -> () -> ()\n" +
                "set genObj = create(genFunc)\n" +
                "}") { G.outer!!.to_str() }
    }
    @Test
    fun bb_02_infer_call () {
        val out = infer("""
                func f: (v: [Int,Int]) -> Int {
                    return(v.1 + v.2)
                }
                var x = f([10,20])
            """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "func f: (v: [Int,Int]) -> Int {\n" +
                "set `mar_ret`: Int = ((v.1) + (v.2))\n" +
                "escape((Return(([]:[]))))\n" +
                "}\n" +
                "var x: Int\n" +
                "set x = (f(([10,20]:[Int,Int])))\n" +
                "}") { G.outer!!.to_str() }
    }
    @Test
    fun bb_03_infer_call () {
        val out = infer("""
                func f: (v: <(),Int>) -> () {
                }
                var x = f(<.1=()>)
            """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "func f: (v: <(),Int>) -> () {\n" +
                "}\n" +
                "var x: ()\n" +
                "set x = (f(<.1=()>:<(),Int>))\n" +
                "}") { G.outer!!.to_str() }
    }
    @Test
    fun bb_04_infer_return () {
        val out = infer("""
                func f: () -> <(),Int> {
                    return(<.1=()>)
                }
                var x = f()
            """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "func f: () -> <(),Int> {\n" +
                "set `mar_ret`: <(),Int> = <.1=()>:<(),Int>\n" +
                "escape((Return(([]:[]))))\n" +
                "}\n" +
                "var x: <(),Int>\n" +
                "set x = (f())\n" +
                "}") { G.outer!!.to_str() }
    }

    // CORO

    @Test
    fun cc_01_infer_start () {
        val out = infer("""
                coro co: (v: <(),Int>) -> () -> () -> () {
                }
                var exe = create(co)
                start exe(<.1=()>)
            """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "coro co: (v: <(),Int>) -> () -> () -> () {\n" +
                "}\n" +
                "var exe: exec (<(),Int>) -> () -> () -> ()\n" +
                "set exe = create(co)\n" +
                "start exe(<.1=()>:<(),Int>)\n" +
                "}") { G.outer!!.to_str() }
    }
    @Test
    fun cc_02_infer_resume () {
        val out = infer("""
                coro co: () -> <(),Int> -> () -> () {
                }
                var exe = create(co)
                start exe()
                resume exe(<.1=()>)
            """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "coro co: () -> <(),Int> -> () -> () {\n" +
                "}\n" +
                "var exe: exec () -> <(),Int> -> () -> ()\n" +
                "set exe = create(co)\n" +
                "start exe()\n" +
                "resume exe(<.1=()>:<(),Int>)\n" +
                "}") { G.outer!!.to_str() }
    }
    @Test
    fun cc_03_infer_yield () {
        val out = infer("""
                coro co: () -> () -> <(),Int> -> () {
                    var x = yield(<.1=()>)
                }
                var exe = create(co)
                start exe()
            """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "coro co: () -> () -> <(),Int> -> () {\n" +
                "var x: ()\n" +
                "set x = yield(<.1=()>:<(),Int>)\n" +
                "}\n" +
                "var exe: exec () -> () -> <(),Int> -> ()\n" +
                "set exe = create(co)\n" +
                "start exe()\n" +
                "}") { G.outer!!.to_str() }
    }
    @Test
    fun cc_04_infer_start_resume () {
        val out = infer("""
                data A: Int
                data B: Int
                data C: Int
                data D: Int
                coro co: (a:A) -> B -> C -> D {
                    var x = yield(`x`)
                }
                var exe = create(co)
                var y = start exe(`x`)
                var z = resume exe(`x`)
            """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "data A: Int\n" +
                "data B: Int\n" +
                "data C: Int\n" +
                "data D: Int\n" +
                "coro co: (a: A) -> B -> C -> D {\n" +
                "var x: B\n" +
                "set x = yield(`x`: C)\n" +
                "}\n" +
                "var exe: exec (A) -> B -> C -> D\n" +
                "set exe = create(co)\n" +
                "var y: <C,D>\n" +
                "set y = start exe(`x`: A)\n" +
                "var z: <C,D>\n" +
                "set z = resume exe(`x`: B)\n" +
                "}") { G.outer!!.to_str() }
    }
    @Test
    fun cc_05_infer_err () {
        val out = infer("""
            coro gen1: (v: Int) -> () -> () -> () {
                `printf("%d\n", mar_exe->mem.v);`
            }
            var co1 = create(gen1)
            start co1(<.1=1>)
        """)
        assert(out == "anon : (lin 6, col 23) : inference error : incompatible types") { out!! }
    }
    @Test
    fun cc_06_infer_err () {
        val out = infer("""
            coro gen1: (v: Int) -> () -> () -> () {
                `printf("%d\n", mar_exe->mem.v);`
            }
            var co1 = create(gen1)
            start co1([])
        """)
        assert(out == "anon : (lin 6, col 23) : inference error : incompatible types") { out!! }
    }

    // DATA / TUPLE / UNION

    @Test
    fun dd_01_infer_err () {
        val out = infer("""
            var x = <.1=()>
        """)
        assert(out == "anon : (lin 2, col 21) : inference error : unknown type") { out!! }
    }
    @Test
    fun dd_02_infer_data_union () {
        val out = infer("""
                data Res: <Err:(),Ok:Int>
                var r = Res <.Ok=10>
            """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "data Res: <Err:(),Ok:Int>\n" +
                "var r: Res\n" +
                "set r = (Res(<.Ok=10>:<Err:(),Ok:Int>))\n" +
                "}") { G.outer!!.to_str() }
    }
    @Test
    fun dd_03_infer_data_tuple () {
        val out = infer("""
                data Pos: [x:Int,y:Int]
                var r = Pos [10,20]
            """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "data Pos: [x:Int,y:Int]\n" +
                "var r: Pos\n" +
                "set r = (Pos(([10,20]:[x:Int,y:Int])))\n" +
                "}") { G.outer!!.to_str() }
    }
    @Test
    fun dd_04_infer_err () {
        val out = infer("""
            var x: Int
            var ts = x.ts
        """)
        assert(out == "anon : (lin 3, col 13) : inference error : unknown type") { out!! }
    }
    @Test
    fun dd_05_infer_tuple () {
        val out = infer("""
                data X: [x:[a:Int]]
                var x = X [[10]]
                var a = x.x.a
            """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "data X: [x:[a:Int]]\n" +
                "var x: X\n" +
                "set x = (X(([([10]:[a:Int])]:[x:[a:Int]])))\n" +
                "var a: Int\n" +
                "set a = ((x.x).a)\n" +
                "}") { G.outer!!.to_str() }
    }
    @Test
    fun dd_06_infer_union () {
        val out = infer("""
                data X: <A:[a:Int]>
                var x: X = X.A [10]
                var a = x!A.a
            """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "data X: <A:[a:Int]>\n" +
                "var x: X\n" +
                "set x = (X.A(([10]:[a:Int])))\n" +
                "var a: Int\n" +
                "set a = ((x!A).a)\n" +
                "}") { G.outer!!.to_str() }
    }
    @Test
    fun dd_07_infer_tuple () {
        val out = infer("""
            print([1,[2]])
        """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "data X: [x:[a:Int]]\n" +
                "var x: X\n" +
                "set x = (X(([([10]:[a:Int])]:[x:[a:Int]])))\n" +
                "var a: Int\n" +
                "set a = ((x.x).a)\n" +
                "}") { G.outer!!.to_str() }
    }

    // NAT

    @Test
    fun ee_01_infer_nat_err () {
        val out = infer("""
            var x = `10`
        """)
        assert(out == "anon : (lin 2, col 21) : inference error : unknown type") { out!! }
    }
    @Test
    fun ee_02_infer_nat () {
        val out = infer("""
                var x = `10`:Int
            """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "var x: Int\n" +
                "set x = `10`: Int\n" +
                "}") { G.outer!!.to_str() }
    }
    @Test
    fun ee_03_infer_nat () {
        val out = infer("""
            `f`([10])
        """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "(`f`(([10]:[Int])))\n" +
                "}") { G.outer!!.to_str() }
    }
    @Test
    fun ee_04_infer_nat_err() {
        val out = infer("""
            `f`(`x`)
        """)
        assert(out == "anon : (lin 2, col 13) : inference error : unknown type") { out!! }
    }
    @Test
    fun ee_05_nat_err () {
        val out = static("""
            var x = `x`
        """)
        assert(out == "anon : (lin 2, col 21) : inference error : unknown type") { out!! }
    }
}
