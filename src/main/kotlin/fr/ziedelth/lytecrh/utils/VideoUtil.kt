package fr.ziedelth.lytecrh.utils

import fr.ziedelth.lytecrh.drawProgressbar
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

object VideoUtil {
    fun FFmpegProbeResult.getTotalBitrate(): Long {
        val videoStream = this.getStreams().find { it.codec_type == FFmpegStream.CodecType.VIDEO }
        val videoBitRate = videoStream!!.bit_rate
        val audioStream = this.getStreams().find { it.codec_type == FFmpegStream.CodecType.AUDIO }
        val audioBitrate = audioStream!!.bit_rate
        return videoBitRate + audioBitrate
    }

    fun calculateBandwidth(durationInSeconds: Double, views: Long, bitrateInKbps: Double) =
        durationInSeconds * views * bitrateInKbps / 8.0

    fun compress(
        fFmpeg: FFmpeg,
        fFprobe: FFprobe,
        fFmpegProbeResult: FFmpegProbeResult,
        fFmpegBuilder: FFmpegBuilder,
        filename: String,
        views: Long? = null
    ) {
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
            val totalSizeDouble = if (isInsideAFolder && filename.substringAfterLast(".").lowercase() == "m3u8") File(
                foldername
            ).calculateFolderSize().toDouble() else progress.total_size.toDouble()
            val diff = (toDouble - totalSizeDouble) / toDouble * 100.0

            val originalBitrateToDouble = fFmpegProbeResult.getTotalBitrate().toDouble()
            val newBitrateToDouble = progress.bitrate.toDouble()

            println()
            println()
            println("Size: ${toDouble.toHumanReadable()} → ${totalSizeDouble.toHumanReadable()} (${diff.toSign()}%)")

            if (!isInsideAFolder) {
                val oldBitrateInKbps = originalBitrateToDouble * 0.001
                val newBitrateInKbps = newBitrateToDouble * 0.001
                println(
                    "Bitrate: ${String.format("%.2f", oldBitrateInKbps)} kbps → ${
                        String.format(
                            "%.2f",
                            newBitrateInKbps
                        )
                    } kbps"
                )

                if (views != null) {
                    val oldBandwidth =
                        calculateBandwidth(fFmpegProbeResult.format.duration, views, oldBitrateInKbps) // In kbps
                    val newBandwidth =
                        calculateBandwidth(fFmpegProbeResult.format.duration, views, newBitrateInKbps) // In kbps
                    println(
                        "Bandwidth: ${
                            String.format(
                                "%.2f",
                                oldBandwidth / (1000 * 1000 * 1000)
                            )
                        } To/day → ${String.format("%.2f", newBandwidth / (1000 * 1000 * 1000))} To/day"
                    )
                }

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
}