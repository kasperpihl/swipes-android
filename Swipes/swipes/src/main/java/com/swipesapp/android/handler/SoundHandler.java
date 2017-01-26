package com.swipesapp.android.handler;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;

import com.swipesapp.android.util.PreferenceUtils;

import java.lang.ref.WeakReference;

/**
 * Handler to deal with in-app sounds.
 *
 * @author Fernanda Bari
 */
public class SoundHandler {

    private static final String LOG_TAG = SoundHandler.class.getSimpleName();

    private static SoundPool sSoundPool;
    private static int sSoundId;
    private static WeakReference<Context> sContext;

    public static void load(Context context) {
        sSoundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        sSoundPool.setOnLoadCompleteListener(sCompleteListener);
    }

    public static void playSound(Context context, int sound) {
        try {
            sContext = new WeakReference<>(context);
            sSoundId = sSoundPool.load(context, sound, 1);
        } catch (NullPointerException e) {
            Log.e(LOG_TAG, "SoundPool not loaded. Make sure to call SoundHandler.load()", e);
        }
    }

    private static SoundPool.OnLoadCompleteListener sCompleteListener = new SoundPool.OnLoadCompleteListener() {
        @Override
        public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
            if (PreferenceUtils.areSoundsEnabled(sContext.get())) {
                float volume = getVolume(sContext.get());
                sSoundPool.play(sSoundId, volume, volume, 1, 0, 1);
            }
        }
    };

    private static float getVolume(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        float actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        return actualVolume / maxVolume;
    }

}
