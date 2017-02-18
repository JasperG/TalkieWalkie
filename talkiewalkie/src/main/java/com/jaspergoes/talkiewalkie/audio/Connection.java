package com.jaspergoes.talkiewalkie.audio;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Connection {

    public static DatagramSocket socket;

    public static InetAddress target;

    public static long sent = 0;

    public static void setSent() {
        sent = System.currentTimeMillis() + Configuration.KEEPALIVE;
    }

    static {

        try {

            target = InetAddress.getByName("www.locationof.com");

        } catch (UnknownHostException e) {

            e.printStackTrace();

        }

    }

}
