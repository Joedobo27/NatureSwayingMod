package com.joedobo27.nsm;


import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import java.util.Collections;
import java.util.List;

public class ConfigureTotemAction implements ModAction, BehaviourProvider, ActionPerformer {

    private final short actionId;
    private final ActionEntry actionEntry;

    ConfigureTotemAction() {
        this.actionId = (short) ModActions.getNextActionId();
        this.actionEntry = ActionEntry.createEntry(this.actionId, "Configure", "Configuring",
                new int[] {48});
        ModActions.registerAction(actionEntry);
    }

    @Override
    public short getActionId() {
        return this.actionId;
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item target) {
        return getBehavioursFor(performer, null, target);
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Item target) {
        if (target.getTemplateId() == Totem.totemTemplateId) {
            return Collections.singletonList(this.actionEntry);
        } else {
            return null;
        }
    }

    @Override
    public boolean action(Action action, Creature performer, Item target, short num, float counter) {
        return action(action, performer, null, target, num, counter);
    }

    @Override
    public boolean action(Action action, Creature performer, Item source, Item target, short num, float counter) {
        if (num == this.actionId) {
            new ConfigureTotemQuestion(
                    performer, "Configure Totem", "Configure the totem with.", 502, target);
            return true;
        }
        return false;
    }
}
