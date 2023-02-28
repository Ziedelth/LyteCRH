package fr.ziedelth.lytecrh.encoders

import fr.ziedelth.lytecrh.Hardware
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.probe.FFmpegProbeResult

interface Encoder {
    fun encode(fFmpegProbeResult: FFmpegProbeResult, hardware: Hardware = Hardware.CPU, output: String, crf: Double = 23.0): FFmpegBuilder

    companion object {
        const val AUDIO_CODEC = "aac"
        const val AUDIO_BITRATE = 128 * 1000L
    }
}