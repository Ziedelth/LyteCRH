package fr.ziedelth.lytecrh.encoders

import fr.ziedelth.lytecrh.Hardware
import fr.ziedelth.lytecrh.encoders.Encoder.Companion.AUDIO_BITRATE
import fr.ziedelth.lytecrh.encoders.Encoder.Companion.AUDIO_CODEC
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.probe.FFmpegProbeResult

class VP8Encoder : Encoder {
    override fun encode(fFmpegProbeResult: FFmpegProbeResult, hardware: Hardware, output: String, crf: Double): FFmpegBuilder {
        val finalExtension = output.substringAfterLast(".").lowercase()

        if (finalExtension == "m3u8") {
            return FFmpegBuilder()
                .setInput(fFmpegProbeResult)
                .overrideOutputFiles(true)
                .addOutput(output)
                .addExtraArgs("-start_number", "0")
                .addExtraArgs("-hls_time", "10")
                .addExtraArgs("-hls_list_size", "0")
                .setVideoCodec("libvpx")
                .setConstantRateFactor(crf)
                .setAudioCodec(AUDIO_CODEC)
                .setAudioBitRate(AUDIO_BITRATE)
                .done()
        }

        return FFmpegBuilder()
            .setInput(fFmpegProbeResult)
            .overrideOutputFiles(true)
            .addOutput(output)
            .setVideoCodec("libvpx")
            .setConstantRateFactor(crf)
            .setAudioCodec(AUDIO_CODEC)
            .setAudioBitRate(AUDIO_BITRATE)
            .done()
    }
}