package fr.ziedelth.lytecrh.encoders

import fr.ziedelth.lytecrh.Hardware
import fr.ziedelth.lytecrh.encoders.Encoder.Companion.AUDIO_BITRATE
import fr.ziedelth.lytecrh.encoders.Encoder.Companion.AUDIO_CODEC
import fr.ziedelth.lytecrh.encoders.Encoder.Companion.CRF
import fr.ziedelth.lytecrh.encoders.Encoder.Companion.RESOLUTION
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.probe.FFmpegProbeResult

class AV1Encoder : Encoder {
    override fun encode(fFmpegProbeResult: FFmpegProbeResult, hardware: Hardware, output: String): FFmpegBuilder {
        return when (hardware) {
            Hardware.INTEL -> FFmpegBuilder()
                .addExtraArgs("-hwaccel", "qsv", "-init_hw_device", "qsv=hw", "-hwaccel_output_format", "qsv")
                .setInput(fFmpegProbeResult)
                .overrideOutputFiles(true)
                .addOutput(output)
                .setVideoCodec("av1_qsv")
                .setVideoFilter("\"scale_qsv=w=-1:h=$RESOLUTION\"")
                .setConstantRateFactor(CRF)
                .setAudioCodec(AUDIO_CODEC)
                .setAudioBitRate(AUDIO_BITRATE)
                .done()

            else -> FFmpegBuilder()
                .setInput(fFmpegProbeResult)
                .overrideOutputFiles(true)
                .addOutput(output)
                .setVideoCodec("libaom-av1")
                .setVideoFilter("scale=-1:$RESOLUTION")
                .setConstantRateFactor(CRF)
                .setAudioCodec(AUDIO_CODEC)
                .setAudioBitRate(AUDIO_BITRATE)
                .done()
        }
    }
}