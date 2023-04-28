package fr.ziedelth.lytecrh.utils

import java.io.File
import kotlin.math.abs

fun Double.toSign(): String = if (this > 0) String.format("-%.2f", this) else String.format("+%.2f", abs(this))
fun File.calculateFolderSize(): Long = this.walkTopDown().filter { it.isFile }.map { it.length() }.sum()

fun Double.toHumanReadable(): String {
    val toLong = this.toLong()

    return when {
        toLong < 1024 -> "$toLong B"
        toLong < 1024 * 1024 -> "${String.format("%.2f", toLong / 1024.0)} KB"
        toLong < 1024 * 1024 * 1024 -> "${String.format("%.2f", toLong / (1024.0 * 1024.0))} MB"
        else -> "${String.format("%.2f", toLong / (1024.0 * 1024.0 * 1024.0))} GB"
    }
}