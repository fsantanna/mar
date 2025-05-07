package mar

fun String.clean (): String {
    return this.replace('*','_')
}

fun Var_Type.coder (tpls: Tpl_Map?, pre: Boolean): String {
    val (id,tp) = this
    return /*tp.coder(tpls) + " " +*/ id.str
}

fun Type.coder (tpls: Tpl_Map?): String {
    return when (this) {
        //is Type.Err,
        is Type.Any, is Type.Bot, is Type.Top -> TODO()
        is Type.Tpl        -> tpls!![this.tk.str]!!.first!!.coder(tpls)
        is Type.Nat        -> this.tk.str
        is Type.Prim       -> this.tk.str
        is Type.Data       -> this.ts.first().str + this.xtpls!!.map { (t,e) -> "_" + t.cond { it.to_str() } + e.cond { it.to_str() } }.joinToString("")
        //is Type.Data       -> this.ts.map { it.str }.joinToString("_") + this.xtpls!!.map { (t,e) -> "_" + t.cond { it.to_str() } + e.cond { it.to_str() } }.joinToString("")
        is Type.Unit       -> "_VOID_"
        is Type.Pointer    -> this.ptr.coder(tpls) + (this.ptr !is Type.Proto).cond { "*" }
        is Type.Tuple      -> "Tuple__${this.ts.map { (id,tp) -> tp.coder(tpls)+id.cond {"_"+it.str} }.joinToString("__")}".clean()
        is Type.Union      -> "Union__${this.ts.map { (id,tp) -> tp.coder(tpls)+id.cond {"_"+it.str} }.joinToString("__")}".clean()
        is Type.Vector     -> "Vector__${this.max.cond2({it.static_int_eval(tpls).toString()},{"0"})}_${this.tp.coder(tpls)}".clean()
        is Type.Proto.Func -> "Func__${this.inps.to_void().map { it.coder(tpls) }.joinToString("__")}__${this.out.coder(tpls)}".clean()
        is Type.Proto.Coro -> this.x_pro_exe(tpls).first
        is Type.Proto.Task -> this.x_pro_exe(tpls).first
        is Type.Exec.Coro  -> this.x_pro_exe(tpls).second
        is Type.Exec.Task  -> this.x_pro_exe(tpls).second
    }
}
