package test

import mar.*
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import java.io.File

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Lexer {
    @Test
    fun ff_01_native() {
        val tks = """
            ` abc `
            `{ijk}`
            ` {i${D}jk} `
            `  {ijk} 
        """.trimIndent().lexer()
        assert(tks.next().let { it is Tk.Nat && it.pos.lin == 1 && it.pos.col == 1 && it.str == " abc " })
        assert(tks.next().let { it is Tk.Nat && it.pos.lin == 2 && it.pos.col == 1 && it.str == "{ijk}" })
        assert(tks.next().let { it is Tk.Nat && it.pos.lin == 3 && it.pos.col == 1 && it.str == " {i\$jk} " })
        assert(trap { tks.next() } == "anon : (lin 4, col 1) : token error : unterminated \"`\"")
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
