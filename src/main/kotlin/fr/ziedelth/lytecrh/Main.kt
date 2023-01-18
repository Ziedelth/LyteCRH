package fr.ziedelth.lytecrh

import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFmpegUtils
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.probe.FFmpegProbeResult
import net.bramp.ffmpeg.progress.Progress
import java.util.concurrent.TimeUnit
import kotlin.math.abs

private const val CRF = 30.0
private const val VBR = 4L

private fun toH264(fFmpegProbeResult: FFmpegProbeResult): FFmpegBuilder {
    return FFmpegBuilder()
        .setInput(fFmpegProbeResult)
        .overrideOutputFiles(true)
        .addOutput("output.mp4")
        .setFormat("mp4")
        .setVideoCodec("libx264")
        .setConstantRateFactor(CRF)
        .setVideoPixelFormat("yuv420p")
        .addExtraArgs("-profile:v", "baseline")
        .addExtraArgs("-level", "3.0")
        .setVideoBitRate(VBR)
        .setAudioCodec("copy")
        .done()
}

private fun Double.toSign(): String = if (this > 0) String.format("-%.2f", this) else String.format("+%.2f", abs(this))

fun main() {
    val fFmpeg = FFmpeg()
    val fFprobe = FFprobe()
    val fFmpegProbeResult = fFprobe.probe("input.mp4")
    val fFmpegBuilder = toH264(fFmpegProbeResult)
    val fileDurationNs = fFmpegProbeResult.format.duration * TimeUnit.SECONDS.toNanos(1)
    println(fFmpegBuilder.build().joinToString(" "))
    val fFmpegExecutor = FFmpegExecutor(fFmpeg, fFprobe)
    val start = System.currentTimeMillis()

    var lastSecond = 0L
    var lastTime = System.currentTimeMillis()
    val times = mutableListOf<Long>()

    fFmpegExecutor.createJob(fFmpegBuilder) { progress ->
        val progressInSec = (progress.out_time_ns * 0.000000001).toLong()

        if (progressInSec != lastSecond) {
            val currentTime = System.currentTimeMillis()
            val time = (currentTime - lastTime) / abs(progressInSec - lastSecond)

            if (times.size > 100) {
                times.removeAt(0)
            }

            times.add(time)
            lastTime = currentTime
            lastSecond = progressInSec
        }

        val percentage = progress.out_time_ns / fileDurationNs * 100.0
        val diffSeconds = fFmpegProbeResult.format.duration - progressInSec
        val remainingTime = diffSeconds * times.average()

        print("\b".repeat(100))
        print(
            String.format(
                "%s\t%s\t%.2f%%\t%s",
                FFmpegUtils.toTimecode(progress.out_time_ns, TimeUnit.NANOSECONDS).split(".")[0],
                "[${"#".repeat((percentage / 2).toInt())}${" ".repeat((50 - percentage / 2).toInt())}]",
                percentage,
                FFmpegUtils.toTimecode(remainingTime.toLong(), TimeUnit.MILLISECONDS).split(".")[0]
            )
        )

        if (progress.status == Progress.Status.END) {
            val toDouble = fFmpegProbeResult.format.size.toDouble()
            val diff = (toDouble - progress.total_size.toDouble()) / toDouble * 100.0

            println(System.lineSeparator())
            println("Original size: ${String.format("%.2f", toDouble / (1024 * 1024))} MB - New size: ${String.format("%.2f", progress.total_size.toDouble() / (1024 * 1024))} MB")
            println("Compression ratio: ${diff.toSign()}%")
            println()
            println("Total taken time: ${String.format("%.2f", (System.currentTimeMillis() - start) / 60000.0)} min")
        }
    }.run()
}