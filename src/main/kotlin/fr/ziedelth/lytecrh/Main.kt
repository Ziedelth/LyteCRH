package fr.ziedelth.lytecrh

import fr.ziedelth.lytecrh.encoders.H264Encoder
import fr.ziedelth.lytecrh.encoders.VP8Encoder
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFmpegUtils
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.probe.FFmpegProbeResult
import net.bramp.ffmpeg.probe.FFmpegStream
import net.bramp.ffmpeg.progress.Progress
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.abs

private fun FFmpegProbeResult.getTotalBitrate(): Long {
    val videoStream = this.getStreams().find { it.codec_type == FFmpegStream.CodecType.VIDEO }
    val videoBitRate = videoStream!!.bit_rate
    val audioStream = this.getStreams().find { it.codec_type == FFmpegStream.CodecType.AUDIO }
    val audioBitrate = audioStream!!.bit_rate
    return videoBitRate + audioBitrate
}

private fun Double.toSign(): String = if (this > 0) String.format("-%.2f", this) else String.format("+%.2f", abs(this))
private fun File.calculateFolderSize(): Long = this.walkTopDown().filter { it.isFile }.map { it.length() }.sum()

private val fFmpeg = FFmpeg()
private val fFprobe = FFprobe()

fun drawProgressbar(currentTime: String, progress: Double, remainingTime: String, speed: Double, length: Int = 50) {
    val copyProgressBar = " ".repeat(length).toCharArray()
    val currentPosition = (copyProgressBar.size * progress).toInt()
    (0 until currentPosition).forEach { copyProgressBar[it] = '•' }
    val str = "$currentTime\t|${copyProgressBar.joinToString("")}|\t${String.format("%.2f", progress * 100)}%\t~$remainingTime (${String.format("%.2f", speed)}x)"
    print("\b".repeat(str.length) + str)
}

fun Double.toHumanReadable(): String {
    val toInt = this.toInt()

    return when {
        toInt < 1024 -> "$toInt B"
        toInt < 1024 * 1024 -> "${String.format("%.2f", toInt / 1024.0)} KB"
        toInt < 1024 * 1024 * 1024 -> "${String.format("%.2f", toInt / (1024.0 * 1024.0))} MB"
        else -> "${String.format("%.2f", toInt / (1024.0 * 1024.0 * 1024.0))} GB"
    }
}

fun calculateBandwidth(durationInSeconds: Double, views: Long, bitrateInKbps: Double): Double {
    return durationInSeconds * views * bitrateInKbps / 8.0
}

private fun compress(fFmpegProbeResult: FFmpegProbeResult, fFmpegBuilder: FFmpegBuilder, filename: String) {
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

        drawProgressbar(
            FFmpegUtils.toTimecode(progress.out_time_ns, TimeUnit.NANOSECONDS).split(".")[0],
            percentage / 100.0,
            FFmpegUtils.toTimecode(remainingTime.toLong(), TimeUnit.MILLISECONDS).split(".")[0],
            speeds.average(),
        )

        if (progress.status != Progress.Status.END) {
            return@createJob
        }

        val toDouble = fFmpegProbeResult.format.size.toDouble()
        val isInsideAFolder = filename.contains("/")
        val foldername = if (isInsideAFolder) filename.substring(0, filename.lastIndexOf("/")) else ""
        val totalSizeDouble = if (isInsideAFolder && filename.substringAfterLast(".").lowercase() == "m3u8") File(foldername).calculateFolderSize().toDouble() else progress.total_size.toDouble()
        val diff = (toDouble - totalSizeDouble) / toDouble * 100.0

        val originalBitrateToDouble = fFmpegProbeResult.getTotalBitrate().toDouble()
        val newBitrateToDouble = progress.bitrate.toDouble()

        println()
        println()
        println("Size: ${toDouble.toHumanReadable()} → ${totalSizeDouble.toHumanReadable()} (${diff.toSign()}%)")

        if (!isInsideAFolder) {
            val oldBitrateInKbps = originalBitrateToDouble * 0.001
            val newBitrateInKbps = newBitrateToDouble * 0.001
            println("Bitrate: ${String.format("%.2f", oldBitrateInKbps)} kbps → ${String.format("%.2f", newBitrateInKbps)} kbps")
            // For one day, the video is seen 40,000 times
            // How many bandwidth is needed to stream the video for one day?
            val oldBandwidth = calculateBandwidth(fFmpegProbeResult.format.duration, 40_000, oldBitrateInKbps) // In kbps
            val newBandwidth = calculateBandwidth(fFmpegProbeResult.format.duration, 40_000, newBitrateInKbps) // In kbps
            println("Bandwidth: ${String.format("%.2f", oldBandwidth / (1000 * 1000 * 1000))} To/day → ${String.format("%.2f", newBandwidth / (1000 * 1000 * 1000))} To/day")
            // 0.02 $/Go
            val pricePerGo = 0.02
            val pricePerDay = pricePerGo * 24
            val oldCost = oldBitrateInKbps / 8.0 / 1024.0 * 3600 * 24 * pricePerGo * pricePerDay
            val newCost = newBitrateInKbps / 8.0 / 1024.0 * 3600 * 24 * pricePerGo * pricePerDay
            println("Cost: ${String.format("%.2f", oldCost)} $/day → ${String.format("%.2f", newCost)} $/day")
        }

        println()
        println("Total taken time: ${String.format("%.2f", (System.currentTimeMillis() - start) / 60000.0)} min")
    }.run()
}

fun main() {
    val crf = 30
    val fFmpegProbeResult = fFprobe.probe("mha.mp4")

    val folder = File("output")
    if (!folder.exists()) folder.mkdirs()

    println("H264 CPU - CRF: $crf")
//    val h264CpuOutput = "${folder.name}/video.m3u8"
    val h264CpuOutput = "video.mp4"
    compress(
        fFmpegProbeResult,
        H264Encoder().encode(fFmpegProbeResult, Hardware.CPU, h264CpuOutput, crf.toDouble()),
        h264CpuOutput,
    )

//    println("VP8 CPU - CRF: $crf")
//    val vp8CpuOutput = "video.webm"
//    compress(
//        fFmpegProbeResult,
//        VP8Encoder().encode(fFmpegProbeResult, Hardware.CPU, vp8CpuOutput, crf.toDouble()),
//        vp8CpuOutput,
//    )

//    println("H264 GPU - CRF: $crf")
//    compress(
//        fFmpegProbeResult,
//        H264Encoder().encode(fFmpegProbeResult, Hardware.INTEL, "output_h264_crf_$crf.mp4", crf.toDouble())
//    )

//    println("H265 CPU - CRF: $crf")
//    compress(
//        fFmpegProbeResult,
//        H265Encoder().encode(fFmpegProbeResult, Hardware.CPU, "output_cpu_h265_crf_$crf.mp4", crf.toDouble())
//    )
//    println("H265 GPU - CRF: $crf")
//    compress(
//        fFmpegProbeResult,
//        H265Encoder().encode(fFmpegProbeResult, Hardware.AMD, "output_amd_h265_crf_$crf.mp4", crf.toDouble())
//    )

//    println("VP9 CPU - CRF: $crf")
//    compress(
//        fFmpegProbeResult,
//        VP9Encoder().encode(fFmpegProbeResult, Hardware.CPU, "output_cpu_vp9_crf_$crf.mp4", crf.toDouble())
//    )
}