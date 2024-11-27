package mar

fun List<Type>.to_void (): List<Type> {
    return when {
        (this.size == 0) -> listOf(Type.Unit(G.tk0!!))
        else -> this
    }
}

fun Type.is_sup_of (other: Type): Boolean {
    return when {
        //(this is Type.Top) -> true
        (this is Type.Any || other is Type.Any) -> true
        (this is Type.Unit       && other is Type.Unit)       -> true
        (this is Type.Prim       && other is Type.Prim)       -> (this.tk.str == other.tk.str)
        (this is Type.Data       && other is Type.Data)       -> (this.ts.size<=other.ts.size && this.ts.zip(other.ts).all { (thi,oth) -> thi.str==oth.str })
        (this is Type.Pointer    && other is Type.Pointer)    -> this.ptr.is_sup_of(other.ptr)
        (this is Type.Tuple      && other is Type.Tuple)      -> (this.ts.size==other.ts.size) && this.ts.zip(other.ts).all { (thi,oth) -> (oth.first==null||thi.first?.str==oth.first?.str) && thi.second.is_sup_of(oth.second) }
        (this is Type.Union      && other is Type.Union)      -> (this.ts.size==other.ts.size) && this.ts.zip(other.ts).all { (thi,oth) -> thi.second.is_sup_of(oth.second) }
        (this is Type.Proto.Func && other is Type.Proto.Func) -> (this.inps.size==other.inps.size) && this.inps.zip(other.inps).all { (thi,oth) -> thi.is_sup_of(oth) } && other.out.is_sup_of(this.out)
        (this is Type.Proto.Coro && other is Type.Proto.Coro) -> (this.inps.size==other.inps.size) && this.inps.zip(other.inps).all { (thi,oth) -> thi.is_sup_of(oth) } && this.res.is_sup_of(other.res) && other.yld.is_sup_of(this.yld) && other.out.is_sup_of(this.out)
        (this is Type.Exec       && other is Type.Exec)       -> (this.inps.size==other.inps.size) && this.inps.zip(other.inps).all { (thi,oth) -> thi.is_sup_of(oth) } && this.res.is_sup_of(other.res) && other.yld.is_sup_of(this.yld) && other.out.is_sup_of(this.out)
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
            tp1.is_sup_of(Type.Prim(Tk.Type( "Int", this.tk.pos.copy()))) &&
            tp2.is_sup_of(Type.Prim(Tk.Type( "Int", this.tk.pos.copy())))
        }
        "||", "&&" -> {
            tp1.is_sup_of(Type.Prim(Tk.Type( "Bool", this.tk.pos.copy()))) &&
            tp2.is_sup_of(Type.Prim(Tk.Type( "Bool", this.tk.pos.copy())))
        }
        else -> error("impossible case")
    }
}

fun Tk.Var.type (fr: Any): Type? {
    return fr.up_first {
        if (it !is Stmt.Block) false else {
            it.to_dcls().find { (_,id,_) -> id.str == this.str }?.third
        }
    } as Type?
}

fun Type.Data.to_flat_hier (): Stmt? {
    return this.to_flat_hier(this.ts)
}

fun Any.to_flat_hier (ts: List<Tk.Type>): Stmt? {
    return this.up_first { blk ->
        if (blk !is Stmt.Block) null else {
            blk.to_flat_hier().find { s ->
                when (s) {
                    is Stmt.Data -> (s.t.str == ts.first().str)
                    //is Stmt.Hier -> (s.ts.to_str() == ts.to_str())
                    else -> error("impossible case")
                }
            }
        }
    } as Stmt?
}

fun Type.no_data (): Type? {
    return when (this) {
        !is Type.Data -> this
        else -> {
            val s = this.to_flat_hier()
            when (s) {
                null -> null
                is Stmt.Data -> {
                    var tp: Type? = s.tp
                    for (id in this.ts.drop(1)) {
                        if (tp !is Type.Union) {
                            tp = null
                            break
                        } else {
                            tp = tp.disc(id.str)?.second
                        }
                    }
                    tp
                }
                //is Stmt.Hier -> s.xtp!!
                else -> error("impossible case")
            }
        }
    }
}

fun Type.Tuple.index (idx: String): Type? {
    val v = idx.toIntOrNull().let {
        if (it == null) {
            this.ts.map { it.first }.indexOfFirst { it?.str==idx }
        } else {
            it - 1
        }
    }
    return this.ts.getOrNull(v)?.second
}

fun Type.discx (idx: String): Pair<Int, Type>? {
    return when (this) {
        is Type.Union -> this.disc(idx)
        is Type.Data -> this.disc(idx)
        else -> null
    }
}

fun Type.Data.disc (idx: String): Pair<Int, Type>? {
    val s = this.to_flat_hier()
    return when (s) {
        is Stmt.Data -> {
            val tp2 = this.no_data()
            if (tp2 is Type.Union) {
                tp2.disc(idx)
            } else {
                null
            }
        }
        /*
        is Stmt.Hier -> {
            val i = s.xsubs.indexOfFirst { it.ts.to_str() == this.to_str()+"."+idx }
            if (i == -1) null else {
                val dat = Type.Data(this.tk, this.ts + listOf(Tk.Type(idx,this.tk.pos.copy())))
                dat.xup = this
                Pair(i, dat)
            }
        }
         */
        else -> error("impossible case")
    }
}

fun Type.Union.disc (idx: String): Pair<Int, Type>? {
    val num = idx.toIntOrNull()
    return when {
        (num == null) -> {
            this.ts.map { it.first }.indexOfFirst { it?.str == idx }.let {
                if (it == -1) {
                    null
                } else {
                    Pair(it, this.ts[it].second)
                }
            }
        }
        (num<=0 || num>this.ts.size) -> null
        else -> Pair(num-1, this.ts[num-1].second)
    }
}

fun Expr.type (): Type {
    return when (this) {
        is Expr.Uno -> when (this.tk_.str) {
            "-" -> Type.Prim(Tk.Type( "Int", this.tk.pos.copy()))
            "ref" -> Type.Pointer(this.tk, this.e.type())
            "deref" -> (this.e.type() as Type.Pointer).ptr
            else -> error("impossible case")
        }
        is Expr.Bin -> when (this.tk_.str) {
            "==", "!=",
            ">", "<", ">=", "<=",
            "||", "&&" -> Type.Prim(Tk.Type( "Bool", this.tk.pos.copy()))
            "+", "-", "*", "/", "%" -> Type.Prim(Tk.Type( "Int", this.tk.pos.copy()))
            else -> error("impossible case")
        }
        is Expr.Call -> this.f.type().let {
            if (it is Type.Any) it else (it as Type.Proto.Func).out
        }

        is Expr.Tuple -> this.xtp!!
        is Expr.Union -> this.xtp!!
        is Expr.Field -> (this.col.type().no_data() as Type.Tuple).index(idx)!!
        is Expr.Disc  -> this.col.type().discx(this.idx)!!.second
        is Expr.Pred  -> Type.Prim(Tk.Type("Bool", this.tk.pos.copy()))
        is Expr.Cons  -> this.dat

        is Expr.Acc -> this.tk_.type(this)!!
        is Expr.Bool -> Type.Prim(Tk.Type( "Bool", this.tk.pos.copy()))
        is Expr.Char -> Type.Prim(Tk.Type( "Char", this.tk.pos.copy()))
        is Expr.Nat -> this.xtp ?: Type.Any(this.tk)
        is Expr.Null -> Type.Pointer(this.tk, Type.Any(this.tk))
        is Expr.Unit -> Type.Unit(this.tk)
        is Expr.Num -> Type.Prim(Tk.Type( "Int", this.tk.pos.copy()))

        is Expr.Create -> {
            val co = this.co.type() as Type.Proto.Coro
            Type.Exec(co.tk, co.inps, co.res, co.yld, co.out)
        }
        is Expr.Start -> (this.exe.type() as Type.Exec).let {
            Type.Union(this.tk, true, listOf(it.yld, it.out).map { Pair(null,it) })
        }
        is Expr.Resume -> (this.exe.type() as Type.Exec).let {
            Type.Union(this.tk, true, listOf(it.yld, it.out).map { Pair(null,it) })
        }
        is Expr.Yield -> (this.up_first { it is Stmt.Proto } as Stmt.Proto.Coro).tp_.res
    }
}
