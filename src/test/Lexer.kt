package test

import mar.*
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import java.io.File

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Lexer {
    @Test
    fun aa_02_syms() {
        val tks = ("= == - ->").lexer()
        assert(tks.next().let { it is Tk.Op && it.str == "=" })
        assert(tks.next().let { it is Tk.Op && it.str == "==" })
        assert(tks.next().let { it is Tk.Op && it.str == "-" })
        assert(tks.next().let { it is Tk.Op && it.str == "->" })
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }
    @Test
    fun bb_02_type() {
        val tks = ("Int").lexer()
        assert(tks.next().let { it is Tk.Type && it.str == "Int" })
        assert(tks.next() is Tk.Eof)
    }
    @Test
    fun gg_01_string() {
        val tks = """
            "xxx"
            "\"aaa\""
            "
        """.trimIndent().lexer()
        assert(tks.next().let { it is Tk.Str && it.pos.lin == 1 && it.pos.col == 1 && it.str == "\"xxx\"" })
        assert(tks.next().let { it is Tk.Str && it.pos.lin == 2 && it.pos.col == 1 && it.str == "\"\\\"aaa\\\"\"" })
        assert(trap { tks.next() } == "anon : (lin 3, col 1) : string error : unterminated \"")
    }
}
