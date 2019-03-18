package de.jlab.cardroid.rules;

import de.jlab.cardroid.rules.storage.ActionEntity;
import de.jlab.cardroid.rules.storage.EventEntity;
import de.jlab.cardroid.rules.storage.RuleDefinition;

public interface FragmentActionListener {

    int COMMAND_ADD     = 0;
    int COMMAND_EDIT    = 1;
    int COMMAND_SAVE    = 2;
    int COMMAND_CANCEL  = 3;
    int COMMAND_DELETE  = 4;
    int COMMAND_UPDATED = 5;

    void onEventChange(int command, EventEntity eventEntity);

    void onActionChange(int command, ActionEntity actionEntity);

    void onRuleChange(int command, RuleDefinition ruleDefinition);

}
