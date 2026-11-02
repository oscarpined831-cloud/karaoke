package com.example.dsp

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DecodedAudio(
    val leftChannel: FloatArray,
    val rightChannel: FloatArray,
    val sampleRate: Int,
    val durationSec: Float
)

/**
 * AudioFileDecoder: Decodes any standard Android-supported audio format
 * (MP3, WAV, FLAC, M4A, OGG, AAC) into normalized 32-bit floating-point PCM.
 */
object AudioFileDecoder {
    private const val TAG = "AudioFileDecoder"

    suspend fun decodeUri(context: Context, uri: Uri): DecodedAudio? = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null

        try {
            extractor.setDataSource(context, uri, null)
            var audioTrackIndex = -1
            var format: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val trackFormat = extractor.getTrackFormat(i)
                val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = trackFormat
                    break
                }
            }

            if (audioTrackIndex == -1 || format == null) {
                Log.e(TAG, "No audio track found in selected file")
                return@withContext null
            }

            extractor.selectTrack(audioTrackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME)!!
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val pcmFloatsL = ArrayList<Float>(sampleRate * 60)
            val pcmFloatsR = ArrayList<Float>(sampleRate * 60)

            val info = MediaCodec.BufferInfo()
            var isEos = false

            val maxSamples = sampleRate * 300 // Max 5 minutes to avoid memory exhaustion

            while (!isEos && pcmFloatsL.size < maxSamples) {
                val inIndex = codec.dequeueInputBuffer(10_000L)
                if (inIndex >= 0) {
                    val inBuffer = codec.getInputBuffer(inIndex)!!
                    val sampleSize = extractor.readSampleData(inBuffer, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isEos = true
                    } else {
                        codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }

                var outIndex = codec.dequeueOutputBuffer(info, 10_000L)
                while (outIndex >= 0) {
                    val outBuffer = codec.getOutputBuffer(outIndex)!!
                    outBuffer.order(ByteOrder.LITTLE_ENDIAN)

                    val shortCount = info.size / 2
                    for (i in 0 until shortCount step channelCount) {
                        val sample16L = outBuffer.getShort(info.offset + i * 2)
                        val floatL = sample16L / 32768.0f
                        val floatR = if (channelCount > 1) {
                            val sample16R = outBuffer.getShort(info.offset + (i + 1) * 2)
                            sample16R / 32768.0f
                        } else {
                            floatL
                        }

                        pcmFloatsL.add(floatL)
                        pcmFloatsR.add(floatR)

                        if (pcmFloatsL.size >= maxSamples) break
                    }

                    codec.releaseOutputBuffer(outIndex, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isEos = true
                        break
                    }
                    outIndex = codec.dequeueOutputBuffer(info, 0)
                }
            }

            val leftArray = FloatArray(pcmFloatsL.size)
            val rightArray = FloatArray(pcmFloatsR.size)
            for (i in pcmFloatsL.indices) {
                leftArray[i] = pcmFloatsL[i]
                rightArray[i] = pcmFloatsR[i]
            }

            val durationSec = leftArray.size.toFloat() / sampleRate
            Log.d(TAG, "Decoded ${leftArray.size} samples ($durationSec sec, $sampleRate Hz)")

            DecodedAudio(
                leftChannel = leftArray,
                rightChannel = rightArray,
                sampleRate = sampleRate,
                durationSec = durationSec
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding audio file", e)
            null
        } finally {
            codec?.stop()
            codec?.release()
            extractor.release()
        }
    }
}
