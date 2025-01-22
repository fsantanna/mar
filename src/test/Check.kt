package test

import mar.*
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

fun check (me: String): String? {
    return trap {
        G.reset()
        G.tks = me.lexer()
        parser_lexer()
        val tk0 = G.tk1!!
        val ss = parser_list(null, { accept_enu("Eof") }, {
            parser_stmt()
        }).flatten()
        G.outer = Stmt.Block(tk0, null, listOf(
                Stmt.Data(tk0, Tk.Type("Return", tk0.pos), emptyList(), Type.Tuple(tk0, emptyList()), emptyList()),
                Stmt.Data(tk0, Tk.Type("Break", tk0.pos), emptyList(), Type.Tuple(tk0, emptyList()), emptyList()),
        ) + ss)
        cache_ups()
        check_vars()
        infer_apply()
        check_types()
    }
    //return null
}

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Check {

    // VAR

    @Test
    fun aa_01_var() {
        val out = check("""
                do {
                    var x: Int
                    do {
                        var y: Int
                    }
                }
            """
        )
        assert(out == null) { out!! }
    }
    @Test
    fun aa_02_var_dup() {
        val out = check("""
            do {
                var x: Int
                do {
                    var x: Int
                }
            }
        """
        )
        //assert(out == "10") { out!! }
        assert(out == "anon : (lin 5, col 25) : declaration error : variable \"x\" is already declared") { out!! }
    }
    @Test
    fun aa_03_var_none() {
        val out = check("""
            do {
                f()
            }
        """)
        assert(out == "anon : (lin 3, col 17) : access error : variable \"f\" is not declared") { out!! }
    }
    @Test
    fun aa_04_func_err () {
        val out = check("""
            ;;do {
                var f: func () -> () ;; missing implementation
            ;;}
        """)
        //assert(out == "anon : (lin 3, col 17) : declaration error : missing implementation") { out!! }
        assert(out == "anon : (lin 3, col 24) : type error : unexpected \"func\"") { out!! }
    }
    @Test
    fun aa_05_coro_err () {
        val out = check("""
            do {
                var co: coro () -> ()   ;; missing implementation
            }
        """)
        assert(out == "anon : (lin 3, col 25) : type error : unexpected \"coro\"") { out!! }
    }
    @Test
    fun aa_06_coro_decl_err () {
        val out = check("""
                ;;do {
                    func f: () -> () {
                    }
                ;;}
            """)
        assert(out == null) { out!! }
        //assert(out == "anon : (lin 5, col 17) : implementation error : variable \"f\" is not declared") { out!! }
    }
    @Test
    fun aa_07_coro_err () {
        val out = check("""
            coro gen1: (v: Int) -> () -> () -> () {
                `printf("%d\n", mar_exe->mem.v);`
            }
            var co1 = create(gen1)
            start co1([])
        """)
        //assert(out == "anon : (lin 6, col 23) : inference error : incompatible types") { out!! }
        assert(out == "anon : (lin 6, col 13) : start error : types mismatch") { out!! }
    }
    @Test
    fun aa_08_var_dup_no() {
        val out = check("""
            do {
                do {
                    var x: Int
                }
                var x: Int
            }
        """
        )
        assert(out == null) { out!! }
    }
    @Test
    fun aa_09_var_dup_no() {
        val out = check("""
            func f: (x:Int) -> () {
            }
            var x:Int
        """
        )
        assert(out == null) { out!! }
    }

    // NUMS

    @Test
    fun ab_01_num() {
        val out = check("""
                print(0.5)
            """
        )
        assert(out == null) { out!! }
    }
    @Test
    fun ab_02_num() {
        val out = check("""
                print(1 + 0.5)
            """
        )
        assert(out == null) { out!! }
    }
    @Test
    fun ab_03_add() {
        val out = check("""
            do(true + false)
        """)
        assert(out == "anon : (lin 2, col 21) : operation error : types mismatch") { out!! }
    }

    // FUNC / CALL

    @Test
    fun ab_01_func_rec () {
        val out = check("""
                do {
                    func f: () -> () {
                        f()
                    }
                }
            """)
        assert(out == null) { out!! }
    }
    @Test
    fun ab_02_func_rec_mutual () {
        val out = check("""
                ;;do {
                    func f: () -> () {
                        g()
                    }
                    func g: () -> () {
                        f()
                    }
                ;;}
            """)
        assert(out == null) { out!! }
    }
    @Test
    fun ab_03_call_err() {
        val out = check("""
            1()
        """)
        //assert(out == "anon : (lin 2, col 13) : inference error : unknown types") { out!! }
        assert(out == "anon : (lin 2, col 13) : call error : types mismatch") { out!! }
    }

    // TYPE

    @Test
    fun bb_01_type() {
        val out = check("""
            ;;do {
                var x: Int
                set x = true
            ;;}
        """)
        assert(out == "anon : (lin 4, col 23) : set error : types mismatch") { out!! }
    }
    @Test
    fun bb_02_func() {
        val out = check("""
                do {
                    func f: (v: Int) -> Int {
                        return(v)
                    }
                    `printf("%d", f(10));`
                }
            """)
        assert(out == null) { out!! }
    }
    @Test
    fun bb_03_func_err() {
        val out = check("""
                do ;;;[f: func () -> ()];;; {
                    func f: () -> Int {
                    }
                }
            """)
        //assert(out == "anon : (lin 3, col 17) : declaration error : types mismatch") { out!! }
        assert(out == null) { out!! }
    }
    @Test
    fun bb_04_func_err() {
        val out = check("""
            ;;do {
                func f: () -> Int {
                    return ()
                }
            ;;}
        """)
        assert(out == "anon : (lin 4, col 21) : set error : types mismatch") { out!! }
    }
    @Test
    fun bb_05_func_nested_ok() {
        val out = check("""
            do {
                do {
                    func g: () -> () {
                    }
                }
            }
        """)
        assert(out == null) { out!! }
    }
    @Test
    fun bb_06_if_err() {
        val out = check("""
            ;;do {
                if null {}  ;; cnd bool
            ;;}
        """)
        assert(out == "anon : (lin 3, col 17) : if error : expected boolean condition") { out!! }
    }

    // TYPE / CORO / XCORO / SPAWN / RESUME / YIELD

    @Test
    fun bc_01_coro_err() {
        val out = check("""
            do {
                var f: \coro () -> () -> () -> ()
                var x:[] = create(f) ;; err: f \coro
            }
        """)
        assert(out == "anon : (lin 4, col 28) : create error : expected coroutine prototype") { out!! }
    }
    @Test
    fun bc_02x_coro_err() {
        val out = check("""
            coro f: (x: Int) -> () -> () -> Int {
            }
            var z: exec (Int) -> () -> () -> Int = create(f)
            start z()  ;; err f(Int)
        """)
        assert(out == "anon : (lin 5, col 13) : start error : types mismatch") { out!! }
    }
    @Test
    fun bc_02y_coro_err() {
        val out = check("""
            coro f: (x: Int) -> () -> () -> Int {
            }
            var z: exec (Int) -> () -> () -> Int = create(f)
            resume z(10)  ;; err f(Int)
        """)
        assert(out == "anon : (lin 5, col 13) : resume error : types mismatch") { out!! }
    }
    @Test
    fun bc_03_coro_ok() {
        val out = check("""
            coro f: (v: Int) -> () -> () -> Int {
            }
            var x: exec (Int) -> () -> () -> Int = create(f)
            start x(10)
            resume x()
        """)
        assert(out == null) { out!! }
    }
    @Test
    fun bc_03x_coro_err() {
        val out = check("""
            coro f: (v: Int) -> () -> () -> Int {
            }
            var x: exec (Int) -> () -> () -> Int = create(f)
            resume x(<.1=10>: <Int>)
        """)
        assert(out == "anon : (lin 5, col 13) : resume error : types mismatch") { out!! }
    }
    @Test
    fun bc_04_exe_coro_err () {
        val out = check("""
            coro co: () -> () -> () -> () {
            }
            var exe: exec (Int) -> () -> () -> () = create(co)
        """)
        assert(out == "anon : (lin 4, col 51) : set error : types mismatch") { out!! }
    }
    @Test
    fun bc_05_exe_coro_err () {
        val out = check("""
            do {
                var x:[] = create(1)
            }
            """)
        assert(out == "anon : (lin 3, col 28) : create error : expected coroutine prototype") { out!! }
    }
    @Test
    fun bc_06_exe_coro_ok () {
        val out = check("""
            coro co: () -> () -> () -> () {}
            var exe: exec () -> () -> () -> ()
            set exe = create(co)
            start exe()
            resume exe()
        """)
        assert(out == null) { out!! }
    }
    @Test
    fun bc_07_exe_resume_ok () {
        val out = check("""
            coro co: () -> () -> () -> Int {}
            var exe: exec () -> () -> () -> Int
            set exe = create(co)
            start exe()
            resume exe()
        """)
        assert(out == null) { out!! }
    }
    @Test
    fun bc_08_exe_resume () {
        val out = check("""
            do {
                resume 1()
            }
        """)
        assert(out == "anon : (lin 3, col 17) : resume error : expected active coroutine") { out!! }
    }
    @Test
    fun bc_09_exe_resume_err () {
        val out = check("""
            coro co: () -> () -> () -> () {}
            var exe: exec () -> () -> () -> ()
            set exe = create(co)
            resume exe(1)
        """)
        assert(out == "anon : (lin 5, col 13) : resume error : types mismatch") { out!! }
    }
    @Test
    fun bc_10_exe_resume_err () {
        val out = check("""
            coro co: () -> () -> () -> () {}
            var exe: exec () -> () -> () -> ()
            set exe = create(co)
            var v: Int = resume exe(<.1=()>: <(),()>)
        """)
        assert(out == "anon : (lin 5, col 26) : resume error : types mismatch") { out!! }
    }
    @Test
    fun bc_11_exe_resume () {
        val out = check("""
            coro co: (x:()) -> () -> () -> () {}
            var exe: exec () -> () -> () -> ()
            set exe = create(co)
            start exe()
            resume exe()
            var v: Int
        """)
        assert(out == "anon : (lin 4, col 21) : set error : types mismatch") { out!! }
        //assert(out == null) { out!! }
    }
    @Test
    fun bc_12_exe_yield () {
        val out = check("""
            do {
                yield()
            }
        """)
        assert(out == "anon : (lin 3, col 17) : yield error : expected enclosing coro") { out!! }
    }
    @Test
    fun bc_13_exe_yield_err () {
        val out = check("""
            coro co: () -> () -> () -> () {
                do {
                    var x: Int = yield()
                }
            }
        """)
        assert(out == "anon : (lin 4, col 32) : set error : types mismatch") { out!! }
    }
    @Test
    fun bc_14_exe_yield_err () {
        val out = check("""
            do {
                coro co: () -> () -> () -> () {
                    yield(<.1=10>: <Int,()>)
                }
            }
        """)
        assert(out == "anon : (lin 4, col 21) : yield error : types mismatch") { out!! }
    }

    // TUPLE

    @Test
    fun cc_01_tuple () {
        val out = check("""
            var t: []
            var x: Int = t
        """)
        assert(out == "anon : (lin 3, col 24) : set error : types mismatch") { out!! }
    }
    @Test
    fun cc_02_tuple () {
        val out = check("""
            var x: [Int] = [10]: [Int]
        """)
        assert(out == null) { out!! }
    }
    @Test
    fun cc_03x_tuple () {
        val out = check("""
            var x: [Int] = [()]: [Int]
        """)
        assert(out == "anon : (lin 2, col 28) : tuple error : types mismatch") { out!! }
    }
    @Test
    fun cc_03y_tuple () {
        val out = check("""
            var x: [Int] = [()]: [()]
        """)
        assert(out == "anon : (lin 2, col 26) : set error : types mismatch") { out!! }
    }
    @Test
    fun cc_04_tuple () {
        val out = check("""
            var x: [] = [()]: [()]
        """)
        assert(out == "anon : (lin 2, col 23) : set error : types mismatch") { out!! }
    }
    @Test
    fun cc_05_disc_err () {
        val out = check("""
            var x: Int
            var y: Int = x.1
        """)
        assert(out == "anon : (lin 3, col 27) : field error : types mismatch") { out!! }
    }
    @Test
    fun cc_06_disc_err () {
        val out = check("""
            var x: [Int]
            var y: Int = x.2
        """)
        assert(out == "anon : (lin 3, col 27) : field error : invalid index") { out!! }
    }
    @Test
    fun cc_07x_tuple_err () {
        val out = check("""
            var pos: [x:Int,y:Int] = [.y=10,.x=20]: [x:Int,y:Int]  ;; x/y inverted
        """)
        assert(out == "anon : (lin 2, col 38) : tuple error : types mismatch") { out!! }
    }
    @Test
    fun cc_07y_tuple_err () {
        val out = check("""
            var pos: [x:Int,y:Int] = [.y=10,.x=20]: [y:Int,x:Int]  ;; x/y inverted
        """)
        assert(out == "anon : (lin 2, col 36) : set error : types mismatch") { out!! }
    }
    @Test
    fun cc_08_disc_err () {
        val out = check("""
            var v: [v:Int]
            var x: Int = v.x
        """)
        assert(out == "anon : (lin 3, col 27) : field error : invalid index") { out!! }
    }
    @Test
    fun cc_09_tuple_err () {
        val out = check("""
            var x:[] = []:[()]
        """)
        assert(out == "anon : (lin 2, col 24) : tuple error : types mismatch") { out!! }
    }
    @Test
    fun cc_10_tuple_err () {
        val out = check("""
            var x:[] = [()]:[]
        """)
        assert(out == "anon : (lin 2, col 24) : tuple error : types mismatch") { out!! }
    }
    @Test
    fun cc_11_tuple_err () {
        val out = check("""
            var x:[] = [()]:[Int]
        """)
        assert(out == "anon : (lin 2, col 24) : tuple error : types mismatch") { out!! }
    }
    @Test
    fun cc_12_tuple () {
        val out = check("""
            var x = []
        """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "var x: []\n" +
                "set x = ([]:[])\n" +
                "}") { G.outer!!.to_str() }
    }
    @Test
    fun cc_13_tuple () {
        val out = check("""
            var x:[] = ([[0,0],[24,24]])
        """)
        assert(out == "anon : (lin 2, col 25) : tuple error : types mismatch") { out!! }
        //assert(out == "anon : (lin 3, col 24) : inference error : incompatible types") { out!! }
    }
    @Test
    fun cc_14_infer_tuple () {
        val out = check("""
            data T: []
            var x: T = [[]]
        """)
        assert(out == "anon : (lin 3, col 22) : set error : types mismatch") { out!! }
    }

    // VECTOR

    @Test
    fun cd_01_vector_err () {
        val out = check("""
            var t: #[Int*10] = #['a']
        """)
        //println(G.outer!!.to_str())
        assert(out == "anon : (lin 2, col 30) : set error : types mismatch") { out!! }
    }
    @Test
    fun cd_02_vector_err () {
        val out = check("""
            var t: #[Int*10]
            do(t['a'])
        """)
        assert(out == "anon : (lin 3, col 18) : index error : expected number") { out!! }
    }
    @Test
    fun cd_03_vector_err () {
        val out = check("""
            var a: #[Int*2]
            var b: #[Int*4]
            set a = b
            set b = a
        """)
        assert(out == null) { out!! }
        //assert(out == "anon : (lin 5, col 19) : set error : types mismatch") { out!! }
    }
    @Test
    fun cd_04_vector_err () {
        val out = check("""
            do(1[0])
        """)
        assert(out == "anon : (lin 2, col 16) : index error : expected vector") { out!! }
    }
    @Test
    fun cd_05_vector_err () {
        val out = check("""
            var v: #[Int*2]
            var x: Bool = #v
        """)
        assert(out == "anon : (lin 3, col 25) : set error : types mismatch") { out!! }
    }
    @Test
    fun cd_06_vector_err () {
        val out = check("""
            do(#1)
        """)
        assert(out == "anon : (lin 2, col 16) : operation error : expected vector") { out!! }
    }
    @Test
    fun cd_07_vector_err () {
        val out = check("""
            var v: #[Int*1]
            var x: Bool = ##v
        """)
        assert(out == "anon : (lin 3, col 25) : set error : types mismatch") { out!! }
    }

    // CONCATENATE

    @Test
    fun cd_01_concat_err () {
        val out = check("""
            do(1++1)
        """)
        assert(out == "anon : (lin 2, col 17) : operation error : types mismatch") { out!! }
    }

    // UNION

    @Test
    fun cd_01_union () {
        val out = check("""
            var t: <>
            var x: Int = t
        """)
        assert(out == "anon : (lin 3, col 24) : set error : types mismatch") { out!! }
    }
    @Test
    fun cd_02_union () {
        val out = check("""
            var x: <Int> = <.1=10>: <Int>
        """)
        assert(out == null) { out!! }
    }
    @Test
    fun cd_03_union () {
        val out = check("""
            var x: <Int> = <.2=10>: <Int>
        """)
        assert(out == "anon : (lin 2, col 28) : union error : types mismatch") { out!! }
    }
    @Test
    fun cd_05_union () {
        val out = check("""
            var x: <Int> = <.1=()>: <Int>
        """)
        assert(out == "anon : (lin 2, col 28) : union error : types mismatch") { out!! }
    }
    @Test
    fun cd_06_disc () {
        val out = check("""
            var x: <Int>
            var y: Int = x!1
        """)
        assert(out == null) { out!! }
    }
    @Test
    fun cd_07_disc () {
        val out = check("""
            var x: Int
            var y: Int = x!1
        """)
        //assert(out == "anon : (lin 3, col 27) : discriminator error : expected union type") { out!! }
        assert(out == "anon : (lin 3, col 27) : discriminator error : types mismatch") { out!! }
    }
    @Test
    fun cd_08_disc_err () {
        val out = check("""
            var x: <Int>
            var y: Int = x!2
        """)
        assert(out == "anon : (lin 3, col 27) : discriminator error : types mismatch") { out!! }
    }
    @Test
    fun cd_09_pred_err () {
        val out = check("""
            var x: <Int>
            var y: Int = x?2
        """)
        assert(out == "anon : (lin 3, col 27) : predicate error : types mismatch") { out!! }
    }
    @Test
    fun cd_10_field_err () {
        val out = check("""
            var x: Int
            var ts = x.ts
        """)
        assert(out == "anon : (lin 3, col 23) : field error : types mismatch") { out!! }
        //assert(out == "anon : (lin 3, col 13) : inference error : unknown types") { out!! }
    }
    @Test
    fun cd_11_field_err () {
        val out = check("""
            data WH: [w:Int, h:Int]
            var log: WH
            var x = log.y*0.9
        """)
        assert(out == "anon : (lin 4, col 24) : field error : invalid index") { out!! }
    }

    // DATA

    @Test
    fun dd_00_data () {
        val out = check("""
            data X: ()
            data X: Int
        """)
        assert(out == "anon : (lin 3, col 13) : type error : data \"X\" is already declared") { out!! }
    }
    @Test
    fun dd_00x_data () {
        val out = check("""
            do {
                data X: ()
            }
            do {
                data X: Int
            }
        """)
        assert(out == null) { out!! }
    }
    @Test
    fun dd_01_data () {
        val out = check("""
            data Pos: [Int, Int]
            var p: Pos = Pos [10, 10]: [Int, Int]
        """)
        assert(out == null) { out!! }
    }
    @Test
    fun dd_02_data_err () {
        val out = check("""
            data Pos: [Int, Int]
            var p: Pos = [10, 10]: [Int, Int]
        """)
        assert(out == "anon : (lin 3, col 24) : set error : types mismatch") { out!! }
    }
    @Test
    fun dd_03_data_err () {
        val out = check("""
            var p: Pos
        """)
        //assert(out == "anon : (lin 2, col 13) : declaration error : data \"Pos\" is not declared") { out!! }
        assert(out == "anon : (lin 2, col 20) : type error : data \"Pos\" is not declared") { out!! }
    }
    @Test
    fun dd_04_data_err () {
        val out = check("""
            var p: Int = Pos ()
        """)
        assert(out == "anon : (lin 2, col 26) : type error : data \"Pos\" is not declared") { out!! }
        //assert(out == "anon : (lin 2, col 26) : constructor error : data \"Pos\" is not declared") { out!! }
    }
    @Test
    fun dd_05_data_err () {
        val out = check("""
            data Pos: [Int, Int]
            var p: Pos = Pos [10, true]: [Int, Bool]
        """)
        assert(out == "anon : (lin 3, col 30) : constructor error : types mismatch") { out!! }
    }
    @Test
    fun dd_06_data_err () {
        val out = check("""
            data Pos: [Int, Int]
            var p: Pos = Pos(null)
        """)
        assert(out == "anon : (lin 3, col 30) : constructor error : types mismatch") { out!! }
    }
    @Test
    fun dd_07_data_err () {
        val out = check("""
            data Pos: [Int, Int]
            var p: Pos = [10, 20]: [Int, Int]
            var x: Bool = p.1
        """)
        assert(out == "anon : (lin 3, col 24) : set error : types mismatch") { out!! }
    }
    @Test
    fun dd_08_data () {
        val out = check("""
            data Pos: [Int, Int]
            var p: Pos = Pos [10, 20]: [Int, Int]
            var x: Int = p.1
        """)
        assert(out == null) { out!! }
    }
    @Test
    fun dd_09_data_err () {
        val out = check("""
            data Pos: [Int, Int]
            var p: Pos = Pos [10, 20]: [Int, Int]
            var x: Int = p.3
        """)
        assert(out == "anon : (lin 4, col 27) : field error : invalid index") { out!! }
    }
    @Test
    fun dd_10_data_err () {
        val out = check("""
            data A: <B:()>
            var c: A = A.B()
            print(c!A!B)
        """)
        assert(out == "anon : (lin 4, col 20) : discriminator error : types mismatch") { out!! }
    }
    @Test
    fun dd_10x_data_err () {
        val out = check("""
            data A: <B:()>
            var c: A = A.C()
        """)
        assert(out == "anon : (lin 3, col 24) : type error : data \"A.C\" is not declared") { out!! }
    }
    @Test
    fun dd_11_data () {
        val out = check("""
            data A: <B: <C:()>>
            var c: A = A.B.C()
            print(c!B!C)
        """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "data A: <B:<C:()>>\n" +
                "var c: A\n" +
                "set c = (A.B.C(()))\n" +
                "print(((c!B)!C))\n" +
                "}") { G.outer!!.to_str() }
    }

    // DATA SUBS

    @Test
    fun de_00a_subs_ee_err () {
        val out = check("""
            var b: B.T
        """)
        assert(out == "anon : (lin 2, col 20) : type error : data \"B.T\" is not declared") { out!! }
    }
    @Test
    fun de_00b_subs_ee () {
        val out = check("""
            data B: <T:(), F:()>
            var b: B
        """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "data B: <T:(),F:()>\n" +
                "var b: B\n" +
                "}") { G.outer!!.to_str() }
        //assert(out == "anon : (lin 3, col 20) : type error : data \"B\" is not hierarchic") { out!! }
    }
    @Test
    fun de_00c_subs_ee () {
        val out = check("""
            data B: <T:(), F:()>
            var b: B.T
        """)
        assert(out == "anon : (lin 3, col 20) : type error : data \"B.T\" is not declared") { out!! }
    }
    @Test
    fun de_00d_subs_ee_err () {
        val out = check("""
            data B: [<T:[],F:[]>]
            var b: B.X
        """)
        assert(out == "anon : (lin 3, col 20) : type error : data \"B.X\" is not declared") { out!! }
        //assert(out == "anon : (lin 3, col 20) : type error : data \"B.X\" is invalid") { out!! }
    }
    @Test
    fun de_00d_subs_ee () {
        val out = check("""
            data B: <T:[],F:[]>
            var b: B.F
        """)
        assert(out == "anon : (lin 3, col 20) : type error : data \"B.F\" is not declared") { out!! }
        /*
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data B: <T:[],F:[]>\n" +
                "var b: B.F\n" +
                "}") { G.outer!!.to_str() }
         */
    }
    @Test
    fun de_01_subs_err () {
        val out = check("""
            data B: <T:[],F:[]>
            var b: B.T = B.F []
        """)
        //assert(out == "anon : (lin 3, col 13) : set error : types mismatch") { out!! }
        //assert(out == "anon : (lin 3, col 13) : set error : types mismatch") { out!! }
        assert(out == "anon : (lin 3, col 20) : type error : data \"B.T\" is not declared") { out!! }
    }
    @Test
    fun de_02_subs_err () {
        val out = check("""
            data B: <T:[],F:[]>
            var b: B = B.F []
            var c: B.T = b
        """)
        //assert(out == "anon : (lin 4, col 13) : set error : types mismatch") { out!! }
        assert(out == "anon : (lin 4, col 20) : type error : data \"B.T\" is not declared") { out!! }
    }
    @Test
    fun de_03_subs () {
        val out = check("""
            data B: <T:[],F:[]>
            var b: B = B.F []
            var c: B = b
            var d: B = b
        """)
        assert(out == null) { out!! }
    }
    @Test
    fun de_04_subs_err () {
        val out = check("""
            data X: [()]
            var x = X.B () 
        """)
        assert(out == "anon : (lin 3, col 21) : type error : data \"X.B\" is not declared") { out!! }
    }
    @Test
    fun de_05_subs_err () {
        val out = check("""
            data X: []
            var x = X.B () 
        """)
        assert(out == "anon : (lin 3, col 21) : type error : data \"X.B\" is not declared") { out!! }
    }
    @Test
    fun de_06_subs_err () {
        val out = check("""
            data X: <A:[a:Int]>
            var x = X.B () 
        """)
        assert(out == "anon : (lin 3, col 21) : type error : data \"X.B\" is not declared") { out!! }
    }
    @Test
    fun de_07_subs () {
        val out = check("""
            data X: <A:[a:Int]>
            var x = X.A [10]:[a:Int] 
            var a = x!A ;; ERR: x is already X.A
        """)
        //assert(out == "anon : (lin 4, col 13) : inference error : unknown type") { out!! }
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "data X: <A:[a:Int]>\n" +
                "var x: X\n" +
                "set x = (X.A(([10]:[a:Int])))\n" +
                "var a: [a:Int]\n" +
                "set a = (x!A)\n" +
                "}") { G.outer!!.to_str() }
    }
    @Test
    fun de_07x_subs () {
        val out = check("""
            data X: <A:[a:Int]>
            var x = X.A [10]:[a:Int] 
            var a: Int = x.a
        """)
        assert(out == "anon : (lin 4, col 27) : field error : types mismatch") { out!! }
    }
    @Test
    fun de_07y_subs () {
        val out = check("""
            data X: <A:[a:Int]>
            var x = X.A [10]:[a:Int] 
            var xa: [Int] = x!A
            var a = xa.1
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
                "var xa: [Int]\n" +
                "set xa = (x!A)\n" +
                "var a: Int\n" +
                "set a = (xa.1)\n" +
                "}") { G.outer!!.to_str() }
    }
    @Test
    fun de_08_subs () {
        val out = check("""
            data X: <A: <B:Int>>
            var x = X.A.B (10) 
        """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "data X: <A:<B:Int>>\n" +
                "var x: X\n" +
                "set x = (X.A.B(10))\n" +
                "}") { G.outer!!.to_str() }
    }
    @Test
    fun de_09_subs_err () {
        val out = check("""
            data B: <T:[],F:[]>
            var b: B.T = B.T ()     ;; () -> []
        """)
        //assert(out == "anon : (lin 3, col 26) : constructor error : expected tuple") { out!! }
        //assert(out == "anon : (lin 3, col 30) : constructor error : types mismatch") { out!! }
        assert(out == "anon : (lin 3, col 20) : type error : data \"B.T\" is not declared") { out!! }
    }
    @Test
    fun de_10_subs () {
        val out = check("""
            data B: <T:[], F:[]>
            var b: B = B.T []
            print(b?T)
            print(b!T)
        """)
        //assert(out == null) { out!! }
        //assert(out == "anon : (lin 4, col 20) : predicate error : types mismatch") { out!! }
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "data B: <T:[],F:[]>\n" +
                "var b: B\n" +
                "set b = (B.T(([]:[])))\n" +
                "print((b?T))\n" +
                "print((b!T))\n" +
                "}") { G.outer!!.to_str() }
    }
    @Test
    fun de_10x_subs () {
        val out = check("""
            data B: <T:[], F:[]>
            var b: B = B.T []
            print(b?T)
            print(b!T)
        """)
        assert(out == null) { out!! }
    }
    @Test
    fun dd_11_data_cons () {
        val out = check("""
            data Res: <Err:(),Ok:Int>
            var r = Res.Ok(10)
        """)
        assert(out == null) { out!! }
    }
    @Test
    fun dd_12_data_cons_err () {
        val out = check("""
            data Res: <Err:(),Ok:Int>
            var r = Res.XXX(10)
        """)
        assert(out == "anon : (lin 3, col 21) : type error : data \"Res.XXX\" is not declared") { out!! }
    }
    @Test
    fun de_13_subs () {
        val out = check("""
            data B: <True:(), False:()>
            var b: B = B.True ()
        """)
        assert(out == null) { out!! }
    }
    @Test
    fun TODO_de_14_subs () {    // TODO: anonymous reuse (+)
        val out = check("""
            data X: [Int, [a:Int]+<[],[]>]
        """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\ndata X: [Int,[a:Int] + <[],[]>]\n}") { G.outer!!.to_str() }
    }
    @Test
    fun de_15_subs_err () {
        val out = check("""
            data B: <T:[], F:[]>
            var b ;;;: B.T;;; = B.T []
            print(b?B)      ;; NO: B is not a subtype
            print(b!B)      ;; NO: no B._o
        """)
        assert(out == "anon : (lin 4, col 20) : predicate error : types mismatch") { out!! }
        //assert(out == "anon : (lin 5, col 20) : discriminator error : types mismatch") { out!! }
    }

    // DATA HIER

    @Test
    fun df_01_hier_err () {
        val out = check("""
            data X.*: [a:Int] {
                Y: []
            }
            var y = X.Y []   ;; missing Int
        """)
        assert(out == "anon : (lin 5, col 25) : tuple error : types mismatch") { out!! }
        //assert(out == "anon : (lin 5, col 25) : constructor error : types mismatch") { out!! }
        //assert(out == "anon : (lin 3, col 21) : constructor error : arity mismatch") { out!! }
    }
    @Test
    fun df_02_hier_err () {
        val out = check("""
            data X.*: [a:Int] {
                Y: []
            }
            var y = X.Y([10]) ;; missing ()
        """)
        //println(out)
        //assert(out == "anon : (lin 3, col 21) : constructor error : arity mismatch") { out!! }
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "data X.*: [a:Int] {\n" +
                "Y: [] {\n" +
                "}\n" +
                "}\n" +
                "var y: X.Y\n" +
                "set y = (X.Y(([10]:[a:Int])))\n" +
                "}") { G.outer!!.to_str() }
    }
    @Test
    fun df_03_hier () {
        val out = check("""
            data X.*: [a:Int] {
                Y: []
            }
            var y = X.Y [10]
        """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "data X.*: [a:Int] {\n" +
                "Y: [] {\n" +
                "}\n" +
                "}\n" +
                "var y: X.Y\n" +
                "set y = (X.Y(([10]:[a:Int])))\n" +
                "}") { G.outer!!.to_str() }
    }
    @Test
    fun df_04_hier () {
        val out = check("""
            data X.*: [a:Int] {
                Y: []
            }
            var xy: X = X.Y [10]
            var y: X.Y = xy!Y    ;; ()
        """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "data X.*: [a:Int] {\n" +
                "Y: [] {\n" +
                "}\n" +
                "}\n" +
                "var xy: X\n" +
                "set xy = (X.Y(([10]:[a:Int])))\n" +
                "var y: X.Y\n" +
                "set y = (xy!Y)\n" +
                "}") { G.outer!!.to_str() }
    }
    @Test
    fun df_04x_hier () {
        val out = check("""
            data X.*: [a:Int] {
                Y: []
            }
            var xy: X = X.Y [10]
            var y = xy!Y    ;; ()
        """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "data X.*: [a:Int] {\n" +
                "Y: [] {\n" +
                "}\n" +
                "}\n" +
                "var xy: X\n" +
                "set xy = (X.Y(([10]:[a:Int])))\n" +
                "var y: X.Y\n" +
                "set y = (xy!Y)\n" +
                "}") { G.outer!!.to_str() }
    }
    @Test
    fun df_04y_hier () {
        val out = check("""
            data X.*: [a:Int] {
                Y: []
            }
            var xy = X.Y [10]
            var y = xy    ;; ()
        """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "data X.*: [a:Int] {\n" +
                "Y: [] {\n" +
                "}\n" +
                "}\n" +
                "var xy: X.Y\n" +
                "set xy = (X.Y(([10]:[a:Int])))\n" +
                "var y: X.Y\n" +
                "set y = xy\n" +
                "}") { G.outer!!.to_str() }
    }
    @Test
    fun df_05_hier_base () {
        val out = check("""
            data X.*: [a:Int] {
                Y: []
            }
            var xy = X.X(10)
            print(xy!X)
        """)
        assert(out == "anon : (lin 5, col 22) : type error : data \"X.X\" is not declared") { out!! }
    }
    @Test
    fun df_06_hier_base_err () {
        val out = check("""
            data X.*: []
            var xy = X.X()
            print(xy!X)
        """)
        //assert(out == "anon : (lin 3, col 22) : constructor error : arity mismatch") { out!! }
        assert(out == "anon : (lin 3, col 22) : type error : data \"X.X\" is not declared") { out!! }
    }
    @Test
    fun df_07_hier_base () {
        val out = check("""
            data A.*: [a:Bool] {
                B: [b:Int] {
                    C: [c:Char]
                }
            }
            var x0: A = A.B [true,100]  ;; ignore subsubtype C
            print(x0)
            print(x0!B)
        """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "data A.*: [a:Bool] {\n" +
                "B: [b:Int] {\n" +
                "C: [c:Char] {\n" +
                "}\n" +
                "}\n" +
                "}\n" +
                "var x0: A\n" +
                "set x0 = (A.B(([true,100]:[a:Bool,b:Int])))\n" +
                "print(x0)\n" +
                "print((x0!B))\n" +
                "}") { G.outer!!.to_str() }
    }
    @Test
    fun df_08_hier () {
        val out = check("""
            data X.*: [a:Int] {
                Y: []
            }
            var xy: X = X.Y [10]
            var x = xy.a    ;; 10
            var y = xy!Y    ;; ()
        """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "data X.*: [a:Int] {\n" +
                "Y: [] {\n" +
                "}\n" +
                "}\n" +
                "var xy: X\n" +
                "set xy = (X.Y(([10]:[a:Int])))\n" +
                "var x: Int\n" +
                "set x = (xy.a)\n" +
                "var y: X.Y\n" +
                "set y = (xy!Y)\n" +
                "}") { G.outer!!.to_str() }
    }
    @Test
    fun df_09_hier () {
        val out = check("""
            data A.*: [x:Int] {
                B: [y:Int] {
                    C: [Int]
                }
            }
            var c: A.B.C = A.B.C([10,20,30])
            ;;var b: [y:Int]+<C: Int>  = c!B
            ;;var bb: [Int] = c!B!B
            var y: Int  = c.y
        """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "data A.*: [x:Int] {\n" +
                "B: [y:Int] {\n" +
                "C: [Int] {\n" +
                "}\n" +
                "}\n" +
                "}\n" +
                "var c: A.B.C\n" +
                "set c = (A.B.C(([10,20,30]:[x:Int,y:Int,Int])))\n" +
                "var y: Int\n" +
                "set y = (c.y)\n" +
                "}") { G.outer!!.to_str() }
    }
    @Test
    fun df_11_hier () {
        val out = check("""
            data X.*: [Int] {}
            var x = X [10]
            print(x.1)
        """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "data X.*: [Int] {\n" +
                "}\n" +
                "var x: X\n" +
                "set x = (X(([10]:[Int])))\n" +
                "print((x.1))\n" +
                "}") { G.outer!!.to_str() }
    }

    // DATA HIER / EXTD / EXTENDS

    @Test
    fun dg_01_hier_extd () {
        val out = check("""
            data X.*: [Int] {
                Y: []
                Z: [Int]
            }
            var xz = X.Z [10,20]
            var x = xz.1    ;; 10
            var z = xz.2    ;; 20
        """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "data X.*: [Int] {\n" +
                "Y: [] {\n" +
                "}\n" +
                "Z: [Int] {\n" +
                "}\n" +
                "}\n" +
                "var xz: X.Z\n" +
                "set xz = (X.Z(([10,20]:[Int,Int])))\n" +
                "var x: Int\n" +
                "set x = (xz.1)\n" +
                "var z: Int\n" +
                "set z = (xz.2)\n" +
                "}") { G.outer!!.to_str() }
    }
    @Test
    fun dg_04_hier_extd_err () {
        val out = check("""
            data X.*: [a:Int] {
                Y: []
                Y: [b:Int]
            }
        """)
        assert(out == "anon : (lin 4, col 17) : type error : data \"Y\" is already declared") { out!! }
    }
    @Test
    fun dg_05_hier_extd () {
        val out = check("""
            data X.*: [x:Int] {
                Y: [] {
                    A: [a:Int]
                }
                Z: [z:Int] {
                    A: []
                }
            }
            var xza = X.Z.A [10,20]
            var xya = X.Y.A [10,20]
         """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "data X.*: [x:Int] {\n" +
                "Y: [] {\n" +
                "A: [a:Int] {\n" +
                "}\n" +
                "}\n" +
                "Z: [z:Int] {\n" +
                "A: [] {\n" +
                "}\n" +
                "}\n" +
                "}\n" +
                "var xza: X.Z.A\n" +
                "set xza = (X.Z.A(([10,20]:[x:Int,z:Int])))\n" +
                "var xya: X.Y.A\n" +
                "set xya = (X.Y.A(([10,20]:[x:Int,a:Int])))\n" +
                "}") { G.outer!!.to_str() }
    }
    @Test
    fun dg_05x_hier_extd_err () {
        val out = check("""
            data X.*: [Int] {
                Y: [] {
                    A: [Int]
                }
                Z: [Int] {
                    A: []
                }
            }
            var xza = X.Z.A [10,20]
            var xya = X.Y.A [10,20]
         """)
        //assert(out == "anon : (lin 5, col 13) : type error : data \"X.Y\" is not extendable") { out!! }
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "data X.*: [Int] {\n" +
                "Y: [] {\n" +
                "A: [Int] {\n" +
                "}\n" +
                "}\n" +
                "Z: [Int] {\n" +
                "A: [] {\n" +
                "}\n" +
                "}\n" +
                "}\n" +
                "var xza: X.Z.A\n" +
                "set xza = (X.Z.A(([10,20]:[Int,Int])))\n" +
                "var xya: X.Y.A\n" +
                "set xya = (X.Y.A(([10,20]:[Int,Int])))\n" +
                "}") { G.outer!!.to_str() }
    }
    @Test
    fun dg_06_hier_extd_field () {
        val out = check("""
            data A.*: [] {
                B: [Int]
            }
            var a: A = A.B [10]
            print(a!B.1)
        """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "data A.*: [] {\n" +
                "B: [Int] {\n" +
                "}\n" +
                "}\n" +
                "var a: A\n" +
                "set a = (A.B(([10]:[Int])))\n" +
                "print(((a!B).1))\n" +
                "}") { G.outer!!.to_str() }
    }

    // THROW / CATCH

    @Test
    fun ff_01_catch_err () {
        val out = check("""
            data X: Int
            catch :X {
            }
        """)
        assert(out == "anon : (lin 3, col 20) : catch error : expected hierarchical data type") { out!! }
    }
    @Test
    fun ff_02_catch () {
        val out = check("""
            data X.*: [] {
                X: []
            }
            catch :X.X {
            }
        """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "data X.*: [] {\n" +
                "X: [] {\n" +
                "}\n" +
                "}\n" +
                "catch :X.X {\n" +
                "}\n" +
                "}") { G.outer!!.to_str() }
    }
    @Test
    fun ff_03_catch_err () {
        val out = check("""
            data X.*: [] {
                X: []
            }
            catch :X.Y {
            }
        """)
        assert(out == "anon : (lin 5, col 20) : type error : data \"X.Y\" is not declared") { out!! }
    }
    @Test
    fun ff_04_catch_err () {
        val out = check("""
            var x: Break = catch :Return {
            }
        """)
        assert(out == "anon : (lin 2, col 26) : set error : types mismatch") { out!! }
    }
    @Test
    fun ff_05_catch_err () {
        val out = check("""
            data X.*: [] {
                Y: []
            }
            var x: X.Y = catch :X {
            }
        """)
        assert(out == "anon : (lin 5, col 24) : set error : types mismatch") { out!! }
    }
    @Test
    fun ff_06_catch () {
        val out = check("""
            data X.*: [] {
                Y: []
            }
            var x: <(),X> = catch :X.Y {
            }
        """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n"+
           "data Return.*: [] {\n"+
           "}\n"+
           "data Break.*: [] {\n"+
           "}\n"+
           "data X.*: [] {\n"+
           "Y: [] {\n"+
           "}\n"+
           "}\n"+
           "var x: <(),X>\n"+
           "set x = catch :X.Y {\n"+
           "}\n"+
           "}") { G.outer!!.to_str() }
    }

    // ESCAPE

    @Test
    fun gg_01_escape_err () {
        val out = check("""
            data X: Int
            do :X {
            }
        """)
        assert(out == "anon : (lin 3, col 17) : block error : expected hierarchical data type") { out!! }
    }
    @Test
    fun gg_02_escape () {
        val out = check("""
            data X.*: [] {
                X: []
            }
            do :X.X {
            }
        """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "data X.*: [] {\n" +
                "X: [] {\n" +
                "}\n" +
                "}\n" +
                "do :X.X {\n" +
                "}\n" +
                "}") { G.outer!!.to_str() }
    }
    @Test
    fun gg_03_escape_err () {
        val out = check("""
            data X.*: [] {
                X: []
            }
            do :X.Y {
            }
        """)
        assert(out == "anon : (lin 5, col 17) : type error : data \"X.Y\" is not declared") { out!! }
    }
    @Test
    fun gg_04_escape_err () {
        val out = check("""
            data X.*: [] {
                X: []
            }
            do :X {
                escape(X.Y[])
            }
        """)
        assert(out == "anon : (lin 6, col 24) : type error : data \"X.Y\" is not declared") { out!! }
    }
    @Test
    fun gg_05_escape_err () {
        val out = check("""
            data X.*: []
            escape(X[])
        """)
        assert(out == "anon : (lin 3, col 13) : escape error : expected matching enclosing block") { out!! }
    }
    @Test
    fun gg_06_escape_err () {
        val out = check("""
            data X.*: []
            do :X {
                func f: () -> () {
                    escape(X[])
                }
            }
        """)
        assert(out == "anon : (lin 5, col 21) : escape error : expected matching enclosing block") { out!! }
    }
    @Test
    fun gg_07_escape () {
        val out = check("""
            data X.*: []
            do :X {
                escape(X[])
            }
        """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "data X.*: [] {\n" +
                "}\n" +
                "do :X {\n" +
                "escape((X(([]:[]))))\n" +
                "}\n" +
                "}") { G.outer!!.to_str() }
    }

    // NAT

    @Test
    fun hh_01_nat_call () {
    val out = check("""
            `f`([])
        """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "do((`f`(([]:[]))))\n" +
                "}") { G.outer!!.to_str() }
    }
    @Test
    fun hh_02_nat_call () {
        val out = check("""
            `f`(`x`)
        """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n" +
                "data Return.*: [] {\n" +
                "}\n" +
                "data Break.*: [] {\n" +
                "}\n" +
                "do((`f`(`x`)))\n" +
                "}") { G.outer!!.to_str() }
    }

    // EXPR / IF / MATCH

    @Test
    fun ii_01_if_err () {
        val out = check("""
            do(if true => 10 => true)
        """)
        assert(out == "anon : (lin 2, col 16) : if error : types mismatch") { out!! }
    }
    @Test
    fun ii_02_if_err () {
        val out = check("""
            do(if 10 => 10 => 20)
        """)
        assert(out == "anon : (lin 2, col 19) : if error : expected boolean condition") { out!! }
    }
    @Test
    fun ii_03_match_err () {
        val out = check("""
            var x = match 10 { else => 10 }
        """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n"+
           "data Return.*: [] {\n"+
           "}\n"+
           "data Break.*: [] {\n"+
           "}\n"+
           "var x: Int\n"+
           "set x = match 10 {\n"+
           "else => 10\n"+
           "}\n"+
           "}") { G.outer!!.to_str() }
    }
    @Test
    fun ii_04_match_err () {
        val out = check("""
            var x: Int
            set x = match true { 10 => 10 }
        """)
        assert(out == "anon : (lin 3, col 21) : match error : types mismatch") { out!! }
    }
    @Test
    fun ii_05_match_err () {
        val out = check("""
            var x: Int = match true { true => 10 ; else => true}
        """)
        assert(out == "anon : (lin 2, col 26) : match error : types mismatch") { out!! }
    }
    @Test
    fun ii_06_match () {
        val out = check("""
            data Error.*: []
            var v: `T`
            var ret: Int = match v {
                `SDL_QUIT` => 10
                else => throw()
            }
        """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n"+
           "data Return.*: [] {\n"+
           "}\n"+
           "data Break.*: [] {\n"+
           "}\n"+
           "data Error.*: [] {\n"+
           "}\n"+
           "var v: `T`\n"+
           "var ret: Int\n"+
           "set ret = match v {\n"+
           "(`SDL_QUIT`: `T`) => 10\n"+
           "else => throw((Error(([]:[]))))\n"+
           "}\n"+
           "}") { G.outer!!.to_str() }
    }

    // STMT / MATCH

    @Test
    fun jj_01_match_err () {
        val out = check("""
            match 10 { else { do(10) } }
        """)
        println(out)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n"+
           "data Return.*: [] {\n"+
           "}\n"+
           "data Break.*: [] {\n"+
           "}\n"+
           "match 10 {\n"+
           "else { do(10) }\n"+
           "}\n"+
           "}") { G.outer!!.to_str() }
    }
    @Test
    fun jj_02_match_err () {
        val out = check("""
            match true { 10 { do(10) } }
        """)
        assert(out == "anon : (lin 2, col 13) : match error : types mismatch") { out!! }
    }
    @Test
    fun jj_03_match () {
        val out = check("""
            data Error.*: []
            var v: `T`
            match v {
                `SDL_QUIT`{}
                else {throw()}
            }
        """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n"+
           "data Return.*: [] {\n"+
           "}\n"+
           "data Break.*: [] {\n"+
           "}\n"+
           "data Error.*: [] {\n"+
           "}\n"+
           "var v: `T`\n"+
           "match v {\n"+
           "(`SDL_QUIT`: `T`) {  }\n"+
           "else { do(throw((Error(([]:[]))))) }\n"+
           "}\n"+
           "}") { G.outer!!.to_str() }
    }

    // TEMPLATE

    @Test
    fun tt_01_data () {
        val out = check("""
            data Maybe {{t:Type}}: <Nothing:(), Just:{{t}}>
            var x: Maybe {{:Int}} = Maybe {{:Int}}.Just(10)
        """)
        assert(out == null) { out!! }
        assert(G.outer!!.to_str() == "do {\n"+
           "data Return.*: [] {\n"+
           "}\n"+
           "data Break.*: [] {\n"+
           "}\n"+
           "data Maybe {{t: Type}}: <Nothing:(),Just:{{t}}>\n"+
           "var x: Maybe {{:Int}}\n"+
           "set x = (Maybe {{:Int}}.Just(10))\n"+
           "}") { G.outer!!.to_str() }
    }
    @Test
    fun tt_02_data_err () {
        val out = check("""
            data Maybe {{t:Type}}: <Nothing:(), Just:{{t}}>
            var x: Maybe {{:Bool}} = Maybe {{:Int}}.Just(10)
        """)
        assert(out == "anon : (lin 3, col 36) : set error : types mismatch") { out!! }
    }
    @Test
    fun tt_03_data_err () {
        val out = check("""
            data Maybe {{t:Type}}: <Nothing:(), Just:{{t}}>
            var x: Maybe {{:Bool}} = Maybe {{:Bool}}.Just(10)
        """)
        assert(out == "anon : (lin 3, col 59) : constructor error : types mismatch") { out!! }
    }
    @Test
    fun tt_04a_data_err () {
        val out = check("""
            data Either {{a:Type}}: <Left:{{a}}, Right:{{b}}>
        """)
        assert(out == "anon : (lin 2, col 13) : type error : template \"b\" is not declared") { out!! }
    }
    @Test
    fun tt_04b_data_err () {
        val out = check("""
            data Maybe {{t:Type}}: <Nothing:(), Just:Int>
        """)
        assert(out == "anon : (lin 2, col 13) : type error : template \"t\" is not used") { out!! }
    }
    @Test
    fun tt_05_data_err () {
        val out = check("""
            data Maybe {{t:Type}}: <Nothing:(), Just:{{t}}>
            var x: Maybe {{:Bool}} = Maybe.Just(10)
        """)
        assert(out == "anon : (lin 3, col 38) : type error : templates mismatch") { out!! }
    }
    @Test
    fun tt_06_data_err () {
        val out = check("""
            data Maybe {{t:Type}}: <Nothing:(), Just:{{t}}>
            var x: Maybe
        """)
        assert(out == "anon : (lin 3, col 20) : type error : templates mismatch") { out!! }
    }
    @Test
    fun tt_07_data_err () {
        val out = check("""
            data Maybe {{t:Type}}: <Nothing:(), Just:{{t}}>
            var x: Maybe
        """)
        assert(out == "{{t:Type}} vs -") { out!! }
    }

}
