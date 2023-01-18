package fr.ziedelth.lytecrh

import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFmpegUtils
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.probe.FFmpegProbeResult
import net.bramp.ffmpeg.progress.Progress
import java.util.concurrent.TimeUnit

private const val CRF = 28.0
private const val VBR = 4L
private const val VIDEO_PRESET = "veryfast"

private fun toH264(fFmpegProbeResult: FFmpegProbeResult): FFmpegBuilder {
    return FFmpegBuilder()
        .setInput(fFmpegProbeResult)
        .overrideOutputFiles(true)
        .addOutput("output.mp4")
        .setFormat("mp4")
        .setVideoCodec("libx264")
        .setConstantRateFactor(CRF)
        .setVideoPixelFormat("yuv420p")
        .addExtraArgs("-preset:v", VIDEO_PRESET)
        .addExtraArgs("-profile:v", "baseline")
        .addExtraArgs("-level", "3.0")
        .setVideoBitRate(VBR)
        .setAudioCodec("copy")
        .done()
}

private fun toH265(fFmpegProbeResult: FFmpegProbeResult): FFmpegBuilder {
    return FFmpegBuilder()
        .setInput(fFmpegProbeResult)
        .overrideOutputFiles(true)
        .addOutput("output.mp4")
        .setFormat("mp4")
        .setVideoCodec("libx265")
        .setVideoPixelFormat("yuv420p")
        .addExtraArgs("-preset:v", VIDEO_PRESET)
        .addExtraArgs("-x265-params", "crf=${CRF.toInt()}")
        .setVideoBitRate(VBR)
        .setAudioCodec("copy")
        .done()
}

private fun toVP9(fFmpegProbeResult: FFmpegProbeResult): FFmpegBuilder {
    return FFmpegBuilder()
        .setInput(fFmpegProbeResult)
        .overrideOutputFiles(true)
        .addOutput("output.mp4")
        .setFormat("mp4")
        .setVideoCodec("libvpx-vp9")
        .setVideoPixelFormat("yuv420p")
        .addExtraArgs("-preset:v", VIDEO_PRESET)
        .addExtraArgs("-crf", CRF.toString())
        .setVideoBitRate(VBR)
        .setAudioCodec("copy")
        .done()
}

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
            val time = currentTime - lastTime

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

        println(String.format(
            "[%.0f%%] Current time encoded: %s - Speed: %.2fx - Remaining: %s",
            percentage,
            FFmpegUtils.toTimecode(progress.out_time_ns, TimeUnit.NANOSECONDS).split(".")[0],
            progress.speed,
            FFmpegUtils.toTimecode(remainingTime.toLong(), TimeUnit.MILLISECONDS).split(".")[0]
        ))

        if (progress.status == Progress.Status.END) {
            val toDouble = fFmpegProbeResult.format.size.toDouble()
            val diff = (toDouble - progress.total_size.toDouble()) / toDouble * 100.0

            println()
            println("Difference: ${String.format("%.2f", diff)}%")
            println("Total taken time: ${String.format("%.2f", (System.currentTimeMillis() - start) / 60000.0)} min")
        }
    }.run()
}