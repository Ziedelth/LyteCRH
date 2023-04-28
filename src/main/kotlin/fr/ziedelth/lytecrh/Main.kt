package fr.ziedelth.lytecrh

import fr.ziedelth.lytecrh.encoders.H264Encoder
import fr.ziedelth.lytecrh.utils.VideoUtil
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFprobe
import java.io.File

fun drawProgressbar(currentTime: String, progress: Double, remainingTime: String, speed: Double, length: Int = 50) {
    val copyProgressBar = " ".repeat(length).toCharArray()
    val currentPosition = (copyProgressBar.size * progress).toInt()
    (0 until currentPosition).forEach { copyProgressBar[it] = '•' }
    val str = "$currentTime\t|${copyProgressBar.joinToString("")}|\t${
        String.format(
            "%.2f",
            progress * 100
        )
    }%\t~$remainingTime (${String.format("%.2f", speed)}x)"
    print("\b".repeat(str.length) + str)
}

fun main() {
    val fFmpeg = FFmpeg()
    val fFprobe = FFprobe()

    val crf = 30
    val fFmpegProbeResult = fFprobe.probe("input.mp4")

    val folder = File("output")
    if (!folder.exists()) folder.mkdirs()

    println("H264 CPU - CRF: $crf")
    val h264CpuOutput = "video.mp4"

    VideoUtil.compress(
        fFmpeg,
        fFprobe,
        fFmpegProbeResult,
        H264Encoder().encode(fFmpegProbeResult, Hardware.AMD, h264CpuOutput, crf.toDouble()),
        h264CpuOutput,
        7_000,
    )
}