package test

import mar.*
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

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
}
