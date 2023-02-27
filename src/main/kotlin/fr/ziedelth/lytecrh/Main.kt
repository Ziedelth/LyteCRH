package fr.ziedelth.lytecrh

import fr.ziedelth.lytecrh.encoders.H264Encoder
import fr.ziedelth.lytecrh.encoders.H265Encoder
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFmpegUtils
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.probe.FFmpegProbeResult
import net.bramp.ffmpeg.progress.Progress
import java.util.concurrent.TimeUnit
import kotlin.math.abs

private fun Double.toSign(): String = if (this > 0) String.format("-%.2f", this) else String.format("+%.2f", abs(this))

private val fFmpeg = FFmpeg()
private val fFprobe = FFprobe()
private const val MEGABYTES = 0.00000095367431640625

private fun compress(fFmpegProbeResult: FFmpegProbeResult, fFmpegBuilder: FFmpegBuilder) {
    println(fFmpegBuilder.build().joinToString(" "))

    val fileDurationNs = fFmpegProbeResult.format.duration * TimeUnit.SECONDS.toNanos(1)
    val fFmpegExecutor = FFmpegExecutor(fFmpeg, fFprobe)
    val start = System.currentTimeMillis()

    var lastSecond = 0L
    var lastTime = System.currentTimeMillis()
    val times = mutableListOf<Long>()
    val speeds = mutableListOf<Float>()

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

        if (progress.speed != 0.0f) {
            if (speeds.size > 100) {
                speeds.removeAt(0)
            }

            speeds.add(progress.speed)
        }

        val percentage = progress.out_time_ns / fileDurationNs * 100.0
        val diffSeconds = fFmpegProbeResult.format.duration - progressInSec
        val remainingTime = diffSeconds * times.average()

        print("\b".repeat(1000))
        print(
            String.format(
                "%s\t%s\t%.2f%%\t%s\tx%s",
                FFmpegUtils.toTimecode(progress.out_time_ns, TimeUnit.NANOSECONDS).split(".")[0],
                "[${"#".repeat((percentage * 0.5).toInt())}${" ".repeat((50 - percentage * 0.5).toInt())}]",
                percentage,
                FFmpegUtils.toTimecode(remainingTime.toLong(), TimeUnit.MILLISECONDS).split(".")[0],
                String.format("%.2f", speeds.average())
            )
        )

        if (progress.status == Progress.Status.END) {
            val toDouble = fFmpegProbeResult.format.size.toDouble()
            val totalSizeDouble = progress.total_size.toDouble()
            val diff = (toDouble - totalSizeDouble) / toDouble * 100.0

            println(System.lineSeparator())
            println(
                "Original size: ${
                    String.format(
                        "%.2f",
                        toDouble * MEGABYTES
                    )
                } MB - New size: ${String.format("%.2f", totalSizeDouble * MEGABYTES)} MB"
            )
            println("Compression ratio: ${diff.toSign()}%")
            println()
            println("Total taken time: ${String.format("%.2f", (System.currentTimeMillis() - start) / 60000.0)} min")
        }
    }.run()
}

fun main() {
    val crf = 30
    val fFmpegProbeResult = fFprobe.probe("input.mp4")

    println("H264 CPU - CRF: $crf")
    compress(
        fFmpegProbeResult,
        H264Encoder().encode(fFmpegProbeResult, Hardware.CPU, "output_cpu_h264_crf_$crf.mp4", crf.toDouble())
    )
    println("H264 GPU - CRF: $crf")
    compress(
        fFmpegProbeResult,
        H264Encoder().encode(fFmpegProbeResult, Hardware.INTEL, "output_intel_h264_crf_$crf.mp4", crf.toDouble())
    )

    println("H265 CPU - CRF: $crf")
    compress(
        fFmpegProbeResult,
        H265Encoder().encode(fFmpegProbeResult, Hardware.CPU, "output_cpu_h265_crf_$crf.mp4", crf.toDouble())
    )
    println("H265 GPU - CRF: $crf")
    compress(
        fFmpegProbeResult,
        H265Encoder().encode(fFmpegProbeResult, Hardware.INTEL, "output_intel_h265_crf_$crf.mp4", crf.toDouble())
    )
}