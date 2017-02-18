package com.jaspergoes.talkiewalkie.audio;

import java.util.Comparator;

public class TalkWalkPacketComparator implements Comparator<TalkWalkPacket> {

    @Override
    public int compare(TalkWalkPacket packet0, TalkWalkPacket packet1) {

        if (packet0.num > 192 && packet1.num < 64) return (packet0.num < packet1.num) ? 1 : -1;
        if (packet0.num < 64 && packet1.num > 192) return (packet0.num < packet1.num) ? 1 : -1;
        return (packet0.num < packet1.num) ? -1 : 1;

    }

}
