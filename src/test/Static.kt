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

    // TYPE / CORO / XCORO / SPAWN / RESUME / YIELD

    @Test
    fun bc_01_coro_err() {
        val out = static("""
            do {
                var f: \coro () -> ()
                set `_` = create(f) ;; err: f \coro
            }
        """)
        assert(out == "anon : (lin 4, col 27) : create error : expected coroutine prototype") { out!! }
    }
    @Test
    fun bc_02_coro_err() {
        val out = static("""
            coro f (x: Int) -> Int {
            }
            var z: xcoro (Int) -> Int = create(f)
            resume z()  ;; err f(Int)
        """)
        assert(out == "anon : (lin 5, col 13) : resume error : types mismatch") { out!! }
    }
    @Test
    fun bc_03_coro_ok() {
        val out = static("""
            coro f (v: Int) -> Int {
            }
            var x: xcoro (Int) -> Int = create(f)
            resume x(10)
        """)
        assert(out == null) { out!! }
    }
    @Test
    fun bc_04_exe_coro_err () {
        val out = static("""
            coro co () -> () {
            }
            var xco: xcoro (Int) -> () = create(co)
        """)
        assert(out == "anon : (lin 4, col 42) : create error : types mismatch") { out!! }
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
            coro co () -> () {}
            var xco: xcoro () -> ()
            set xco = create(co)
            resume xco()
        """)
        assert(out == null) { out!! }
    }
    @Test
    fun bc_07_exe_resume_ok () {
        val out = static("""
            coro co () -> Int {}
            var xco: xcoro () -> Int
            set xco = create(co)
            resume xco()
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
            coro co () -> () {}
            var xco: xcoro () -> ()
            set xco = create(co)
            resume xco(1)
        """)
        assert(out == "anon : (lin 5, col 13) : resume error : types mismatch") { out!! }
    }
    @Test
    fun bc_10_exe_resume_err () {
        val out = static("""
            coro co () -> () {}
            var xco: xcoro () -> ()
            set xco = create(co)
            var v: Int = resume xco(1)
        """)
        assert(out == "anon : (lin 5, col 26) : resume error : types mismatch") { out!! }
    }
    @Test
    fun bc_11_exe_resume_err () {
        val out = static("""
                coro co () -> () {}
                var xco: xcoro () -> ()
                set xco = create(co)
                var v: Int = resume xco()
        """)
        assert(out == "anon : (lin 5, col 30) : resume error : types mismatch") { out!! }
    }
    @Test
    fun bc_12_exe_resume () {
        val out = static("""
            coro co () -> () {}
            var xco: xcoro () -> ()
            set xco = create(co)
            resume xco()
            var v: Int
        """)
        assert(out == null) { out!! }
    }
    @Test
    fun bc_13_exe_yield () {
        val out = static("""
            do {
                yield()
            }
        """)
        assert(out == "anon : (lin 3, col 17) : yield error : expected enclosing coro") { out!! }
    }
    @Test
    fun bc_14_exe_yield_err () {
        val out = static("""
                coro co () -> () {
                    do {
                        var x: Int = yield()
                    }
                }
        """)
        assert(out == "anon : (lin 4, col 38) : yield error : types mismatch") { out!! }
    }
    @Test
    fun bc_15_exe_yield_err () {
        val out = static("""
            do {
                coro co () -> () {
                    yield(10)
                }
            }
        """)
        assert(out == "anon : (lin 4, col 21) : yield error : types mismatch") { out!! }
    }
}
