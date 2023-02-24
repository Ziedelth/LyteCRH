package fr.ziedelth.lytecrh.encoders

import fr.ziedelth.lytecrh.Hardware
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.probe.FFmpegProbeResult

interface Encoder {
    fun encode(fFmpegProbeResult: FFmpegProbeResult, hardware: Hardware = Hardware.CPU, output: String = "output.$EXTENSION"): FFmpegBuilder

    companion object {
        const val EXTENSION = "mp4"
        const val RESOLUTION = 1080
        const val CRF = 23.0
        const val AUDIO_CODEC = "aac"
        const val AUDIO_BITRATE = 128 * 1000L
    }
}