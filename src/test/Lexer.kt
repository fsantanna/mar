package test

import mar.*
import org.junit.Test

class Lexer {
    
    // SYMBOL
    
    @Test
    fun aa_01_syms() {
        val l = ("{ } ( ; ( = ) ) - , ][ / * + .").lexer()
        val tks = l.iterator()
        assert(tks.next().str == "{")
        assert(tks.next().str == "}")
        assert(tks.next().str == "(")
        assert(tks.next().str == "(")
        assert(tks.next().str == "=")
        assert(tks.next().str == ")")
        assert(tks.next().str == ")")
        assert(tks.next().str == "-")
        assert(tks.next().str == ",")
        assert(tks.next().str == "]")
        assert(tks.next().str == "[")
        assert(tks.next().str == "/")
        assert(tks.next().str == "*")
        assert(tks.next().str == "+")
        assert(tks.next().str == ".")
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }

    // ID / KEYWORD / VAR / TYPE

    @Test
    fun bb_01_ids() {
        val l = ("if xxx Type").lexer()
        val tks = l.iterator()
        assert(tks.next().let { it is Tk.Fix  && it.str == "if" })
        assert(tks.next().let { it is Tk.Var  && it.str == "xxx" })
        assert(tks.next().let { it is Tk.Type && it.str == "Type" })
        assert(tks.next() is Tk.Eof)
        assert(!tks.hasNext())
    }

    // COMMENT
    
    @Test
    fun cc_01_comments() {
        val l = ("""
            x - y - z ;;
            var ;;x
            ;;
            val ;; x
            ;; -
            -
        """.trimIndent()).lexer()
        val tks = l.iterator()
        assert(tks.next().let { it is Tk.Var && it.pos.lin==1 && it.pos.col==1 && it.str == "x" })
        assert(tks.next().let { it is Tk.Op  && it.pos.lin==1 && it.pos.col==3 && it.str == "-" })
        assert(tks.next().let { it is Tk.Var && it.pos.lin==1 && it.pos.col==5 && it.str == "y" })
        assert(tks.next().let { it is Tk.Op  && it.pos.lin==1 && it.pos.col==7 && it.str == "-" })
        assert(tks.next().let { it is Tk.Var && it.pos.lin==1 && it.pos.col==9 && it.str == "z" })
        assert(tks.next().let { it is Tk.Fix && it.pos.lin==2 && it.pos.col==1 && it.str == "var" })
        assert(tks.next().let { it is Tk.Fix && it.pos.lin==4 && it.pos.col==1 && it.str == "val" })
        assert(tks.next().let { it is Tk.Op  && it.pos.lin==6 && it.pos.col==1 && it.str == "-" })
        assert(tks.next().let { it is Tk.Eof && it.pos.lin==6 && it.pos.col==2 })
    }
    @Test
    fun cc_02_comments() {
        val l = ("""
            x ;;;
            var ;;x
            val ;;; y
            z
        """.trimIndent()).lexer()
        val tks = l.iterator()
        //println(tks.next())
        assert(tks.next().let { it is Tk.Var && it.pos.lin==1 && it.pos.col==1 && it.str == "x" })
        assert(tks.next().let { it is Tk.Var && it.pos.lin==3 && it.pos.col==9 && it.str == "y" })
        assert(tks.next().let { it is Tk.Var && it.pos.lin==4 && it.pos.col==1 && it.str == "z" })
    }
    @Test
    fun cc_03_comments() {
        val l = ("""
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
        val tks = l.iterator()
        assert(tks.next().let { it is Tk.Var && it.str == "x" })
        assert(tks.next().let { it is Tk.Var && it.str == "y" })
    }
    @Test
    fun cc_04_comments_err() {
        val l = ("""
            x
            ;;;
            ;;;;
            ;;;
            y
        """.trimIndent()).lexer()
        val tks = l.iterator()
        assert(tks.next().let { it is Tk.Var && it.str == "x" })
        assert(tks.next().let { it is Tk.Eof })
    }
    @Test
    fun cc_05_comments_err() {
        val l = ("""
            x
            ;;;;
            ;;;
            ;;;;
            y
        """.trimIndent()).lexer()
        val tks = l.iterator()
        assert(tks.next().let { it is Tk.Var && it.str == "x" })
        assert(tks.next().let { it is Tk.Var && it.str == "y" })
        assert(tks.next().let { it is Tk.Eof })
    }
}
