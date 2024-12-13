package mar.test

import mar.*
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import java.io.File

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
    @Test
    fun aa_03_cmds() {
        assert(File("/x/x.txt").parentFile.toString() == "/x")
        assert(File("x/x.txt").parentFile.toString() == "x")
    }
    @Test
    fun aa_04_main() {
        File("/tmp/x.tst").printWriter().use { out ->
            out.println("xxx")
        }
        val f1 = FileX("test.mar", null)
        assert(f1 == null)
        val f2 = FileX("/tmp/x.tst", "xxx")
        assert(f2.toString() == "/tmp/x.tst")
        val f3 = FileX("x.tst", "/tmp")
        assert(f3.toString() == "/tmp/x.tst")
        PATH = "/tmp"
        val f4 = FileX("@/x.tst", null)
        assert(f4.toString() == "/tmp/x.tst")
    }
}
