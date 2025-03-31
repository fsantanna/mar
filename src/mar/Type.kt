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

fun Type.Data.path (str: String): String {
    return this.ts.map { it.str }.joinToString(str)
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
        (this is Type.Vector     && other is Type.Vector)     -> (this.max?.static_int_eval(null)==other.max?.static_int_eval(null)) && this.tp.is_same_of(other.tp)
        (this is Type.Union      && other is Type.Union)      -> (this.ts.size==other.ts.size) && this.ts.zip(other.ts).all { (thi,oth) -> thi.second.is_same_of(oth.second) }
        (this is Type.Proto.Func && other is Type.Proto.Func) -> (this.inps.size==other.inps.size) && this.inps.zip(other.inps).all { (thi,oth) -> thi.is_same_of(oth) } && other.out.is_same_of(this.out)
        (this is Type.Proto.Coro && other is Type.Proto.Coro) -> (this.inps.size==other.inps.size) && this.inps.zip(other.inps).all { (thi,oth) -> thi.is_same_of(oth) } && this.res.is_same_of(other.res) && other.yld.is_same_of(this.yld) && other.out.is_same_of(this.out)
        (this is Type.Proto.Task && other is Type.Proto.Task) -> (this.inps.size==other.inps.size) && this.inps.zip(other.inps).all { (thi,oth) -> thi.is_same_of(oth) } && other.out.is_same_of(this.out)
        (this is Type.Exec.Coro  && other is Type.Exec.Coro)  -> (this.inps.size==other.inps.size) && this.inps.zip(other.inps).all { (thi,oth) -> thi.is_same_of(oth) } && this.res.is_same_of(other.res) && other.yld.is_same_of(this.yld) && other.out.is_same_of(this.out)
        (this is Type.Exec.Task  && other is Type.Exec.Task)  -> (this.inps.size==other.inps.size) && this.inps.zip(other.inps).all { (thi,oth) -> thi.is_same_of(oth) } && other.out.is_same_of(this.out)
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
                (e1!=null && e2!=null) -> e1.static_int_eval(null) == e2.static_int_eval(null)
                else -> false
            }
        }
    }
}

fun Type.is_sub_of (other: Type): Boolean {
    return when {
        (this is Type.Bot)  -> true
        (other is Type.Top) -> true
        (other is Type.Tpl) -> true
        (this is Type.Any || other is Type.Any) -> true
        (this is Type.Nat || other is Type.Nat) -> true
        (this is Type.Top)  -> false
        (this is Type.Tpl)  -> false
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
        (this is Type.Proto.Coro && other is Type.Proto.Coro) -> when {
                (this.xpro?.str != other.xpro?.str) -> false
                (this.inps.size != other.inps.size) -> false
                !this.inps.zip(other.inps).all { (thi,oth) -> thi.is_sub_of(oth) } -> false
                !this.res.is_sub_of(other.res) -> false
                !other.yld.is_sub_of(this.yld) -> false
                !other.out.is_sub_of(this.out) -> false
                else -> true
        }
        (this is Type.Proto.Task && other is Type.Proto.Task) -> when {
            (this.xpro?.str != other.xpro?.str) -> false
            (this.inps.size != other.inps.size) -> false
            !this.inps.zip(other.inps).all { (thi,oth) -> thi.is_sub_of(oth) } -> false
            !other.out.is_sub_of(this.out) -> false
            else -> true
        }
        (this is Type.Exec.Coro  && other is Type.Exec.Coro)  -> when {
            (this.xpro?.str != other.xpro?.str) -> false
            (this.inps.size != other.inps.size) -> false
            !this.inps.zip(other.inps).all { (thi,oth) -> thi.is_sub_of(oth) } -> false
            !this.res.is_sub_of(other.res) -> false
            !other.yld.is_sub_of(this.yld) -> false
            !other.out.is_sub_of(this.out) -> false
            else -> true
        }
        (this is Type.Exec.Task  && other is Type.Exec.Task)  -> when {
            (this.xpro?.str != other.xpro?.str) -> false
            (this.inps.size != other.inps.size) -> false
            !this.inps.zip(other.inps).all { (thi,oth) -> thi.is_sub_of(oth) } -> false
            !other.out.is_sub_of(this.out) -> false
            else -> true
        }
        else -> false
    }
}

fun Type.is_sup_of (other: Type): Boolean {
    return when {
        (this is Type.Top)  -> true
        (other is Type.Bot) -> true
        (this is Type.Tpl)  -> true
        (this is Type.Any || other is Type.Any) -> true
        (this is Type.Nat || other is Type.Nat) -> true
        (this is Type.Bot)  -> false
        (other is Type.Tpl) -> false
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
        (this is Type.Proto.Task && other is Type.Proto.Task) -> (this.inps.size==other.inps.size) && this.inps.zip(other.inps).all { (thi,oth) -> thi.is_sup_of(oth) } && other.out.is_sup_of(this.out)
        (this is Type.Exec.Coro  && other is Type.Exec.Coro)  -> (this.inps.size==other.inps.size) && this.inps.zip(other.inps).all { (thi,oth) -> thi.is_sup_of(oth) } && this.res.is_sup_of(other.res) && other.yld.is_sup_of(this.yld) && other.out.is_sup_of(this.out)
        (this is Type.Exec.Task  && other is Type.Exec.Task)  -> (this.inps.size==other.inps.size) && this.inps.zip(other.inps).all { (thi,oth) -> thi.is_sup_of(oth) } && other.out.is_sup_of(this.out)
        else -> false
    }
}

fun Type?.sub_vs_null (other: Type?): Type? {
    return when {
        (this==null && other==null) -> null
        (this==null && other!=null) -> other
        (this!=null && other==null) -> this
        (this!=null && other!=null) -> this.sub_vs(other)
        else -> error("impossible case")
    }
}

// TODO: favors this over other (if no sub relationship)
fun Type.sub_vs (other: Type): Type {
    return when {
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
                (l.size == 0) -> this //null   // X.* vs Y.*
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
        else -> this //null
    }
}

fun Type.sup_vs (other: Type): Type? {
    return when {
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

fun Type.template_apply (map: Tpl_Map): Type? {
    if (!this.has_tpls_dn()) {
        return this
    }
    return when (this) {
        is Type.Any -> this
        is Type.Bot -> this
        is Type.Top -> this
        is Type.Tpl -> map[this.tk.str]!!.first!!
        is Type.Nat -> this
        is Type.Unit -> this
        is Type.Prim -> this
        is Type.Data -> this
        is Type.Pointer -> {
            val tp = this.ptr.template_apply(map)
            if (tp == null) null else {
                Type.Pointer(this.tk, tp)
            }
        }
        is Type.Tuple -> {
            val ts = this.ts.map { (id,tp) -> Pair(id, tp.template_apply(map)) }
            if (ts.any { it.second==null }) null else {
                Type.Tuple(this.tk, ts as List<Pair<Tk.Var?, Type>>)
            }
        }
        is Type.Vector -> {
            val tp = this.tp.template_apply(map)
            when {
                (tp == null) -> null
                (this.max == null) -> null
                else -> {
                    val max = this.max.template_apply(map)
                    if (max == null) null else {
                        Type.Vector(this.tk, max, tp)
                    }
                }
            }
        }
        is Type.Union -> {
            val ts = this.ts.map { (t, tp) -> Pair(t, tp.template_apply(map)) }
            if (ts.any { it.first==null }) null else {
                Type.Union(this.tk, this.tagged, ts as List<Pair<Tk.Type?, Type>>)
            }
        }
        is Type.Proto.Func -> {
            val inps = this.inps.map { it.template_apply(map) }
            val out = this.out.template_apply(map)
            if (inps.any { it==null } || out==null) null else {
                Type.Proto.Func(this.tk, null, inps as List<Type>, out)
            }
        }
        is Type.Proto.Coro -> {
            val inps = this.inps.map { it.template_apply(map) }
            val res = this.res.template_apply(map)
            val yld = this.yld.template_apply(map)
            val out = this.out.template_apply(map)
            if (inps.any { it==null } || res==null || yld==null || out==null) null else {
                Type.Proto.Coro(this.tk, null, null, inps as List<Type>, res, yld, out)
            }
        }
        is Type.Proto.Task -> {
            val inps = this.inps.map { it.template_apply(map) }
            val out = this.out.template_apply(map)
            if (inps.any { it==null } || out==null) null else {
                Type.Proto.Task(this.tk, null, null, inps as List<Type>, out)
            }
        }
        is Type.Exec.Coro -> {
            val inps = this.inps.map { it.template_apply(map) }
            val res = this.res.template_apply(map)
            val yld = this.yld.template_apply(map)
            val out = this.out.template_apply(map)
            if (inps.any { it==null } || res==null || yld==null || out==null) null else {
                Type.Exec.Coro(this.tk, this.xpro, inps as List<Type>, res, yld, out)
            }
        }
        is Type.Exec.Task -> {
            val inps = this.inps.map { it.template_apply(map) }
            val out = this.out.template_apply(map)
            if (inps.any { it==null } || out==null) null else {
                Type.Exec.Task(this.tk, this.xpro, inps as List<Type>, out)
            }
        }
    }
}

fun Expr.template_apply (map: Tpl_Map): Expr? {
    return when (this) {
        is Expr.Tpl -> map[this.tk.str]?.second?.template_apply(map)
        is Expr.Num -> this
        is Expr.Uno -> this.e.template_apply(map).let {
            if (it == null) null else {
                Expr.Uno(this.tk_, it)
            }
        }
        is Expr.Bin -> {
            val e1 = this.e1.template_apply(map)
            val e2 = this.e2.template_apply(map)
            if (e1==null || e2==null) null else {
                Expr.Bin(this.tk_, e1, e2)
            }
        }
        else        -> TODO()
    }
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
        (this is Type.Proto.Task && tp is Type.Proto.Task) -> {
            this.out.template_con_abs(tp.out) +
                    this.inps.zip(tp.inps).map { (t1,t2) ->
                        t1.template_con_abs(t2)
                    }.union()
        }
        (this is Type.Exec.Coro && tp is Type.Exec.Coro)    -> {
            this.out.template_con_abs(tp.out) +
                    this.inps.zip(tp.inps).map { (t1,t2) ->
                        t1.template_con_abs(t2)
                    }.union() +
                    this.yld.template_con_abs(tp.yld) +
                    this.res.template_con_abs(tp.res)
        }
        (this is Type.Exec.Task && tp is Type.Exec.Task)    -> {
            this.out.template_con_abs(tp.out) +
                    this.inps.zip(tp.inps).map { (t1,t2) ->
                        t1.template_con_abs(t2)
                    }.union()
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

fun Stmt.to_tpl_abss (): List<Tpl_Abs> {
    return when (this) {
        is Stmt.Data -> this.tpls
        is Stmt.Proto -> this.tpls
        else -> error("impossible case")
    }
}

fun template_map (abss: List<Tpl_Abs>, cons: List<Tpl_Con>): Tpl_Map {
    return abss.map { (id, _) -> id.str }.zip(cons).toMap()
}

fun Stmt.template_map_all (): List<Tpl_Map>? {
    val abss = this.to_tpl_abss()
    return if (abss.isEmpty()) null else {
        G.tpls[this]?.values?.map { template_map(abss,it) }
            ?: emptyList()
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
        if (it==null) tp else tp.template_apply(template_map(s.to_tpl_abss(),it))!!
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
                    if (it==null) tpx else tpx.template_apply(template_map(s.to_tpl_abss(), it))!!
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
            val co = this.pro.type()
            when (co) {
                !is Type.Proto -> null
                is Type.Proto.Func -> null
                is Type.Proto.Coro -> Type.Exec.Coro(co.tk, co.xpro, co.inps, co.res, co.yld, co.out)
                is Type.Proto.Task -> Type.Exec.Task(co.tk, co.xpro, co.inps, co.out)
                else -> error("impossible case")
            }
        }
        is Stmt.Start -> {
            val tp = this.exe.type()
            when (tp) {
                is Type.Exec.Coro -> Type.Union(this.tk, true, listOf(tp.yld, tp.out).map { Pair(null,it) })
                is Type.Exec.Task -> TODO() // Maybe(tp.out)
                else -> error("impossible case")
            }
        }
        is Stmt.Resume -> this.exe.type().let {
            if (it !is Type.Exec.Coro) null else {
                Type.Union(this.tk, true, listOf(it.yld, it.out).map { Pair(null,it) })
            }
        }
        is Stmt.Yield -> (this.up_first { it is Stmt.Proto } as Stmt.Proto.Coro).tp_.res
        is Stmt.Await -> this.evt
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
            "ref" -> this.e.type().let {
                if (it == null) null else Type.Pointer(this.tk, it)
            }
            "deref" -> this.e.type().let {
                if (it !is Type.Pointer) null else it.ptr
            }
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
                is Type.Proto.Func -> {
                    this.f.let { f ->
                        if (f is Expr.Acc) {
                            val dcl = f.to_xdcl()!!.first as Stmt.Proto
                            it.out.template_apply(template_map(dcl.to_tpl_abss(),this.xtpls!!))
                        } else {
                            it.out
                        }
                    }
                }
                else -> null
            }
        }

        is Expr.Tuple -> this.xtp ?: Type.Any(this.tk)
        is Expr.Vector -> this.xtp ?: Type.Any(this.tk)
        is Expr.Union -> this.xtp ?: Type.Any(this.tk)
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
        is Expr.Nat -> this.xtp ?: Type.Any(this.tk)
        is Expr.Tpl -> {
            val proto = this.up_first { it is Stmt.Proto } as Stmt.Proto
            val tpl = proto.tpls.find { it.first.str == this.tk.str }!!
            tpl.second
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
