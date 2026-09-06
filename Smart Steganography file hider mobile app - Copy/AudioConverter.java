package com.example.smartstego;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

/**
 * Yeh class kisi bhi audio format (jaise MP3) ko WAV format mein badalti hai.
 * Steganography ke liye WAV format isliye zaroori hai kyunke yeh lossless hota hai.
 */
public class AudioConverter {

    // Kaam khatam hone par result dene ke liye interface
    public interface ConversionCallback {
        void onSuccess(File convertedFile);
        void onFailure(Exception e);
    }

    // Audio conversion ko background thread mein shuru karne wala function
    public static void convertToWav(Context context, Uri inputUri, ConversionCallback callback) {
        new Thread(() -> {
            File outFile = new File(context.getCacheDir(), "converted_" + System.currentTimeMillis() + ".wav");
            try {
                decodeAudioToWav(context, inputUri, outFile);
                callback.onSuccess(outFile);
            } catch (Exception e) {
                if (outFile.exists()) outFile.delete();
                Log.e("AudioConverter", "Conversion failed", e);
                callback.onFailure(e);
            }
        }).start();
    }

    // Asli logic yahan hai: Audio ko decode kar ke raw data nikalna
    private static void decodeAudioToWav(Context context, Uri uri, File outFile) throws IOException {
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(context, uri, null);

        int trackIndex = -1;
        // Audio track dhoond rahe hain file ke andar
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith("audio/")) {
                trackIndex = i;
                break;
            }
        }

        if (trackIndex < 0) throw new IOException("Koi audio track nahi mila");

        extractor.selectTrack(trackIndex);
        MediaFormat inputFormat = extractor.getTrackFormat(trackIndex);
        
        // Original properties le rahe hain taakay WAV sahi banay
        int sampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channelCount = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        
        String mime = inputFormat.getString(MediaFormat.KEY_MIME);
        MediaCodec codec = MediaCodec.createDecoderByType(mime);
        codec.configure(inputFormat, null, null, 0);
        codec.start();

        FileOutputStream fos = new FileOutputStream(outFile);
        // WAV header ke liye 44 bytes chhor rahe hain, baad mein fill karenge
        fos.write(new byte[44]);

        ByteBuffer[] inputBuffers = codec.getInputBuffers();
        ByteBuffer[] outputBuffers = codec.getOutputBuffers();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        
        boolean isEOS = false;
        long totalAudioLen = 0;

        // Data processing loop
        while (!isEOS) {
            int inIndex = codec.dequeueInputBuffer(10000);
            if (inIndex >= 0) {
                ByteBuffer buffer = inputBuffers[inIndex];
                int sampleSize = extractor.readSampleData(buffer, 0);
                if (sampleSize < 0) {
                    codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    isEOS = true;
                } else {
                    codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                    extractor.advance();
                }
            }

            int outIndex = codec.dequeueOutputBuffer(info, 10000);
            while (outIndex >= 0) {
                ByteBuffer buffer = outputBuffers[outIndex];
                byte[] chunk = new byte[info.size];
                buffer.get(chunk);
                buffer.clear();
                fos.write(chunk); // Raw PCM data likh rahe hain
                totalAudioLen += info.size;
                codec.releaseOutputBuffer(outIndex, false);
                outIndex = codec.dequeueOutputBuffer(info, 10000);
            }
        }

        codec.stop();
        codec.release();
        extractor.release();
        fos.close();

        // Ab 44 bytes wala header sahi metadata ke sath fill kar rahe hain
        updateWavHeader(outFile, totalAudioLen, sampleRate, channelCount);
    }

    // WAV format ka standard header banane wala function
    private static void updateWavHeader(File wavFile, long totalAudioLen, int sampleRate, int channels) throws IOException {
        long totalDataLen = totalAudioLen + 36;
        long byteRate = (long) sampleRate * channels * 16 / 8;

        RandomAccessFile raf = new RandomAccessFile(wavFile, "rw");
        raf.seek(0); // File ke shuru mein wapis jana
        raf.writeBytes("RIFF");
        raf.write(intToByteArray((int) totalDataLen), 0, 4);
        raf.writeBytes("WAVE");
        raf.writeBytes("fmt ");
        raf.write(intToByteArray(16), 0, 4);
        raf.write(shortToByteArray((short) 1), 0, 2); // 1 ka matlab hai PCM (Raw)
        raf.write(shortToByteArray((short) channels), 0, 2);
        raf.write(intToByteArray(sampleRate), 0, 4);
        raf.write(intToByteArray((int) byteRate), 0, 4);
        raf.write(shortToByteArray((short) (channels * 16 / 8)), 0, 2);
        raf.write(shortToByteArray((short) 16), 0, 2);
        raf.writeBytes("data");
        raf.write(intToByteArray((int) totalAudioLen), 0, 4);
        raf.close();
    }

    // Int value ko byte array mein badalne ke liye helper (WAV requirements)
    private static byte[] intToByteArray(int i) {
        return new byte[]{(byte) (i & 0xff), (byte) ((i >> 8) & 0xff), (byte) ((i >> 16) & 0xff), (byte) ((i >> 24) & 0xff)};
    }

    // Short value ko byte array mein badalne ke liye helper
    private static byte[] shortToByteArray(short i) {
        return new byte[]{(byte) (i & 0xff), (byte) ((i >> 8) & 0xff)};
    }
}
