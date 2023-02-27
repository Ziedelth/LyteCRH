package fr.ziedelth.lytecrh.encoders

import fr.ziedelth.lytecrh.Hardware
import fr.ziedelth.lytecrh.encoders.Encoder.Companion.AUDIO_BITRATE
import fr.ziedelth.lytecrh.encoders.Encoder.Companion.AUDIO_CODEC
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.probe.FFmpegProbeResult

class H265Encoder : Encoder {
    override fun encode(fFmpegProbeResult: FFmpegProbeResult, hardware: Hardware, output: String, crf: Double): FFmpegBuilder {
        return when (hardware) {
            Hardware.AMD -> FFmpegBuilder()
                .setInput(fFmpegProbeResult)
                .overrideOutputFiles(true)
                .addOutput(output)
                .setVideoCodec("hevc_amf")
                .addExtraArgs("-rc", "cqp", "-qp_i", crf.toInt().toString(), "-qp_p", crf.toInt().toString())
                .setAudioCodec(AUDIO_CODEC)
                .setAudioBitRate(AUDIO_BITRATE)
                .done()

            Hardware.INTEL -> FFmpegBuilder()
                .setInput(fFmpegProbeResult)
                .overrideOutputFiles(true)
                .addOutput(output)
                .setVideoCodec("hevc_qsv")
                .addExtraArgs("-global_quality", crf.toInt().toString())
                .addExtraArgs("-look_ahead", "1")
                .setAudioCodec(AUDIO_CODEC)
                .setAudioBitRate(AUDIO_BITRATE)
                .done()

            else -> FFmpegBuilder()
                .setInput(fFmpegProbeResult)
                .overrideOutputFiles(true)
                .addOutput(output)
                .setVideoCodec("libx265")
                .setConstantRateFactor(crf)
                .setAudioCodec(AUDIO_CODEC)
                .setAudioBitRate(AUDIO_BITRATE)
                .done()
        }
    }
}