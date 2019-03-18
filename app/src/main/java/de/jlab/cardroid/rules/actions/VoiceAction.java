package de.jlab.cardroid.rules.actions;

import android.content.Context;
import android.content.Intent;
import android.speech.RecognizerIntent;

import java.util.HashMap;

import de.jlab.cardroid.rules.Action;
import de.jlab.cardroid.rules.properties.ActionPropertyEditor;

public final class VoiceAction extends Action {

    @Override
    public void execute(Context context) {
        Intent voiceIntent = new Intent(RecognizerIntent.ACTION_VOICE_SEARCH_HANDS_FREE);
        voiceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(voiceIntent);
    }

    @Override
    protected void setKnownProperties(HashMap<String, ActionPropertyEditor> knownProperties) {
        // Intentionally left blank
    }
}
