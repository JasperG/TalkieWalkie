package com.jaspergoes.talkiewalkie.audio.codecs.opus;

import com.jaspergoes.talkiewalkie.audio.Configuration;

import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Pointer;

import java.util.Arrays;

public class OpusEncoder {

    private final int mFrameSize;
    public final byte[] mEncodeBuffer;

    private int mEncodedLength;

    private Pointer mState;

    public OpusEncoder(int sampleRate, int channels, int frameSize, int bitrate) throws NativeAudioException {

        mEncodeBuffer = new byte[frameSize];
        mFrameSize = frameSize;

        IntPointer error = new IntPointer(1);
        error.put(0);

        mState = Opus.opus_encoder_create(sampleRate, channels, Opus.OPUS_APPLICATION_VOIP, error);

        if (error.get() < 0)
            throw new NativeAudioException("Opus encoder initialization failed with error: " + error.get());

        if (Configuration.USE_FEC) {
            Opus.opus_encoder_ctl(mState, Opus.OPUS_SET_INBAND_FEC_REQUEST, 1);
        }

        Opus.opus_encoder_ctl(mState, Opus.OPUS_SET_VBR_REQUEST, 1);
        Opus.opus_encoder_ctl(mState, Opus.OPUS_SET_BITRATE_REQUEST, bitrate);

    }

    public int encode(short[] input, int inputSize) throws NativeAudioException {

        if (inputSize < mFrameSize) {
            // If encoding is performed before frameSize is filled, fill rest of packet.
            short[] buffer = new short[mFrameSize];
            System.arraycopy(input, 0, buffer, 0, inputSize);
            Arrays.fill(buffer, inputSize, mFrameSize, (short) 0);
            input = buffer;
        }

        int result = Opus.opus_encode(mState, input, mFrameSize, mEncodeBuffer, mEncodeBuffer.length);

        if (result < 0) {
            throw new NativeAudioException("Opus encoding failed with error: " + result);
        }

        return mEncodedLength = result;

    }

    public byte[] getEncodedData() {

        byte[] out = new byte[mEncodedLength];
        System.arraycopy(mEncodeBuffer, 0, out, 0, mEncodedLength);
        mEncodedLength = 0;

        return out;

    }

    public int getBitrate() {

        IntPointer ptr = new IntPointer(1);
        Opus.opus_encoder_ctl(mState, Opus.OPUS_GET_BITRATE_REQUEST, ptr);
        return ptr.get();

    }

    public void destroy() {
        Opus.opus_encoder_destroy(mState);
    }

}