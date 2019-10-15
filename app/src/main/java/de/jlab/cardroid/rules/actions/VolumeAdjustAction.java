package de.jlab.cardroid.rules.actions;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.jlab.cardroid.rules.properties.ActionPropertyEditor;
import de.jlab.cardroid.rules.properties.OptionPropertyEditor;
import de.jlab.cardroid.rules.properties.PropertyValue;

public final class VolumeAdjustAction extends AudioAction {

    private static final String PREFIX = "action_volume";

    private static final String PROPERTY_ACTION = PREFIX + "_action";
    private static final String PROPERTY_STREAM = PREFIX + "_stream";

    @Override
    public void execute(Context context) {
        int streamType = (Integer)getPropertyValue(PROPERTY_STREAM);
        int direction = (Integer)getPropertyValue(PROPERTY_ACTION);

        AudioManager audioManager = this.getAudioManager(context);

        if (audioManager != null) {
            audioManager.adjustStreamVolume(streamType, direction, AudioManager.FLAG_SHOW_UI);
        }
    }

    @Override
    protected void setKnownProperties(HashMap<String, ActionPropertyEditor> knownProperties) {
        knownProperties.put(PROPERTY_STREAM, new OptionPropertyEditor(new PropertyValue[] {
                new PropertyValue<>(AudioManager.STREAM_NOTIFICATION, PREFIX + "_notification"),
                new PropertyValue<>(AudioManager.STREAM_ALARM, PREFIX + "_alarm"),
                new PropertyValue<>(AudioManager.STREAM_MUSIC, PREFIX + "_music"),
                new PropertyValue<>(AudioManager.USE_DEFAULT_STREAM_TYPE, PREFIX + "_automatic")
        }, 2));

        List<PropertyValue> actionValues = new ArrayList<>();
        actionValues.add(new PropertyValue<>(AudioManager.ADJUST_LOWER, PREFIX + "_down"));
        actionValues.add(new PropertyValue<>(AudioManager.ADJUST_RAISE, PREFIX + "_up"));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            actionValues.add(new PropertyValue<>(AudioManager.ADJUST_MUTE, PREFIX + "_mute"));
            actionValues.add(new PropertyValue<>(AudioManager.ADJUST_UNMUTE, PREFIX + "_unmute"));
            actionValues.add(new PropertyValue<>(AudioManager.ADJUST_TOGGLE_MUTE, PREFIX + "_togglemute"));
        }

        knownProperties.put(PROPERTY_ACTION, new OptionPropertyEditor(actionValues.toArray(new PropertyValue[0]), -1));
    }

}
