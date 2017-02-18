package com.jaspergoes.talkiewalkie.audio;

import android.content.Context;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.jaspergoes.talkiewalkie.R;
import com.jaspergoes.talkiewalkie.audio.codecs.opus.NativeAudioException;
import com.jaspergoes.talkiewalkie.audio.codecs.opus.OpusEncoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;

public class Input {

    private static volatile boolean _die;
    private static ArrayList<byte[]> terminationSound = new ArrayList<byte[]>();

    public static void encodeTerminationSound(Context context) {

        terminationSound.clear();

        InputStream is = null;
        OpusEncoder encoder = null;

        try {

            is = context.getResources().openRawResource(R.raw.over);

            encoder = new OpusEncoder(Configuration.AUDIO_SAMPLERATE, Configuration.CHANNELS, Configuration.FRAME_SIZE, Configuration.BITRATE);

            ByteArrayOutputStream buffer = new ByteArrayOutputStream(1024);
            byte[] data = new byte[1024];
            int nRead, i;

            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();
            data = buffer.toByteArray();

            nRead = data.length / 2;
            short[] pcm = new short[nRead];
            for (i = 22; i < nRead; i++) {
                pcm[i] = (short) ((data[i * 2] & 0xff) | (data[i * 2 + 1] << 8));
            }

            nRead = pcm.length / Configuration.FRAME_SIZE;
            short[] pcm_frame_shorts = new short[Configuration.FRAME_SIZE];
            for (i = 0; i < nRead; i++) {

                System.arraycopy(pcm, i * Configuration.FRAME_SIZE, pcm_frame_shorts, 0, Configuration.FRAME_SIZE);

                if (encoder.encode(pcm_frame_shorts, Configuration.FRAME_SIZE) > 0) {
                    terminationSound.add(encoder.getEncodedData());
                }

            }

        } catch (NativeAudioException e) {

            terminationSound.clear();
            e.printStackTrace();

        } catch (IOException e) {

            terminationSound.clear();
            e.printStackTrace();

        } finally {

            if (is != null) {

                try {

                    is.close();

                } catch (IOException e) {

                    e.printStackTrace();

                }

            }

            if (encoder != null) {

                encoder.destroy();

            }

        }

    }

    public static void start() {

        _die = false;

        final int bufferSize = ((AudioRecord.getMinBufferSize(Configuration.AUDIO_SAMPLERATE, Configuration.AUDIO_CHANNEL_IN, Configuration.AUDIO_FORMAT) / Configuration.FRAME_SIZE) + 1) * Configuration.FRAME_SIZE;

        final AudioRecord mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, Configuration.AUDIO_SAMPLERATE, Configuration.AUDIO_CHANNEL_IN, Configuration.AUDIO_FORMAT, bufferSize);

        if (mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {

            mAudioRecord.startRecording();

            new Thread(new Runnable() {

                OpusEncoder encoder;

                @Override
                public void run() {

                    InetAddress target = Connection.target;

                    if (encoder == null) {

                        try {

                            encoder = new OpusEncoder(Configuration.AUDIO_SAMPLERATE, Configuration.CHANNELS, Configuration.FRAME_SIZE, Configuration.BITRATE);

                        } catch (NativeAudioException e) {

                            e.printStackTrace();
                            _die = true;

                        }

                    }

                    short[] pcm_audio_shorts = new short[bufferSize];
                    short[] pcm_frame_shorts = new short[Configuration.FRAME_SIZE];

                    byte[] opus_audio_bytes;
                    int opus_audio_bytes_encoded;

                    int i;

                    int noOnce = 0;

                    int bytes_read;
                    int byte_queue;
                    int bytes_sent;

                    while (!_die) {

                        /* Fill audio buffer */
                        bytes_read = mAudioRecord.read(pcm_audio_shorts, 0, bufferSize);

                        for (i = 0; i < bytes_read / Configuration.FRAME_SIZE; i++) {

                            System.arraycopy(pcm_audio_shorts, i * Configuration.FRAME_SIZE, pcm_frame_shorts, 0, Configuration.FRAME_SIZE);

                            try {

                                if ((opus_audio_bytes_encoded = encoder.encode(pcm_frame_shorts, Configuration.FRAME_SIZE)) > 0) {

                                    opus_audio_bytes = encoder.getEncodedData();

                                    /* Send audio buffer */
                                    try {

                                        bytes_sent = 0;

                                        while ((byte_queue = Math.min(444, opus_audio_bytes_encoded - bytes_sent)) > 0) {

                                            byte[] data = new byte[byte_queue + 64];

                                            data[0] = (byte) noOnce;
                                            noOnce = noOnce + 1 % 256;

                                            System.arraycopy(opus_audio_bytes, bytes_sent, data, 64, byte_queue);

                                            //Log.e("SENDING", Integer.toString(data.length) + " bytes");

                                            Connection.socket.send(new DatagramPacket(data, 0, data.length, target, 8200));

                                            bytes_sent += byte_queue;

                                        }

                                    } catch (IOException e) {

                                        _die = true;

                                    }

                                    Connection.setSent();

                                }

                            } catch (NativeAudioException e) {

                                e.printStackTrace();

                            }

                        }

                    }

                    /* Send *Over* */
                    for (i = 0; i < terminationSound.size(); i++) {

                        opus_audio_bytes = terminationSound.get(i);
                        opus_audio_bytes_encoded = opus_audio_bytes.length;

                        /* Send *terminationSound* buffer */
                        try {

                            bytes_sent = 0;

                            while ((byte_queue = Math.min(444, opus_audio_bytes_encoded - bytes_sent)) > 0) {

                                byte[] data = new byte[byte_queue + 64];

                                data[0] = (byte) noOnce;
                                noOnce = noOnce + 1 % 256;

                                System.arraycopy(opus_audio_bytes, bytes_sent, data, 64, byte_queue);

                                //Log.e("SENDING (OVER SOUND)", Integer.toString(data.length) + " bytes");

                                Connection.socket.send(new DatagramPacket(data, 0, data.length, target, 8200));

                                bytes_sent += byte_queue;

                            }

                        } catch (IOException e) {

                            _die = true;

                        }

                    }

                    Connection.setSent();

                    //encoder.destroy();

                    mAudioRecord.stop();
                    mAudioRecord.release();

                }

            }).start();

        }

    }

    public static void stop() {

        new Thread(new Runnable() {

            @Override
            public void run() {

                try {

                    Thread.sleep(120);

                } catch (InterruptedException e) {

                    e.printStackTrace();

                }

                _die = true;

            }

        }).start();

    }

}
