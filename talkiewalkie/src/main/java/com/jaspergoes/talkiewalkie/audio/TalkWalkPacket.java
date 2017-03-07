package com.jaspergoes.talkiewalkie.audio;

/**
 * Created by Jasper on 18-2-2017.
 */

public class TalkWalkPacket {

    public TalkWalkPacket(int num, int ident, byte[] data) {
        this.num = num;
        this.ident = ident;
        this.data = data;
    }

    public byte[] data;
    public int num;
    public int ident;

}
