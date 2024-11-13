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
        (this is Type.Tuple      && other is Type.Tuple)      -> (this.ts.size==other.ts.size) && this.ts.zip(other.ts).all { (thi,oth) -> thi.is_sup_of(oth) } && (this.ids==null || other.ids==null || this.ids.zip(other.ids).all { (thi,oth) -> thi.str==oth.str })
        (this is Type.Union      && other is Type.Union)      -> (this.ts.size==other.ts.size) && this.ts.zip(other.ts).all { (thi,oth) -> thi.is_sup_of(oth) }
        (this is Type.Proto.Func && other is Type.Proto.Func) -> (this.inps.size==other.inps.size) && this.inps.zip(other.inps).all { (thi,oth) -> thi.is_sup_of(oth) } && other.out.is_sup_of(this.out)
        (this is Type.Proto.Coro && other is Type.Proto.Coro) -> (this.inps.size==other.inps.size) && this.inps.zip(other.inps).all { (thi,oth) -> thi.is_sup_of(oth) } && this.res.is_sup_of(other.res) && other.yld.is_sup_of(this.yld) && other.out.is_sup_of(this.out)
        (this is Type.Exec       && other is Type.Exec)        -> (this.inps.size==other.inps.size) && this.inps.zip(other.inps).all { (thi,oth) -> thi.is_sup_of(oth) } && this.res.is_sup_of(other.res) && other.yld.is_sup_of(this.yld) && other.out.is_sup_of(this.out)
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

fun String.to_data (): Stmt.Data? {
    return G.outer!!.dn_filter_pre({ it is Stmt.Data }, {null}, {null})
        .let { it as List<Stmt.Data> }
        .find { it.id.str == this }
}

fun Type.Data.to_data (): Stmt.Data? {
    return this.ts.first().str.to_data()
}

fun Type.no_data (): Type {
    return if (this !is Type.Data) this else {
        this.to_data()!!.tp
    }
}

fun Type.Union.index (sup: String?, sub: String): Pair<Int, Type>? {
    val num = sub.toIntOrNull()
    return if (num != null) {
        when {
            (num<0 || num>this.ts.size) -> null
            (num==0 && this._0==null)   -> null
            (num==0 && this._0!=null)   -> Pair(-1, this._0)
            else                        -> Pair(num-1, this.ts[num-1])
        }
    } else {
        when {
            (sup==sub && this._0==null) -> null
            (sup==sub && this._0!=null) -> {
                Pair(-1, this._0)
            }
            (this.ids == null)          -> null
            else -> this.ids.indexOfFirst { it.str == sub }.let {
                if (it == -1) {
                    null
                } else {
                    Pair(it, this.ts[it])
                }
            }
        }
    }
}

fun Type.Union.indexes (sup: String?, subs: List<String>): List<Pair<Int,Type?>>? {
    assert(subs.size > 0)
    var xsup = sup
    var cur: Type.Union = this
    val l: MutableList<Pair<Int,Type?>> = mutableListOf()
    for (i in 0 .. subs.size-1) {
        val sub = subs[i]
        //println(listOf(xsup, sub))
        val idx_tp = cur.index(xsup, sub)
        if (idx_tp == null) {
            return null
        }
        val (idx,tp) = idx_tp
        l.add(Pair(idx, cur._0))
        when {
            (i == subs.size-1) -> if (idx == -1) { /* added above */ } else {
                l.add(idx_tp)
            }
            (tp !is Type.Union) -> return null
            else -> {
                l.add(Pair(idx, null))
                cur = tp
                xsup = sub
            }
        }
    }
    return l
}

fun Type.walk (sups: List<Tk.Type>, subs: List<String>): Pair<List<Int>,List<Type?>>? {
    TODO()
    /*
    return when (this) {
        is Type.Data -> {
            assert(sups.isEmpty())
            this.no_data().walk(this.ts, subs)
        }
        is Type.Union -> this.walk(sups, subs)
        else -> null
    }

     */
}

fun Stmt.Data.walk (l: List<String>): Pair<List<Int>,List<Type?>>? {
    TODO()
    /*
    var cur: Type = this.tp
    var sup: String? = l.first()
    var lst = true
    var idx: Int? = null
    val idxs = mutableListOf<Int>()
    val tps = mutableListOf<Type?>()
    if (this.id.str != sup) {
        return null
    }
    for (i in 1..l.size-1) {
        val id = l[i]
        if (cur !is Type.Union) {
            return null
        }
        val uni = cur
        val num = id.toIntOrNull()
        if (num != null) {
            when {
                (num<0 || num>uni.ts.size) -> return null
                (num==0 && uni._0==null)   -> return null
                (num==0 && uni._0!=null)   -> { idx=-1    ; cur=uni._0        }
                else                       -> { idx=num-1 ; cur=uni.ts[num-1] }
            }
        } else {
            when {
                (sup==id && uni._0==null) -> return null
                (sup==id && uni._0!=null) -> { idx=-1 ; cur=uni._0 }
                (uni.ids == null)         -> return null
                else -> uni.ids.indexOfFirst { it.str == id }.let {
                    if (it == -1) {
                        return null
                    } else {
                        idx = it
                        cur = uni.ts[it]
                    }
                }
            }
        }
        tps.add(uni._0)
        if (i==l.size-1 && idx==-1) {
            lst = false     // last is _0: do not add again outside
        }
        sup = id
    }
    if (lst) {
        tps.add(cur)
    }
    return Pair(idxs, tps)

     */
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
        is Expr.Field -> {
            val tp = this.col.type().no_data() as Type.Tuple
            val idx = this.idx.toIntOrNull().let {
                if (it == null) {
                    tp.ids!!.indexOfFirst { it.str==this.idx }
                } else {
                    it - 1
                }
            }
            tp.ts[idx]
        }
        is Expr.Disc  -> {
            val tp  = this.col.type()
            val tpx = tp.no_data() as Type.Union
            val sup = if (tp is Type.Data) tp.ts.first().str else null
            tpx.indexes(sup, this.path)!!.last().second!!
        }
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
            Type.Union(this.tk, true, null, mutableListOf(it.yld, it.out), null)
        }
        is Expr.Resume -> (this.exe.type() as Type.Exec).let {
            Type.Union(this.tk, true, null, mutableListOf(it.yld, it.out), null)
        }
        is Expr.Yield -> (this.up_first { it is Stmt.Proto } as Stmt.Proto.Coro).tp_.res
    }
}
