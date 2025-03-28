package test

import mar.*
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import java.io.File

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Lexer {
    
    // SYMBOL
    @Test
    fun aa_01_syms() {
        val tks = ("{ } ( ; < {{ > ( = ) ) # - , ][ #[ ## / * + .").lexer()
        assert(tks.next().str == "{")
        assert(tks.next().str == "}")
        assert(tks.next().str == "(")
        assert(tks.next().str == "<")
        assert(tks.next().str == "{{")
        assert(tks.next().str == ">")
        assert(tks.next().str == "(")
        assert(tks.next().str == "=")
        assert(tks.next().str == ")")
        assert(tks.next().str == ")")
        assert(tks.next().str == "#")
        assert(tks.next().str == "-")
        assert(tks.next().str == ",")
        assert(tks.next().str == "]")
        assert(tks.next().str == "[")
        assert(tks.next().str == "#[")
        assert(tks.next().str == "##")
        assert(tks.next().str == "/")
        assert(tks.next().str == "*")
        assert(tks.next().str == "+")
        assert(tks.next().str == ".")
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }
    @Test
    fun aa_02_syms() {
        val tks = ("= == - ->").lexer()
        assert(tks.next().let { it is Tk.Op && it.str=="=" })
        assert(tks.next().let { it is Tk.Op && it.str=="==" })
        assert(tks.next().let { it is Tk.Op && it.str=="-" })
        assert(tks.next().let { it is Tk.Op && it.str=="->" })
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }

    // ID / KEYWORD / VAR / TYPE

    @Test
    fun bb_01_ids() {
        val tks = ("if xxx Type coro match task break until while return escape await emit loop throw defer exec create spawn data start catch resume yield where").lexer()
        assert(tks.next().let { it is Tk.Fix  && it.str == "if" })
        assert(tks.next().let { it is Tk.Var  && it.str == "xxx" })
        assert(tks.next().let { it is Tk.Type && it.str == "Type" })
        assert(tks.next().let { it is Tk.Fix  && it.str == "coro" })
        assert(tks.next().let { it is Tk.Fix  && it.str == "match" })
        assert(tks.next().let { it is Tk.Fix  && it.str == "task" })
        assert(tks.next().let { it is Tk.Fix  && it.str == "break" })
        assert(tks.next().let { it is Tk.Fix  && it.str == "until" })
        assert(tks.next().let { it is Tk.Fix  && it.str == "while" })
        assert(tks.next().let { it is Tk.Fix  && it.str == "return" })
        assert(tks.next().let { it is Tk.Fix  && it.str == "escape" })
        assert(tks.next().let { it is Tk.Fix  && it.str == "await" })
        assert(tks.next().let { it is Tk.Fix  && it.str == "emit" })
        assert(tks.next().let { it is Tk.Fix  && it.str == "loop" })
        assert(tks.next().let { it is Tk.Fix  && it.str == "throw" })
        assert(tks.next().let { it is Tk.Fix  && it.str == "defer" })
        assert(tks.next().let { it is Tk.Fix  && it.str == "exec" })
        assert(tks.next().let { it is Tk.Fix  && it.str == "create" })
        assert(tks.next().let { it is Tk.Fix  && it.str == "spawn" })
        assert(tks.next().let { it is Tk.Fix  && it.str == "data" })
        assert(tks.next().let { it is Tk.Fix  && it.str == "start" })
        assert(tks.next().let { it is Tk.Fix  && it.str == "catch" })
        assert(tks.next().let { it is Tk.Fix  && it.str == "resume" })
        assert(tks.next().let { it is Tk.Fix  && it.str == "yield" })
        assert(tks.next().let { it is Tk.Fix  && it.str == "where" })
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }
    @Test
    fun bb_02_type() {
        val tks = ("Int").lexer()
        assert(tks.next().let { it is Tk.Type && it.str == "Int" })
        assert(tks.next() is Tk.Eof)
    }

    // COMMENT
    
    @Test
    fun cc_01_comments() {
        val tks = ("""
            x - y - z ;;
            var ;;x
            ;;
            val ;; x
            ;; -
            -
        """.trimIndent()).lexer()
        assert(tks.next().let { it is Tk.Var && it.pos.lin==1 && it.pos.col==1 && it.str == "x" })
        assert(tks.next().let { it is Tk.Op  && it.pos.lin==1 && it.pos.col==3 && it.str == "-" })
        assert(tks.next().let { it is Tk.Var && it.pos.lin==1 && it.pos.col==5 && it.str == "y" })
        assert(tks.next().let { it is Tk.Op  && it.pos.lin==1 && it.pos.col==7 && it.str == "-" })
        assert(tks.next().let { it is Tk.Var && it.pos.lin==1 && it.pos.col==9 && it.str == "z" })
        assert(tks.next().let { it is Tk.Fix && it.pos.lin==2 && it.pos.col==1 && it.str == "var" })
        assert(tks.next().let { it is Tk.Var && it.pos.lin==4 && it.pos.col==1 && it.str == "val" })
        assert(tks.next().let { it is Tk.Op  && it.pos.lin==6 && it.pos.col==1 && it.str == "-" })
        assert(tks.next().let { it is Tk.Eof && it.pos.lin==6 && it.pos.col==2 })
    }
    @Test
    fun cc_02_comments() {
        val tks = ("""
            x ;;;
            var ;;x
            val ;;; y
            z
        """.trimIndent()).lexer()
        //println(tks.next())
        assert(tks.next().let { it is Tk.Var && it.pos.lin==1 && it.pos.col==1 && it.str == "x" })
        assert(tks.next().let { it is Tk.Var && it.pos.lin==3 && it.pos.col==9 && it.str == "y" })
        assert(tks.next().let { it is Tk.Var && it.pos.lin==4 && it.pos.col==1 && it.str == "z" })
    }
    @Test
    fun cc_03_comments() {
        val tks = ("""
            x
            ;;;
            ;;;;
            ;;;
            ;;
            ;;;;
            ;;;;
            ;;;
            ;;;;
            ;;;
            y
        """.trimIndent()).lexer()
        assert(tks.next().let { it is Tk.Var && it.str == "x" })
        assert(tks.next().let { it is Tk.Var && it.str == "y" })
    }
    @Test
    fun cc_04_comments_err() {
        val tks = ("""
            x
            ;;;
            ;;;;
            ;;;
            y
        """.trimIndent()).lexer()
        assert(tks.next().let { it is Tk.Var && it.str == "x" })
        assert(tks.next().let { it is Tk.Eof })
    }
    @Test
    fun cc_05_comments_err() {
        val tks = ("""
            x
            ;;;;
            ;;;
            ;;;;
            y
        """.trimIndent()).lexer()
        assert(tks.next().let { it is Tk.Var && it.str == "x" })
        assert(tks.next().let { it is Tk.Var && it.str == "y" })
        assert(tks.next().let { it is Tk.Eof })
    }

    // OPERATOR

    @Test
    fun dd_01_ops() {
        val tks = ("&& + \\ ! ++ >=").lexer()
        assert(tks.next().let { it is Tk.Op  && it.str == "&&" })
        assert(tks.next().let { it is Tk.Op  && it.str == "+" })
        assert(tks.next().let { it is Tk.Op  && it.str == "\\" })
        assert(tks.next().let { it is Tk.Op  && it.str == "!" })
        assert(tks.next().let { it is Tk.Op  && it.str == "++" })
        assert(tks.next().let { it is Tk.Op  && it.str == ">=" })
        assert(tks.next().let { it is Tk.Eof && it.pos.lin==1 && it.pos.col==15 })
    }

    // CHAR

    @Test
    fun ee_01_chr() {
        val tks = ("'x' '\\n' '\\'' '\\\\'").lexer()
        assert(tks.next().let { it is Tk.Chr && it.str == "'x'" })
        assert(tks.next().let { it is Tk.Chr && it.str == "'\\n'" })
        assert(tks.next().let { it is Tk.Chr && it.str == "'\\\''" })
        assert(tks.next().let { it is Tk.Chr && it.str == "'\\\\'" })
        assert(tks.next().let { it is Tk.Eof && it.pos.lin==1 && it.pos.col==19 })
    }
    @Test
    fun ee_02_chr_err() {
        val tks = "'x".lexer()
        assert(trap { tks.next() } == "anon : (lin 1, col 3) : char error : expected '")
    }
    @Test
    fun ee_03_chr_err() {
        val tks = "'\\'".lexer()
        assert(trap { tks.next() } == "anon : (lin 1, col 4) : char error : expected '")
    }
    @Test
    fun ee_04_chr_err() {
        val tks = "'\\n".lexer()
        assert(trap { tks.next() } == "anon : (lin 1, col 4) : char error : expected '")
    }
    @Test
    fun ee_05_chr_err() {
        val tks = "'abc'".lexer()
        assert(trap { tks.next() } == "anon : (lin 1, col 4) : char error : expected '")
    }
    @Test
    fun TODO_ee_06_chr() {
        val tks = "\"\\\\\"".lexer()
        assert(tks.next().let { it.str=="#["})
        print(tks.next().let { it.str=="\\" })    // TODO: eh pra ser false ou true?
        assert(tks.next().let { it.str=="]"})
    }
    @Test
    fun ee_07_chr() {
        val tks = "\"\\\"".lexer()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : string error : unterminated \"")
    }
    @Test
    fun ee_08_chr() {
        val tks = "'\\n'".lexer()
        assert(tks.next().str == "'\\n'")
    }

    // NAT

    @Test
    fun ff_01_native() {
        val tks = """
            ` abc `
            `{ijk}`
            ` {i${D}jk} `
            `  {ijk} 
        """.trimIndent().lexer()
        assert(tks.next().let { it is Tk.Nat && it.pos.lin==1 && it.pos.col==1 && it.str==" abc " })
        assert(tks.next().let { it is Tk.Nat && it.pos.lin==2 && it.pos.col==1 && it.str=="{ijk}" })
        assert(tks.next().let { it is Tk.Nat && it.pos.lin==3 && it.pos.col==1 && it.str==" {i\$jk} " })
        assert(trap { tks.next() } == "anon : (lin 4, col 1) : token error : unterminated \"`\"")
    }

    // STRING

    @Test
    fun gg_01_string() {
        val tks = """
            "xxx"
            "\"aaa\""
            "
        """.trimIndent().lexer()
        assert(tks.next().let { it is Tk.Str && it.pos.lin==1 && it.pos.col==1 && it.str=="\"xxx\"" })
        assert(tks.next().let { it is Tk.Str && it.pos.lin==2 && it.pos.col==1 && it.str=="\"\\\"aaa\\\"\"" })
        assert(trap { tks.next() } == "anon : (lin 3, col 1) : string error : unterminated \"")
    }

    // PREPROCESSOR / INCLUDE

    @Test
    fun hh_00_pre_err() {
        val tks = ("^xxx").lexer()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : preprocessor error : unexpected \"xxx\"")
    }
    @Test
    fun hh_01_inc() {
        File("/tmp/x.tst").printWriter().use { out ->
            out.println("1")
            out.println("2")
            out.println("")
        }
        val tks = """
            before
            ^include(/tmp/x.tst)
            after
        """.trimIndent().lexer()
        assert(tks.next().let { it is Tk.Var && it.pos.file=="anon"       && it.pos.lin==1 && it.pos.col==1 && it.str == "before" })
        assert(tks.next().let { it is Tk.Num && it.pos.file=="/tmp/x.tst" && it.pos.lin==1 && it.pos.col==1 && it.str == "1" })
        assert(tks.next().let { it is Tk.Num && it.pos.file=="/tmp/x.tst" && it.pos.lin==2 && it.pos.col==1 && it.str == "2" })
        assert(tks.next().let { it is Tk.Var && it.pos.file=="anon"       && it.pos.lin==3 && it.pos.col==1 && it.str == "after" })
        assert(tks.next().let { it is Tk.Eof && it.pos.lin==3 && it.pos.col==6 })
    }
    @Test
    fun hh_02_inc_err() {
        val tks = ("^include(").lexer()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : include error : exoected \")\"")
    }
    @Test
    fun hh_03_inc_err() {
        val tks = ("^include()").lexer()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : include error : file not found : ")
    }
    @Test
    fun hh_04_inc_err() {
        val tks = ("^include(\\x\\\\n.mar)").lexer()
        assert(trap { tks.next() } == "anon : (lin 1, col 1) : include error : file not found : \\x\\\\n.mar")
    }
    @Test
    fun hh_05_inc_dub() {
        G.incs.clear()
        File("/tmp/x.tst").printWriter().use { out ->
            out.println("1")
            out.println("2")
            out.println("")
        }
        val tks = """
            before
            ^include(/tmp/x.tst)
            ^include(/tmp/x.tst)
            after
        """.trimIndent().lexer()
        assert(tks.next().let { it is Tk.Var && it.pos.file=="anon"       && it.pos.lin==1 && it.pos.col==1 && it.str == "before" })
        assert(tks.next().let { it is Tk.Num && it.pos.file=="/tmp/x.tst" && it.pos.lin==1 && it.pos.col==1 && it.str == "1" })
        assert(tks.next().let { it is Tk.Num && it.pos.file=="/tmp/x.tst" && it.pos.lin==2 && it.pos.col==1 && it.str == "2" })
        assert(tks.next().let { it is Tk.Var && it.pos.file=="anon"       && it.pos.lin==4 && it.pos.col==1 && it.str == "after" })
        assert(tks.next().let { it is Tk.Eof && it.pos.lin==4 && it.pos.col==6 })
    }
}
