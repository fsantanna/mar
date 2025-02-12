package mar

fun Type.has_tpls_dn (): Boolean {
    return this.dn_collect_pos(
        { if (it is Expr.Tpl) listOf(Unit) else  emptyList() },
        { if (it is Type.Tpl) listOf(Unit) else  emptyList() }
    ).isNotEmpty()
}

fun Type.assert_no_tpls_up (): Tpl_Map? {
    this.dn_visit_pos(
        {},
        { me ->
            when (me) {
                is Type.Data -> {
                    assert(me.xtpls!!.isEmpty(), {"TODO: sub tpls"})
                }
                else -> {}
            }
        }
    )
    return null
}

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

fun Type.is_sup_sub_of (other: Type): Boolean {
    return this.is_sup_of(other) && other.is_sup_of(this)
}

fun Type.Data.is_tpl_sup_of (other: Type.Data): Boolean {
    val thi = this.xtpls!!
    val oth = other.xtpls!!
    return when {
        (thi.size != oth.size) -> false
        else -> thi.zip(oth).all { (thi,oth) ->
            val (t1,e1) = thi
            val (t2,e2) = oth
            when {
                (t1!=null && t2!=null) -> t1.is_same_of(t2)
                (e1!=null && e2!=null) -> e1.static_int_eval() == e2.static_int_eval()
                else -> false
            }
        }
    }
}

fun Type.is_sub_of (other: Type): Boolean {
    return when {
        (this is Type.Bot)  -> true
        (other is Type.Top) -> true
        (this is Type.Top)  -> false
        (this is Type.Any || other is Type.Any) -> true
        (this is Type.Nat || other is Type.Nat) -> true
        (this is Type.Unit       && other is Type.Unit)       -> true
        (this is Type.Prim       && other is Type.Prim)       -> (this.tk.str == other.tk.str) || (this.is_num() && other.is_num())
        (this is Type.Data       && other is Type.Data)       -> {
            when {
                !this.is_tpl_sup_of(other) -> false
                (this.ts.size < other.ts.size) -> false
                else -> this.ts.zip(other.ts).all { (thi, oth) ->
                    (thi.str == oth.str)
                }
            }
        }
        (this is Type.Pointer    && other is Type.Pointer)    -> this.ptr.is_sub_of(other.ptr)
        (this is Type.Tuple      && other is Type.Tuple)      -> (this.ts.size==other.ts.size) && this.ts.zip(other.ts).all { (thi,oth) -> (thi.first==null||thi.first?.str==oth.first?.str) && thi.second.is_sub_of(oth.second) }
        (this is Type.Vector     && other is Type.Vector)     -> this.tp.is_same_of(other.tp)
        (this is Type.Union      && other is Type.Union)      -> (this.ts.size==other.ts.size) && this.ts.zip(other.ts).all { (thi,oth) -> thi.second.is_sub_of(oth.second) }
        (this is Type.Proto.Func && other is Type.Proto.Func) -> (this.inps.size==other.inps.size) && this.inps.zip(other.inps).all { (thi,oth) -> thi.is_sub_of(oth) } && other.out.is_sub_of(this.out)
        (this is Type.Proto.Coro && other is Type.Proto.Coro) -> (this.inps.size==other.inps.size) && this.inps.zip(other.inps).all { (thi,oth) -> thi.is_sub_of(oth) } && this.res.is_sub_of(other.res) && other.yld.is_sub_of(this.yld) && other.out.is_sub_of(this.out)
        (this is Type.Exec       && other is Type.Exec)       -> (this.inps.size==other.inps.size) && this.inps.zip(other.inps).all { (thi,oth) -> thi.is_sub_of(oth) } && this.res.is_sub_of(other.res) && other.yld.is_sub_of(this.yld) && other.out.is_sub_of(this.out)
        else -> false
    }
}

fun Type.sub_vs (other: Type): Type? {
    return when {
        (this is Type.Any) -> other
        (other is Type.Any) -> this
        (this is Type.Data && other is Type.Data) -> {
            //if (!this.is_tpl_sub_of(other) || !other.is_tpl_sub_of(this)) {
            val l = this.ts.commonPrefix(other.ts) { x, y ->
                (x.str == y.str)
            }

            assert(this.xtpls!!.size == other.xtpls!!.size)
            this.xtpls!!.zip(other.xtpls!!).forEach { (thi, oth) ->
                val (a1,b1) = thi
                val (a2,b2) = oth
                when {
                    (a1==null && a2==null) -> {}
                    (a1==null || a2==null) -> TODO("xtpls sub_vs 1")
                    !a1.is_same_of(a2)     -> TODO("xtpls sub_vs 2")
                }
                when {
                    (b1==null && b2==null) -> {}
                    (b1==null || b2==null) -> TODO("xtpls sub_vs 3")
                    else                   -> TODO("xtpls sub_vs 4")
                }
            }

            when {
                (l.size == 0) -> null   // X.* vs Y.*
                else -> Type.Data(this.tk, this.xtpls, l).let {
                    it.xup = this.xup
                    it
                }
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
        this.is_sub_of(other) -> this
        other.is_sub_of(this) -> other
        else -> null
    }
}

fun Type.is_sup_of (other: Type): Boolean {
    return when {
        (this is Type.Top)  -> true
        (other is Type.Bot) -> true
        (this is Type.Bot)  -> false
        (this is Type.Any || other is Type.Any) -> true
        (this is Type.Nat || other is Type.Nat) -> true
        (this is Type.Unit       && other is Type.Unit)       -> true
        (this is Type.Prim       && other is Type.Prim)       -> (this.tk.str == other.tk.str) || (this.is_num() && other.is_num())
        (this is Type.Data       && other is Type.Data)       -> {
            when {
                !this.is_tpl_sup_of(other) -> false
                (this.ts.size > other.ts.size) -> false
                else -> this.ts.zip(other.ts).all { (thi, oth) ->
                    (thi.str == oth.str)
                }
            }
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

fun Type.sup_vs (other: Type): Type? {
    return when {
        (this is Type.Any) -> other
        (other is Type.Any) -> this
        (this is Type.Data && other is Type.Data) -> {
            //if (!this.is_tpl_sup_of(other) || !other.is_tpl_sup_of(this)) {
            val l = this.ts.commonPrefix(other.ts) { x, y ->
                (x.str == y.str)
            }

            assert(this.xtpls!!.size == other.xtpls!!.size)
            this.xtpls!!.zip(other.xtpls!!).forEach { (thi, oth) ->
                val (a1,b1) = thi
                val (a2,b2) = oth
                when {
                    (a1==null && a2==null) -> {}
                    (a1==null || a2==null) -> TODO("xtpls sup_vs 1")
                    !a1.is_same_of(a2)     -> TODO("xtpls sup_vs 2")
                }
                when {
                    (b1==null && b2==null) -> {}
                    (b1==null || b2==null) -> TODO("xtpls sup_vs 3")
                    else                   -> TODO("xtpls sup_vs 4")
                }
            }

            when {
                (l.size == 0) -> null   // X.* vs Y.*
                else -> Type.Data(this.tk, this.xtpls, l).let {
                    it.xup = this.xup
                    it
                }
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

fun Type.template_con_abs (tp: Type): Tpl_Map {
    // Example: T [Bool,Int] --> T {a=Int,b=Bool}
    // this: [Bool,Int]
    // tp: [b,a]
    // --> {a=Int,b=Bool}
    return when {
        (tp is Type.Tpl) -> mapOf(Pair(tp.tk.str, Pair(this,null)))
        (this is Type.Any || this is Type.Nat || this is Type.Unit || this is Type.Prim) -> emptyMap()
        (this::class != tp::class) -> emptyMap()
        (this is Type.Pointer && tp is Type.Pointer) -> this.ptr.template_con_abs(tp.ptr)
        (this is Type.Tuple   && tp is Type.Tuple)   -> {
            this.ts.zip(tp.ts).map { (t1,t2) ->
                t1.second.template_con_abs(t2.second)
            }.union()
        }
        (this is Type.Vector  && tp is Type.Vector)  -> this.tp.template_con_abs(tp.tp)
        (this is Type.Union   && tp is Type.Union)   -> {
            this.ts.zip(tp.ts).map { (t1,t2) ->
                t1.second.template_con_abs(t2.second)
            }.union()
        }
        (this is Type.Exec    && tp is Type.Exec)    -> {
            this.out.template_con_abs(tp.out) +
                    this.inps.zip(tp.inps).map { (t1,t2) ->
                        t1.template_con_abs(t2)
                    }.union() +
                    this.yld.template_con_abs(tp.yld) +
                    this.res.template_con_abs(tp.res)
        }
        (this is Type.Proto.Func && tp is Type.Proto.Func) -> {
            this.out.template_con_abs(tp.out) +
                    this.inps.zip(tp.inps).map { (t1,t2) ->
                        t1.template_con_abs(t2)
                    }.union()
        }
        (this is Type.Proto.Coro && tp is Type.Proto.Coro) -> {
            this.out.template_con_abs(tp.out) +
                    this.inps.zip(tp.inps).map { (t1,t2) ->
                        t1.template_con_abs(t2)
                    }.union() +
                    this.yld.template_con_abs(tp.yld) +
                    this.res.template_con_abs(tp.res)
        }
        else -> error("impossible case")
    }
}

fun Type.Data.abs_con (s: Stmt.Data, map: Tpl_Map): Type.Data {
    val tpls = s.tpls.map {
        map[it.first.str]!!
    }
    return Type.Data(this.tk, tpls, this.ts)
}

fun Expr.template_abs_con (s: Stmt.Data, tpl: List<Tpl_Con>): Expr {
    return when (this) {
        is Expr.Tpl -> {
            val i = s.tpls.indexOfFirst { it.first.str==this.tk.str }
            tpl[i].second!!
        }
        is Expr.Uno -> Expr.Uno(this.tk_, this.e.template_abs_con(s,tpl))
        is Expr.Bin -> Expr.Bin(this.tk_, this.e1.template_abs_con(s,tpl), this.e2.template_abs_con(s,tpl))
        else        -> this
    }
}

fun Type.template_abs_con (s: Stmt.Data, tpl: List<Tpl_Con>): Type {
    // Example: T [b,a] --> T [Bool,Int]
    // this: [b,a]
    // s: data X {{a:Type,b:Type}}
    // tpls: {{Int,Bool}}
    // --> [Bool,Int]
    //println(listOf(this.to_str(), s.to_str(), tpl))
    return when (this) {
        is Type.Any -> this
        is Type.Bot -> this
        is Type.Top -> this
        is Type.Tpl -> {
            val i = s.tpls.indexOfFirst { it.first.str==this.tk.str }
            tpl[i].first!!
            //tpls.first { it.first.str == this.tk.str }.second.first!!
        }
        is Type.Nat -> this
        is Type.Unit -> this
        is Type.Prim -> this
        is Type.Data -> { this.assert_no_tpls_up() ; this }
        is Type.Pointer -> {
            val tp = this.ptr.template_abs_con(s, tpl)
            Type.Pointer(this.tk, tp)
        }
        is Type.Tuple -> {
            val ts = this.ts.map { (id,tp) -> Pair(id, tp.template_abs_con(s, tpl)) }
            Type.Tuple(this.tk, ts)
        }
        is Type.Vector -> {
            val tp = this.tp.template_abs_con(s, tpl)
            Type.Vector(this.tk, this.max?.template_abs_con(s,tpl), tp)
        }
        is Type.Union -> {
            val ts = this.ts.map { (t, tp) -> Pair(t, tp.template_abs_con(s, tpl)) }
            Type.Union(this.tk, this.tagged, ts)
        }
        is Type.Proto.Func -> {
            val inps = this.inps.map { it.template_abs_con(s, tpl) }
            val out = this.out.template_abs_con(s, tpl)
            Type.Proto.Func(this.tk, inps, out)
        }
        is Type.Proto.Coro -> {
            val inps = this.inps.map { it.template_abs_con(s, tpl) }
            val res = this.res.template_abs_con(s, tpl)
            val yld = this.yld.template_abs_con(s, tpl)
            val out = this.out.template_abs_con(s, tpl)
            Type.Proto.Coro(this.tk, inps, res, yld, out)
        }
        is Type.Exec -> {
            val inps = this.inps.map { it.template_abs_con(s, tpl) }
            val res = this.res.template_abs_con(s, tpl)
            val yld = this.yld.template_abs_con(s, tpl)
            val out = this.out.template_abs_con(s, tpl)
            Type.Exec(this.tk, inps, res, yld, out)
        }
    }
}

@JvmName("Any_walk_List_String")
fun Any.walk (ts: List<String>): Triple<Stmt.Data,List<Int>,Type>? {
    val s = this.up_data(ts.first())
    //println(listOf("walk", ts, s?.to_str()))
    //println(s)
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
            Triple(s, l, Type.Tuple(ss.tk, tup))
        }
    }
}

fun Any.walk (ts: List<Tk.Type>): Triple<Stmt.Data,List<Int>,Type>? {
    return this.walk(ts.map { it.str })
}

fun Type.Data.walk (): Triple<Stmt.Data,List<Int>,Type>? {
    return this.walk(this.ts)
}

@JvmName("Any_walk_tpl_List_String")
fun Any.walk_tpl (ts: List<String>, tpls: List<Tpl_Con>?): Triple<Stmt.Data,List<Int>,Type> {
    val (s,l,tp) = this.walk(ts)!!
    return Triple(s, l, tpls.let {
        if (it==null) tp else tp.template_abs_con(s, it)
    })
}

fun Any.walk_tpl (ts: List<Tk.Type>, tpls: List<Tpl_Con>?): Triple<Stmt.Data,List<Int>,Type> {
    return this.walk_tpl(ts.map { it.str }, tpls)
}

fun Type.Data.walk_tpl (): Triple<Stmt.Data,List<Int>,Type> {
    return this.walk_tpl(this.ts, this.xtpls)
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
                val (s,i,tpx) = xxx
                val tp = this.xtpls.let {
                    if (it==null) tpx else tpx.template_abs_con(s, it)
                }
                //println(listOf("discx", tp.to_str()))
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
    //println(this.to_str())
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
                            Expr.Num(Tk.Num((tp1.max!!.tk.str.toInt() + this.e2.tk.str.length-2).toString(), this.tk.pos)),
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
                            Expr.Num(Tk.Num((this.e1.tk.str.length-2 + tp2.max!!.tk.str.toInt()).toString(), this.tk.pos)),
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
                            Expr.Num(Tk.Num((this.e1.tk.str.length-2 + this.e2.tk.str.length-2).toString(), this.tk.pos)),
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
            val tup = this.col.type().let { tp ->
                when (tp) {
                    is Type.Tuple -> tp
                    is Type.Data  -> tp.walk_tpl().third
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
        is Expr.Cons  -> this.walk(this.tp.ts)!!.let { (s,_,_) ->
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
        is Expr.Null -> Type.Pointer(this.tk, Type.Any(this.tk))
        is Expr.Unit -> Type.Unit(this.tk)
        is Expr.Num -> {
            val x = if (this.tk.str.contains(".")) "Float" else "Int"
            Type.Prim(Tk.Type(x, this.tk.pos))
        }
        is Expr.Nat -> this.xtp //?: Type.Nat(Tk.Nat("TODO",this.tk.pos))
        is Expr.Tpl -> TODO("Expr.Tpl.type()")

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
