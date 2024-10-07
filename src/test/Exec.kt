package test

import mar.*
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

fun test_int (src: String): String {
    return test("""
        do [v: Int] {
            set v = $src
            `printf("%d\n", v);`
        }
    """)
}

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Exec  {
    // EXPR

    @Test
    fun aa_01_num() {
        val out = test_int("""
            1
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun aa_02_bin() {
        val out = test_int("""
            1 + 10
        """)
        assert(out == "11\n") { out }
    }
    @Test
    fun aa_03_uno() {
        val out = test_int("""
            -10
        """)
        assert(out == "-10\n") { out }
    }

    // NAT

    @Test
    fun bb_01_nat_printf() {
        val out = test("""
            do [x: Int] {
                set x = 1 + 10
                `printf("%d\n", x);`
            }
        """)
        assert(out == "11\n") { out }
    }

    // CALL / PRINT

    @Test
    fun cc_01_print() {
        val out = test("""
            do [] {
                ```
                void print_int (int v) {
                    printf("%d\n", v);
                }
                ```
                (`print_int`)(1)
            }
        """)
        assert(out == "1\n") { out }
    }

    // TYPE / VAR / CHECK

    @Test
    fun dd_01_type() {
        val out = test("""
            do [x: Int] {
                set x = true
            }
        """)
        assert(out == "anon : (lin 3, col 17) : invalid set : types mismatch\n") { out }
    }
    @Test
    fun dd_02_var_dup() {
        val out = test("""
            do [x: Int] {
                do [x: Int] {
                }
            }
        """
        )
        //assert(out == "10\n") { out }
        assert(out == "anon : (lin 3, col 21) : declaration error : variable \"x\" is already declared\n") { out }
    }
    @Test
    fun dd_03_var_none() {
        val out = test("""
            do [] {
                f()
            }
        """
        )
        //assert(out == "10\n") { out }
        assert(out == "anon : (lin 3, col 17) : access error : variable \"f\" is not declared\n") { out }
    }

    // FUNC

    @Test
    fun ee_01_func() {
        val out = test("""
            do [f: func (Int) -> Int] {
                set f = func (v: Int) -> Int {
                    return(v)
                }
                `printf("%d\n", f(10));`
            }
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun ee_02_func_err() {
        val out = test("""
            do [f: func () -> ()] {
                set f = func () -> Int {
                }
            }
        """)
        assert(out == "anon : (lin 3, col 21) : invalid declaration : types mismatch\n") { out }
    }
    @Test
    fun ee_03_func_err() {
        val out = test("""
            do [f: func () -> Int] {
                set f = func () -> Int {
                    return ()
                }
            }
        """)
        assert(out == "anon : (lin 4, col 21) : invalid return : types mismatch\n") { out }
    }

    // PROTOS / TYPES / NORMAL / NESTED / CLOSURE

    @Test
    fun ef_01_func_nested() {
        val out = test("""
            do [i: Int, f: func () -> ()] {
                set i = 10
                set f = func () -> () {
                    do [j: Int, g: func () -> Int] {
                        set j = 20
                        set g = func () -> Int {
                            return(i + j)
                        }
                        `printf("%d\n", g());`
                    }
                }
                f()
            }
        """)
        assert(out == "30\n") { out }
    }
    @Test
    fun ef_02_func_nested_out() {
        val out = test("""
            do [i: Int, f: func () -> (\func () -> Int), gg: (\func () -> Int)] {
                set i = 10
                set f = func () -> (\func () -> Int) {
                    do [j: Int, g: func () -> Int] {
                        set j = 20
                        set g = func () -> Int {
                            return(i + j)
                        }
                        return(\g)
                    }
                }
                set gg = f()
                `printf("%d\n", gg());`
            }
        """)
        assert(out == "30\n") { out }
    }

    // COROS

    @Test
    fun ff_01_coro () {
        val out = test("""
            do [co: coro () -> () -> ()] {
                set co = coro () -> () -> () {
                    do [v: Int] {
                        set v = 10
                        `printf("%d\n", v);`
                        yield()
                        set v = v + 10
                        `printf("%d\n", v);`
                    }
                }
                set xco = spawn co()
                resume co()
                resume co()
            }
        """)
        assert(out == "30\n") { out }
    }
}
