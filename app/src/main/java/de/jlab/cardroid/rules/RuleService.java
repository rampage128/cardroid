package de.jlab.cardroid.rules;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.FeatureObserver;
import de.jlab.cardroid.devices.serial.carduino.EventObservable;
import de.jlab.cardroid.rules.storage.ActionEntity;
import de.jlab.cardroid.rules.storage.EventEntity;
import de.jlab.cardroid.rules.storage.EventRepository;
import de.jlab.cardroid.rules.storage.RuleDefinition;
import de.jlab.cardroid.service.FeatureService;

// TODO: make this a service
public final class RuleService extends FeatureService implements FeatureObserver<EventObservable> {

    private KnownEvents knownEvents;
    private SparseArray<Rule> rules = new SparseArray<>();
    private ArrayList<EventObservable> eventObservables = new ArrayList<>();
    private RuleServiceBinder binder = new RuleServiceBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        this.knownEvents = new KnownEvents(this.getApplication());
        EventRepository eventRepo = new EventRepository(this.getApplication());
        List<RuleDefinition> rules = eventRepo.getAllRules();
        this.updateRuleDefinitions(rules);
    }

    @Override
    public void onDestroy() {
        for (EventObservable observable: eventObservables) {
            observable.removeListener(this::triggerRule);
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return this.binder;
    }

    @Override
    protected void onDeviceServiceConnected(DeviceService.DeviceServiceBinder service) {
        service.subscribe(this, EventObservable.class);
    }

    @Override
    protected void onDeviceServiceDisconnected() {
        this.stopSelf();
    }

    @Override
    public void onFeatureAvailable(@NonNull EventObservable feature) {
        eventObservables.add(feature);
        feature.addListener(this::triggerRule);
    }

    @Override
    public void onFeatureUnavailable(@NonNull EventObservable feature) {
        eventObservables.remove(feature);
    }

    public void triggerRule(int eventIdentifier) {
        Event event = this.getEvent(eventIdentifier);

        Rule rule = this.rules.get(event.getIdentifier());
        if (rule != null) {
            rule.trigger(event, this.getApplication());
        }
    }

    private Event getEvent(int eventIdentifier) {
        // Get matching event from known events
        Event event = this.knownEvents.getEventFromIdentifier(eventIdentifier);

        // Create event and insert into database if not known yet
        if (event == null) {
            event = new Event(eventIdentifier);
            this.knownEvents.addEvent(event);

            String eventName = Event.getLocalizedNameFromIdentifier(eventIdentifier, this.getApplication());
            EventRepository repository = new EventRepository(this.getApplication());
            repository.insert(new EventEntity(eventIdentifier, eventName));
        }

        return event;
    }

    public void updateRuleDefinition(@NonNull RuleDefinition ruleDefinition) {
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

    public void updateRuleDefinitions(@NonNull List<RuleDefinition> definitions) {
        this.rules.clear();

        for (RuleDefinition definition : definitions) {
            this.updateRuleDefinition(definition);
        }
    }

    public class RuleServiceBinder extends Binder {
        public void updateRuleDefinition(RuleDefinition definition) {
            RuleService.this.updateRuleDefinition(definition);
        }
    }
}
