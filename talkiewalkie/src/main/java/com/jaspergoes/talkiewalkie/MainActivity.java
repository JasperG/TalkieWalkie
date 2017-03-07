package com.jaspergoes.talkiewalkie;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.jaspergoes.talkiewalkie.audio.Input;
import com.jaspergoes.talkiewalkie.audio.Output;
import com.jaspergoes.talkiewalkie.helpers.NativeLib;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        NativeLib.init(getApplicationContext(), "talkwalkopus");

        Output.start(getApplicationContext());

        /* Keep screen on while in foreground */
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Input.encodeTerminationSound(getApplicationContext());

        final Handler handler = new Handler(Looper.getMainLooper());

        final Button talk = (Button) findViewById(R.id.mic_talk);
        talk.setOnTouchListener(new View.OnTouchListener() {

            private boolean enabled = true;
            private boolean pressed = false;

            private Runnable setEnabled = new Runnable() {

                @Override
                public void run() {

                    enabled = true;

                    talk.setEnabled(true);
                    talk.setText(getString(R.string.mic_tlk));

                }

            };

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:

                        if (enabled) {

                            pressed = true;
                            talk.setText(getString(R.string.mic_rec));
                            Input.start();

                        }

                        return false;

                    case MotionEvent.ACTION_MOVE:
                        return true;

                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:

                        if (enabled && pressed) {

                            enabled = false;
                            pressed = false;

                            talk.setEnabled(false);
                            talk.setText("Wait");

                            Input.stop();

                            handler.postDelayed(setEnabled, 1000);

                        }

                        return false;

                }

                return false;

            }

        });

        final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        DiscreteSeekBar volume = (DiscreteSeekBar) findViewById(R.id.volume);
        volume.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        volume.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        volume.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {

            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {

                if (fromUser) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0);
                }

            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {

            }

        });

    }

    private BroadcastReceiver volumeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals("android.media.VOLUME_CHANGED_ACTION")) {

                updateVolume();

            }

        }

    };

    private void updateVolume() {

        ((DiscreteSeekBar) findViewById(R.id.volume)).setProgress(((AudioManager) getSystemService(Context.AUDIO_SERVICE)).getStreamVolume(AudioManager.STREAM_MUSIC));

    }

    @Override
    protected void onResume() {

        super.onResume();

        updateVolume();

        registerReceiver(volumeReceiver, new IntentFilter("android.media.VOLUME_CHANGED_ACTION"));

    }

    @Override
    protected void onPause() {

        super.onPause();

        unregisterReceiver(volumeReceiver);

    }

}
