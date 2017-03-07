package com.jaspergoes.talkiewalkie.audio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import com.jaspergoes.talkiewalkie.audio.codecs.opus.NativeAudioException;
import com.jaspergoes.talkiewalkie.audio.codecs.opus.OpusDecoder;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.PriorityQueue;

public class Output {

    private static volatile boolean _die;

    public static short identcode;

    private static byte[] keepAlivePacket;

    private static BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals("android.net.conn.CONNECTIVITY_CHANGE")) {

                if (Connection.socket != null) {

                    new Thread(new Runnable() {

                        @Override
                        public void run() {

                            try {


                                Connection.socket.send(new DatagramPacket(keepAlivePacket, 4, Connection.target, 8200));

                                Connection.setSent();

                            } catch (IOException e) {

                                e.printStackTrace();

                            }

                        }

                    }).start();

                }
            }

        }

    };

    public static void start(Context context) {

        context.registerReceiver(receiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));

        identcode = (short) (Math.random() * 65535);

        keepAlivePacket = new byte[]{(byte) 100, (byte) 64, (byte) ((Output.identcode >> 8) & 0xFF), (byte) (Output.identcode & 0xFF)};

        _die = false;

        final int bufferSize = AudioTrack.getMinBufferSize(Configuration.AUDIO_SAMPLERATE, Configuration.AUDIO_CHANNEL_OUT, Configuration.AUDIO_FORMAT);
        final PriorityQueue<TalkWalkPacket> queue = new PriorityQueue<TalkWalkPacket>(10, new TalkWalkPacketComparator());

        final int queueSize = 6; /* Wait (queueSize * (FRAME_SIZE / AUDIO_SAMPLERATE)) seconds */

        final Object lock = new Object();

        /* Receiving thread */
        new Thread(new Runnable() {

            @Override
            public void run() {

                try {

                    Connection.socket = new DatagramSocket(null);
                    Connection.socket.setSoTimeout(1000);

                } catch (SocketException e) {

                    _die = true;

                }

                InetAddress target = Connection.target;

                byte[] buffer = new byte[512];
                DatagramPacket packet = new DatagramPacket(buffer, 512);

                int i;
                int len;
                byte[] totaldata;
                byte[] data;

                while (!_die) {

                    innerLoop:
                    while (!_die) {

                        try {

                            if (Connection.sent < System.currentTimeMillis()) {

                                Connection.socket.send(new DatagramPacket(keepAlivePacket, 4, target, 8200));

                                Connection.setSent();

                            }

                        } catch (IOException e) {

                            e.printStackTrace();

                        }

                        i = 0;

                        do {

                            try {

                                Connection.socket.receive(packet);

                                totaldata = packet.getData();

                                len = packet.getLength() - 64;
                                data = new byte[len];
                                System.arraycopy(totaldata, 64, data, 0, len);

                                queue.add(new TalkWalkPacket(totaldata[0] & 0xff, ((totaldata[2] & 0xFF) << 8 | totaldata[3] & 0xFF), data));

                                if (i == queueSize) synchronized (lock) {
                                    lock.notify();
                                }

                            } catch (SocketTimeoutException e) {

                                break innerLoop;

                            } catch (IOException e) {

                                break innerLoop;

                            }

                        } while (++i < 500);

                    }

                    /* Give cpu time to rest */
                    try {

                        Thread.sleep(200);

                    } catch (InterruptedException e) {

                        e.printStackTrace();

                    }

                }

            }

        }).start();

        /* Playback thread */
        new Thread(new Runnable() {

            @Override
            public void run() {

                try {

                    OpusDecoder decoder = new OpusDecoder(Configuration.AUDIO_SAMPLERATE, Configuration.CHANNELS);

                    AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, Configuration.AUDIO_SAMPLERATE, Configuration.AUDIO_CHANNEL_OUT, Configuration.AUDIO_FORMAT, bufferSize, AudioTrack.MODE_STREAM);

                    TalkWalkPacket handlePack;

                    short[] pcmOut = new short[Configuration.FRAME_SIZE];
                    int pcmOutLength;

                    while (!_die) {

                        try {

                            synchronized (lock) {

                                lock.wait(200);

                            }

                        } catch (InterruptedException e) {

                            e.printStackTrace();

                        }

                        if (queue.size() >= queueSize) {

                            try {

                                handlePack = queue.remove();
                                pcmOutLength = decoder.decodeShort(handlePack.data, handlePack.data.length, pcmOut, Configuration.FRAME_SIZE);
                                track.write(pcmOut, 0, pcmOutLength);
                                Log.e("OHJA", Integer.toString(handlePack.ident));
                                track.play();

                            } catch (NativeAudioException e) {

                                e.printStackTrace();
                                _die = true;

                            }

                            if (!_die) do {

                                try {

                                    handlePack = queue.remove();
                                    pcmOutLength = decoder.decodeShort(handlePack.data, handlePack.data.length, pcmOut, Configuration.FRAME_SIZE);
                                    track.write(pcmOut, 0, pcmOutLength);
                                    Log.e("OHJA", Integer.toString(handlePack.ident));

                                } catch (NativeAudioException e) {

                                    e.printStackTrace();
                                    _die = true;
                                    break;

                                }

                            } while (!queue.isEmpty());

                            track.write(new short[bufferSize], 0, bufferSize);
                            track.stop();
                            track.flush();

                        }

                    }

                    track.stop();
                    track.flush();
                    track.release();

                    decoder.destroy();

                } catch (NativeAudioException e) {

                    e.printStackTrace();
                    _die = true;

                }

            }

        }).start();

    }

    public static void stop() {

        _die = true;

    }

}
