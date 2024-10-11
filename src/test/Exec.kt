package test

import mar.*
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

fun test_int (src: String): String {
    return test("""
        var v: Int = $src
        `printf("%d\n", v);`
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
            var x: Int = 1 + 10
            `printf("%d\n", x);`
        """)
        assert(out == "11\n") { out }
    }

    // CALL / PRINT / IF

    @Test
    fun cc_01_print() {
        val out = test("""
            do {
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
    @Test
    fun cc_02_if() {
        val out = test("""
            if true {
                `puts("1");`
            }
            if 2 < 0 {
                `puts("err");`
            } else {
                `puts("2");`
            }
        """)
        assert(out == "1\n2\n") { out }
    }

    // FUNC

    @Test
    fun ee_01_func() {
        val out = test("""
            do {
                func f (v: Int) -> Int {
                    return(v)
                }
                `printf("%d\n", f(10));`
            }
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun ee_02_func_rec () {
        val out = test("""
            func f (v: Int) -> Int {
                if v == 0 {
                    return(v)
                } else {
                    return(v + f(v-1))
                }
            }
            `printf("%d\n", f(5));`
        """)
        assert(out == "15\n") { out }
    }
    @Test
    fun ab_02_func_rec_mutual () {
        val out = test("""
            do {
                func f (v: Int) -> Int {
                    if v == 0 {
                        return(v)
                    } else {
                        return(v + g(v-1))
                    }
                }
                func g (v: Int) -> Int {
                    return(f(v))
                }
                `printf("%d\n", g(5));`
            }
        """)
        assert(out == "15\n") { out }
    }

    // PROTOS / TYPES / NORMAL / NESTED / CLOSURE

    @Test
    fun ef_01_func_nested() {
        val out = test("""
            var i: Int = 10
            func f () -> () {
                var j: Int = 20
                func g () -> Int {
                    return(i + j)
                }
                `printf("%d\n", g());`
            }
            f()
        """)
        assert(out == "30\n") { out }
    }
    @Test
    fun ef_02_func_nested_out() {
        val out = test("""
            do {
                var i: Int = 10
                func f () -> (\func () -> Int) {
                    do {
                        var j: Int = 20
                        func g () -> Int {
                            return(i + j)
                        }
                        return(\g)
                    }
                }
                var gg: (\func () -> Int) = f()
                `printf("%d\n", gg());`
            }
        """)
        assert(out == "30\n") { out }
    }

    // COROS

    @Test
    fun ff_01_coro () {
        val out = test("""
            do {
                coro co () -> () {
                    `puts("OK");`
                }
                var xco: xcoro () -> () = create(co)
                `puts("END");`
            }
        """)
        assert(out == "END\n") { out }
    }
    @Test
    fun ff_02_coro () {
        val out = test("""
            coro co () -> () {
                `puts("OK");`
            }
            var xco: xcoro () -> () = create(co)
            resume xco()
        """)
        assert(out == "OK\n") { out }
    }
    @Test
    fun ff_03_coro () {
        val out = test("""
            coro co (v: Int) -> () {
                `printf("%d\n", ceu_xco->mem.v);`
            }
            var xco: xcoro (Int) -> () = create(co)
            resume xco(10)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun ff_04_coro () {
        val out = test("""
            coro co (v: Int) -> () {
                `printf("%d\n", ceu_xco->mem.v);`
                yield()
                set v = v + 10
                `printf("%d\n", ceu_xco->mem.v);`
            }
            var xco: xcoro (Int) -> () = create(co)
            resume xco(10)
            resume xco(99)
        """)
        assert(out == "10\n20\n") { out }
    }
    @Test
    fun ff_05_coro () {
        val out = test("""
            do {
                coro co (x2:Int) -> Int {
                    var y2:Int = yield(x2*2)
                    return(y2 * 2)
                }
                var xco: xcoro (Int) -> Int = create(co)
                var x1: Int = resume xco(5)
                var y1: Int = resume xco(x1 + 10)
                `printf("%d\n", y1);`
            }
        """)
        assert(out == "40\n") { out }
    }

    // TUPLE

    @Test
    fun gg_01_tuple () {
        val out = test("""
            var x: [Int] = [10]
            `printf("%d\n", x._1);`
        """)
        assert(out == "40\n") { out }
    }
}
