package com.jaspergoes.talkiewalkie.audio.codecs.opus;

import com.jaspergoes.talkiewalkie.audio.Configuration;

import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Pointer;

public class OpusDecoder {

    private Pointer mState;

    public OpusDecoder(int sampleRate, int channels) throws NativeAudioException {
        IntPointer error = new IntPointer(1);
        error.put(0);
        mState = Opus.opus_decoder_create(sampleRate, channels, error);
        if (error.get() < 0)
            throw new NativeAudioException("Opus decoder initialization failed with error: " + error.get());
    }

    /*public int decodeFloat(ByteBuffer input, int inputSize, float[] output, int frameSize) throws NativeAudioException {
        int result = Opus.opus_decode_float(mState, input, inputSize, output, frameSize, 0);
        if (result < 0)
            throw new NativeAudioException("Opus decoding failed with error: " + result);
        return result;
    }*/

    public int decodeShort(byte[] input, int inputSize, short[] output, int frameSize) throws NativeAudioException {
        int result = Opus.opus_decode(mState, input, inputSize, output, frameSize, Configuration.USE_FEC ? 1 : 0);
        if (result < 0)
            throw new NativeAudioException("Opus decoding failed with error: " + result);
        return result;
    }

    public void destroy() {
        Opus.opus_decoder_destroy(mState);
    }

}