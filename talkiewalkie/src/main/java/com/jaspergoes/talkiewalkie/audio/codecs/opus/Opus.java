package com.jaspergoes.talkiewalkie.audio.codecs.opus;

import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.Cast;
import org.bytedeco.javacpp.annotation.Platform;

@Platform(library = "opus", cinclude = {"<opus.h>", "<opus_types.h>"})
public class Opus {

    static final int OPUS_APPLICATION_VOIP = 2048;
    static final int OPUS_SET_BITRATE_REQUEST = 4002;
    static final int OPUS_GET_BITRATE_REQUEST = 4003;
    static final int OPUS_SET_VBR_REQUEST = 4006;
    static final int OPUS_SET_INBAND_FEC_REQUEST = 4012;

    /* Encoder */

    public static native Pointer opus_encoder_create(int sampleRate, int channels, int application, IntPointer error);

    public static native int opus_encoder_ctl(@Cast("OpusEncoder*") Pointer st, int request, Pointer value);

    public static native int opus_encoder_ctl(@Cast("OpusEncoder*") Pointer st, int request, int value);

    public static native int opus_encode(@Cast("OpusEncoder*") Pointer st, @Cast("const short*") short[] pcm, int frameSize, @Cast("unsigned char*") byte[] dataBytes, int maxDataBytes);

    public static native void opus_encoder_destroy(@Cast("OpusEncoder*") Pointer st);

    /* Decoder */

    public static native Pointer opus_decoder_create(int fs, int channels, IntPointer error);

    public static native int opus_decode(@Cast("OpusDecoder*") Pointer st, @Cast("const unsigned char*") byte[] data, int len, short[] out, int frameSize, int decodeFec);

    public static native void opus_decoder_destroy(@Cast("OpusDecoder*") Pointer st);

}