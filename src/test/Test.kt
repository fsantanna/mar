package test

import mar.*
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters
import org.junit.Test

val pos = Pos("a",1,1, 0)

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Test {
    @Test
    fun aa_01_sup () {
        val tp1 = Type.Any(Tk.Fix("", pos))
        val tp2 = Type.Prim(Tk.Type("Int", pos))
        assert(tp1.is_sup_of(tp2))
    }
}