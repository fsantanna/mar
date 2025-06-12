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
                print(e)
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
                print(e)
                var f = await(:Y)
                print(f)
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
                print(e)
                var f = await(:Y)
                print(f)
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
                print(e)
                var f = await(:Y)
                print(f)
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
                print(e)
                var f = await(:Y)
                print(f)
            }
            var e = spawn tsk()
            ;;print(e)
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
                print(e)
            }
            var e = spawn tsk()
            ;;print(e)
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
                print(e)
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
                print(">>> B")
                await(:X)
                print("<<< B")
                ;;emit(Event.Task [`mar_exe`])
            }
            spawn {
                var exe = create(t2)
                start exe()
                print(">>> A")
                var e = await(exe)
                print("<<< A")
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
                    print("first")
                }
                await(true)
            }
            spawn {
                defer {
                    print("last")
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
            print(1)
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
                print("nok")
            }
            emit(Y())
            print("ok")
        """)
        assert(out == "ok\n") { out }
    }
    @Test
    fun oo_12_task_terminated () {
        val out = test("""
            data X: ()
            task t2: () -> () {
                print("t2")
            }
            spawn {
                var exe = create(t2)
                start exe()
                await(exe)
                print("main")
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
                    print("x")
                } with {
                    every :X {
                        print("no")
                    }
                }
                print("or")
            }
            emit(X())
            emit(X())
            emit(X())
            emit(X())
            print("ok")
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
                    print("x")
                } with {
                    every :X {
                        print("no")
                    }
                }
                print("or")
            }
            emit(X())
            print("ok")
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
                print(e)
                var f = await(:Y)
                print(f)
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
                    print(e)
                } with {
                    var f = await(:Y)
                    print(f)
                }
                print("nooo")
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
            print("ok")
        """)
        assert(out == "ok\n") { out }
    }
    @Test
    fun oo_03_task_term () {
        val out = test("""
            data X: ()
            task t2: () -> () {
                print(">>> B")
                await(:X)
                print("<<< B")
                ;;emit(Event.Task [`mar_exe`])
            }
            spawn {
                var exe = create(t2)
                start exe()
                print(">>> A")
                var e = await(exe)
                print("<<< A")
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
                print(">>> B")
                await(:X)
                print("<<< B")
                ;;emit(Event.Task [`mar_exe`])
            }
            spawn {
                print(">>> A")
                await t2()
                print("<<< A")
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
                print("ok")
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
                    print("x")
                } with {
                    await(:Y)
                    print("y")
                }
                print("ok")
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
                    print("x")
                } with {
                    await(:Y)
                    print("y")
                }
                print("ok")
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
                    print("x")
                } with {
                    await(:Y)
                    print("y")
                }
                print("ok")
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
                    print("x")
                } with {
                    print("y")
                }
                print("ok")
            }
        """)
        assert(out == "y\nok\n") { out }
    }
    @Test
    fun op_11_every () {
        val out = test("""
            spawn {
                every %10 {
                    print("ms")
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
                    print("x")
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
                        print("x")
                    }
                } with {
                    every %10 {
                        print("ms")
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
            print("ok")
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
                        print("2")
                    }
                    await(false)
                }
                await(true)
                print("1")
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
                    print(e)
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
                print("antes")
                await(%10)
                print("depois")
            }
            emit(Event.Clock [10])
        """)
        assert(out == "antes\ndepois\n") { out }
    }
    @Test
    fun oq_02_clock () {
        val out = test("""
            spawn {
                print("antes")
                await(%10)
                print("depois")
            }
            emit(Event.Clock [5])
            emit(Event.Clock [5])
        """)
        assert(out == "antes\ndepois\n") { out }
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
