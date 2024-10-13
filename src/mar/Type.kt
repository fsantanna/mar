package mar

fun List<Type>.to_void (): List<Type> {
    return when {
        (this.size == 0) -> listOf(Type.Unit(Tk.Fix("(", G.tk0!!.pos.copy())))
        else -> this
    }
}

fun Type.is_sup_of (other: Type): Boolean {
    return when {
        //(this is Type.Top) -> true
        (this is Type.Any || other is Type.Any) -> true
        (this is Type.Unit       && other is Type.Unit)       -> true
        (this is Type.Basic      && other is Type.Basic)      -> (this.tk.str == other.tk.str)
        (this is Type.Pointer    && other is Type.Pointer)    -> this.ptr.is_sup_of(other.ptr)
        (this is Type.Tuple      && other is Type.Tuple)      -> (this.ts.size==other.ts.size) && this.ts.zip(other.ts).all { (thi,oth) -> thi.is_sup_of(oth) }
        (this is Type.Union      && other is Type.Union)      -> (this.ts.size==other.ts.size) && this.ts.zip(other.ts).all { (thi,oth) -> thi.is_sup_of(oth) }
        (this is Type.Proto.Func && other is Type.Proto.Func) -> (this.inp_.size==other.inp_.size) && this.inp_.zip(other.inp_).all { (thi,oth) -> thi.is_sup_of(oth) } && other.out.is_sup_of(this.out)
        (this is Type.Proto.Coro && other is Type.Proto.Coro) -> this.inp_.is_sup_of(other) && other.out.is_sup_of(this.out)
        (this is Type.XCoro      && other is Type.XCoro)      -> this.inp.is_sup_of(other) && other.out.is_sup_of(this.out)
        else -> false
    }
}

fun Type.is_same_of (other: Type): Boolean {
    return this.is_sup_of(other) && other.is_sup_of(this)
}

fun Expr.Bin.args (tp1: Type, tp2: Type): Boolean {
    return when (this.tk_.str) {
        "==", "!=" -> tp1.is_same_of(tp2)
        ">", "<", ">=", "<=",
        "+", "-", "*", "/", "%" -> {
            tp1.is_sup_of(Type.Basic(Tk.Type( "Int", this.tk.pos.copy()))) &&
            tp2.is_sup_of(Type.Basic(Tk.Type( "Int", this.tk.pos.copy())))
        }
        "||", "&&" -> {
            tp1.is_sup_of(Type.Basic(Tk.Type( "Bool", this.tk.pos.copy()))) &&
            tp2.is_sup_of(Type.Basic(Tk.Type( "Bool", this.tk.pos.copy())))
        }
        else -> error("impossible case")
    }
}

fun Tk.Var.type (fr: Any): Type? {
    return fr.up_first {
        if (it !is Stmt.Block) false else {
            it.to_dcls().find { (_,vt) -> vt.first.str == this.str }?.second?.second
        }
    } as Type?
}

fun Expr.type (): Type {
    return when (this) {
        is Expr.Uno -> when (this.tk_.str) {
            "-" -> Type.Basic(Tk.Type( "Int", this.tk.pos.copy()))
            "ref" -> Type.Pointer(Tk.Op("\\", this.tk.pos.copy()), this.e.type())
            "deref" -> (this.e.type() as Type.Pointer).ptr
            else -> error("impossible case")
        }
        is Expr.Bin -> when (this.tk_.str) {
            "==", "!=",
            ">", "<", ">=", "<=",
            "||", "&&" -> Type.Basic(Tk.Type( "Bool", this.tk.pos.copy()))
            "+", "-", "*", "/", "%" -> Type.Basic(Tk.Type( "Int", this.tk.pos.copy()))
            else -> error("impossible case")
        }
        is Expr.Call -> this.f.type().let {
            if (it is Type.Any) it else (it as Type.Proto.Func).out
        }

        is Expr.Tuple -> Type.Tuple(this.tk_, vs.map { it.type() })
        is Expr.Union -> this.tp
        is Expr.Index -> (this.col.type() as Type.Tuple).ts[this.idx.toInt()-1]
        is Expr.Disc  -> (this.col.type() as Type.Union).ts[this.idx.toInt()-1]
        is Expr.Pred  -> Type.Basic(Tk.Type( "Bool", this.tk.pos.copy()))

        is Expr.Acc -> this.tk_.type(this)!!
        is Expr.Bool -> Type.Basic(Tk.Type( "Bool", this.tk.pos.copy()))
        is Expr.Char -> Type.Basic(Tk.Type( "Char", this.tk.pos.copy()))
        is Expr.Nat -> Type.Any(this.tk)
        is Expr.Null -> Type.Pointer(Tk.Op("\\", this.tk.pos.copy()), Type.Any(this.tk))
        is Expr.Unit -> Type.Unit(this.tk_)
        is Expr.Num -> Type.Basic(Tk.Type( "Int", this.tk.pos.copy()))
    }
}
