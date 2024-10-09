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
        val out = static("""
            do [
                f: func () -> (),
            ] {
                ;; missing implementation
            }
        """)
        assert(out == "anon : (lin 3, col 17) : declaration error : missing implementation") { out!! }
    }
    @Test
    fun aa_05_coro_err () {
        val out = static("""
            do [
                co: coro () -> () -> (),
            ] {
                ;; missing implementation
            }
        """)
        assert(out == "anon : (lin 3, col 17) : declaration error : missing implementation") { out!! }
    }
    @Test
    fun aa_06_coro_decl_err () {
        val out = static("""
            do [
                ;; missing declaration
            ] {
                func f () -> () {
                }
            }
        """)
        assert(out == "anon : (lin 5, col 17) : implementation error : variable \"f\" is not declared") { out!! }
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
    fun bb_15_func_nested_ok() {
        val out = static("""
            do [] {
                do [g: func () -> ()] {
                    func g () -> () {
                    }
                }
            }
        """)
        assert(out == null) { out!! }
    }

    // TYPE / CORO / XCORO / SPAWN / RESUME

    @Test
    fun bc_01_coro_err() {
        val out = static("""
            do [f: \coro () -> () -> Int] {
                set `_` = spawn f()
            }
        """)
        assert(out == "anon : (lin 3, col 27) : invalid spawn : expected coroutine prototype") { out!! }
    }
    @Test
    fun bc_02_coro_err() {
        val out = static("""
            do [f: coro (Int) -> () -> Int] {
                coro f (x: Int) -> () -> Int {
                }
                set `_` = spawn f()
            }
        """)
        assert(out == "anon : (lin 5, col 27) : invalid spawn : types mismatch") { out!! }
    }
    @Test
    fun bc_03_coro_ok() {
        val out = static("""
            do [f: coro (Int) -> () -> Int] {
                coro f (x: Int) -> () -> Int {
                }
                set `_` = spawn f(10)
            }
        """)
        assert(out == null) { out!! }
    }
    @Test
    fun bc_04_exe_coro_err () {
        val out = static("""
            do [
                xco: xcoro (Int) -> (),
                co:  coro () -> () -> (),
            ] {
                coro co () -> () -> () {
                }
                set xco = spawn co()
            }
        """)
        assert(out == "anon : (lin 8, col 27) : invalid spawn : types mismatch") { out!! }
    }
    @Test
    fun bc_05_exe_coro_err () {
        val out = static("""
            do [] {
                set `_` = spawn (1)()
            }
        """)
        assert(out == "anon : (lin 3, col 27) : invalid spawn : expected coroutine prototype") { out!! }
    }
    @Test
    fun bc_06_exe_coro_ok () {
        val out = static("""
            do [
                xco: xcoro () -> (),
                co:  coro () -> () -> (),
            ] {
                coro co () -> () -> () {}
                set xco = spawn co()
            }
        """)
        assert(out == null) { out!! }
    }
    @Test
    fun bc_07_exe_resume_ok () {
        val out = static("""
            do [
                xco: xcoro () -> Int,
                co:  coro () -> () -> Int,
            ] {
                coro co () -> () -> Int {}
                set xco = spawn co()
                resume xco()
            }
        """)
        assert(out == null) { out!! }
    }
    @Test
    fun bc_08_exe_resume () {
        val out = static("""
            do [] {
                resume 1()
            }
        """)
        assert(out == "anon : (lin 3, col 17) : invalid resume : expected active coroutine") { out!! }
    }
    @Test
    fun bc_09_exe_resume_err () {
        val out = static("""
            do [
                xco: xcoro () -> (),
                co:  coro () -> () -> (),
            ] {
                coro co () -> () -> () {}
                set xco = spawn co()
                resume xco(1)
            }
        """)
        assert(out == "anon : (lin 8, col 17) : invalid resume : types mismatch") { out!! }
    }
    @Test
    fun bc_10_exe_resume_err () {
        val out = static("""
            do [
                xco: xcoro () -> (),
                co:  coro () -> () -> (),
                v: Int,
            ] {
                coro co () -> () -> () {}
                set xco = spawn co()
                set v = resume xco(1)
            }
        """)
        assert(out == "anon : (lin 9, col 25) : invalid resume : types mismatch") { out!! }
    }
    @Test
    fun bc_11_exe_resume_err () {
        val out = static("""
            do [
                xco: xcoro () -> (),
                co:  coro () -> () -> (),
                v: Int,
            ] {
                coro co () -> () -> () {}
                set xco = spawn co()
                set v = resume xco()
            }
        """)
        assert(out == "anon : (lin 9, col 25) : invalid resume : types mismatch") { out!! }
    }
    @Test
    fun bc_12_exe_resume_err () {
        val out = static("""
            do [
                xco: xcoro () -> (),
                co:  coro () -> () -> (),
                v: Int,
            ] {
                coro co () -> () -> () {}
                set xco = spawn co()
                resume xco()
            }
        """)
        assert(out == null) { out!! }
    }
}
