package mar

fun List<Type>.x_inp_tup (tk: Tk, tpls: Tpl_Map?, pre: Boolean): Pair<String, Type.Tuple> {
    val tp = Type.Tuple(tk, this.map { Pair(null,it) })
    val id = tp.coder(tpls)
    return Pair(id, tp)
}

fun Type.to_exe (): Type.Exec? {
    return when (this) {
        is Type.Proto.Coro -> Type.Exec.Coro(this.tk, this.xpro, this.inps, this.res, this.yld, this.out)
        is Type.Proto.Task -> Type.Exec.Task(this.tk, this.xpro, this.inps, this.out)
        else -> null
    }
}

fun Type.Exec.to_pro (): Type.Proto {
    return when (this) {
        is Type.Exec.Coro -> Type.Proto.Coro(this.tk, this.xpro, null, this.inps, this.out, this.res, this.yld)
        is Type.Exec.Task -> Type.Proto.Task(this.tk, this.xpro, null, this.inps, this.out)
    }
}

fun Type.Proto.x_sig (pre: Boolean): String {
    val (_,exe) = this.x_pro_exe(null)
    val inps = this.inps.x_inp_tup(this.tk,null, pre).first
    val (xout,_) = this.x_out(null,pre)
    return when (this) {
        is Type.Proto.Func -> TODO() //this.out.coder(tpls) + " " + xid + " (" + this.tp_.inps_.map { it.coder(xtpls,pre) }.joinToString(",") + ")"
        is Type.Proto.Coro -> {
            val res = this.res().coder(null)
            "void (*) (MAR_EXE_ACTION, $exe*, ${inps}*, ${res}*, $xout*)"
        }
        is Type.Proto.Task -> "void (*) (MAR_EXE_ACTION, $exe*, ${inps}*, int, void*)"
    }
}

fun Stmt.Proto.x_sig (tpls: Tpl_Map?, pre: Boolean): String {
    val xid = this.proto(tpls)
    val (xout,_) = this.tp.x_out(tpls, pre)
    return when (this) {
        is Stmt.Proto.Func -> this.tp.out.coder(tpls) + " " + xid + " (" + this.tp_.inps_.map { it.coder(tpls,pre) }.joinToString(", ") + ")"
        is Stmt.Proto.Coro -> {
            val (_,exe) = this.tp.x_pro_exe(null)
            val inps = this.tp.inps.x_inp_tup(this.tp.tk,null, pre).first
            val res = this.tp.res().coder(null)
            "void $xid (MAR_EXE_ACTION mar_act, $exe* mar_exe, ${inps}* mar_inps, ${res}* mar_res, $xout* mar_out)"
        }
        is Stmt.Proto.Task -> {
            val (_,exe) = this.tp.x_pro_exe(null)
            val inps = this.tp.inps.x_inp_tup(this.tp.tk,null, pre).first
            "void $xid (MAR_EXE_ACTION mar_act, $exe* mar_exe, ${inps}* mar_inps, int mar_evt_tag, void* mar_evt_pay)"
      }
    }
}

// Type.*.xpro()
// Type.*.inps()
// Type.*.out()
// Type.*.res()
// Type.*.yld()

fun Type.xpro (): Tk.Var? {
    return when (this) {
        is Type.Proto.Coro -> this.xpro
        is Type.Exec.Coro -> this.xpro
        is Type.Proto.Task -> this.xpro
        is Type.Exec.Task -> this.xpro
        else -> error("impossible case")
    }
}
fun Type.inps (): List<Type> {
    return when (this) {
        is Type.Proto.Coro -> this.inps
        is Type.Exec.Coro -> this.inps
        is Type.Proto.Task -> this.inps
        is Type.Exec.Task -> this.inps
        else -> error("impossible case")
    }
}
fun Type.inps_ (): List<Var_Type> {
    return when (this) {
        is Type.Proto.Coro.Vars -> this.inps_
        is Type.Proto.Task.Vars -> this.inps_
        else -> error("impossible case")
    }
}
fun Type.out (): Type {
    return when (this) {
        is Type.Proto.Func -> this.out
        is Type.Proto.Coro -> this.out
        is Type.Exec.Coro -> this.out
        is Type.Proto.Task -> this.out
        is Type.Exec.Task -> this.out
        else -> error("impossible case")
    }
}
fun Type.res (): Type {
    return when (this) {
        is Type.Proto.Coro -> this.res
        is Type.Exec.Coro -> this.res
        else -> error("impossible case")
    }
}
fun Type.yld (): Type {
    return when (this) {
        is Type.Proto.Coro -> this.yld
        is Type.Exec.Coro -> this.yld
        else -> error("impossible case")
    }
}

// Type.*.pro_exe

fun Type.x_pro_exe (tpls: Tpl_Map?): Pair<String,String> {
    val inps = this.inps()
    val out  = this.out()
    val xpro = this.xpro()!!.str
    return when (this) {
        is Type.Proto.Coro, is Type.Exec.Coro -> {
            val tps = (inps.to_void() + listOf(this.res(), this.yld(), out)).map { it.coder(tpls) }.joinToString("__").clean()
            Pair("Coro_${xpro}__$tps", "Exec__Coro__$tps")
        }
        is Type.Proto.Task, is Type.Exec.Task -> {
            Pair (
                "Task_${xpro}__${inps.to_void().map { it.coder(tpls) }.joinToString("__")}__${out.coder(tpls)}".clean(),
                "Exec__Task_${xpro}__${inps.to_void().map { it.coder(tpls) }.joinToString("__")}__${out.coder(tpls)}".clean(),
            )
        }
        else -> error("impossible case")
    }
}

// Type.*.x_out

fun Type.x_out (tpls: Tpl_Map?, pre: Boolean): Pair<String, Type> {
    val out = this.out()
    return when (this) {
        is Type.Proto.Coro, is Type.Exec.Coro -> {
            val tp = Type.Union(this.tk, true, listOf(this.yld(), out).map { Pair(null, it) })
            val id = tp.coder(tpls)
            Pair(id, tp)
        }
        is Type.Proto.Func, is Type.Proto.Task, is Type.Exec.Task -> {
            val id = out.coder(tpls)
            Pair(id, out)
        }
        else -> error("impossible case")
    }
}
