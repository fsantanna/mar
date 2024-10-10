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

    // FUNC

    @Test
    fun ee_01_func() {
        val out = test("""
            do [f: func (Int) -> Int] {
                func f (v: Int) -> Int {
                    return(v)
                }
                `printf("%d\n", f(10));`
            }
        """)
        assert(out == "10\n") { out }
    }

    // PROTOS / TYPES / NORMAL / NESTED / CLOSURE

    @Test
    fun ef_01_func_nested() {
        val out = test("""
            do [i: Int, f: func () -> ()] {
                set i = 10
                func f () -> () {
                    do [j: Int, g: func () -> Int] {
                        set j = 20
                        func g () -> Int {
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
                func f () -> (\func () -> Int) {
                    do [j: Int, g: func () -> Int] {
                        set j = 20
                        func g () -> Int {
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
            do [
                xco: xcoro () -> (),
                co:  coro () -> () -> (),
            ] {
                coro co () -> () -> () {
                    `puts("OK");`
                }
                set xco = spawn co()
                `puts("END");`
            }
        """)
        assert(out == "END\n") { out }
    }
    @Test
    fun ff_02_coro () {
        val out = test("""
            do [
                co: coro () -> () -> (),
                xco: xcoro () -> (),
            ] {
                coro co () -> () -> () {
                    `puts("OK");`
                }
                set xco = spawn co()
                resume xco()
            }
        """)
        assert(out == "OK\n") { out }
    }
    @Test
    fun ff_03_coro () {
        val out = test("""
            do [
                co: coro (Int) -> (Int) -> (),
                xco: xcoro (Int) -> (),
            ] {
                coro co (v: Int) -> (Int) -> () {
                    `printf("%d\n", ceu_xcoro->mem.v);`
                }
                set xco = spawn co(10)
                resume xco(20)
            }
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun ff_04_coro () {
        val out = test("""
            do [
                co: coro (Int) -> () -> (),
                xco: xcoro () -> (),
            ] {
                coro co (v: Int) -> () -> () {
                    `printf("%d\n", ceu_xcoro->mem.v);`
                    yield()
                    set v = v + 10
                    `printf("%d\n", ceu_xcoro->mem.v);`
                }
                set xco = spawn co(10)
                resume xco()
                resume xco()
            }
        """)
        assert(out == "10\n20\n") { out }
    }
    @Test
    fun ff_05_coro () {
        val out = test("""
            do [
                co: coro (Int,Int) -> (Int) -> Int,
                xco: xcoro (Int) -> Int,
                xy: Int,
                z2: Int,
            ] {
                coro co (x:Int, y:Int) -> (Int) -> Int {
                    do [z: Int] {
                        set z = yield(x+y)
                        return(z * 2)
                    }
                }
                set xco = spawn co(10,20)
                set xy = resume xco(0)
                set z2 = resume xco(xy)
                `printf("%d\n", z2);`
            }
        """)
        assert(out == "60\n") { out }
    }
}
