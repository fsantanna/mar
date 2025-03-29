package test

import mar.*
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters
import org.junit.Test

val pos = Pos("a",1,1, 0)
val fix = Tk.Fix("", pos)
val any = Type.Any(fix)
val int = Type.Prim(Tk.Type("Int", pos))
val float = Type.Prim(Tk.Type("Float", pos))

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class XType {
    // SUP / SUB

    @Test
    fun aa_01_sup () {
        assert(any.is_sup_of(int))
        assert(int.is_sup_of(any))
    }
    @Test
    fun aa_02_sub () {
        assert(any.is_sub_of(int))
        assert(int.is_sub_of(any))
    }
    @Test
    fun aa_03_sup_sub () {
        val ptr = Type.Pointer(fix, int)
        assert(!int.is_sup_of(ptr))
        assert(!ptr.is_sup_of(int))
        assert(!int.is_sub_of(ptr))
        assert(!ptr.is_sub_of(int))
    }
    @Test
    fun aa_04_tpl () {
        val tpl = Type.Tpl(Tk.Var("a", pos))
        assert(!int.is_sup_of(tpl))
        assert( tpl.is_sup_of(int))
        assert( int.is_sub_of(tpl))
        assert(!tpl.is_sub_of(int))
    }
    @Test
    fun aa_05_task () {
        val t1 = Type.Proto.Task(fix, Tk.Var("x",pos), null, listOf(int), int)
        val t2 = Type.Proto.Task(fix, Tk.Var("y",pos), null, listOf(int), int)
        assert(!t1.is_sub_of(t2))
    }

    // SUP_VS / SUB_VS

    @Test
    fun bb_01_sup () {
        assert(any.sup_vs(int)!!.to_str() == "Int")
        assert(int.sup_vs(any)!!.to_str() == "Int")
    }
    @Test
    fun bb_02_sub () {
        assert(any.sub_vs(int)!!.to_str() == "Int")
        assert(int.sub_vs(any)!!.to_str() == "Int")
    }
    @Test
    fun bb_03_sub () {
        val tup = Type.Tuple(fix, emptyList())
        assert(any.sub_vs(tup)!!.to_str() == "*")
        assert(tup.sub_vs(any)!!.to_str() == "[]")
    }
    @Test
    fun bb_04_sub () {
        val tup1 = Type.Tuple(fix, listOf(Pair(null,Type.Any(fix))))
        val tup2 = Type.Tuple(fix, emptyList())
        assert(tup1.sub_vs(tup2) == tup1)
        assert(tup2.sup_vs(tup1) == null)
    }

    // SAME / SUP_SUB

    @Test
    fun cc_01_same () {
        assert(!float.is_same_of(int))
        assert(!int.is_same_of(float))
    }
    @Test
    fun cc_02_same () {
        assert(test.int.is_same_of(int))
        assert(int.is_same_of(test.int))
    }
    @Test
    fun cc_03_supsub () {
        assert(int.is_sup_sub_of(float))
        assert(float.is_sup_sub_of(int))
    }

    // TOP / BOT

    @Test
    fun dd_01_top_bot () {
        val top = Type.Top(fix)
        val bot = Type.Bot(fix)
        assert(top.is_sup_of(bot))
        assert(bot.is_sub_of(top))
    }
    @Test
    fun dd_02_top_bot () {
        val top = Type.Top(fix)
        val bot = Type.Bot(fix)
        val typ = Type.Prim(Tk.Type("Int", pos))
        assert(top.is_sup_of(typ))
        assert(typ.is_sub_of(top))
        assert(bot.is_sub_of(typ))
        assert(typ.is_sup_of(bot))
    }
}