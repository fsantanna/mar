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
        check_fix("do")
        G.outer = parser_stmt() as Stmt.Block
        check_enu_err("Eof")
        cache_ns()
        cache_ups()
        check_vars()
        check_types()
    }
}

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Static {

    // VAR

    @Test
    fun aa_01_var() {
        val out = static("""
            do [x: Int] {
                do [y: Int] {
                }
            }
        """
        )
        assert(out == null) { out!! }
    }
    @Test
    fun aa_02_var_dup() {
        val out = static("""
            do [x: Int] {
                do [x: Int] {
                }
            }
        """
        )
        //assert(out == "10") { out!! }
        assert(out == "anon : (lin 3, col 21) : declaration error : variable \"x\" is already declared") { out!! }
    }
    @Test
    fun aa_03_var_none() {
        val out = static("""
            do [] {
                f()
            }
        """
        )
        assert(out == "anon : (lin 3, col 17) : access error : variable \"f\" is not declared") { out!! }
    }
    @Test
    fun aa_04_func_err () {
        val out = test("""
            do [
                f: func () -> (),
            ] {
                ;; missing implementation
            }
        """)
        assert(out == "anon : (lin 3, col 17) : declaration error : missing implementation\n") { out }
    }
    @Test
    fun aa_05_coro_err () {
        val out = test("""
            do [
                co: coro () -> () -> (),
            ] {
                ;; missing implementation
            }
        """)
        assert(out == "anon : (lin 3, col 17) : declaration error : missing implementation\n") { out }
    }
    @Test
    fun aa_06_coro_decl_err () {
        val out = test("""
            do [
                ;; missing declaration
            ] {
                func f () -> () {
                }
            }
        """)
        assert(out == "anon : (lin 5, col 17) : implementation error : variable \"f\" is not declared\n") { out }
    }


    // TYPE

    @Test
    fun bb_01_type() {
        val out = static("""
            do [x: Int] {
                set x = true
            }
        """)
        assert(out == "anon : (lin 3, col 17) : invalid set : types mismatch") { out!! }
    }
    @Test
    fun bb_02_func() {
        val out = static("""
            do [f: func (Int) -> Int] {
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
            do [f: func () -> ()] {
                func f () -> Int {
                }
            }
        """)
        assert(out == "anon : (lin 3, col 17) : invalid declaration : types mismatch") { out!! }
    }
    @Test
    fun bb_04_func_err() {
        val out = static("""
            do [f: func () -> Int] {
                func f () -> Int {
                    return ()
                }
            }
        """)
        assert(out == "anon : (lin 4, col 21) : invalid return : types mismatch") { out!! }
    }
    @Test
    fun bb_05_coro_err() {
        val out = static("""
            do [f: \coro () -> () -> Int] {
                spawn f()
            }
        """)
        assert(out == "anon : (lin 3, col 17) : invalid spawn : expected coroutine prototype") { out!! }
    }
    @Test
    fun bb_06_coro_err() {
        val out = static("""
            do [f: coro (Int) -> () -> Int] {
                coro f (x: Int) -> () -> Int {
                }
                spawn f()
            }
        """)
        assert(out == "anon : (lin 5, col 17) : invalid spawn : types mismatch") { out!! }
    }
    @Test
    fun bb_07_coro_ok() {
        val out = static("""
            do [f: coro (Int) -> () -> Int] {
                coro f (x: Int) -> () -> Int {
                }
                spawn f(10)
            }
        """)
        assert(out == null) { out!! }
    }
    @Test
    fun bb_08_exe_coro_err () {
        val out = test("""
            do [
                xco: xcoro (Int) -> (),
                co:  coro () -> () -> (),
            ] {
                coro co () -> () -> () {
                }
                set xco = spawn co()
            }
        """)
        assert(out == "anon : (lin 8, col 27) : invalid spawn : types mismatch\n") { out }
    }
    @Test
    fun bb_09_exe_coro_err () {
        val out = test("""
            do [] {
                spawn (1)()
            }
        """)
        assert(out == "anon : (lin 3, col 17) : invalid spawn : expected coroutine prototype\n") { out }
    }
}
