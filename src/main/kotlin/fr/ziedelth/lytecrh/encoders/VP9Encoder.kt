package fr.ziedelth.lytecrh.encoders

import fr.ziedelth.lytecrh.Hardware
import fr.ziedelth.lytecrh.encoders.Encoder.Companion.AUDIO_CODEC
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.probe.FFmpegProbeResult

class VP9Encoder : Encoder {
    override fun encode(fFmpegProbeResult: FFmpegProbeResult, hardware: Hardware, output: String, crf: Double): FFmpegBuilder {
        return FFmpegBuilder()
            .setInput(fFmpegProbeResult)
            .overrideOutputFiles(true)
            .addOutput(output)
            .setVideoCodec("libvpx-vp9")
            .setConstantRateFactor(crf)
            .setAudioCodec(AUDIO_CODEC)
//            .setAudioBitRate(AUDIO_BITRATE)
            .done()
    }
}