package test

import mar.*
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

fun test_int (src: String): String {
    return test("""
        var v: Int = $src
        dump(v)
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
        assert(out == "11.1\n") { out }
    }
    @Test
    fun aa_05_float() {
        val out = test("""
            dump(10 + 1.1)
        """)
        assert(out == "11.1\n") { out }
    }
    @Test
    fun aa_06_string() {
        val out = test("""
            var s = "10 + 1.1"
            dump(s)
        """)
        assert(out == "10 + 1.1\n") { out }
    }

    // EXPR / BIN / UNO

    @Test
    fun ab_01_float() {
        val out = test("""
            var x: Float = 1/2
            dump(x)
        """)
        assert(out == "0.5\n") { out }
    }
    @Test
    fun ab_02_float() {
        val out = test("""
            var x: Float = -1
            dump(x)
        """)
        assert(out == "-1\n") { out }
    }
    @Test
    fun ab_03_float() {
        val out = test("""
            var dy: Float = (10 - 9) / (3 - 1)
            dump(dy)
        """)
        assert(out == "0.5\n") { out }
    }
    @Test
    fun ab_04_field_float() {
        val out = test("""
            data X: [x:Int]
            var x1: X = X [1]
            var x2: X = X [2]
            var dy: Float = x1.x / x2.x
            dump(dy)
        """)
        assert(out == "0.5\n") { out }
    }
    @Test
    fun ab_05_uno_not() {
        val out = test("""
            dump(!false)
        """)
        assert(out == "true\n") { out }
    }

    // EQUAL

    @Test
    fun ac_01_eq_tup () {
        val out = test("""
            dump([] == [])
            dump([] != [])
        """)
        assert(out == "true\nfalse\n") { out }
    }
    @Test
    fun ac_02_eq_tup () {
        val out = test("""
            dump([1] == [1])
            dump([1] != [1])
        """)
        assert(out == "true\nfalse\n") { out }
    }
    @Test
    fun ac_03_eq_tup () {
        val out = test("""
            dump([1,[2],3] == [1,[2],3])
            dump([1,[2],3] != [1,[2],3])
        """)
        assert(out == "true\nfalse\n") { out }
    }
    @Test
    fun ad_01_eq_data () {
        val out = test("""
            data X: Int
            dump(X(10) == X(10))
            dump(X(10) != X(10))
        """)
        assert(out == "true\nfalse\n") { out }
    }
    @Test
    fun ad_02_eq_data () {
        val out = test("""
            data X: [Int]
            dump(X[10] == X[10])
            dump(X[10] != X[10])
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
            dump(xy)
        """)
        assert(out == "XY [10,20]\n") { out }
    }
    @Test
    fun bb_03_nat_infer() {
        val out = test("""
            var x = `10`:Int
            dump(x)
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
            dump(v)
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
            dump(y)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun bb_09_nat_equal() {
        val out = test("""
            expect(`1` == 1)
            dump(10)
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
            match 10 { 10 {dump(20)} ; else {dump(99)} }
        """)
        assert(out == "20\n") { out }
    }
    @Test
    fun cc_05_match () {
        val out = test("""
            match 10 { 20 {dump(99)} ; else {dump(10)} }
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun cc_06_match () {
        val out = test("""
            match 10 { 10 {dump(10)} ; else {throw()} }
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
                :X.A  { dump(x) }
                else { throw()  }
            }
        """)
        assert(out == "X.A [10]\n") { out }
    }
    @Test
    fun cc_08_loop_num () {
        val out = test("""
            loop n in 2 {
                dump(n)
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
            dump(1)
            loop {
                dump(2)
                if true {
                    break
                }
                dump(99)
            }
            dump(3)
        """)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun cd_02_loop_until () {
        val out = test("""
            dump(1)
            loop {
                dump(2)
                until(true)
                dump(99)
            }
            dump(3)
        """)
        assert(out == "1\n2\n3\n") { out }
    }
    @Test
    fun cd_03_loop_until () {
        val out = test("""
            dump(1)
            loop {
                dump(2)
                while(false)
                dump(99)
            }
            dump(3)
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
            dump(f(5))
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
                dump(g(5))
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
                dump(g())
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
                dump(gg\())
            }
        """)
        assert(out == "30\n") { out }
    }

    // CORO

    @Test
    fun ff_01_coro () {
        val out = test("""
            do {
                coro co: [co] () -> () -> () -> () {
                    `puts("OK");`
                }
                var exe: exec coro [co] () -> () -> () -> () = create(co)
                `puts("END");`
            }
        """)
        assert(out == "END\n") { out }
    }
    @Test
    fun ff_02x_coro () {
        val out = test("""
            coro co: () -> () -> () -> () {
            }
            `puts("OK");`
        """)
        assert(out == "OK\n") { out }
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
                dump(y1!2)
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
                dump(y1!2)
            }
        """)
        assert(out == "50\n") { out }
    }
    @Test
    fun ff_06_exec_no_coro () {
        val out = test("""
            var exe: exec coro (Int) -> Int -> Int -> Int
            dump(10)
        """)
        assert(out == "anon : (lin 2, col 22) : inference error : unknown size\n") { out }
    }
    @Test
    fun ff_07_exec_coro_defer () {
        val out = test("""
            coro co: () -> () -> () -> () {
                defer {
                    dump("ok")
                }
                yield()
            }
            do {
                var exe = create(co)
                start exe()
            }
        """)
        assert(out == "ok\n") { out }
    }
    @Test
    fun ff_08_exec_coro_defer_no_create () {
        val out = test("""
            coro co: () -> () -> () -> () {
                defer {
                    dump("ok")
                }
                yield()
            }
            do {
                var exe: exec coro [co] () -> () -> () -> ()
            }
            dump("ok")
        """)
        assert(out == "ok\n") { out }
    }
    @Test
    fun ff_09_coro_func_gcc_bug () {
        val out = test("""
            func f: () -> () {
            }
            coro co: () -> () -> () -> () {
                f()
            }
            dump("ok")
        """)
        assert(out == "ok\n") { out }
    }

    // TUPLE

    @Test
    fun gg_01_tuple () {
        val out = test("""
            var x: [Int] = [10,20]: [Int]
            dump(x[1])
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun gg_02_tuple () {
        val out = test("""
            var x: [Int] = [10]: [Int]
            var y: [Int] = [20]: [Int]
            dump([x,y])
        """)
        assert(out == "[[10],[20]]\n") { out }
    }
    @Test
    fun gg_03_tuple () {
        val out = test("""
            var x: [Int] = [10]: [Int]
            var y: Int = x.1
            dump(y)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun gg_04_tuple () {
        val out = test("""
            var pos: [x:Int,y:Int] = [10,20]: [x:Int,y:Int]
            dump(pos.y)
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
            dump([y, w])
        """)
        assert(out == "[20,10]\n") { out }
    }
    @Test
    fun gg_06_tuple () {
        val out = test("""
            var v: [x:Int] = [10*0.3]
            dump(v)
        """)
        assert(out == "[3]\n") { out }
    }
    @Test
    fun gg_07_tuple () {
        val out = test("""
            var v: [x:Int] = [10*0.3]
            set v.x = v.x + 1
            dump(v)
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
            dump(v)
        """)
        assert(out == "[3]\n") { out }
    }
    @Test
    fun gg_09_tuple () {
        val out = test("""
            func f: (v: [Int]) -> () {
                dump(v)
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
            dump(a)
        """)
        assert(out == "[3]\n") { out }
    }

    // CONCATENATE

    @Test
    fun gi_01_concat () {
        val out = test("""
            var a: #[Int*3] = #[1,2]
            var c = a ++ #[3,4]
            dump([#a,#c,##c])
            dump(c)
        """)
        assert(out == "[2,4,5]\n#[1,2,3,4]\n") { out }
    }
    @Test
    fun gi_02_concat () {
        val out = test("""
            var a: #[Char*3] = #['a','b']
            var c = a ++ #['c','d','\0']
            dump([#c,##c])
            `puts(c.buf)`
        """)
        assert(out == "[5,6]\nabcd\n") { out }
    }
    @Test
    fun TODO_gi_03a_concat () { // missing \0
        val out = test("""
            var a: #[Char*3] = #['a','b']
            var c = a ++ "cd" ;; ++ ""
            dump([#c,##c])
            `puts(c.buf)`
        """)
        assert(out == "[4,5]\nabcd\n") { out }
    }
    @Test
    fun gi_03b_concat () {
        val out = test("""
            var a: #[Char*3] = #['a','b']
            var c = a ++ "cd" ++ #['\0']
            dump([#c,##c])
            `puts(c.buf)`
        """)
        assert(out == "[5,6]\nabcd\n") { out }
    }
    @Test
    fun gi_04_concat () {
        val out = test("""
            var c = "ab" ++ "cd" ++ #['\0']
            dump([#c,##c])
            `puts(c.buf)`
        """)
        assert(out == "[5,5]\nabcd\n") { out }
    }

    // DATA

    @Test
    fun hh_00_data () {
        val out = test("""
            data Pos: [Bool]
            var p = Pos [true]
            dump(p)
        """)
        assert(out == "Pos [true]\n") { out }
    }
    @Test
    fun hh_01_data () {
        val out = test("""
            data Pos: [Int, Int]
            var p: Pos = Pos [10, 20]:[Int,Int]
            var x: Int = p.1
            dump(x)
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
            dump(w)
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
            dump([i,r!2,r])
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
            dump([i1, e2])
        """)
        assert(out == "[10,true]\n") { out }
    }
    @Test
    fun hh_06x_data () {
        val out = test("""
            data X: <A:[a:Int]>
            var xa = X.A [10]
            var x: X = xa
            dump(xa!A.a)
            dump(x!A.a)
        """)
        assert(out == "10\n10\n") { out }
    }
    @Test
    fun hh_06_data () {
        val out = test("""
            data X: <A:[a:Int]>
            var x = X.A [10]
            dump(x!A.a)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun hh_07_data () {
        val out = test("""
            data B: <T:[], F:[]>
            var b: B = B.T []
            dump(b?T)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun hh_08_data () {
        val out = test("""
            data A: <B: <C: Int>>
            var c: A = A.B.C(10)
            var v = c
            dump(c)
            dump(c!B!C)
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
            dump(y)        ;; TODO: X.Y [10]
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
            dump(y)
        """)
        assert(out == "X.Y [10]\n") { out }
    }
    @Test
    fun hh_10_data () {
        val out = test("""
            data B: <T:[], F:[]>
            var b = B.T []
            dump(b?B)      ;; OK
            ;;dump(b!B)      ;; NO: no B._o
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
            dump(a)
            dump(a.1)
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
            dump(x0)
            dump(x0.1)
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
            dump(a)
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
            dump(a!B)
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
            dump(x0)
            dump(x0!B)
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
            dump(x0)
            dump(x0.a)
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
            dump(x0)
            dump(xa)
            dump(xb)
            
            dump(x0.a)
            dump(xa.a)
            dump(xa!B.b)
            dump(xb.a)
            dump(xb.b)
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
            dump(c)
            dump(c!B!C)
            dump(c!B!C.c)
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
            dump(x)
            dump(y)
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
            dump(r)
            dump(r!B.x)
        """)
        assert(out == "X <.2=[10]>\n10\n") { out }
    }
    @Test
    fun hi_10_data_many () {
        val out = test("""
            data B: <True:(), False:()>
            var b: B = B.True ()
            dump(b?True)
            
            data Maybe: <Nothing:(), Just:Int>
            var x = Maybe.Just(10)
            dump(x)
            
            data Meter: Int
            var y: Meter = Meter(10)
            dump(y)
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
            dump(v)
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
            dump(b)
            dump(y)
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
            dump(x)
            dump(y.2)
        """)
        assert(out == "X [10]\n20\n") { out }
    }
    @Test
    fun hj_02_hier_extd () {
        val out = test("""
            data X.*: [Int] {
                Y: []
                Z: [Int]
            }
            var xz: X = X.Z [10,20]
            dump([xz?Y, xz?Z])
        """)
        assert(out == "[false,true]\n") { out }
    }

    // INFER

    @Test
    fun ii_01_infer () {
        val out = test("""
            var n = 10
            dump(n)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun ii_02_infer () {
        val out = test("""
            var t = [10,20]
            dump(t.2)
        """)
        assert(out == "20\n") { out }
    }
    @Test
    fun ii_03_infer () {
        val out = test("""
            var t: <(),Int> = <.2=10>
            var n = t!2
            dump(n)
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
            dump(ok)
        """)
        assert(out == "true\n") { out }
    }

    // PRINT

    @Test
    fun jj_01_print_int () {
        val out = test("""
            dump(10)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun jj_02_print_tuple () {
        val out = test("""
            dump([10,20])
        """)
        assert(out == "[10,20]\n") { out }
    }
    @Test
    fun jj_03_print_tuple () {
        val out = test("""
            dump([10, [20], 30])
        """)
        assert(out == "[10,[20],30]\n") { out }
    }
    @Test
    fun jj_04_print_union () {
        val out = test("""
            var x = <.2=10>: <(),Int>
            dump(x)
        """)
        assert(out == "<.2=10>\n") { out }
    }
    @Test
    fun jj_05_print_data () {
        val out = test("""
            data Meter: Int
            dump(Meter(10))
        """)
        assert(out == "Meter(10)\n") { out }
    }
    @Test
    fun jj_06_print_data () {
        val out = test("""
            data X: <Y:(), Z:Int>
            var x = X.Z(10)
            dump(x)
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
        assert(out == "2000000\n" +
                "2100000\n" +
                "2200000\n" +
                "2208000\n" +
                "3000000\n") { out }
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
            dump(10)
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
                dump(999)
            }
            dump(10)
        """)
        //assert(out == "anon : (lin 2, col 23) : declaration error : data :T is not declared\n") { out }
        assert(out == "10\n") { out }
    }
    @Test
    fun kk_03_throw_err () {
        val out = test("""
            data X.*: []
            throw(X [])
            dump(10)
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
            dump(10)
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
            dump(10)
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
            dump(10)
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
                dump(2)
                throw()
                dump(99)
            }
            dump(1)
            f()
            dump(99)
        """)
        assert(out == "1\n2\nuncaught exception\n") { out }
    }
    @Test
    fun kk_09_throw () {
        val out = test("""
            data X.*: []
            var e = catch :X {
                throw(X [])
                dump(999)
            }
            dump(e)
        """)
        //assert(out == "anon : (lin 2, col 23) : declaration error : data :T is not declared\n") { out }
        assert(out == "<.2=X []>\n") { out }
    }
    @Test
    fun kk_10_throw () {
        val out = test("""
            data X.*: []
            var e = catch :X {
                dump(10)
            }
            dump([e?Ok, e!Ok, e])
        """)
        //assert(out == "anon : (lin 2, col 23) : declaration error : data :T is not declared\n") { out }
        assert(out == "10\n[true,(),<.1=()>]\n") { out }
    }

    // DEFER

    @Test
    fun ll_01_defer () {
        val out = test("""
            dump(1)
            defer { dump(2) }
            defer { dump(3) }
            dump(4)
        """)
        assert(out == "1\n4\n3\n2\n") { out }
    }
    @Test
    fun ll_02_defer () {
        val out = test("""
            dump(1)
            defer { dump(2) }
            do {
                defer { dump(3) }
                defer { dump(4) }
            }
            defer { dump(5) }
            dump(6)
        """)
        assert(out == "1\n4\n3\n6\n5\n2\n") { out }
    }
    @Test
    fun ll_03_defer () {
        val out = test("""
            data X.*: []
            do {
                defer {
                    dump(10)
                }
                throw(X[])
                dump(99)
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
                    dump(99)
                }
                dump(99)
            }
            dump(10)
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
            dump(x)
        """)
        assert(out == "0\n0\n10\n") { out }
    }

    // EXPR / IF / MATCH

    @Test
    fun nn_01_if () {
        val out = test("""
            dump(if true => 10 => 99)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun nn_02_match () {
        val out = test("""
            var x = match 10 { 10 => 20 ; else => 99 }
            dump(x)
        """)
        assert(out == "20\n") { out }
    }
    @Test
    fun nn_03_match () {
        val out = test("""
            var x = match 10 { 20 => 99 ; else => 10 }
            dump(x)
        """)
        assert(out == "10\n") { out }
    }
    @Test
    fun nn_04_match () {
        val out = test("""
            var x: Int = match 10 { 10 => 10 ; else => 99 ;;;throw();;; }
            dump(x)
        """)
        assert(out == "10\n") { out }
    }

    // TASK / AWAIT / EMIT

    @Test
    fun oo_01_task_create () {
        val out = test("""
            ;;var x: Event
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
    fun oo_03_task_await_emit () {
        val out = test("""
            data X: Int
            task tsk: () -> () {
                var e = await(:X)
                dump(e)
            }
            var exe: exec task () -> () = create(tsk)
            start exe()
            emit(X(10))
        """)
        assert(out == "X(10)\n") { out }
    }
    @Test
    fun oo_04_task_await_emit () {
        val out = test("""
            data X: Int
            data Y: Int
            task tsk: () -> () {
                var e = await(:X)
                dump(e)
                var f = await(:Y)
                dump(f)
            }
            var exe: exec task () -> () = create(tsk)
            start exe()
            emit(X(10))
            emit(X(10))
        """)
        assert(out == "X(10)\n") { out }
    }
    @Test
    fun oo_05_task_await_emit () {
        val out = test("""
            data X: Int
            data Y: Int
            task tsk: () -> () {
                var e = await(:X)
                dump(e)
                var f = await(:Y)
                dump(f)
            }
            var exe: exec task () -> () = create(tsk)
            start exe()
            emit(X(10))
            emit(Y(10))
        """)
        assert(out == "X(10)\nY(10)\n") { out }
    }
    @Test
    fun oo_06_task_spawn () {
        val out = test("""
            data X: Int
            data Y: Int
            task tsk: () -> () {
                var e = await(:X)
                dump(e)
                var f = await(:Y)
                dump(f)
            }
            spawn tsk()
            emit(X(10))
            emit(Y(10))
        """)
        assert(out == "X(10)\nY(10)\n") { out }
    }
    @Test
    fun oo_07_task_spawn () {
        val out = test("""
            data X: Int
            data Y: Int
            task tsk: () -> () {
                var e = await(:X)
                dump(e)
                var f = await(:Y)
                dump(f)
            }
            var e = spawn tsk()
            ;;dump(e)
            emit(X(10))
            emit(Y(20))
        """)
        assert(out == "X(10)\nY(20)\n") { out }
    }
    @Test
    fun oo_07x_task_spawn () {
        val out = test("""
            data X: Int
            task tsk: () -> () {
                var e = await(:X)
                dump(e)
            }
            var e = spawn tsk()
            ;;dump(e)
            emit(X(10))
        """)
        assert(out == "X(10)\n") { out }
    }
    @Test
    fun oo_08_task_await_until () {
        val out = test("""
            data X: Int
            data Y: Int
            task tsk: () -> () {
                var e = await(:X, it==X(10))
                dump(e)
            }
            spawn tsk()
            emit(X(99))
            emit(X(10))
            emit(X(99))
        """)
        assert(out == "X(10)\n") { out }
    }
    @Test
    fun oo_09_task_term () {
        val out = test("""
            data X: ()
            task t2: () -> () {
                dump(">>> B")
                await(:X)
                dump("<<< B")
                ;;emit(Event.Task [`mar_exe`])
            }
            spawn {
                var exe = create(t2)
                start exe()
                dump(">>> A")
                var e = await(exe)
                dump("<<< A")
            }
            emit(X())
        """)
        assert(out == ">>> B\n>>> A\n<<< B\n<<< A\n") { out }
    }
    @Test
    fun oo_10_task_defer () {
        val out = test("""
            data X: ()
            task t2: () -> () {
                defer {
                    dump("first")
                }
                await(true)
            }
            spawn {
                defer {
                    dump("last")
                }
                var exe = create(t2)
                start exe()
            }
        """)
        assert(out == "first\nlast\n") { out }
    }
    @Test
    fun oo_10x_task_defer () {
        val out = test("""
            task t: () -> () {
                defer {
                }
            }
            dump(1)
        """)
        assert(out == "1\n") { out }
    }
    @Test
    fun oo_11_task_term () {
        val out = test("""
            data X: ()
            data Y: ()
            task t2: () -> () {
                await(:X)
            }
            task t3: () -> () {
                await(:Y)
            }
            spawn {
                var e2 = create(t2)
                var e3 = create(t3)
                start e2()
                start e3()
                var e = await(e2)
                dump("nok")
            }
            emit(Y())
            dump("ok")
        """)
        assert(out == "ok\n") { out }
    }
    @Test
    fun oo_12_task_terminated () {
        val out = test("""
            data X: ()
            task t2: () -> () {
                dump("t2")
            }
            spawn {
                var exe = create(t2)
                start exe()
                await(exe)
                dump("main")
            }
        """)
        assert(out == "t2\nmain\n") { out }
    }
    @Test
    fun oo_13_task_undead_bug () {
        val out = test("""
            data X: ()
            spawn {
                par_or {
                    await(:X)
                    dump("x")
                } with {
                    every :X {
                        dump("no")
                    }
                }
                dump("or")
            }
            emit(X())
            emit(X())
            emit(X())
            emit(X())
            dump("ok")
        """)
        assert(out == "x\nor\nok\n") { out }
    }
    @Test
    fun oo_13x_task_undead_bug () {
        val out = test("""
            data X: ()
            spawn {
                par_or {
                    await(:X)
                    dump("x")
                } with {
                    every :X {
                        dump("no")
                    }
                }
                dump("or")
            }
            emit(X())
            dump("ok")
        """)
        assert(out == "x\nor\nok\n") { out }
    }

    // SPAWN / PAR / AWAIT(exe) / AWAIT t(...) / EVERY

    @Test
    fun op_01_task_spawn () {
        val out = test("""
            data X: Int
            data Y: Int
            spawn {
                var e = await(:X)
                dump(e)
                var f = await(:Y)
                dump(f)
            }
            emit(X(10))
            emit(Y(10))
        """)
        assert(out == "X(10)\nY(10)\n") { out }
    }
    @Test
    fun op_02_par () {
        val out = test("""
            data X: Int
            data Y: Int
            spawn {
                par {
                    var e = await(:X)
                    dump(e)
                } with {
                    var f = await(:Y)
                    dump(f)
                }
                dump("nooo")
            }
            emit(X(10))
            emit(Y(10))
        """)
        assert(out == "X(10)\nY(10)\n") { out }
    }
    @Test
    fun op_02x_par () {
        val out = test("""
            data X: Int
            spawn {
                spawn {
                    await(:X)
                }
                await(false)
            }
            emit(X(10))
            dump("ok")
        """)
        assert(out == "ok\n") { out }
    }
    @Test
    fun oo_03_task_term () {
        val out = test("""
            data X: ()
            task t2: () -> () {
                dump(">>> B")
                await(:X)
                dump("<<< B")
                ;;emit(Event.Task [`mar_exe`])
            }
            spawn {
                var exe = create(t2)
                start exe()
                dump(">>> A")
                var e = await(exe)
                dump("<<< A")
            }
            emit(X())
        """)
        assert(out == ">>> B\n>>> A\n<<< B\n<<< A\n") { out }
    }
    @Test
    fun oo_04_task_term () {
        val out = test("""
            data X: ()
            task t2: () -> () {
                dump(">>> B")
                await(:X)
                dump("<<< B")
                ;;emit(Event.Task [`mar_exe`])
            }
            spawn {
                dump(">>> A")
                await t2()
                dump("<<< A")
            }
            emit(X())
        """)
        assert(out == ">>> A\n>>> B\n<<< B\n<<< A\n") { out }
    }
    @Test
    fun op_05_par_and () {
        val out = test("""
            data X: Int
            data Y: Int
            spawn {
                par_and {
                    await(:X)
                } with {
                    await(:Y)
                }
                dump("ok")
            }
            emit(X(10))
            emit(Y(10))
        """)
        assert(out == "ok\n") { out }
    }
    @Test
    fun op_06_par_or () {
        val out = test("""
            data X: Int
            data Y: Int
            spawn {
                par_or {
                    await(:X)
                    dump("x")
                } with {
                    await(:Y)
                    dump("y")
                }
                dump("ok")
            }
            emit(X(10))
        """)
        assert(out == "x\nok\n") { out }
    }
    @Test
    fun op_07_par_or () {
        val out = test("""
            data X: Int
            data Y: Int
            spawn {
                par_or {
                    await(:X)
                    dump("x")
                } with {
                    await(:Y)
                    dump("y")
                }
                dump("ok")
            }
            emit(Y(10))
        """)
        assert(out == "y\nok\n") { out }
    }
    @Test
    fun op_08_par_or () {
        val out = test("""
            data X: Int
            data Y: Int
            spawn {
                par_or {
                    dump("x")
                } with {
                    await(:Y)
                    dump("y")
                }
                dump("ok")
            }
        """)
        assert(out == "x\nok\n") { out }
    }
    @Test
    fun op_09_par_or () {
        val out = test("""
            data X: Int
            data Y: Int
            spawn {
                par_or {
                    await(:X)
                    dump("x")
                } with {
                    dump("y")
                }
                dump("ok")
            }
        """)
        assert(out == "y\nok\n") { out }
    }
    @Test
    fun op_10_every () {
        val out = test("""
            data X: ()
            spawn {
                every :X {
                    dump("x")
                }
            }
            emit(X())
            emit(X())
        """)
        assert(out == "x\nx\n") { out }
    }
    @Test
    fun op_11_every () {
        val out = test("""
            spawn {
                every %10 {
                    dump("ms")
                }
            }
            emit(Event.Clock [10])
            emit(Event.Clock [5])
            emit(Event.Clock [5])
        """)
        assert(out == "ms\nms\n") { out }
    }
    @Test
    fun op_11x_await_loop () {
        val out = test("""
            data X: ()
            spawn {
                loop {
                    await(:X)
                    dump("x")
                }
            }
            emit(X())
            emit(X())
        """)
        assert(out == "x\nx\n") { out }
    }
    @Test
    fun op_12_every () {
        val out = test("""
            data X: ()
            spawn {
                par {
                    every :X {
                        dump("x")
                    }
                } with {
                    every %10 {
                        dump("ms")
                    }
                }
            }
            emit(Event.Clock [10])
            emit(X())
            emit(Event.Clock [5])
            emit(X())
            emit(Event.Clock [5])
        """)
        assert(out == "ms\nx\nx\nms\n") { out }
    }
    @Test
    fun op_13a_spawn_defer () {
        val out = test("""
            spawn {
                spawn {
                }
            }
            dump("ok")
        """)
        assert(out == "ok\n") { out }
    }
    @Test
    fun op_13b_spawn_defer () {
        val out = test("""
            data X: []
            spawn {
                spawn {
                    defer {
                        dump("2")
                    }
                    await(false)
                }
                await(true)
                dump("1")
            }
            emit(X [])
        """)
        assert(out == "1\n2\n") { out }
    }
    @Test
    fun op_14_spawn_spawn () {
        val out = test("""
            data X: Int
            spawn {
                spawn {
                    var e = await(:X, it==X(10))
                    dump(e)
                }
                await(false)
            }
            emit(X(10))
        """)
        assert(out == "X(10)\n") { out }
    }

    // AWAIT / TIME / CLOCK

    @Test
    fun oq_01_clock () {
        val out = test("""
            spawn {
                dump("antes")
                await(%10)
                dump("depois")
            }
            emit(Event.Clock [10])
        """)
        assert(out == "antes\ndepois\n") { out }
    }
    @Test
    fun oq_02_clock () {
        val out = test("""
            spawn {
                dump("antes")
                await(%10)
                dump("depois")
            }
            emit(Event.Clock [5])
            emit(Event.Clock [5])
        """)
        assert(out == "antes\ndepois\n") { out }
    }

    // TEST

    @Test
    fun xx_01_test () {
        G.test = false
        val out = test("""
            dump("1")
            test {
                dump("2")
            }
            dump("3")
        """)
        assert(out == "1\n3\n") { out }
    }
    @Test
    fun xx_02_test () {
        G.test = true
        val out = test("""
            dump("1")
            test {
                dump("2")
            }
            dump("3")
        """)
        assert(out == "1\n2\n3\n") { out }
    }

    // PRELUDE

    @Test
    fun xj_01_pre_assert () {
        val out = test("""
            dump(1)
            expect(true)
            dump(2)
            expect(false)
            dump(3)
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
            dump(a.x)
            var b = X.B [10]
            dump(b.x)
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
        dump(f(\1))
        """)
        assert(out == "1\n") { out }
    }
}
