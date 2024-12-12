package mar.test

import mar.*
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Main  {
    @Test
    fun aa_01_cmds() {
        val (xs,ys) = arrayOf("--lib=10", "1", "--a", "--x=abc", "aaa", "--b").cmds_opts()
        assert(xs.size==2 && xs.contains("1") && xs.contains("aaa"))
        assert(ys.size==4 && ys.contains("--a") && ys.contains("--b"))
        //assert(ys.size==4 && ys["--lib"]=="10" && ys["--x"]=="abc")
    }
    @Test
    fun aa_02_cmds() {
        val (xs,ys) = arrayOf("--lib=10", "--lib", "--lib=", "--lib=abc").cmds_opts()
        assert(
            xs.size==0 && ys.size==1 && ys.containsKey("--lib") &&
            ys["--lib"]!!.let { it.size==2 && it.contains("10") && it.contains("abc") }
        )
    }
}
