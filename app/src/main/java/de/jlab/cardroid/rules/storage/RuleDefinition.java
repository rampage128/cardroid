package de.jlab.cardroid.rules.storage;

import java.util.List;

import androidx.room.Embedded;
import androidx.room.Relation;

public class RuleDefinition {

    @Embedded
    public EventEntity event;

    @Relation(parentColumn = "identifier", entityColumn = "event_uid", entity = ActionEntity.class)
    public List<ActionEntity> actions;

}
