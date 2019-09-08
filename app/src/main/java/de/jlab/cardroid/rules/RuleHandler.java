package de.jlab.cardroid.rules;

import android.app.Application;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import de.jlab.cardroid.rules.storage.ActionEntity;
import de.jlab.cardroid.rules.storage.EventEntity;
import de.jlab.cardroid.rules.storage.EventRepository;
import de.jlab.cardroid.rules.storage.RuleDefinition;
import de.jlab.cardroid.usb.carduino.serial.CarSystemSerialPacket;
import de.jlab.cardroid.usb.carduino.CarduinoService;
import de.jlab.cardroid.usb.carduino.serial.SerialEventPacket;
import de.jlab.cardroid.usb.carduino.serial.SerialPacket;

public final class RuleHandler implements CarduinoService.PacketHandler<CarSystemSerialPacket> {

    private Application application;

    private KnownEvents knownEvents;
    private SparseArray<Rule> rules = new SparseArray<>();

    public RuleHandler(Application application) {
        this.application = application;
        this.knownEvents = new KnownEvents(application);
    }

    public void triggerRule(int eventIdentifier) {
        Event event = this.getEvent(eventIdentifier);

        Rule rule = this.rules.get(event.getIdentifier());
        if (rule != null) {
            rule.trigger(event, this.application);
        }
    }

    private Event getEvent(int eventIdentifier) {
        // Get matching event from known events
        Event event = this.knownEvents.getEventFromIdentifier(eventIdentifier);

        // Create event and insert into database if not known yet
        if (event == null) {
            event = new Event(eventIdentifier);
            this.knownEvents.addEvent(event);

            String eventName = Event.getLocalizedNameFromIdentifier(eventIdentifier, this.application);
            EventRepository repository = new EventRepository(this.application);
            repository.insert(new EventEntity(eventIdentifier, eventName));
        }

        return event;
    }

    @Override
    public void handleSerialPacket(@NonNull CarSystemSerialPacket packet) {
        this.triggerRule(packet.getId());
    }

    @Override
    public boolean shouldHandlePacket(@NonNull SerialPacket packet) {
        return packet instanceof SerialEventPacket;
    }

    public void updateRuleDefinition(RuleDefinition ruleDefinition) {
        if (ruleDefinition.actions == null || ruleDefinition.actions.isEmpty()) {
            this.rules.remove(ruleDefinition.event.identifier);
            return;
        }
        Event event = this.getEvent(ruleDefinition.event.identifier);
        List<Action> actions = new ArrayList<>();
        for (ActionEntity actionEntity : ruleDefinition.actions) {
            Action action = Action.createFromEntity(actionEntity);
            actions.add(action);
        }
        Rule rule = new Rule(new Trigger(event), actions.toArray(new Action[0]));

        this.rules.put(ruleDefinition.event.identifier, rule);
    }

    public void updateRuleDefinitions(List<RuleDefinition> definitions) {
        this.rules.clear();

        for (RuleDefinition definition : definitions) {
            if (definition.actions == null || definition.actions.isEmpty()) {
                continue;
            }
            Event event = new Event(definition.event.identifier);
            List<Action> actions = new ArrayList<>();
            for (ActionEntity actionEntity : definition.actions) {
                Action action = Action.createFromEntity(actionEntity);
                actions.add(action);
            }
            Rule rule = new Rule(new Trigger(event), actions.toArray(new Action[0]));
            this.rules.put(definition.event.identifier, rule);
        }
    }

}
