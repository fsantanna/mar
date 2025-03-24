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
    @Test
    fun aa_04_float() {
        val out = test_int("""
            10 + 1.1
        """)
        assert(out == "11\n") { out }
    }
    @Test
    fun aa_05_float() {
        val out = test("""
            print(10 + 1.1)
        """)
        assert(out == "11.100000\n") { out }
    }
    @Test
    fun aa_06_string() {
        val out = test("""
            var s = "10 + 1.1"
            print(s)
        """)
        assert(out == "10 + 1.1\n") { out }
    }

    // EXPR / BIN / UNO

    @Test
    fun ab_01_float() {
        val out = test("""
            var x: Float = 1/2
            print(x)
        """)
        assert(out == "0.500000\n") { out }
    }
    @Test
    fun ab_02_float() {
        val out = test("""
            var x: Float = -1
            print(x)
        """)
        assert(out == "-1.000000\n") { out }
    }
    @Test
    fun ab_03_float() {
        val out = test("""
            var dy: Float = (10 - 9) / (3 - 1)
            print(dy)
        """)
        assert(out == "0.500000\n") { out }
    }
    @Test
    fun ab_04_field_float() {
        val out = test("""
            data X: [x:Int]
            var x1: X = X [1]
            var x2: X = X [2]
            var dy: Float = x1.x / x2.x
            print(dy)
        """)
        assert(out == "0.500000\n") { out }
    }
    @Test
    fun ab_05_uno_not() {
        val out = test("""
            print(!false)
        """)
        assert(out == "true\n") { out }
    }

    // EQUAL

    @Test
    fun ac_01_eq_tup () {
        val out = test("""
            print([] == [])
            print([] != [])
        """)
        assert(out == "true\nfalse\n") { out }
    }
    @Test
    fun ac_02_eq_tup () {
        val out = test("""
            print([1] == [1])
            print([1] != [1])
        """)
        assert(out == "true\nfalse\n") { out }
    }
    @Test
    fun ac_03_eq_tup () {
        val out = test("""
            print([1,[2],3] == [1,[2],3])
            print([1,[2],3] != [1,[2],3])
        """)
        assert(out == "true\nfalse\n") { out }
    }
    @Test
    fun ac_04_eq_vec () {
        val out = test("""
            print(#[] == #[])
            print(#[] != #[])
        """)
        assert(out == "anon : (lin 2, col 19) : inference error : unknown type\n") { out }
    }
    @Test
    fun ac_05_eq_vec () {
        val out = test("""
            print(#[1] == #[1])
            print(#[1] != #[1])
        """)
        assert(out == "true\nfalse\n") { out }
    }
    @Test
    fun ac_06_eq_vec () {
        val out = test("""
            print(#[#[3],#[1]] == #[#[3],#[1]])
            print(#[#[3],#[1]] != #[#[3],#[1]])
        """)
        assert(out == "true\nfalse\n") { out }
    }
    @Test
    fun ac_07_eq_vec () {
        val out = test("""
            var vs: #[Int*2]
            var ok = (vs == #[3])
            print(ok)
        """)
        assert(out == "false\n") { out }
    }
    @Test
    fun ad_01_eq_data () {
        val out = test("""
            data X: Int
            print(X(10) == X(10))
            print(X(10) != X(10))
        """)
        assert(out == "true\nfalse\n") { out }
    }
    @Test
    fun ad_02_eq_data () {
        val out = test("""
            data X: [Int]
            print(X[10] == X[10])
            print(X[10] != X[10])
        """)
        assert(out == "true\nfalse\n") { out }
    }

    // NAT

    @Test
    fun bb_01_nat_printf() {
        val out = test("""
            var x: Int = 1 + `10`
            `printf("%d\n", x)`
        """)
        assert(out == "11\n") { out }
    }
    @Test
    fun bb_02_nat_cast() {
        val out = test("""
            data XY: [Int,Int]
            ```
            typedef struct _XY {
                int x, y;
            } _XY;
            _XY _xy = { 10,20 };
            ```
            var xy: XY = `_xy`
            print(xy)
        """)
        assert(out == "XY [10,20]\n") { out }
    }
    @Test
    fun bb_03_nat_infer() {
        val out = test("""
            var x = `10`:Int
            print(x)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun bb_04_nat_infer() {
        val out = test("""
            ```
            typedef struct T {
                int v;
            } T;
            int f (T t) {
                return t.v;
            }
            ```
            data X: [Int]
            var v: Int = `f`(X[10])
            print(v)
        """)
        assert(out.contains("error: incompatible type for argument 1 of ‘f’")) { out }
        //assert(out == "10\n") { out }
    }
    @Test
    fun bb_05_nat_f() {
        val out = test("""
            `f`(`x`)
        """)
        assert(out.contains("error: implicit declaration of function ‘f’")) { out }
    }
    @Test
    fun bb_06_nat_f() {
        val out = test("""
            `f`([])
        """)
        assert(out.contains("error: implicit declaration of function ‘f’")) { out }
    }
    @Test
    fun bb_07_nat_f() {
        val out = test("""
            `f`([1])
        """)
        assert(out.contains("error: implicit declaration of function ‘f’")) { out }
    }
    @Test
    fun bb_08_nat_type() {
        val out = test("""
            var x: `int` = 10
            var y: Int = x
            print(y)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun bb_09_nat_equal() {
        val out = test("""
            expect(`1` == 1)
            print(10)
        """)
        assert(out == "10\n") { out }
    }

    // CALL / PRINT / IF / LOOP / MATCH

    @Test
    fun cc_01_print() {
        val out = test("""
            do {
                ```
                _VOID_ print_int (int v) {
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
    @Test
    fun cc_03_if() {
        val out = test("""
            var n: Int = 0
            var i: Int = 5
            loop {
                if i == 0 {
                    break
                }
                set n = n + i
                set i = i - 1
            }
            `printf("%d\n", n);`
        """)
        assert(out == "15\n") { out }
    }
    @Test
    fun cc_04_match () {
        val out = test("""
            match 10 { 10 {print(20)} ; else {print(99)} }
        """)
        assert(out == "20\n") { out }
    }
    @Test
    fun cc_05_match () {
        val out = test("""
            match 10 { 20 {print(99)} ; else {print(10)} }
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun cc_06_match () {
        val out = test("""
            match 10 { 10 {print(10)} ; else {throw()} }
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun cc_07_match () {
        val out = test("""
            data X.*: [] {
                A: [Int]
                B: []
            }
            var x = X.A [10]
            match x {
                :X.A  { print(x) }
                else { throw()  }
            }
        """)
        assert(out == "X.A [10]\n") { out }
    }
    @Test
    fun cc_08_loop_num () {
        val out = test("""
            loop n in 2 {
                print(n)
            }
        """)
        assert(out == "0\n1\n") { out }
    }
    @Test
    fun cc_09_call_err () {
        val out = test("""
            func f: (v: Int) -> () {
            }
            f (10, [2,1])
        """)
        assert(out == "anon : (lin 4, col 13) : call error : types mismatch\n") { out }
    }

    // BREAK / WHILE / UNTIL

    @Test
    fun cd_01_loop_break () {
        val out = test("""
            print(1)
            loop {
                print(2)
                if true {
                    break
                }
                print(99)
            }
            print(3)
        """)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun cd_02_loop_until () {
        val out = test("""
            print(1)
            loop {
                print(2)
                until(true)
                print(99)
            }
            print(3)
        """)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun cd_03_loop_until () {
        val out = test("""
            print(1)
            loop {
                print(2)
                while(false)
                print(99)
            }
            print(3)
        """)
        assert(out == "1\n2\n3\n") { out }
    }

    // FUNC

    @Test
    fun ee_01_func() {
        val out = test("""
            do {
                func f: (v: Int) -> Int {
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
            func f: (v: Int) -> Int {
                if v == 0 {
                    return(v)
                } else {
                    return(v + f(v-1))
                }
            }
            print(f(5))
        """)
        assert(out == "15\n") { out }
    }
    @Test
    fun ab_02_func_rec_mutual () {
        val out = test("""
            do {
                func f: (v: Int) -> Int {
                    if v == 0 {
                        return(v)
                    } else {
                        return(v + g(v-1))
                    }
                }
                func g: (v: Int) -> Int {
                    return(f(v))
                }
                print(g(5))
            }
        """)
        assert(out == "15\n") { out }
    }

    // PROTO / TYPES / NORMAL / NESTED / CLOSURE

    @Test
    fun ef_01_func_nested() {
        val out = test("""
            var i: Int = 10
            func f: () -> () {
                var j: Int = 20
                func g: () -> Int {
                    return(i + j)
                }
                print(g())
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
                func f: () -> (\func () -> Int) {
                    do {
                        var j: Int = 20
                        func g: () -> Int {
                            return(i + j)
                        }
                        return(\g)
                    }
                }
                var gg: (\func () -> Int) = f()
                print(gg\())
            }
        """)
        assert(out == "30\n") { out }
    }

    // CORO

    @Test
    fun ff_01_coro () {
        val out = test("""
            do {
                coro co: [0] () -> () -> () -> () {
                    `puts("OK");`
                }
                var exe: exec coro [0] () -> () -> () -> () = create(co)
                `puts("END");`
            }
        """)
        assert(out == "END\n") { out }
    }
    @Test
    fun ff_02_coro () {
        val out = test("""
            coro co: () -> () -> () -> () {
                `puts("OK");`
            }
            var exe: exec coro () -> () -> () -> () = create(co)
            start exe()
            ;;resume exe()
        """)
        assert(out == "OK\n") { out }
    }
    @Test
    fun ff_03_coro () {
        val out = test("""
            coro co: (v: Int) -> () -> () -> () {
                `printf("%d\n", mar_exe->mem.v);`
            }
            var exe: exec coro (Int) -> () -> () -> () = create(co)
            start exe(10)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun ff_04_coro () {
        val out = test("""
            coro co: (v: Int) -> Int -> () -> () {
                `printf("%d\n", mar_exe->mem.v);`
                yield()
                set v = v + 10
                `printf("%d\n", mar_exe->mem.v);`
            }
            var exe: exec coro (Int) -> Int -> () -> () = create(co)
            start exe(10)
            resume exe(99)
        """)
        assert(out == "10\n20\n") { out }
    }
    @Test
    fun ff_05_coro () {
        val out = test("""
            do {
                coro co: (x2: Int) -> Int -> Int -> Int {
                    var y2:Int = yield(x2*2)
                    return(y2 * 2)
                }
                var exe: exec coro (Int) -> Int -> Int -> Int = create(co)
                var x1: <Int,Int> = start exe(5)
                var y1: <Int,Int> = resume exe(x1!1 + 10)
                print(y1!2)
            }
        """)
        assert(out == "40\n") { out }
    }
    @Test
    fun ff_06_coro_global () {
        val out = test("""
            var x = 10
            do {
                coro co: (x2: Int) -> Int -> Int -> Int {
                    var y2:Int = yield(x2*2)
                    return((y2 * 2) + x)
                }
                var exe: exec coro (Int) -> Int -> Int -> Int = create(co)
                var x1: <Int,Int> = start exe(5)
                var y1: <Int,Int> = resume exe(x1!1 + 10)
                print(y1!2)
            }
        """)
        assert(out == "50\n") { out }
    }
    @Test
    fun ff_06_exec_no_coro () {
        val out = test("""
            var exe: exec coro (Int) -> Int -> Int -> Int
            print(10)
        """)
        assert(out == "10\n") { out }
    }

    // TUPLE

    @Test
    fun gg_01_tuple () {
        val out = test("""
            var x: [Int] = [10]: [Int]
            print(x.1)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun gg_02_tuple () {
        val out = test("""
            var x: [Int] = [10]: [Int]
            var y: [Int] = [20]: [Int]
            print([x,y])
        """)
        assert(out == "[[10],[20]]\n") { out }
    }
    @Test
    fun gg_03_tuple () {
        val out = test("""
            var x: [Int] = [10]: [Int]
            var y: Int = x.1
            print(y)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun gg_04_tuple () {
        val out = test("""
            var pos: [x:Int,y:Int] = [10,20]: [x:Int,y:Int]
            print(pos.y)
        """)
        assert(out == "20\n") { out }
    }
    @Test
    fun gg_05_tuple () {
        val out = test("""
            var dim: [w:Int,h:Int] = [10,20]: [w:Int,h:Int]
            var pos: [x:Int,y:Int] = [10,20]: [x:Int,y:Int]
            var y: Int = pos.y
            var w: Int = dim.w
            print([y, w])
        """)
        assert(out == "[20,10]\n") { out }
    }
    @Test
    fun gg_06_tuple () {
        val out = test("""
            var v: [x:Int] = [10*0.3]
            print(v)
        """)
        assert(out == "[3]\n") { out }
    }
    @Test
    fun gg_07_tuple () {
        val out = test("""
            var v: [x:Int] = [10*0.3]
            set v.x = v.x + 1
            print(v)
        """)
        assert(out == "[4]\n") { out }
    }
    @Test
    fun gg_08_tuple () {
        val out = test("""
            var a: [x:Int] = [10*0.3]
            var v: [Int]
            set v = a
            ;;set a = v
            print(v)
        """)
        assert(out == "[3]\n") { out }
    }
    @Test
    fun gg_09_tuple () {
        val out = test("""
            func f: (v: [Int]) -> () {
                print(v)
            }
            var a: [x:Int] = [10*0.3]
            f(a)
        """)
        assert(out == "[3]\n") { out }
    }
    @Test
    fun TODO_gg_10_tuple_field () {
        val out = test("""
            var a: [x:Int] = [x=10]
            print(a)
        """)
        assert(out == "[3]\n") { out }
    }

    // VECTOR

    @Test
    fun gh_01_vector () {
        val out = test("""
            var v: #[Int*3] = #[1,0,3]
            set v[1] = v[1] + 2
            print(v)
            print(#v)
        """)
        assert(out == "#[1,2,3]\n3\n") { out }
    }
    @Test
    fun gh_02_vector () {
        val out = test("""
            var v: #[Int*3]
            print(##v)
            print(#v)
        """)
        assert(out == "3\n0\n") { out }
    }
    @Test
    fun gh_03_vector () {
        val out = test("""
            var v: #[Int*3] = #[1,0,3]
            var vv: \#[Int] = \v
            print(vv\[2])
        """)
        assert(out == "3\n") { out }
    }
    @Test
    fun gh_04_vector_copy () {
        val out = test("""
            var a = #[1,2,3]
            var b = a
            set a[1] = 0
            print(a)
            print(b)
        """)
        assert(out == "#[1,0,3]\n#[1,2,3]\n") { out }
    }
    @Test
    fun gh_05_vector_max () {
        val out = test("""
            var a = #[1,2,3]
            set #a = 2
            print(#a)
            print(##a)
        """)
        assert(out == "2\n3\n") { out }
    }
    @Test
    fun gh_06_vector_cur () {
        val out = test("""
            var a = #[1,2,3]
            print(#a)
            print(##a)
        """)
        assert(out == "3\n3\n") { out }
    }
    @Test
    fun gh_07_vector_cur () {
        val out = test("""
            var a: #[Int*3] = #[1,2]
            print(#a)
            print(##a)
        """)
        assert(out == "2\n3\n") { out }
    }
    @Test
    fun gh_08_vector_cur () {
        val out = test("""
            var a: #[Int*2] = #[1,2,3]
            print(#a)
            print(##a)
        """)
        assert(out == "2\n2\n") { out }
    }
    @Test
    fun gh_09_vector_cur () {
        val out = test("""
            func f: (a: #[Int*2]) -> () {
                print(#a)
                print(##a)
            }
            f(#[1,2,3])
        """)
        assert(out == "2\n2\n") { out }
    }
    @Test
    fun gh_10_vector_print () {
        val out = test("""
            var a: #[Int*2] = #[10]
            print(a)
        """)
        assert(out == "#[10]\n") { out }
    }
    @Test
    fun gh_11_vector_dynamic () {
        val out = test("""
            var n = 2
            var a: #[Int*n] = #[10,20,30]
            print([##a, #a, a])
        """)
        //assert(out == "#[10,20]\n") { out }
        assert(out == "anon : (lin 3, col 26) : type error : expected constant integer expression\n") { out }
    }
    @Test
    fun gh_12_vector () {
        val out = test("""
            var i = 10
            var v: #[Int*3] = #[1,0,i]
            set v[1] = v[1] + 2
            print(v)
            print(#v)
        """)
        assert(out == "#[1,2,10]\n3\n") { out }
    }

    // CONCATENATE

    @Test
    fun gi_01_concat () {
        val out = test("""
            var a: #[Int*3] = #[1,2]
            var c = a ++ #[3,4]
            print([#a,#c,##c])
            print(c)
        """)
        assert(out == "[2,4,5]\n#[1,2,3,4]\n") { out }
    }
    @Test
    fun gi_02_concat () {
        val out = test("""
            var a: #[Char*3] = #['a','b']
            var c = a ++ #['c','d','\0']
            print([#c,##c])
            `puts(c.buf)`
        """)
        assert(out == "[5,6]\nabcd\n") { out }
    }
    @Test
    fun TODO_gi_03a_concat () { // missing \0
        val out = test("""
            var a: #[Char*3] = #['a','b']
            var c = a ++ "cd" ;; ++ ""
            print([#c,##c])
            `puts(c.buf)`
        """)
        assert(out == "[4,5]\nabcd\n") { out }
    }
    @Test
    fun gi_03b_concat () {
        val out = test("""
            var a: #[Char*3] = #['a','b']
            var c = a ++ "cd" ++ #['\0']
            print([#c,##c])
            `puts(c.buf)`
        """)
        assert(out == "[5,6]\nabcd\n") { out }
    }
    @Test
    fun gi_04_concat () {
        val out = test("""
            var c = "ab" ++ "cd" ++ #['\0']
            print([#c,##c])
            `puts(c.buf)`
        """)
        assert(out == "[5,5]\nabcd\n") { out }
    }

    // DATA

    @Test
    fun hh_01_data () {
        val out = test("""
            data Pos: [Int, Int]
            var p: Pos = Pos [10, 20]:[Int,Int]
            var x: Int = p.1
            print(x)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun hh_02_data () {
        val out = test("""
            data Pos: [Int, Int]
            data Dim: [Int, Int]
            data Obj: [Pos, Dim]
            var o: Obj = Obj [Pos [3,5]:[Int,Int], Dim [20,30]:[Int,Int]]: [Pos, Dim]
            var w: Int = o.2.1
            print(w)
        """)
        assert(out == "20\n") { out }
    }
    @Test
    fun hh_03_data () {
        val out = test("""
            data Km: Int
            var km: Km = Km(10)
            `printf("%d\n", km);`
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun hh_04_data () {
        val out = test("""
            data Result: <(),Int>
            var r: Result = Result <.2=10>: <(),Int>
            var i: Int = r!2
            print([i,r!2,r])
        """)
        assert(out == "[10,10,Result <.2=10>]\n") { out }
    }
    @Test
    fun hh_05_data () {
        val out = test("""
            data Res: <Err:(),Ok:Int>
            var r1: Res  = Res <.Ok=10>:  <Err:(),Ok:Int>
            var r2: Res  = Res <.Err=()>: <Err:(),Ok:Int>
            var i1: Int  = r1!Ok
            var e2: Bool = r2?Err
            print([i1, e2])
        """)
        assert(out == "[10,true]\n") { out }
    }
    @Test
    fun hh_06x_data () {
        val out = test("""
            data X: <A:[a:Int]>
            var xa = X.A [10]
            var x: X = xa
            print(xa!A.a)
            print(x!A.a)
        """)
        assert(out == "10\n10\n") { out }
    }
    @Test
    fun hh_06_data () {
        val out = test("""
            data X: <A:[a:Int]>
            var x = X.A [10]
            print(x!A.a)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun hh_07_data () {
        val out = test("""
            data B: <T:[], F:[]>
            var b: B = B.T []
            print(b?T)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun hh_08_data () {
        val out = test("""
            data A: <B: <C: Int>>
            var c: A = A.B.C(10)
            var v = c
            print(c)
            print(c!B!C)
        """)
        assert(out == "A <.1=<.1=10>>\n" +
                "10\n") { out }
    }
    @Test
    fun TODO_hh_09x_data () {
        val out = test("""
            data X: <
                Y: [Int],
            >
            var xy = X.Y [10]
            var y = xy
            print(y)        ;; TODO: X.Y [10]
        """)
        //assert(out == "X.Y [10]\n") { out }
        assert(out == "X <.1=[10]>\n") { out }
    }
    @Test
    fun hh_09y_data () {
        val out = test("""
            data X.*: [] {
                Y: [Int]
            }
            var xy = X.Y [10]
            var y = xy    ;; ()
            print(y)
        """)
        assert(out == "X.Y [10]\n") { out }
    }
    @Test
    fun hh_10_data () {
        val out = test("""
            data B: <T:[], F:[]>
            var b = B.T []
            print(b?B)      ;; OK
            ;;print(b!B)      ;; NO: no B._o
        """)
        assert(out == "anon : (lin 4, col 20) : predicate error : types mismatch\n") { out }
        //assert(out == "true\n") { out }
    }

    // DATA / HIER

    @Test
    fun hi_00_data () {
        val out = test("""
            data A.*: [Int]
            var a: A = A [100]
            print(a)
            print(a.1)
        """)
        assert(out == "A [100]\n" +
                "100\n") { out }
    }
    @Test
    fun hi_01_data () {
        val out = test("""
            data A.*: [Int] {
                B: [b:Int]
            }
            var x0: A = A [100]  ;; ignore subtype B
            print(x0)
            print(x0.1)
        """)
        assert(out == "A [100]\n" +
                "100\n") { out }
    }
    @Test
    fun hi_01x_data () {
        val out = test("""
            data A.*: [] {
                B: [Int]
            }
            var a: A = A.B [10]
            print(a)
        """)
        assert(out == "A []\n") { out }
    }
    @Test
    fun TODO_hi_01y_data () {   // sizeof
        val out = test("""
            data A.*: []
            data A.B.*: [Int,Int,Int,Int]
            var a: A = A.B [10,20,30,40]
            `printf("%ld\n", sizeof(A));`
            `printf("%ld\n", sizeof(A_B));`
            print(a!B)
        """)
        assert(out == "20\n20\nA.B [10,20,30,40]\n") { out }
    }
    @Test
    fun hi_01z_data () {
        val out = test("""
            data A.*: [Int] {
                B: [Int] {
                    C: [Int]
                }
            }
            var x0: A = A.B[10,20]  ;; ignore subsubtype C
            print(x0)
            print(x0!B)
        """)
        assert(out == "A [10]\n" +
                "A.B [10,20]\n") { out }
    }
    @Test
    fun hi_02_data () {
        val out = test("""
            data A.*: [a:Int] {
                B: [b:Int]
            }
            var x0 = A([100])  ;; ignore subtype
            print(x0)
            print(x0.a)
        """)
        assert(out == "A [100]\n" +
                "100\n") { out }
    }
    @Test
    fun hi_03_data () {
        val out = test("""
            data A.*: [a:Int] {
                B: [b:Int]
            }
            var x0: A   = A([100])
            var xa: A   = A.B([10,20])
            var xb: A.B = A.B([30,40])
            print(x0)
            print(xa)
            print(xb)
            
            print(x0.a)
            print(xa.a)
            print(xa!B.b)
            print(xb.a)
            print(xb.b)
        """)
        assert(out == "A [100]\n" +
                "A [10]\n" +
                "A.B [30,40]\n" +
                "100\n" +
                "10\n" +
                "20\n" +
                "30\n" +
                "40\n") { out }
    }
    @Test
    fun hi_04_data () {
        val out = test("""
            data A.*: [a:Int] {
                B: [b:Int] {
                    C: [c:Int]
                }
            }
            var c: A = A.B.C ([1,2,3])
            var v = c
            print(c)
            print(c!B!C)
            print(c!B!C.c)
        """)
        assert(out == "A [1]\n" +
                "A.B.C [1,2,3]\n" +
                "3\n") { out }
    }
    @Test
    fun hi_05_data () {
        val out = test("""
            data A: <B: <C: Int>>
            var c: A = A.B.C(10)
            var v = c!B!C
            `printf("%d\n", v);`
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun TODO_hi_06_data () {
        val out = test("""
            data A: [x:Int] + <B: [y:Int] + <C: Int>>
            var c: A.B.C = A.B.C([10],[20],30)
            var x = c!A.x
            var y = c!B.y
            print(x)
            print(y)
        """)
        assert(out == "10 / 1\n") { out }
    }
    @Test
    fun TODO_hi_07_data () {
        val out = test("""
            data Event: <
                Quit:  [ts:Int],
                Frame: [ts:Int, ms:Int],
                Key: <
                    Dn: [ts:Int, key:Int],
                    Up: [ts:Int, key:Int],
                >,
            >
            var q  = Event.Quit [10]
            var a  = q?Quit
            var ts = q!Quit.ts
            var up = Event.Key.Up [10,50]
            var b  = up?Key && up!Key?Up
            var k  = up!Key!Up.key
            `printf("%d / %d\n", a, ts);`
            `printf("%d / %d\n", b, k);`
        """)
        assert(out == "10 / 1\n") { out }
    }
    @Test
    fun TODO_hi_08_data () {
        val out = test("""
            data Event: [
                ts: Int,
                sub: <
                    Quit:  (),
                    Frame: Int,
                    Key: [
                        key: Int,
                        sub: <
                            Dn: (),
                            Up: (),
                        >,
                    ],
                >,
            ]
            ;;;
            data Event.*: [
                ts: Int,
                *: <
                    Quit:   (),
                    Frame:  Int,
                    Key:    [
                        key: Int,
                        *: <Dn:(), Up:()>
                    ],
                >,
            ]
            var q  = Event.Quit [10]
            var a  = q?Quit
            var ts = q!Quit.ts
            var up = Event.Key.Up [10,50]
            var b  = up?Key && up!Key?Up
            var k  = up!Key!Up.key
            `printf("%d / %d\n", a, ts);`
            `printf("%d / %d\n", b, k);`
            ;;;
        """)
        assert(out == "10 / 1\n") { out }
    }
    @Test
    fun hi_09_data_rep () {
        val out = test("""
            data X: <A:[x:Int],B:[x:Int]>
            var r: X = X.B [10]
            print(r)
            print(r!B.x)
        """)
        assert(out == "X <.2=[10]>\n10\n") { out }
    }
    @Test
    fun hi_10_data_many () {
        val out = test("""
            data B: <True:(), False:()>
            var b: B = B.True ()
            print(b?True)
            
            data Maybe: <Nothing:(), Just:Int>
            var x = Maybe.Just(10)
            print(x)
            
            data Meter: Int
            var y: Meter = Meter(10)
            print(y)
        """)
        assert(out == "true\n" +
                "Maybe <.2=10>\n" +
                "Meter(10)\n") { out }
    }
    @Test
    fun TODO_hi_10x_data_sub_print () {
        val out = test("""
            data V: <Z:(), V:Int>
            var v: V = V.V(10)
            print(v)
        """)
        assert(out == "V <.2=10>\n") { out }
        //assert(out == "V.V(10)\n") { out }
    }
    @Test
    fun hi_11_data () {
        val out = test("""
            data A.*: [x:Int] {
                B: [y:Int] {
                    C: [Int]
                }
            }
            var c: A = A.B.C([10,20,30])
            var b  = c!B
            var y  = c!B.y
            print(b)
            print(y)
        """)
        assert(out == "A.B [10,20]\n" +
                "20\n") { out }
    }

    // DATA / HIER / EXTD / EXTENDS

    @Test
    fun hj_01_hier_extd () {
        val out = test("""
            data X.*: [Int] {
                Y: []
                Z: [Int]
            }
            var xz: X = X.Z [10,20]
            var x = xz    ;; 10
            var y = xz!Z    ;; 20
            print(x)
            print(y.2)
        """)
        assert(out == "X [10]\n20\n") { out }
    }

    // INFER

    @Test
    fun ii_01_infer () {
        val out = test("""
            var n = 10
            print(n)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun ii_02_infer () {
        val out = test("""
            var t = [10,20]
            print(t.2)
        """)
        assert(out == "20\n") { out }
    }
    @Test
    fun ii_03_infer () {
        val out = test("""
            var t: <(),Int> = <.2=10>
            var n = t!2
            print(n)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun ii_04_infer () {
        val out = test("""
            data Res: <Err:(),Ok:Int>
            var r1 = Res.Ok(10)
            var r2 = r1
            var ok = r2?Ok
            print(ok)
        """)
        assert(out == "true\n") { out }
    }

    // PRINT

    @Test
    fun jj_01_print_int () {
        val out = test("""
            print(10)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun jj_02_print_tuple () {
        val out = test("""
            print([10,20])
        """)
        assert(out == "[10,20]\n") { out }
    }
    @Test
    fun jj_03_print_tuple () {
        val out = test("""
            print([10, [20], 30])
        """)
        assert(out == "[10,[20],30]\n") { out }
    }
    @Test
    fun jj_04_print_union () {
        val out = test("""
            var x = <.2=10>: <(),Int>
            print(x)
        """)
        assert(out == "<.2=10>\n") { out }
    }
    @Test
    fun jj_05_print_data () {
        val out = test("""
            data Meter: Int
            print(Meter(10))
        """)
        assert(out == "Meter(10)\n") { out }
    }
    @Test
    fun jj_06_print_data () {
        val out = test("""
            data X: <Y:(), Z:Int>
            var x = X.Z(10)
            print(x)
        """)
        assert(out == "X <.2=10>\n") { out }
    }

    // DATA / HIER / ENUM

    @Test
    fun jk_01_data_hier_enum () {
        val out = test("""
            data X.*: [] {
                Y: []
                Z: [] {
                    A: []
                }
            }
            data J.*: []
            var x   = X []
            var xy  = X.Y []
            var xz  = X.Z []
            var xza = X.Z.A []
            var j   = J []
            `printf("%X\n", x.tag)`
            `printf("%X\n", xy.tag)`
            `printf("%X\n", xz.tag)`
            `printf("%X\n", xza.tag)`
            `printf("%X\n", j.tag)`
        """)
        assert(out == "6000000\n" +
                "6100000\n" +
                "6200000\n" +
                "6208000\n" +
                "8000000\n") { out }
    }
    @Test
    fun jk_02_data_hier_enum () {
        val out = test("""
            data X.*: [] {
                Y: []
                Z: [] {
                    A: []
                }
            }
            data J.*: []
            var x   = X []
            var xy  = X.Y []
            var xz  = X.Z []
            var xza = X.Z.A []
            var j   = J []
            `printf("%d\n", mar_sup(x.tag,  x.tag))`
            `printf("%d\n", mar_sup(x.tag,  xy.tag))`
            `printf("%d\n", mar_sup(xy.tag, x.tag))`
            `printf("%d\n", mar_sup(xy.tag, xz.tag))`
            `printf("%d\n", mar_sup(xz.tag, xza.tag))`
            `printf("%d\n", mar_sup(j.tag,  x.tag))`
        """)
        assert(out == "1\n" +
                "1\n" +
                "0\n" +
                "0\n" +
                "1\n" +
                "0\n") { out }
    }

    // CATCH / THROW

    @Test
    fun kk_01_catch () {
        val out = test("""
            data X.*: []
            catch :X {
            }
            print(10)
        """)
        //assert(out == "anon : (lin 2, col 23) : declaration error : data :T is not declared\n") { out }
        assert(out == "10\n") { out }
    }
    @Test
    fun kk_02_throw () {
        val out = test("""
            data X.*: []
            catch {
                throw(X [])
                print(999)
            }
            print(10)
        """)
        //assert(out == "anon : (lin 2, col 23) : declaration error : data :T is not declared\n") { out }
        assert(out == "10\n") { out }
    }
    @Test
    fun kk_03_throw_err () {
        val out = test("""
            data X.*: []
            throw(X [])
            print(10)
        """)
        assert(out == "uncaught exception\n") { out }
    }
    @Test
    fun kk_04_throw_err () {
        val out = test("""
            data X.*: [] {
                Y: []
                Z: []
            }
            catch :X.Y {
                throw(X.Z [])
            }
            print(10)
        """)
        assert(out == "uncaught exception\n") { out }
    }
    @Test
    fun kk_05_throw () {
        val out = test("""
            data X.*: [] {
                Y: []
                Z: []
            }
            catch :X {
                throw(X.Z [])
            }
            print(10)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun kk_06_throw () {
        val out = test("""
            data X.*: [] {
                Y: []
                Z: []
            }
            catch :X.Z {
                throw(X.Z [])
            }
            print(10)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun kk_07_throw () {
        val out = test("""
            ;;;var x: Int =;;; throw()
        """)
        assert(out == "uncaught exception\n") { out }
    }
    @Test
    fun kk_08_throw_call () {
        val out = test("""
            func f: () -> () {
                print(2)
                throw()
                print(99)
            }
            print(1)
            f()
            print(99)
        """)
        assert(out == "1\n2\nuncaught exception\n") { out }
    }
    @Test
    fun kk_09_throw () {
        val out = test("""
            data X.*: []
            var e = catch :X {
                throw(X [])
                print(999)
            }
            print(e)
        """)
        //assert(out == "anon : (lin 2, col 23) : declaration error : data :T is not declared\n") { out }
        assert(out == "<.2=X []>\n") { out }
    }
    @Test
    fun kk_10_throw () {
        val out = test("""
            data X.*: []
            var e = catch :X {
                print(10)
            }
            print([e?Ok, e!Ok, e])
        """)
        //assert(out == "anon : (lin 2, col 23) : declaration error : data :T is not declared\n") { out }
        assert(out == "10\n[true,(),<.1=()>]\n") { out }
    }

    // DEFER

    @Test
    fun ll_01_defer () {
        val out = test("""
            print(1)
            defer { print(2) }
            defer { print(3) }
            print(4)
        """)
        assert(out == "1\n4\n3\n2\n") { out }
    }
    @Test
    fun ll_02_defer () {
        val out = test("""
            print(1)
            defer { print(2) }
            do {
                defer { print(3) }
                defer { print(4) }
            }
            defer { print(5) }
            print(6)
        """)
        assert(out == "1\n4\n3\n6\n5\n2\n") { out }
    }
    @Test
    fun ll_03_defer () {
        val out = test("""
            data X.*: []
            do {
                defer {
                    print(10)
                }
                throw(X[])
                print(99)
            }
        """)
        assert(out == "10\n" +
                "uncaught exception\n") { out }
    }

    // ESCAPE

    @Test
    fun mm_01_escape () {
        val out = test("""
            data X.*: [] {
                Y: []
                Z: []
            }
            do :X.Z {
                do :X.Y {
                    escape(X.Z [])
                    print(99)
                }
                print(99)
            }
            print(10)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun mm_02_return () {
        val out = test("""
            func f: () -> Int {
                return(10)
            }
            `printf("%d\n", MAR_ESCAPE.tag)`
            var x: Int = f()
            `printf("%d\n", MAR_ESCAPE.tag)`
            print(x)
        """)
        assert(out == "0\n0\n10\n") { out }
    }

    // EXPR / IF / MATCH

    @Test
    fun nn_01_if () {
        val out = test("""
            print(if true => 10 => 99)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun nn_02_match () {
        val out = test("""
            var x = match 10 { 10 => 20 ; else => 99 }
            print(x)
        """)
        assert(out == "20\n") { out }
    }
    @Test
    fun nn_03_match () {
        val out = test("""
            var x = match 10 { 20 => 99 ; else => 10 }
            print(x)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun nn_04_match () {
        val out = test("""
            var x: Int = match 10 { 10 => 10 ; else => 99 ;;;throw();;; }
            print(x)
        """)
        assert(out == "10\n") { out }
    }

    // TASK / AWAIT / EMIT

    @Test
    fun oo_01_task_create () {
        val out = test("""
            do {
                task tsk: () -> () {
                    `puts("OK");`
                }
                var exe: exec task () -> () = create(tsk)
                `puts("END");`
            }
        """)
        assert(out == "END\n") { out }
    }
    @Test
    fun oo_02_task_create_start () {
        val out = test("""
            task tsk: () -> () {
                `puts("OK");`
            }
            var exe: exec task () -> () = create(tsk)
            start exe()
            ;;resume exe()
        """)
        assert(out == "OK\n") { out }
    }
    @Test
    fun oo_03_coro () {
        val out = test("""
            coro co: (v: Int) -> () -> () -> () {
                `printf("%d\n", mar_exe->mem.v);`
            }
            var exe: exec coro (Int) -> () -> () -> () = create(co)
            start exe(10)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun oo_04_coro () {
        val out = test("""
            coro co: (v: Int) -> Int -> () -> () {
                `printf("%d\n", mar_exe->mem.v);`
                yield()
                set v = v + 10
                `printf("%d\n", mar_exe->mem.v);`
            }
            var exe: exec coro (Int) -> Int -> () -> () = create(co)
            start exe(10)
            resume exe(99)
        """)
        assert(out == "10\n20\n") { out }
    }
    @Test
    fun oo_05_coro () {
        val out = test("""
            do {
                coro co: (x2: Int) -> Int -> Int -> Int {
                    var y2:Int = yield(x2*2)
                    return(y2 * 2)
                }
                var exe: exec coro (Int) -> Int -> Int -> Int = create(co)
                var x1: <Int,Int> = start exe(5)
                var y1: <Int,Int> = resume exe(x1!1 + 10)
                print(y1!2)
            }
        """)
        assert(out == "40\n") { out }
    }
    @Test
    fun oo_06_coro_global () {
        val out = test("""
            var x = 10
            do {
                coro co: (x2: Int) -> Int -> Int -> Int {
                    var y2:Int = yield(x2*2)
                    return((y2 * 2) + x)
                }
                var exe: exec coro (Int) -> Int -> Int -> Int = create(co)
                var x1: <Int,Int> = start exe(5)
                var y1: <Int,Int> = resume exe(x1!1 + 10)
                print(y1!2)
            }
        """)
        assert(out == "50\n") { out }
    }

    // TEMPLATE / TYPE

    @Test
    fun tt_01_tpl () {
        val out = test("""
            data Maybe {{t:Type}}: <Nothing:(), Just:{{t}}>
            var x: Maybe {{:Int}} = Maybe {{:Int}}.Just(10)
            print(x!Just)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun tt_02_tpl () {
        val out = test("""
            data Maybe {{t:Type}}: <Nothing:(), Just:{{t}}>
            var x: Maybe {{:Int}} = Maybe {{:Int}}.Just(10)
            print(x)
        """)
        assert(out == "Maybe <.2=10>\n") { out }
    }
    @Test
    fun tt_03_catch () {
        val out = test("""
            data X.* {{t:Type}}: [{{t}}]
            catch :X {{:Int}} {
            }
            print(10)
        """)
        //assert(out == "anon : (lin 2, col 23) : declaration error : data :T is not declared\n") { out }
        assert(out == "10\n") { out }
    }
    @Test
    fun tt_04_field () {
        val out = test("""
            data Pos {{t:Type}}: [{{t}}, Int]
            var p: Pos = Pos [10, 20]:[Int,Int]
            var x: Int = p.1
            print(x)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun tt_05_data () {
        val out = test("""
            data A {{t:Type}}: [{{t}}]
            var a: A = A [100]
            print(a)
            print(a.1)
        """)
        assert(out == "A [100]\n" +
                "100\n") { out }
    }

    // TEMPLATE / NUMBER

    @Test
    fun ts_01_tpl () {
        val out = test("""
            data Vec {{n:Int}}: #[Int * {{n}}]
            var vs: Vec {{10}} = Vec {{10}} (#[])
            print(##vs)
        """)
        assert(out == "10\n") { out }
    }

    // TEMPLATE / FUNC

    @Test
    fun tu_01_tpl () {
        val out = test("""
            func f {{n:Int}}: () -> Int {
                return({{n}})
            }
            print(f {{10}} ())
            print(f {{20}} ())
        """)
        assert(out == "10\n20\n") { out }
    }
    @Test
    fun tu_02_tpl () {
        val out = test("""
            func f {{n:Int}}: () -> Int {
                var xs: #[Int*{{n}}]
                return(##xs+1)
            }
            print(f {{10}} ())
            print(f {{20}} ())
        """)
        assert(out == "11\n21\n") { out }
    }
    @Test
    fun tu_03_tpl () {
        val out = test("""
            func f {{w:Int,h:Int}}: () -> Int {
                var xs: #[Int*{{w}}*{{h}}]
                return(##xs)
            }
            print(f {{10,20}} ())
            print(f {{20,30}} ())
        """)
        assert(out == "200\n600\n") { out }
    }
    @Test
    fun tu_04_tpl () {
        val out = test("""
            func f {{n:Int}}: () -> #[Int*({{n}})] {
            }
            var rs = f {{10}} ()
            print(##rs)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun tu_05_tpl () {
        val out = test("""
            func f {{n:Int}}: (xs: #[Int*({{n}})]) -> Int {
                return (##xs)
            }
            print(f {{10}} (#[]))
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun tu_06_tpl () {
        val out = test("""
            func f {{n:Int}}: (xs: #[Int*({{n}})]) -> Int {
                return (##xs)
            }
            print(f {{10}} (#[]))
            print(f {{10}} (#[]))
        """)
        assert(out == "10\n10\n") { out }
    }

    // TEST

    @Test
    fun xx_01_test () {
        G.test = false
        val out = test("""
            print("1")
            test {
                print("2")
            }
            print("3")
        """)
        assert(out == "1\n3\n") { out }
    }
    @Test
    fun xx_02_test () {
        G.test = true
        val out = test("""
            print("1")
            test {
                print("2")
            }
            print("3")
        """)
        assert(out == "1\n2\n3\n") { out }
    }

    // PRELUDE

    @Test
    fun xj_01_pre_assert () {
        val out = test("""
            print(1)
            expect(true)
            print(2)
            expect(false)
            print(3)
        """)
        assert(out == "1\n"+
            "2\n"+
            "uncaught exception\n") { out }
    }

    // MISC

    @Test
    fun zz_01_data () {
        val out = test("""
            data X.*: [] {
                A: [x: Char]
                B: [x: Int]
            }
            var a = X.A ['a']
            print(a.x)
            var b = X.B [10]
            print(b.x)
        """)
        assert(out == "a\n10\n") { out }
    }
    @Test
    fun zz_02_xxx () {
        val out = test("""
            return()
        """)
        assert(out == "anon : (lin 2, col 13) : escape error : expected matching enclosing block\n") { out }
    }
    @Test
    fun zz_03_yyy () {
        val out = test("""
            var a = #[]     ;; infer null, check error
            var b = []
            var c = b.x     ;; infer exception, do not check infer
        """)
        assert(out == "anon : (lin 4, col 22) : field error : invalid index\n") { out }
        //assert(out == "anon : (lin 2, col 21) : inference error : unknown type\n") { out }
        //assert(out == "anon : (lin 2, col 17) : inference error : unknown type\n") { out }
    }
    @Test
    fun zz_04 () {
        val out = test("""
        do(1\)
        """)
        assert(out == "anon : (lin 2, col 13) : operation error : expected pointer\n") { out }
        //assert(out == "anon : (lin 2, col 21) : inference error : unknown type\n") { out }
        //assert(out == "anon : (lin 2, col 17) : inference error : unknown type\n") { out }
    }
    @Test
    fun zz_05 () {
        val out = test("""
        func f: (v: \Int) -> Int {
            return(v\)
        }
        print(f(\1))
        """)
        assert(out == "1\n") { out }
    }
}
