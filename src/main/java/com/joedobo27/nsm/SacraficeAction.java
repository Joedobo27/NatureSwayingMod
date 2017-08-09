package com.joedobo27.nsm;


import com.wurmonline.server.Items;
import com.wurmonline.server.Point;
import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SacraficeAction implements ModAction, BehaviourProvider, ActionPerformer {

    private final short actionId;
    private final ActionEntry actionEntry;
    private static final float TIME_TO_COUNTER_DIVISOR = 10.0f;
    private static final float ACTION_START_TIME = 1.0f;

    SacraficeAction() {
        this.actionId = (short) ModActions.getNextActionId();
        this.actionEntry = ActionEntry.createEntry(this.actionId, "Sacrifice", "Sacrificing",
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
    public boolean action(Action action, Creature performer, Item totem, short _actionId, float counter) {
        return action(action, performer, null, totem, _actionId, counter);
    }

    @Override
    public boolean action(Action action, Creature performer, Item source, Item totem, short _actionId, float counter) {
        if (_actionId == this.actionId && totem.getTemplateId() == Totem.totemTemplateId) {
            int time;
            String youMessage;
            String broadcastMessage;
            if (counter == ACTION_START_TIME) {
                if (hasAFailureCondition(performer, totem)) {
                    return true;
                }
                youMessage = String.format("You start %s.", action.getActionEntry().getVerbString());
                broadcastMessage = String.format("%s starts to %s.", performer.getName(), action.getActionString());
                performer.getCommunicator().sendNormalServerMessage(youMessage);
                Server.getInstance().broadCastAction(broadcastMessage, performer, 5);
                time = (int) NatureSwayingMod.getBaseUnitActionTime(totem, performer, action);
                action.setTimeLeft(time);
                performer.sendActionControl(action.getActionEntry().getVerbString(), true, time);
                return false;
            }
            boolean actionInProcess = counter < action.getTimeLeft() / TIME_TO_COUNTER_DIVISOR;
            if (actionInProcess)
                return false;
            if (hasAFailureCondition(performer, totem)) {
                return true;
            }
            Item[] items = totem.getItemsAsArray();
            Totem.increaseZoneFuel(totem, items.length);
            Arrays.stream(items)
                    .forEach(item -> Items.destroyItem(item.getWurmId()));
            youMessage = String.format("You finish %s.", action.getActionEntry().getVerbString());
            broadcastMessage = String.format("%s finishes %s.", performer.getName(), action.getActionString());
            performer.getCommunicator().sendNormalServerMessage(youMessage);
            Server.getInstance().broadCastAction(broadcastMessage, performer, 5);
            return true;
        }
        return false;
    }

    static private boolean hasAFailureCondition(Creature performer, Item totem) {

        Comparator<Item> comparator1 = Comparator.comparing(Item::getTemplateId);
        boolean moreThenOneItemType = Arrays.stream(totem.getAllItems(true))
                .mapToInt(Item::getTemplateId)
                .distinct()
                .count() > 1;
        if (moreThenOneItemType) {
            performer.getCommunicator().sendNormalServerMessage("Sacrificing failed because the all items must be of the same type.");
            return true;
        }


        return false;
    }

}
