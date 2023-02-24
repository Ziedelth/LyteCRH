package fr.ziedelth.lytecrh.encoders

import fr.ziedelth.lytecrh.Hardware
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.probe.FFmpegProbeResult

interface Encoder {
    fun encode(fFmpegProbeResult: FFmpegProbeResult, hardware: Hardware = Hardware.CPU): FFmpegBuilder

    companion object {
        const val EXTENSION = "mp4"
        const val RESOLUTION = 1080
        const val AUDIO_CODEC = "copy"
        const val PIXEL_FORMAT = "yuv420p"
        const val CRF = 23.0
    }
}