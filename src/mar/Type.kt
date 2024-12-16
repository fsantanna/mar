package mar

fun List<Type>.to_void (): List<Type> {
    return when {
        (this.size == 0) -> listOf(Type.Unit(G.tk0!!))
        else -> this
    }
}

val nums = setOf("Char", "Float", "Int", "U8")
fun Type.is_num (): Boolean {
    return (this is Type.Prim) && nums.contains(this.tk.str)
}
fun Type.Prim.compat (other: Type.Prim): Boolean {
    return (this.tk.str == other.tk.str) || (this.is_num() && other.is_num())
}

fun Type.is_sup_of (other: Type): Boolean {
    return when {
        //(this is Type.Top) -> true
        //(this is Type.Any || other is Type.Any) -> true
        (this is Type.Nat || other is Type.Nat) -> true
        (this is Type.Unit       && other is Type.Unit)       -> true
        (this is Type.Prim       && other is Type.Prim)       -> (this.tk.str == other.tk.str) || (this.is_num() && other.is_num())
        (this is Type.Data       && other is Type.Data)       -> (this.ts.size<=other.ts.size && this.ts.zip(other.ts).all { (thi,oth) -> thi.str==oth.str })
        (this is Type.Pointer    && other is Type.Pointer)    -> (this.ptr==null || other.ptr==null || this.ptr.is_sup_of(other.ptr))
        (this is Type.Tuple      && other is Type.Tuple)      -> (this.ts.size==other.ts.size) && this.ts.zip(other.ts).all { (thi,oth) -> (thi.first==null||thi.first?.str==oth.first?.str) && thi.second.is_sup_of(oth.second) }
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
        "+", "-", "*", "/", "%" -> (tp1.is_num() && tp2.is_num())
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

@JvmName("Any_walk_List_String")
fun Any.walk (ts: List<String>): Triple<Stmt.Data,List<Int>,Type>? {
    val s = this.up_first { blk ->
        if (blk !is Stmt.Block) null else {
            blk.dn_filter_pre(
                {
                    when (it) {
                        is Stmt.Data -> true
                        is Stmt.Block -> if (it == blk) false else null
                        else -> false
                    }
                },
                {null},
                {null}
            ).let {
                it as List<Stmt.Data>
            }.find {
                it.t.str == ts.first()
            }
        }
    } as Stmt.Data?
    return when {
        (s == null) -> null
        (s.subs == null) -> {
            val l: MutableList<Int> = mutableListOf()
            var tp = s.tp
            for (sub in ts.drop(1)) {
                if (tp !is Type.Union) {
                    return null
                }
                val xxx = tp.disc(sub)
                if (xxx == null) {
                    return null
                }
                val (i,xtp) = xxx
                l.add(i)
                tp = xtp
            }
            Triple(s, l, tp)
        }
        else -> {
            val l: MutableList<Int> = mutableListOf()
            var ss = s!!
            val tup = (ss.tp as Type.Tuple).ts.toMutableList()
            for (sub in ts.drop(1)) {
                val i = ss.subs!!.indexOfFirst { it.t.str == sub }
                if (i == -1) {
                    return null
                }
                l.add(i)
                ss = ss.subs!![i]
                tup.addAll((ss.tp as Type.Tuple).ts)
            }
            Triple(ss, l, Type.Tuple(ss.tk, tup))
        }
    }
}

fun Any.walk (ts: List<Tk.Type>): Triple<Stmt.Data,List<Int>,Type>? {
    return this.walk(ts.map { it.str })
}

fun Type.Data.walk (): Triple<Stmt.Data,List<Int>,Type>? {
    return this.walk(this.ts)
}

fun Type.no_data (): Type {
    return if (this !is Type.Data) this else this.walk()!!.third
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
        is Type.Union -> {
            this.disc(idx)
        }
        is Type.Data -> {
            val xts = this.ts.map { it.str } + listOf(idx)
            val xxx = this.walk(xts)
            if (xxx == null) null else {
                val (s,i,tp) = xxx
                if (s.subs == null) {
                    Pair(i.last(),tp)
                } else {
                    val xtp = Type.Data(this.tk, xts.map { Tk.Type(it, this.tk.pos.copy()) })
                    xtp.xup = this
                    Pair(i.last(), xtp)
                }
            }
        }
        else -> null
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
            "deref" -> (this.e.type() as Type.Pointer).ptr!!
            else -> error("impossible case")
        }
        is Expr.Bin -> when (this.tk_.str) {
            "==", "!=",
            ">", "<", ">=", "<=",
            "||", "&&" -> Type.Prim(Tk.Type( "Bool", this.tk.pos.copy()))
            "+", "-", "*", "/", "%" -> {
                val t1 = (this.e1.type() as Type.Prim).tk.str
                val t2 = (this.e2.type() as Type.Prim).tk.str
                if (t1=="Float" || t2=="Float") {
                    Type.Prim(Tk.Type("Float", this.tk.pos.copy()))
                } else {
                    Type.Prim(Tk.Type("Int", this.tk.pos.copy()))
                }
            }
            else -> error("impossible case")
        }
        is Expr.Call -> this.f.type().let {
            if (it is Type.Nat) it else (it as Type.Proto.Func).out
        }

        is Expr.Tuple -> this.xtp!!
        is Expr.Union -> this.xtp!!
        is Expr.Field -> (this.col.type().no_data() as Type.Tuple).index(idx)!!
        is Expr.Disc  -> this.col.type().discx(this.idx)!!.second
        is Expr.Pred  -> Type.Prim(Tk.Type("Bool", this.tk.pos.copy()))
        is Expr.Cons  -> this.walk(ts)!!.let { (s,_,_) ->
            if (s.subs == null) {
                Type.Data(this.tk, this.ts.take(1))
            } else {
                Type.Data(this.tk, this.ts)
            }
        }
        is Expr.Acc -> this.tk_.type(this)!!
        is Expr.Bool -> Type.Prim(Tk.Type( "Bool", this.tk.pos.copy()))
        is Expr.Str -> Type.Pointer(this.tk, Type.Prim(Tk.Type( "Char", this.tk.pos.copy())))
        is Expr.Chr -> Type.Prim(Tk.Type( "Char", this.tk.pos.copy()))
        is Expr.Nat -> this.xtp ?: Type.Nat(Tk.Nat("TODO",this.tk.pos.copy()))
        is Expr.Null -> Type.Pointer(this.tk, null /*Type.Any(this.tk)*/)
        is Expr.Unit -> Type.Unit(this.tk)
        is Expr.Num -> {
            val x = if (this.tk.str.contains(".")) "Float" else "Int"
            Type.Prim(Tk.Type(x, this.tk.pos.copy()))
        }

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
    }.let {
        if (it.xup == null) {
            it.xup = this
        }
        it
    }
}
