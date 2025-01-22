package mar

fun List<Type>.to_void (): List<Type> {
    return when {
        (this.size == 0) -> listOf(Type.Unit(G.tk0!!))
        else -> this
    }
}

val nums = setOf("Float", "Int", "U8")
fun Type.is_num (): Boolean {
    return (this is Type.Nat || this is Type.Any || (this is Type.Prim && nums.contains(this.tk.str)))
}

fun Type.is_str (): Boolean {
    return when {
        (this is Type.Pointer) -> this.ptr.let { it is Type.Prim && it.tk.str=="Char" }
        (this is Type.Vector)  -> this.tp.let  { it is Type.Prim && it.tk.str=="Char" }
        else -> false
    }
}

fun Type.is_same_of (other: Type): Boolean {
    return when {
        (this is Type.Any && other is Type.Any) -> true
        (this is Type.Nat && other is Type.Nat) -> (this.tk.str == other.tk.str)
        (this is Type.Unit       && other is Type.Unit)       -> true
        (this is Type.Prim       && other is Type.Prim)       -> (this.tk.str == other.tk.str)
        (this is Type.Data       && other is Type.Data)       -> (this.ts.size==other.ts.size && this.ts.zip(other.ts).all { (thi,oth) -> thi.str==oth.str })
        (this is Type.Pointer    && other is Type.Pointer)    -> this.ptr.is_same_of(other.ptr)
        (this is Type.Tuple      && other is Type.Tuple)      -> (this.ts.size==other.ts.size) && this.ts.zip(other.ts).all { (thi,oth) -> (thi.first?.str==oth.first?.str) && thi.second.is_same_of(oth.second) }
        (this is Type.Vector     && other is Type.Vector)     -> (this.max==other.max) && this.tp.is_same_of(other.tp)
        (this is Type.Union      && other is Type.Union)      -> (this.ts.size==other.ts.size) && this.ts.zip(other.ts).all { (thi,oth) -> thi.second.is_same_of(oth.second) }
        (this is Type.Proto.Func && other is Type.Proto.Func) -> (this.inps.size==other.inps.size) && this.inps.zip(other.inps).all { (thi,oth) -> thi.is_same_of(oth) } && other.out.is_same_of(this.out)
        (this is Type.Proto.Coro && other is Type.Proto.Coro) -> (this.inps.size==other.inps.size) && this.inps.zip(other.inps).all { (thi,oth) -> thi.is_same_of(oth) } && this.res.is_same_of(other.res) && other.yld.is_same_of(this.yld) && other.out.is_same_of(this.out)
        (this is Type.Exec       && other is Type.Exec)       -> (this.inps.size==other.inps.size) && this.inps.zip(other.inps).all { (thi,oth) -> thi.is_same_of(oth) } && this.res.is_same_of(other.res) && other.yld.is_same_of(this.yld) && other.out.is_same_of(this.out)
        else -> false
    }
}

fun Type.is_sup_of (other: Type): Boolean {
    return when {
        (this is Type.Any || other is Type.Any) -> true
        (this is Type.Nat || other is Type.Nat) -> true
        (this is Type.Unit       && other is Type.Unit)       -> true
        (this is Type.Prim       && other is Type.Prim)       -> (this.tk.str == other.tk.str) || (this.is_num() && other.is_num())
        (this is Type.Data       && other is Type.Data)       -> {
            //println(listOf(this.to_str(),other.to_str(),this.xtpls,other.xtpls))
            val tpl = Pair(this.xtpls!!,other.xtpls!!).let { (thi,oth) -> thi.size==oth.size && thi.zip(oth).all { (thi,oth) ->
                val (t1,e1) = thi
                val (t2,e2) = oth
                when {
                    (t1!=null && t2!=null) -> t1.is_same_of(t2)
                    (e1!=null && e2!=null) -> e1.static_int_eval() == e2.static_int_eval()
                    else -> false
                }
            }}
            val sub = this.ts.size<=other.ts.size && this.ts.zip(other.ts).all { (thi,oth) -> thi.str==oth.str }
            tpl && sub
        }
        (this is Type.Pointer    && other is Type.Pointer)    -> this.ptr.is_sup_of(other.ptr)
        (this is Type.Tuple      && other is Type.Tuple)      -> (this.ts.size==other.ts.size) && this.ts.zip(other.ts).all { (thi,oth) -> (thi.first==null||thi.first?.str==oth.first?.str) && thi.second.is_sup_of(oth.second) }
        (this is Type.Vector     && other is Type.Vector)     -> this.tp.is_same_of(other.tp)
        (this is Type.Union      && other is Type.Union)      -> (this.ts.size==other.ts.size) && this.ts.zip(other.ts).all { (thi,oth) -> thi.second.is_sup_of(oth.second) }
        (this is Type.Proto.Func && other is Type.Proto.Func) -> (this.inps.size==other.inps.size) && this.inps.zip(other.inps).all { (thi,oth) -> thi.is_sup_of(oth) } && other.out.is_sup_of(this.out)
        (this is Type.Proto.Coro && other is Type.Proto.Coro) -> (this.inps.size==other.inps.size) && this.inps.zip(other.inps).all { (thi,oth) -> thi.is_sup_of(oth) } && this.res.is_sup_of(other.res) && other.yld.is_sup_of(this.yld) && other.out.is_sup_of(this.out)
        (this is Type.Exec       && other is Type.Exec)       -> (this.inps.size==other.inps.size) && this.inps.zip(other.inps).all { (thi,oth) -> thi.is_sup_of(oth) } && this.res.is_sup_of(other.res) && other.yld.is_sup_of(this.yld) && other.out.is_sup_of(this.out)
        else -> false
    }
}

fun Type.is_sup_sub_of (other: Type): Boolean {
    return this.is_sup_of(other) && other.is_sup_of(this)
}

fun Type.sup_vs (other: Type): Type? {
    return when {
        (this is Type.Data && other is Type.Data) -> {
            val l = this.ts.commonPrefix(other.ts) { x,y ->
                (x.str == y.str)
            }
            if (l.size == 0) null else {
                Type.Data(this.tk, TODO("10"), l)
            }
        }
        (this.is_num() && other.is_num()) -> {
            when {
                // TODO: complete with other num types
                (this.tk.str == "Float") -> this
                (other.tk.str == "Float") -> other
                (this.tk.str == "Int") -> this
                (other.tk.str == "Int") -> other
                else -> this
            }
        }
        this.is_sup_of(other) -> this
        other.is_sup_of(this) -> other
        else -> null
    }
}

fun Tk.Var.type (fr: Any): Type? {
    return fr.up_first {
        if (it !is Stmt.Block) false else {
            it.to_dcls().find { (_,id,_) -> id.str == this.str }?.third
        }
    } as Type?
}

fun Type.template_resolve (tpls: List<Pair<Tk.Var,Type_Expr>>): Type {
    //return this
    return when (this) {
        is Type.Any -> this
        is Type.Tpl -> tpls.first { it.first.str == this.tk.str }.second.first!!
        is Type.Nat -> this
        is Type.Unit -> this
        is Type.Prim -> this
        is Type.Data -> TODO("this.tpls")
        is Type.Pointer -> {
            val tp = this.ptr.template_resolve(tpls)
            Type.Pointer(this.tk, tp)
        }
        is Type.Tuple -> {
            val ts = this.ts.map { (id,tp) -> Pair(id, tp.template_resolve(tpls)) }
            Type.Tuple(this.tk, ts)
        }
        is Type.Vector -> {
            val tp = this.tp.template_resolve(tpls)
            Type.Vector(this.tk, this.max, tp)
        }
        is Type.Union -> {
            val ts = this.ts.map { (t, tp) -> Pair(t, tp.template_resolve(tpls)) }
            Type.Union(this.tk, this.tagged, ts)
        }
        is Type.Proto.Func -> {
            val inps = this.inps.map { it.template_resolve(tpls) }
            val out = this.out.template_resolve(tpls)
            Type.Proto.Func(this.tk, inps, out)
        }
        is Type.Proto.Coro -> {
            val inps = this.inps.map { it.template_resolve(tpls) }
            val res = this.res.template_resolve(tpls)
            val yld = this.yld.template_resolve(tpls)
            val out = this.out.template_resolve(tpls)
            Type.Proto.Coro(this.tk, inps, res, yld, out)
        }
        is Type.Exec -> {
            val inps = this.inps.map { it.template_resolve(tpls) }
            val res = this.res.template_resolve(tpls)
            val yld = this.yld.template_resolve(tpls)
            val out = this.out.template_resolve(tpls)
            Type.Exec(this.tk, inps, res, yld, out)
        }
    }
}

@JvmName("Any_walk_List_String")
fun Any.walk (tpls: List<Type_Expr>?, ts: List<String>): Triple<Stmt.Data,List<Int>,Type>? {
    val s = this.up_data(ts.first())
    return when {
        (s == null) -> null
        (s.subs == null) -> {
            val l: MutableList<Int> = mutableListOf()
            var tp = if (tpls==null) s.tp else {
                //println(listOf(s.tpls, tpls))
                s.tp.template_resolve(s.tpls.zip(tpls).map { (v,tpl) ->
                    Pair(v.first, tpl)
                })
            }
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

fun Any.walk (tpls: List<Type_Expr>?, ts: List<Tk.Type>): Triple<Stmt.Data,List<Int>,Type>? {
    return this.walk(tpls, ts.map { it.str })
}

fun Type.Data.walk (tpl: Boolean): Triple<Stmt.Data,List<Int>,Type>? {
    return this.walk(if (tpl) this.xtpls else null, this.ts)
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
            val xxx = this.walk(null,xts)
            if (xxx == null) null else {
                val (s,i,tp) = xxx
                if (s.subs == null) {
                    Pair(i.last(),tp)
                } else {
                    val xtp = Type.Data(this.tk, this.xtpls, xts.map { Tk.Type(it, this.tk.pos) })
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

fun Stmt.typex (): Type {
    return this.type()!!
}

fun Stmt.type (): Type? {
    return when (this) {
        is Stmt.Catch -> if (this.tp == null) null else {
            Type.Union(this.tk, true, listOf(
                Pair(Tk.Type("Ok",this.tk.pos), Type.Unit(this.tk)),
                Pair(Tk.Type("Err",this.tk.pos), this.tp)
            ))
        }
        is Stmt.Create -> {
            val co = this.co.type()
            if (co !is Type.Proto.Coro) null else {
                Type.Exec(co.tk, co.inps, co.res, co.yld, co.out)
            }
        }
        is Stmt.Start -> (this.exe.type() as Type.Exec).let {
            Type.Union(this.tk, true, listOf(it.yld, it.out).map { Pair(null,it) })
        }
        is Stmt.Resume -> this.exe.type().let {
            if (it !is Type.Exec) null else {
                Type.Union(this.tk, true, listOf(it.yld, it.out).map { Pair(null,it) })
            }
        }
        is Stmt.Yield -> (this.up_first { it is Stmt.Proto } as Stmt.Proto.Coro).tp_.res
        else -> error("impossible case")
    }
}

fun Expr.typex (): Type {
    return this.type()!!
}

fun Expr.type (): Type? {
    return when (this) {
        is Expr.Uno -> when (this.tk_.str) {
            "!" -> Type.Prim(Tk.Type( "Bool", this.tk.pos))
            "-" -> Type.Prim(Tk.Type( "Int", this.tk.pos))
            "ref" -> Type.Pointer(this.tk, this.e.type()!!)
            "deref" -> this.e.type().let { if (it !is Type.Pointer) null else it.ptr }
            "#" -> Type.Prim(Tk.Type( "Int", this.tk.pos))
            "##" -> Type.Prim(Tk.Type( "Int", this.tk.pos))
            else -> error("impossible case")
        }
        is Expr.Bin -> when (this.tk_.str) {
            "==", "!=",
            ">", "<", ">=", "<=",
            "||", "&&" -> Type.Prim(Tk.Type( "Bool", this.tk.pos))
            "+", "-", "*", "/", "%" -> {
                val t1 = this.e1.type().let { if (it is Type.Prim) it.tk.str else null }
                val t2 = this.e2.type().let { if (it is Type.Prim) it.tk.str else null }
                if (t1=="Float" || t2=="Float") {
                    Type.Prim(Tk.Type("Float", this.tk.pos))
                } else {
                    Type.Prim(Tk.Type("Int", this.tk.pos))
                }
            }
            "++" -> {
                val tp1 = this.e1.type()
                val tp2 = this.e2.type()
                fun bin (op: String, e1: Expr, e2: Expr): Expr {
                    return Expr.Bin(Tk.Op(op, this.tk.pos), e1, e2)
                }
                fun num (n: Int): Expr {
                    return Expr.Num(Tk.Num(n.toString(), this.tk.pos))
                }
                when {
                    (tp1 is Type.Vector && tp2 is Type.Vector) -> {
                        Type.Vector(this.tk, Expr.Num(Tk.Num((tp1.max!!.tk.str.toInt()+tp2.max!!.tk.str.toInt()).toString(), this.tk.pos)) /*bin("+",tp1.max!!,tp2.max!!)*/, tp1.tp)
                    }
                    (tp1 is Type.Vector && this.e2 is Expr.Str) -> {
                        Type.Vector(this.tk,
                            Expr.Num(Tk.Num((tp1.max!!.tk.str.toInt() + this.e2.tk.str.length).toString(), this.tk.pos)),
                            /*
                            bin("+",
                                tp1.max!!,
                                bin("-", num(this.e2.tk.str.length), num(2))
                            ),
                             */
                            tp1.tp
                        )
                    }
                    (tp2 is Type.Vector && this.e1 is Expr.Str) -> {
                        Type.Vector(this.tk,
                            Expr.Num(Tk.Num((this.e1.tk.str.length + tp2.max!!.tk.str.toInt()).toString(), this.tk.pos)),
                            /*
                            bin("+",
                                bin("-", num(this.e1.tk.str.length), num(2)),
                                tp2.max!!
                            ),
                             */
                            tp2.tp
                        )
                    }
                    (this.e1 is Expr.Str && this.e2 is Expr.Str) -> {
                        Type.Vector(this.tk,
                            Expr.Num(Tk.Num((this.e1.tk.str.length + this.e2.tk.str.length).toString(), this.tk.pos)),
                            /*
                            bin("+",
                                bin("-", num(this.e1.tk.str.length), num(2)),
                                bin("-", num(this.e2.tk.str.length), num(2))
                            ),
                             */
                            Type.Prim(Tk.Type("Char", this.tk.pos))
                        )
                    }
                    else -> null
                }
            }
            else -> error("impossible case")
        }
        is Expr.Call -> this.f.type().let {
            when (it) {
                is Type.Nat, is Type.Any -> it
                is Type.Proto.Func -> it.out
                else -> null
            }
        }
        is Expr.Throw -> this.xtp

        is Expr.Tuple -> this.xtp
        is Expr.Vector -> this.xtp
        is Expr.Union -> this.xtp
        is Expr.Field -> {
            val tup = this.col.type().let {
                when (it) {
                    is Type.Tuple -> it
                    is Type.Data  -> it.walk(false)!!.third
                    else -> null
                }
            }
            if (tup !is Type.Tuple) null else {
                tup.index(idx)
            }
        }
        is Expr.Index -> this.col.type().let {
            if (it !is Type.Vector) null else { it.tp }
        }
        is Expr.Disc  -> this.col.type()?.discx(this.idx)?.second
        is Expr.Pred  -> Type.Prim(Tk.Type("Bool", this.tk.pos))
        is Expr.Cons  -> this.walk(null,this.tp.ts)!!.let { (s,_,_) ->
            if (s.subs == null) {
                Type.Data(this.tk, this.tp.xtpls, this.tp.ts.take(1))
            } else {
                Type.Data(this.tk, this.tp.xtpls, this.tp.ts)
            }
        }
        is Expr.Acc -> this.tk_.type(this)
        is Expr.Bool -> Type.Prim(Tk.Type( "Bool", this.tk.pos))
        is Expr.Str -> Type.Pointer(this.tk, Type.Prim(Tk.Type( "Char", this.tk.pos)))
        is Expr.Chr -> Type.Prim(Tk.Type( "Char", this.tk.pos))
        is Expr.Nat -> this.xtp //?: Type.Nat(Tk.Nat("TODO",this.tk.pos))
        is Expr.Null -> Type.Pointer(this.tk, Type.Any(this.tk))
        is Expr.Unit -> Type.Unit(this.tk)
        is Expr.Num -> {
            val x = if (this.tk.str.contains(".")) "Float" else "Int"
            Type.Prim(Tk.Type(x, this.tk.pos))
        }

        is Expr.If -> {
            val tt = this.t.type()
            val tf = this.f.type()
            if (tt == null || tf == null) null else {
                tt.sup_vs(tf)
            }
        }
        is Expr.MatchT -> this.cases.map { it.second.type() }.fold(this.cases.first().second.type(), {a,b->a?.sup_vs(b!!)})
        is Expr.MatchE -> this.cases.map { it.second.type() }.fold(this.cases.first().second.type(), {a,b->a?.sup_vs(b!!)})
    }.let {
        if (it?.xup == null) {
            it?.xup = this
        }
        it
    }
}
