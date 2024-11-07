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

    // CALL / PRINT / IF / LOOP

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

    // PROTOS / TYPES / NORMAL / NESTED / CLOSURE

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

    // COROS

    @Test
    fun ff_01_coro () {
        val out = test("""
            do {
                coro co: () -> () -> () -> () {
                    `puts("OK");`
                }
                var exe: exec () -> () -> () -> () = create(co)
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
            var exe: exec () -> () -> () -> () = create(co)
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
            var exe: exec (Int) -> () -> () -> () = create(co)
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
            var exe: exec (Int) -> Int -> () -> () = create(co)
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
                var exe: exec (Int) -> Int -> Int -> Int = create(co)
                var x1: <Int,Int> = start exe(5)
                var y1: <Int,Int> = resume exe(x1!1 + 10)
                print(y1!2)
            }
        """)
        assert(out == "40\n") { out }
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
            var b: B.T = B.T []
            print(b?T)
        """)
        assert(out == "true\n") { out }
    }
    @Test
    fun hh_08_data () {
        val out = test("""
            data A: <B: <C: Int>>
            var c = A.B.C(10)
            var v = c
            print(c)
            print(c!B!C)
        """)
        assert(out == "A <.1=<.1=10>>\n" +
                "10\n") { out }
    }
    @Test
    fun hh_09_data () {
        val out = test("""
            data K: <None:()>
            data X: Int + <Y:()>
            var xy: X.Y = X.Y(10,())
            var y = xy!Y    ;; ()
            print(y)
        """)
        assert(out == "()\n") { out }
    }
    @Test
    fun hh_10_data () {
        val out = test("""
            data B: <T:[], F:[]>
            var b: B.T = B.T []
            print(b?B)      ;; OK
            ;;print(b!B)      ;; NO: no B._o
        """)
        assert(out == "anon : (lin 4, col 20) : predicate error : types mismatch\n") { out }
        //assert(out == "true\n") { out }
    }

    // DATA / HIER

    @Test
    fun hi_01_data () {
        val out = test("""
            data A: Int + <B: [b:Int]>
            var x0: A = A.A(100)  ;; ignore subtype B
            print(x0)
            print(x0!A)
        """)
        assert(out == "A 100 + <.0=>\n" +
                "100\n") { out }
    }
    @Test
    fun hi_01x_data () {
        val out = test("""
            data A: Int + <B: Int + <C: Int>>
            var x0: A = A.B.B(100,100)  ;; ignore subsubtype C
            print(x0)
            print(x0!B)
        """)
        assert(out == "A 100 + <.1=100 + <.0=>>\n" +
                "100 + <.0=>\n") { out }
    }
    @Test
    fun hi_02_data () {
        val out = test("""
            data A: [a:Int] + <B: [b:Int]>
            var x0 = A.A([100])  ;; ignore subtype
            print(x0)
            print(x0!A.a)
        """)
        assert(out == "A [100] + <.0=>\n" +
                "100\n") { out }
    }
    @Test
    fun hi_03_data () {
        val out = test("""
            data A: [a:Int] + <B: [b:Int]>
            var x0: A   = A.A([100])
            var xa: A   = A.B([10],[20])
            var xb: A.B = A.B([30],[40])
            print(x0)
            print(xa)
            print(xb)
            
            print(x0!A.a)
            print(xa!A.a)
            print(xa!B.b)
            print(xb!A.a)
            print(xb!B.b)
        """)
        assert(out == "A [100] + <.0=>\n" +
                "A [10] + <.1=[20]>\n" +
                "A [30] + <.1=[40]>\n" +
                "100\n" +
                "10\n" +
                "20\n" +
                "30\n" +
                "40\n") { out }
    }
    @Test
    fun TODO_hi_04_data () {
        val out = test("""
            data A: [a:Int] + <B: [b:Int] + <C: [c:Int]>>
            var c = A.B.C ([1],[2],[3])
            var v = c
            print(c)
            print(c!B!C)
        """)
        assert(out == "A [1] + <.1=[2] + <.1=[3]>>\n" +
                "[3]\n") { out }
    }
    @Test
    fun hi_05_data () {
        val out = test("""
            data A: <B: <C: Int>>
            var c = A.B.C(10)
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
    fun TODO_hi_11_data () {
        val out = test("""
            data A: [x:Int] + <B: [y:Int] + <C: Int>>
            var c: A.B.C = A.B.C([10],[20],30)
            var b  = c!B
            var bb = c!B!B
            var y  = c!B!B.y
            print(b)
            print(bb)
            print(y)
        """)
        assert(out == "TODO\n") { out }
    }

    // DATA / HIER / EXTD / EXTENDS

    @Test
    fun hj_01_hier_extd () {
        val out = test("""
            data X: Int + <Y:()>
            data X.Z: Int
            var xz = X.Z(10,20)
            var x = xz!X    ;; 10
            var y = xz!Z    ;; 20
        """)
        assert(out == "10\n20\n") { out }
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

    // CATCH / THROW

    @Test
    fun kk_01_catch () {
        val out = test("""
            catch Exception {
            }
            print(10)
        """)
        //assert(out == "anon : (lin 2, col 23) : declaration error : data :T is not declared\n") { out }
        assert(out == "10\n") { out }
    }
    @Test
    fun kk_02_throw () {
        val out = test("""
            throw(Exception.X())
        """)
        //assert(out == "anon : (lin 2, col 23) : declaration error : data :T is not declared\n") { out }
        assert(out == "10\n") { out }
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
}
