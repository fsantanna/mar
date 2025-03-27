package mar

fun List<Type>.x_inp_tup (tk: Tk, tpls: Tpl_Map?, pre: Boolean): Pair<String, Type.Tuple> {
    val tp = Type.Tuple(tk, this.map { Pair(null,it) })
    val id = tp.coder(tpls)
    return Pair(id, tp)
}

fun Type.Proto.x_sig (pre: Boolean): String {
    val (_,exe) = this.x_pro_exe(null)
    val inps = this.inps.x_inp_tup(this.tk,null, pre).first
    val (xout,_) = this.x_out(null,pre)
    return when (this) {
        is Type.Proto.Func -> TODO() //this.out.coder(tpls) + " " + xid + " (" + this.tp_.inps_.map { it.coder(xtpls,pre) }.joinToString(",") + ")"
        is Type.Proto.Coro -> {
            val res = this.res().coder(null)
            "void (*) ($exe*, ${inps}*, ${res}*, $xout*)"
        }
        is Type.Proto.Task -> "int (*) ($exe*, ${inps}*, void*)"
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
            "void $xid ($exe* _mar_exe_, ${inps}* mar_inps, ${res}* mar_res, $xout* mar_out)"
        }
        is Stmt.Proto.Task -> {
            val (_,exe) = this.tp.x_pro_exe(null)
            val inps = this.tp.inps.x_inp_tup(this.tp.tk,null, pre).first
            "int $xid ($exe* _mar_exe_, ${inps}* mar_inps, void* mar_evt)"
      }
    }
}

// Type.*.xn()
// Type.*.inps()
// Type.*.out()
// Type.*.res()
// Type.*.yld()

fun Type.xn (): Expr? {
    return when (this) {
        is Type.Proto.Coro -> this.xn
        is Type.Exec.Coro -> this.xn
        is Type.Proto.Task -> this.xn
        is Type.Exec.Task -> this.xn
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
    val xn   = this.xn()!!.static_int_eval(null)
    return when (this) {
        is Type.Proto.Coro, is Type.Exec.Coro -> {
            val tps = (inps.to_void() + listOf(this.res(), this.yld(), out)).map { it.coder(tpls) }.joinToString("__").clean()
            Pair("Coro_${xn}__$tps", "Exec__Coro__$tps")
        }
        is Type.Proto.Task, is Type.Exec.Task -> {
            Pair (
                "Task_${xn}__${inps.to_void().map { it.coder(tpls) }.joinToString("__")}__${out.coder(tpls)}".clean(),
                "Exec__Task_${xn}__${inps.to_void().map { it.coder(tpls) }.joinToString("__")}__${out.coder(tpls)}".clean(),
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
