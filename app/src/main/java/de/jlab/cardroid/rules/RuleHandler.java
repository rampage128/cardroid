package de.jlab.cardroid.rules;

import android.app.Application;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.serial.carduino.CarduinoEventProvider;
import de.jlab.cardroid.providers.DataProviderService;
import de.jlab.cardroid.rules.storage.ActionEntity;
import de.jlab.cardroid.rules.storage.EventEntity;
import de.jlab.cardroid.rules.storage.EventRepository;
import de.jlab.cardroid.rules.storage.RuleDefinition;

public final class RuleHandler {

    private Application application;
    private CarduinoEventProvider eventProvider;

    private KnownEvents knownEvents;
    private SparseArray<Rule> rules = new SparseArray<>();

    public RuleHandler(@NonNull DataProviderService service) {
        this.application = service.getApplication();
        this.knownEvents = new KnownEvents(application);
        this.eventProvider = service.getDeviceProvider(CarduinoEventProvider.class);
        if (this.eventProvider != null) {
            this.eventProvider.subscribe(this::triggerRule);
        }
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

    public void dispose() {
        this.eventProvider.unsubscribe(this::triggerRule);
    }

}
