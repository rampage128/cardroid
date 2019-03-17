package de.jlab.cardroid.rules;

import android.content.Context;

import java.util.HashMap;

import androidx.annotation.NonNull;
import de.jlab.cardroid.rules.actions.MediaKeyAction;
import de.jlab.cardroid.rules.actions.VolumeAdjustAction;
import de.jlab.cardroid.rules.actions.StartAppAction;
import de.jlab.cardroid.rules.actions.VoiceAction;
import de.jlab.cardroid.rules.actions.WifiToggleAction;
import de.jlab.cardroid.rules.properties.ActionPropertyEditor;
import de.jlab.cardroid.rules.properties.Property;
import de.jlab.cardroid.rules.storage.ActionEntity;

public abstract class Action {

    private HashMap<String, ActionPropertyEditor> knownProperties = new HashMap<>();

    private HashMap<String, Object> properties = new HashMap<>();

    public Action() {
        this.setKnownProperties(this.knownProperties);
    }

    public abstract void execute(Context context);

    protected abstract void setKnownProperties(HashMap<String, ActionPropertyEditor> knownProperties);

    public void setPropertyValue(String propertyName, Object propertyValue) {
        if (this.properties.containsKey(propertyName)) {
            this.properties.put(propertyName, propertyValue);
        }
    }

    public HashMap<String, ActionPropertyEditor> getKnownProperties() {
        return this.knownProperties;
    }

    public Object getPropertyValue(String propertyName) {
        return properties.get(propertyName);
    }

    public static ActionDefinition[] getActionDefinitions(Context context) {
        return new ActionDefinition[] {
                new ActionDefinition(MediaKeyAction.class, context),
                new ActionDefinition(VolumeAdjustAction.class, context),
                new ActionDefinition(StartAppAction.class, context),
                new ActionDefinition(VoiceAction.class, context),
                new ActionDefinition(WifiToggleAction.class, context)
        };
    }

    public static Action createFromEntity(ActionEntity entity) {
        try {
            Class actionClass = Class.forName(entity.className);
            Action action = (Action)actionClass.newInstance();
            for (Property<?> property : entity.properties) {
                action.properties.put(property.getKey(), property.getValue());
            }
            return action;
        } catch (ClassNotFoundException e) {
            throw new UnsupportedOperationException("Action type \"" + entity.className + "\" not found!");
        } catch (IllegalAccessException e) {
            throw new UnsupportedOperationException("Action type \"" + entity.className + "\" not accessible!");
        } catch (InstantiationException e) {
            throw new UnsupportedOperationException("Action type \"" + entity.className + "\" could not be instantiated!");
        }
    }

}
