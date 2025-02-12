package test

import mar.*
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters
import org.junit.Test

val pos = Pos("a",1,1, 0)

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class XType {
    // SUP / SUB

    @Test
    fun aa_01_sup () {
        val tp1 = Type.Any(Tk.Fix("", pos))
        val tp2 = Type.Prim(Tk.Type("Int", pos))
        assert(tp1.is_sup_of(tp2))
        assert(tp2.is_sup_of(tp1))
    }
    @Test
    fun aa_02_sub () {
        val tp1 = Type.Any(Tk.Fix("", pos))
        val tp2 = Type.Prim(Tk.Type("Int", pos))
        assert(tp1.is_sub_of(tp2))
        assert(tp2.is_sub_of(tp1))
    }
    @Test
    fun aa_03_sup_sub () {
        val tp1 = Type.Prim(Tk.Type("Int", pos))
        val tp2 = Type.Pointer(Tk.Fix("", pos), tp1)
        assert(!tp1.is_sup_of(tp2))
        assert(!tp2.is_sup_of(tp1))
        assert(!tp1.is_sub_of(tp2))
        assert(!tp2.is_sub_of(tp1))
    }

    // SUP_VS / SUB_VS

    @Test
    fun bb_01_sup () {
        val tp1 = Type.Any(Tk.Fix("", pos))
        val tp2 = Type.Prim(Tk.Type("Int", pos))
        assert(tp1.sup_vs(tp2)!!.to_str() == "Int")
        assert(tp2.sup_vs(tp1)!!.to_str() == "Int")
    }
    @Test
    fun bb_02_sub () {
        val tp1 = Type.Any(Tk.Fix("", pos))
        val tp2 = Type.Prim(Tk.Type("Int", pos))
        assert(tp1.sub_vs(tp2)!!.to_str() == "Int")
        assert(tp2.sub_vs(tp1)!!.to_str() == "Int")
    }

    // SAME / SUP_SUB

    @Test
    fun cc_01_same () {
        val tp1 = Type.Prim(Tk.Type("Float", pos))
        val tp2 = Type.Prim(Tk.Type("Int", pos))
        assert(!tp1.is_same_of(tp2))
        assert(!tp2.is_same_of(tp1))
    }
    @Test
    fun cc_02_same () {
        val tp1 = Type.Prim(Tk.Type("Int", pos))
        val tp2 = Type.Prim(Tk.Type("Int", pos))
        assert(tp1.is_same_of(tp2))
        assert(tp2.is_same_of(tp1))
    }
    @Test
    fun cc_03_supsub () {
        val tp1 = Type.Prim(Tk.Type("Int", pos))
        val tp2 = Type.Prim(Tk.Type("Float", pos))
        assert(tp1.is_sup_sub_of(tp2))
        assert(tp2.is_sup_sub_of(tp1))
    }
}