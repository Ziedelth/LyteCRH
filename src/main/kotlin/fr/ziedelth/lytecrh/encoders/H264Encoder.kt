package fr.ziedelth.lytecrh.encoders

import fr.ziedelth.lytecrh.Hardware
import fr.ziedelth.lytecrh.encoders.Encoder.Companion.AUDIO_BITRATE
import fr.ziedelth.lytecrh.encoders.Encoder.Companion.AUDIO_CODEC
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.probe.FFmpegProbeResult

class H264Encoder : Encoder {
    override fun encode(fFmpegProbeResult: FFmpegProbeResult, hardware: Hardware, output: String, crf: Double): FFmpegBuilder {
        return when (hardware) {
            Hardware.AMD -> FFmpegBuilder()
                .setInput(fFmpegProbeResult)
                .overrideOutputFiles(true)
                .addOutput(output)
                .setVideoCodec("h264_amf")
                .setConstantRateFactor(crf)
                .setAudioCodec(AUDIO_CODEC)
                .setAudioBitRate(AUDIO_BITRATE)
                .done()

            else -> FFmpegBuilder()
                .setInput(fFmpegProbeResult)
                .overrideOutputFiles(true)
                .addOutput(output)
                .setVideoCodec("libx264")
                .setConstantRateFactor(crf)
                .setAudioCodec(AUDIO_CODEC)
                .setAudioBitRate(AUDIO_BITRATE)
                .done()
        }
    }
}