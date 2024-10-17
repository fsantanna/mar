package test

import mar.*
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

fun static (me: String): String? {
    return trap {
        G.reset()
        G.tks = me.lexer()
        parser_lexer()
        val tk0 = G.tk1!!
        val ss = parser_list(null, { accept_enu("Eof") }, {
            parser_stmt()
        }).flatten()
        G.outer = Stmt.Block(tk0, ss)
        cache_ns()
        cache_ups()
        check_vars()
        infer_types()
        check_types()
    }
    //return null
}

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Static {

    // VAR

    @Test
    fun aa_01_var() {
        val out = static("""
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
        val out = static("""
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
        val out = static("""
            do {
                f()
            }
        """
        )
        assert(out == "anon : (lin 3, col 17) : access error : variable \"f\" is not declared") { out!! }
    }
    @Test
    fun aa_04_func_err () {
        val out = static("""
            ;;do {
                var f: func () -> () ;; missing implementation
            ;;}
        """)
        //assert(out == "anon : (lin 3, col 17) : declaration error : missing implementation") { out!! }
        assert(out == "anon : (lin 3, col 24) : type error : unexpected \"func\"") { out!! }
    }
    @Test
    fun aa_05_coro_err () {
        val out = static("""
            do {
                var co: coro () -> ()   ;; missing implementation
            }
        """)
        assert(out == "anon : (lin 3, col 25) : type error : unexpected \"coro\"") { out!! }
    }
    @Test
    fun aa_06_coro_decl_err () {
        val out = static("""
            ;;do {
                func f () -> () {
                }
            ;;}
        """)
        assert(out == null) { out!! }
        //assert(out == "anon : (lin 5, col 17) : implementation error : variable \"f\" is not declared") { out!! }
    }

    // FUNC

    @Test
    fun ab_01_func_rec () {
        val out = static("""
            do {
                func f () -> () {
                    f()
                }
            }
        """)
        assert(out == null) { out!! }
    }
    @Test
    fun ab_02_func_rec_mutual () {
        val out = static("""
            ;;do {
                func f () -> () {
                    g()
                }
                func g () -> () {
                    f()
                }
            ;;}
        """)
        assert(out == null) { out!! }
    }

    // TYPE

    @Test
    fun bb_01_type() {
        val out = static("""
            ;;do {
                var x: Int
                set x = true
            ;;}
        """)
        assert(out == "anon : (lin 4, col 17) : set error : types mismatch") { out!! }
    }
    @Test
    fun bb_02_func() {
        val out = static("""
            do {
                func f (v: Int) -> Int {
                    return(v)
                }
                `printf("%d", f(10));`
            }
        """)
        assert(out == null) { out!! }
    }
    @Test
    fun bb_03_func_err() {
        val out = static("""
            do ;;;[f: func () -> ()];;; {
                func f () -> Int {
                }
            }
        """)
        //assert(out == "anon : (lin 3, col 17) : declaration error : types mismatch") { out!! }
        assert(out == null) { out!! }
    }
    @Test
    fun bb_04_func_err() {
        val out = static("""
            ;;do {
                func f () -> Int {
                    return ()
                }
            ;;}
        """)
        assert(out == "anon : (lin 4, col 21) : return error : types mismatch") { out!! }
    }
    @Test
    fun bb_05_func_nested_ok() {
        val out = static("""
            do {
                do {
                    func g () -> () {
                    }
                }
            }
        """)
        assert(out == null) { out!! }
    }
    @Test
    fun bb_06_if_err() {
        val out = static("""
            ;;do {
                if null {}  ;; cnd bool
            ;;}
        """)
        assert(out == "anon : (lin 3, col 17) : if error : expected boolean condition") { out!! }
    }
    @Test
    fun bb_07_call_err() {
        val out = static("""
            1()
        """)
        assert(out == "anon : (lin 2, col 13) : call error : types mismatch") { out!! }
    }

    // TYPE / CORO / XCORO / SPAWN / RESUME / YIELD

    @Test
    fun bc_01_coro_err() {
        val out = static("""
            do {
                var f: \coro () -> () -> () -> ()
                set `_` = create(f) ;; err: f \coro
            }
        """)
        assert(out == "anon : (lin 4, col 27) : create error : expected coroutine prototype") { out!! }
    }
    @Test
    fun bc_02x_coro_err() {
        val out = static("""
            coro f (x: Int) -> () -> () -> Int {
            }
            var z: exec (Int) -> () -> () -> Int = create(f)
            start z()  ;; err f(Int)
        """)
        assert(out == "anon : (lin 5, col 13) : start error : types mismatch") { out!! }
    }
    @Test
    fun bc_02y_coro_err() {
        val out = static("""
            coro f (x: Int) -> () -> () -> Int {
            }
            var z: exec (Int) -> () -> () -> Int = create(f)
            resume z(10)  ;; err f(Int)
        """)
        assert(out == "anon : (lin 5, col 13) : resume error : types mismatch") { out!! }
    }
    @Test
    fun bc_03_coro_ok() {
        val out = static("""
            coro f (v: Int) -> () -> () -> Int {
            }
            var x: exec (Int) -> () -> () -> Int = create(f)
            start x(10)
            resume x()
        """)
        assert(out == null) { out!! }
    }
    @Test
    fun bc_03x_coro_err() {
        val out = static("""
            coro f (v: Int) -> () -> () -> Int {
            }
            var x: exec (Int) -> () -> () -> Int = create(f)
            resume x(<.1=10>: <Int>)
        """)
        assert(out == "anon : (lin 5, col 13) : resume error : types mismatch") { out!! }
    }
    @Test
    fun bc_04_exe_coro_err () {
        val out = static("""
            coro co () -> () -> () -> () {
            }
            var exe: exec (Int) -> () -> () -> () = create(co)
        """)
        assert(out == "anon : (lin 4, col 53) : create error : types mismatch") { out!! }
    }
    @Test
    fun bc_05_exe_coro_err () {
        val out = static("""
            do {
                set `_` = create(1)
            }
        """)
        assert(out == "anon : (lin 3, col 27) : create error : expected coroutine prototype") { out!! }
    }
    @Test
    fun bc_06_exe_coro_ok () {
        val out = static("""
            coro co () -> () -> () -> () {}
            var exe: exec () -> () -> () -> ()
            set exe = create(co)
            start exe()
            resume exe()
        """)
        assert(out == null) { out!! }
    }
    @Test
    fun bc_07_exe_resume_ok () {
        val out = static("""
            coro co () -> () -> () -> Int {}
            var exe: exec () -> () -> () -> Int
            set exe = create(co)
            start exe()
            resume exe()
        """)
        assert(out == null) { out!! }
    }
    @Test
    fun bc_08_exe_resume () {
        val out = static("""
            do {
                resume 1()
            }
        """)
        assert(out == "anon : (lin 3, col 17) : resume error : expected active coroutine") { out!! }
    }
    @Test
    fun bc_09_exe_resume_err () {
        val out = static("""
            coro co () -> () -> () -> () {}
            var exe: exec () -> () -> () -> ()
            set exe = create(co)
            resume exe(1)
        """)
        assert(out == "anon : (lin 5, col 13) : resume error : types mismatch") { out!! }
    }
    @Test
    fun bc_10_exe_resume_err () {
        val out = static("""
            coro co () -> () -> () -> () {}
            var exe: exec () -> () -> () -> ()
            set exe = create(co)
            var v: Int = resume exe(<.1=()>: <(),()>)
        """)
        assert(out == "anon : (lin 5, col 26) : resume error : types mismatch") { out!! }
    }
    @Test
    fun bc_11_exe_resume () {
        val out = static("""
            coro co (x:()) -> () -> () -> () {}
            var exe: exec () -> () -> () -> ()
            set exe = create(co)
            start exe()
            resume exe()
            var v: Int
        """)
        assert(out == "anon : (lin 4, col 23) : create error : types mismatch") { out!! }
        //assert(out == null) { out!! }
    }
    @Test
    fun bc_12_exe_yield () {
        val out = static("""
            do {
                yield()
            }
        """)
        assert(out == "anon : (lin 3, col 17) : yield error : expected enclosing coro") { out!! }
    }
    @Test
    fun bc_13_exe_yield_err () {
        val out = static("""
                coro co () -> () -> () -> () {
                    do {
                        var x: Int = yield()
                    }
                }
        """)
        assert(out == "anon : (lin 4, col 38) : yield error : types mismatch") { out!! }
    }
    @Test
    fun bc_14_exe_yield_err () {
        val out = static("""
            do {
                coro co () -> () -> () -> () {
                    yield(<.1=10>: <Int,()>)
                }
            }
        """)
        assert(out == "anon : (lin 4, col 21) : yield error : types mismatch") { out!! }
    }

    // TUPLE

    @Test
    fun cc_01_tuple () {
        val out = static("""
            var t: []
            var x: Int = t
        """)
        assert(out == "anon : (lin 3, col 13) : set error : types mismatch") { out!! }
    }
    @Test
    fun cc_02_tuple () {
        val out = static("""
            var x: [Int] = [10]: [Int]
        """)
        assert(out == null) { out!! }
    }
    @Test
    fun cc_03x_tuple () {
        val out = static("""
            var x: [Int] = [()]: [Int]
        """)
        assert(out == "anon : (lin 2, col 28) : tuple error : types mismatch") { out!! }
    }
    @Test
    fun cc_03y_tuple () {
        val out = static("""
            var x: [Int] = [()]: [()]
        """)
        assert(out == "anon : (lin 2, col 13) : set error : types mismatch") { out!! }
    }
    @Test
    fun cc_04_tuple () {
        val out = static("""
            var x: [] = [()]: [()]
        """)
        assert(out == "anon : (lin 2, col 13) : set error : types mismatch") { out!! }
    }
    @Test
    fun cc_05_disc_err () {
        val out = static("""
            var x: Int
            var y: Int = x.1
        """)
        assert(out == "anon : (lin 3, col 27) : field error : types mismatch") { out!! }
    }
    @Test
    fun cc_06_disc_err () {
        val out = static("""
            var x: [Int]
            var y: Int = x.2
        """)
        assert(out == "anon : (lin 3, col 27) : field error : types mismatch") { out!! }
    }
    @Test
    fun cc_07x_tuple_err () {
        val out = static("""
            var pos: [x:Int,y:Int] = [.y=10,.x=20]: [x:Int,y:Int]  ;; x/y inverted
        """)
        assert(out == "anon : (lin 2, col 38) : tuple error : types mismatch") { out!! }
    }
    @Test
    fun cc_07y_tuple_err () {
        val out = static("""
            var pos: [x:Int,y:Int] = [.y=10,.x=20]: [y:Int,x:Int]  ;; x/y inverted
        """)
        assert(out == "anon : (lin 2, col 13) : set error : types mismatch") { out!! }
    }
    @Test
    fun cc_08_disc_err () {
        val out = static("""
            var v: [v:Int]
            set `x` = v.x
        """)
        assert(out == "anon : (lin 3, col 24) : field error : types mismatch") { out!! }
    }
    @Test
    fun cc_09_tuple_err () {
        val out = static("""
            set `x` = []:[()]
        """)
        assert(out == "anon : (lin 2, col 23) : tuple error : types mismatch") { out!! }
    }
    @Test
    fun cc_10_tuple_err () {
        val out = static("""
            set `x` = [()]:[]
        """)
        assert(out == "anon : (lin 2, col 23) : tuple error : types mismatch") { out!! }
    }
    @Test
    fun cc_11_tuple_err () {
        val out = static("""
            set `x` = [()]:[Int]
        """)
        assert(out == "anon : (lin 2, col 23) : tuple error : types mismatch") { out!! }
    }

    // UNION

    @Test
    fun cd_01_union () {
        val out = static("""
            var t: <>
            var x: Int = t
        """)
        assert(out == "anon : (lin 3, col 13) : set error : types mismatch") { out!! }
    }
    @Test
    fun cd_02_union () {
        val out = static("""
            var x: <Int> = <.1=10>: <Int>
        """)
        assert(out == null) { out!! }
    }
    @Test
    fun cd_03_union () {
        val out = static("""
            var x: <Int> = <.2=10>: <Int>
        """)
        assert(out == "anon : (lin 2, col 28) : union error : types mismatch") { out!! }
    }
    @Test
    fun cd_05_union () {
        val out = static("""
            var x: <Int> = <.1=()>: <Int>
        """)
        assert(out == "anon : (lin 2, col 28) : union error : types mismatch") { out!! }
    }
    @Test
    fun cd_06_disc () {
        val out = static("""
            var x: <Int>
            var y: Int = x!1
        """)
        assert(out == null) { out!! }
    }
    @Test
    fun cd_07_disc () {
        val out = static("""
            var x: Int
            var y: Int = x!1
        """)
        assert(out == "anon : (lin 3, col 27) : discriminator error : types mismatch") { out!! }
    }
    @Test
    fun cd_08_disc_err () {
        val out = static("""
            var x: <Int>
            var y: Int = x!2
        """)
        assert(out == "anon : (lin 3, col 27) : discriminator error : types mismatch") { out!! }
    }
    @Test
    fun cd_09_pred_err () {
        val out = static("""
            var x: <Int>
            var y: Int = x?2
        """)
        assert(out == "anon : (lin 3, col 27) : predicate error : types mismatch") { out!! }
    }

    // DATA

    @Test
    fun dd_01_data () {
        val out = static("""
            data Pos: [Int, Int]
            var p: Pos = Pos [10, 10]: [Int, Int]
        """)
        assert(out == null) { out!! }
    }
    @Test
    fun dd_02_data_err () {
        val out = static("""
            data Pos: [Int, Int]
            var p: Pos = [10, 10]: [Int, Int]
        """)
        assert(out == "anon : (lin 3, col 13) : set error : types mismatch") { out!! }
    }
    @Test
    fun dd_03_data_err () {
        val out = static("""
            var p: Pos
        """)
        assert(out == "anon : (lin 2, col 13) : declaration error : data \"Pos\" is not declared") { out!! }
    }
    @Test
    fun dd_04_data_err () {
        val out = static("""
            var p: Int = Pos ()
        """)
        assert(out == "anon : (lin 2, col 26) : constructor error : data \"Pos\" is not declared") { out!! }
    }
    @Test
    fun dd_05_data_err () {
        val out = static("""
            data Pos: [Int, Int]
            var p: Pos = Pos [10, true]: [Int, Bool]
        """)
        assert(out == "anon : (lin 3, col 26) : constructor error : types mismatch") { out!! }
    }
    @Test
    fun dd_06_data_err () {
        val out = static("""
            data Pos: [Int, Int]
            var p: Pos = Pos(null)
        """)
        assert(out == "anon : (lin 3, col 26) : constructor error : types mismatch") { out!! }
    }
    @Test
    fun dd_07_data_err () {
        val out = static("""
            data Pos: [Int, Int]
            var p: Pos = [10, 20]: [Int, Int]
            var x: Bool = p.1
        """)
        assert(out == "anon : (lin 3, col 13) : set error : types mismatch") { out!! }
    }
    @Test
    fun dd_08_data () {
        val out = static("""
            data Pos: [Int, Int]
            var p: Pos = Pos [10, 20]: [Int, Int]
            var x: Int = p.1
        """)
        assert(out == null) { out!! }
    }
    @Test
    fun dd_09_data_err () {
        val out = static("""
            data Pos: [Int, Int]
            var p: Pos = Pos [10, 20]: [Int, Int]
            var x: Int = p.3
        """)
        assert(out == "anon : (lin 4, col 27) : field error : types mismatch") { out!! }
    }
    @Test
    fun dd_10_data_cons () {
        val out = static("""
            data Res: <Err:(),Ok:Int>
            var r = Res.Ok(10)
        """)
        assert(out == null) { out!! }
    }
    @Test
    fun dd_11_data_cons_err () {
        val out = static("""
            data Res: <Err:(),Ok:Int>
            var r = Res.XXX(10)
        """)
        assert(out == "TODO") { out!! }
    }
}
