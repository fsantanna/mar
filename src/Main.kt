package mar

import java.io.File

fun main (args: Array<String>) {
    val (xs, ys) = args.cmds_opts()
    try {
        val xinp = if (xs.size > 0) xs[0] else null
        G.libs.addAll(
            when {
                !ys.containsKey("--lib") -> emptyList()
                (ys["--lib"]!!.size == 0) -> {
                    throw Exception("argument error : --lib : expected \"=\"")
                }
                else -> ys["--lib"]!!.map { PATH + "/" + it }
            }
        )
        val xccs = run {
            val libs = G.libs.map {
                File(it + "/mar.lib")
                    .readText()
                    .trim()
                    .replace("@/",PATH+"/"+it+"/")
                    .split(" ")
            }.flatten()
            val cc = if (!ys.containsKey("--cc")) emptyList() else {
                ys["--cc"]!!.map { it.split(" ") }.flatten()
            }
            libs + cc
        }

        //TEST = ys.containsKey("--test")
        //DEBUG = ys.containsKey("--debug")
        //DUMP = DEBUG

        when {
            ys.containsKey("--version") -> println("mar " + VERSION)
            (xinp == null) -> println("expected filename")
            else -> {
                val f = File(xinp)
                val inps = listOf(
                    Pair(Triple(xinp,1,1), f.reader()),
                    Pair(Triple("prelude.mar",1,1), FileX("prelude.mar")!!.reader())
                )
                val out = all(false, ys.containsKey("--verbose"), inps, f.nameWithoutExtension, xccs)
                print(out)
            }
        }
    } catch (e: Throwable) {
        println(e.message!!)
    }
}
