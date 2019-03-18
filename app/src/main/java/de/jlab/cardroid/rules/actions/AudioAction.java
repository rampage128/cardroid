package de.jlab.cardroid.rules.actions;

import android.content.Context;
import android.media.AudioManager;

import de.jlab.cardroid.rules.Action;

abstract class AudioAction extends Action {

    protected AudioManager getAudioManager(Context context) {
        return (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
    }

}
