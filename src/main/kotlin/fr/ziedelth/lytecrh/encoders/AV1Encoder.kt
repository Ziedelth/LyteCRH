package fr.ziedelth.lytecrh.encoders

import fr.ziedelth.lytecrh.Hardware
import fr.ziedelth.lytecrh.encoders.Encoder.Companion.AUDIO_BITRATE
import fr.ziedelth.lytecrh.encoders.Encoder.Companion.AUDIO_CODEC
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.probe.FFmpegProbeResult

class AV1Encoder : Encoder {
    override fun encode(fFmpegProbeResult: FFmpegProbeResult, hardware: Hardware, output: String, crf: Double): FFmpegBuilder {
        return when (hardware) {
            Hardware.INTEL -> FFmpegBuilder()
                .addExtraArgs("-hwaccel", "qsv", "-init_hw_device", "qsv=hw", "-hwaccel_output_format", "qsv")
                .setInput(fFmpegProbeResult)
                .overrideOutputFiles(true)
                .addOutput(output)
                .setVideoCodec("av1_qsv")
                .setConstantRateFactor(crf)
                .setAudioCodec(AUDIO_CODEC)
                .setAudioBitRate(AUDIO_BITRATE)
                .done()

            else -> FFmpegBuilder()
                .setInput(fFmpegProbeResult)
                .overrideOutputFiles(true)
                .addOutput(output)
                .setVideoCodec("libaom-av1")
                .setConstantRateFactor(crf)
                .setAudioCodec(AUDIO_CODEC)
                .setAudioBitRate(AUDIO_BITRATE)
                .done()
        }
    }
}