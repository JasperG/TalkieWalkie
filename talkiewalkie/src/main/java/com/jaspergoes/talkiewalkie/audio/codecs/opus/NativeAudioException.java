package com.jaspergoes.talkiewalkie.audio.codecs.opus;

public class NativeAudioException extends Exception {

    public NativeAudioException(String message) {
        super(message);
    }

    public NativeAudioException(Throwable throwable) {
        super(throwable);
    }

    public NativeAudioException(String message, Throwable throwable) {
        super(message, throwable);
    }

}