package com.wakeup.alarm;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

/**
 * Synthesised two-tone siren on the alarm stream. Generating it means no sound asset to ship,
 * and the gain can be changed instantly for the rising volume and the quiet window.
 */
public class SirenPlayer {
    private static final int SAMPLE_RATE = 22050;

    private AudioTrack track;
    private Thread thread;
    private volatile boolean running;
    private volatile float gain = 0.3f;

    public void start() {
        int min = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        track = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setBufferSizeInBytes(Math.max(min, 4096))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();
        running = true;
        track.setVolume(gain);
        track.play();
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                short[] buf = new short[1024];
                double phase = 0;
                long n = 0;
                while (running) {
                    for (int i = 0; i < buf.length; i++) {
                        double t = (n++) / (double) SAMPLE_RATE;
                        double sweep = (t % 0.9) / 0.9;
                        double tri = sweep < 0.5 ? sweep * 2 : 2 - sweep * 2;
                        double f = 650 + 650 * tri;
                        phase += 2 * Math.PI * f / SAMPLE_RATE;
                        double s = Math.sin(phase) + 0.5 * Math.sin(2 * phase) + 0.33 * Math.sin(3 * phase);
                        buf[i] = (short) (s / 1.83 * 32000);
                    }
                    track.setVolume(gain);
                    track.write(buf, 0, buf.length);
                }
            }
        }, "siren");
        thread.start();
    }

    public void setGain(float g) {
        gain = g < 0 ? 0 : (g > 1 ? 1 : g);
    }

    public void stop() {
        running = false;
        try {
            if (thread != null) thread.join(600);
        } catch (InterruptedException ignored) {
        }
        if (track != null) {
            try {
                track.stop();
            } catch (IllegalStateException ignored) {
            }
            track.release();
            track = null;
        }
    }
}
