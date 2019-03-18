package de.jlab.cardroid.rules.actions;

import android.content.Context;
import android.media.AudioManager;
import android.view.KeyEvent;

import java.util.HashMap;

import de.jlab.cardroid.rules.properties.ActionPropertyEditor;
import de.jlab.cardroid.rules.properties.OptionPropertyEditor;
import de.jlab.cardroid.rules.properties.PropertyValue;

public final class MediaKeyAction extends AudioAction {

    private static final String PREFIX = "action_mediakey";

    private static final String PROPERTY_KEY_CODE = PREFIX + "_key_code";

    @Override
    public void execute(Context context) {
        int keyCode = (Integer)getPropertyValue(PROPERTY_KEY_CODE);
        AudioManager audioManager = this.getAudioManager(context);

        if (audioManager != null) {
            KeyEvent downEvent = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
            audioManager.dispatchMediaKeyEvent(downEvent);

            KeyEvent upEvent = new KeyEvent(KeyEvent.ACTION_UP, keyCode);
            audioManager.dispatchMediaKeyEvent(upEvent);
        }
    }

    @Override
    protected void setKnownProperties(HashMap<String, ActionPropertyEditor> knownProperties) {
        knownProperties.put(PROPERTY_KEY_CODE, new OptionPropertyEditor(new PropertyValue[] {
                new PropertyValue<>(KeyEvent.KEYCODE_MEDIA_PLAY, PREFIX + "_play"),
                new PropertyValue<>(KeyEvent.KEYCODE_MEDIA_PAUSE, PREFIX + "_pause"),
                new PropertyValue<>(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, PREFIX + "_play_pause"),
                new PropertyValue<>(KeyEvent.KEYCODE_MEDIA_PREVIOUS, PREFIX + "_previous"),
                new PropertyValue<>(KeyEvent.KEYCODE_MEDIA_NEXT, PREFIX + "_next")
        }, 2));
    }
}
