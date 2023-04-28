package fr.ziedelth.lytecrh.utils

import java.io.File
import kotlin.math.abs

fun Double.toSign(): String = if (this > 0) String.format("-%.2f", this) else String.format("+%.2f", abs(this))
fun File.calculateFolderSize(): Long = this.walkTopDown().filter { it.isFile }.map { it.length() }.sum()

fun Double.toHumanReadable(): String {
    val toInt = this.toInt()

    return when {
        toInt < 1024 -> "$toInt B"
        toInt < 1024 * 1024 -> "${String.format("%.2f", toInt / 1024.0)} KB"
        toInt < 1024 * 1024 * 1024 -> "${String.format("%.2f", toInt / (1024.0 * 1024.0))} MB"
        else -> "${String.format("%.2f", toInt / (1024.0 * 1024.0 * 1024.0))} GB"
    }
}