package de.jlab.cardroid.rules;

import android.app.Application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.Observer;
import de.jlab.cardroid.devices.Device;
import de.jlab.cardroid.devices.DeviceController;
import de.jlab.cardroid.devices.Feature;
import de.jlab.cardroid.devices.identification.DeviceUid;
import de.jlab.cardroid.devices.serial.carduino.EventObservable;
import de.jlab.cardroid.rules.storage.ActionEntity;
import de.jlab.cardroid.rules.storage.EventEntity;
import de.jlab.cardroid.rules.storage.EventRepository;
import de.jlab.cardroid.rules.storage.RuleDefinition;

public final class RuleController {

    private KnownEvents knownEvents;
    private HashMap<EventEntity, Rule> rules = new HashMap<>();
    private Device.FeatureChangeObserver<EventObservable> eventFilter = this::onEventFeatureStateChange;

    private Application app;
    private DeviceController deviceController;
    private EventRepository eventRepo;

    private EventObservable.EventListener eventTrigger = this::triggerRule;
    private Observer<? super List<EventEntity>> knownEventUpdater = this::updateKnownEvents;
    private Observer<? super List<RuleDefinition>> ruleUpdater = this::updateRuleDefinitions;


    public RuleController(@NonNull DeviceController deviceController, @NonNull Application app) {
        this.app = app;
        this.deviceController = deviceController;
        this.deviceController.subscribeFeature(this.eventFilter, EventObservable.class);

        this.knownEvents = new KnownEvents();
        this.eventRepo = new EventRepository(app);
        this.eventRepo.getAll().observeForever(this.knownEventUpdater);
        this.eventRepo.getAllRules().observeForever(this.ruleUpdater);
    }

    public void dispose() {
        this.eventRepo.getAll().removeObserver(this.knownEventUpdater);
        this.eventRepo.getAllRules().removeObserver(this.ruleUpdater);
        this.deviceController.unsubscribeFeature(this.eventFilter, EventObservable.class);
    }

    private void onEventFeatureStateChange(@NonNull EventObservable feature, @NonNull Feature.State state) {
        if (state == Feature.State.AVAILABLE) {
            feature.addListener(this.eventTrigger);
        } else {
            feature.removeListener(this.eventTrigger);
        }
    }

    private void triggerRule(int identifier, @NonNull DeviceUid deviceUid) {
        Event event = this.getEvent(identifier, deviceUid);

        Rule rule = this.rules.get(event.getDescriptor());
        if (rule != null) {
            rule.trigger(event, this.app);
        }
    }

    private Event getEvent(int identifier, @NonNull DeviceUid deviceUid) {
        EventEntity descriptor = new EventEntity(identifier, deviceUid, "");

        // Get matching event from known events
        Event event = this.knownEvents.getEvent(descriptor);

        // Create event and insert into database if not known yet
        if (event == null) {
            event = new Event(descriptor);
            this.knownEvents.addEvent(event);

            descriptor.name = Event.getLocalizedNameFromIdentifier(identifier, this.app);
            this.eventRepo.insert(descriptor);
        }

        return event;
    }

    private void updateKnownEvents(@NonNull List<EventEntity> events) {
        for (EventEntity eventEntity : events) {
            this.knownEvents.addEvent(new Event(eventEntity));
        }
    }

    private void updateRuleDefinition(@NonNull RuleDefinition ruleDefinition) {
        if (ruleDefinition.actions == null || ruleDefinition.actions.isEmpty()) {
            this.rules.remove(ruleDefinition.event);
            return;
        }
        Event event = this.getEvent(ruleDefinition.event.identifier, ruleDefinition.event.deviceUid);
        List<Action> actions = new ArrayList<>();
        for (ActionEntity actionEntity : ruleDefinition.actions) {
            Action action = Action.createFromEntity(actionEntity);
            actions.add(action);
        }
        Rule rule = new Rule(new Trigger(event), actions.toArray(new Action[0]));

        this.rules.put(ruleDefinition.event, rule);
    }

    private void updateRuleDefinitions(@NonNull List<RuleDefinition> definitions) {
        this.rules.clear();

        for (RuleDefinition definition : definitions) {
            this.updateRuleDefinition(definition);
        }
    }

}
