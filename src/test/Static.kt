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

    // TYPE / VAR / CHECK

    @Test
    fun dd_01_type() {
        val out = static("""
            do [x: Int] {
                set x = true
            }
        """)
        assert(out == "anon : (lin 3, col 17) : invalid set : types mismatch") { out!! }
    }
    @Test
    fun dd_02_var_dup() {
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
    fun dd_03_var_none() {
        val out = static("""
            do [] {
                f()
            }
        """
        )
        //assert(out == "10") { out!! }
        assert(out == "anon : (lin 3, col 17) : access error : variable \"f\" is not declared") { out!! }
    }
    @Test
    fun ee_01_func() {
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
    fun ee_02_func_err() {
        val out = static("""
            do [f: func () -> ()] {
                func f () -> Int {
                }
            }
        """)
        assert(out == "anon : (lin 3, col 17) : invalid declaration : types mismatch") { out!! }
    }
    @Test
    fun ee_03_func_err() {
        val out = static("""
            do [f: func () -> Int] {
                func f () -> Int {
                    return ()
                }
            }
        """)
        assert(out == "anon : (lin 4, col 21) : invalid return : types mismatch") { out!! }
    }

}
