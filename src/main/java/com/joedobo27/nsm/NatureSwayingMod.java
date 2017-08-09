package com.joedobo27.nsm;

import com.wurmonline.server.Servers;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemTypes;


import com.wurmonline.server.items.RuneUtilities;
import com.wurmonline.server.skills.SkillList;
import org.gotti.wurmunlimited.modloader.interfaces.*;
import org.gotti.wurmunlimited.modsupport.IdFactory;
import org.gotti.wurmunlimited.modsupport.IdType;
import org.gotti.wurmunlimited.modsupport.ItemTemplateBuilder;

import java.io.IOException;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NatureSwayingMod implements WurmServerMod, Configurable, ServerStartedListener, ItemTemplatesCreatedListener, ServerPollListener {

    long pollingIntervalNanoSec;
    static final Logger logger = Logger.getLogger(NatureSwayingMod.class.getName());
    private static double minimumUnitActionTime;
    private static double actionTimeExtension;
    static Random random = new Random();

    @Override
    public void configure(Properties properties) {
        pollingIntervalNanoSec =  TimeUnit.SECONDS.toNanos(
                Long.parseLong(properties.getProperty("pollingIntervalSec")));
        minimumUnitActionTime = Double.parseDouble(properties.getProperty("minimumUnitActionTime", Double.toString(minimumUnitActionTime)));
        actionTimeExtension = Double.parseDouble(properties.getProperty("actionTimeExtension", Double.toString(actionTimeExtension)));
    }

    @Override
    public void onServerStarted() {
        NatureZone.startUpInitializeZones();
        NatureZone.lastPollTimeNanoSec = System.nanoTime();
    }

    @Override
    public void onItemTemplatesCreated() {
        ItemTemplateBuilder totem = new ItemTemplateBuilder("jdbTotem");
        Totem.totemTemplateId = IdFactory.getIdFor("jdbTotem", IdType.ITEMTEMPLATE);
        totem.name("nature totem","nature totem", "A shrine to pay tribute to nature.");
        totem.size(3);
        //totem.descriptions();
        totem.itemTypes(new short[]{ItemTypes.ITEM_TYPE_WOOD, ItemTypes.ITEM_TYPE_NAMED, ItemTypes.ITEM_TYPE_REPAIRABLE,
                ItemTypes.ITEM_TYPE_COLORABLE, ItemTypes.ITEM_TYPE_HASDATA});
        totem.imageNumber((short) 881);
        totem.behaviourType((short) 1);
        totem.combatDamage(0);
        totem.decayTime(19353600L);
        totem.dimensions(10, 30, 180);
        totem.primarySkill(-10);
        //totem.bodySpaces();
        totem.modelName("model.decoration.practicedoll.");
        totem.difficulty(1);
        totem.weightGrams(7000);
        totem.material((byte) 14);
        totem.value(10000);
        totem.isTraded(true);
        //totem.armourType();
        try {
            totem.build();
        } catch (IOException e){
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }

    /**
     * onServerPoll() triggers about once ever 20 milliseconds.
     */
    @Override
    public void onServerPoll() {
        if (System.nanoTime() - NatureZone.lastPollTimeNanoSec > pollingIntervalNanoSec) {
            NatureZone.doNatureZonePolling();
        }
    }

    /**
     * It shouldn't be necessary to have a fantastic, 104woa, speed rune, 99ql, 99 skill in order to get the fastest time.
     * Aim for just skill as getting close to shortest time and the other boosts help at lower levels but aren't needed to have
     * the best at end game.
     */
    static double getBaseUnitActionTime(Item activeTool, Creature performer, Action action){
        final double MAX_WOA_EFFECT = 0.20;
        final double TOOL_RARITY_EFFECT = 0.1;
        final double ACTION_RARITY_EFFECT = 0.33;
        final double MAX_SKILL = 100.0d;
        double time;
        double modifiedKnowledge = Math.min(MAX_SKILL, performer.getSkills().getSkillOrLearn(SkillList.SMITHING_METALLURGY)
                .getKnowledge(activeTool, 0));
        time = Math.max(minimumUnitActionTime, (130.0 + actionTimeExtension - modifiedKnowledge) * 1.3f / Servers.localServer.getActionTimer());

        // woa
        if (activeTool != null && activeTool.getSpellSpeedBonus() > 0.0f)
            time = Math.max(minimumUnitActionTime, time * (1 - (MAX_WOA_EFFECT * activeTool.getSpellSpeedBonus() / 100.0)));
        //rare item, 10% speed reduction per rarity level.
        if (activeTool != null && activeTool.getRarity() > 0)
            time = Math.max(minimumUnitActionTime, time * (1 - (activeTool.getRarity() * TOOL_RARITY_EFFECT)));
        //rare action, 33% speed reduction per rarity level.
        if (action.getRarity() > 0)
            time = Math.max(minimumUnitActionTime, time * (1 - (action.getRarity() * ACTION_RARITY_EFFECT)));
        // rune effects
        if (activeTool != null && activeTool.getSpellEffects() != null && activeTool.getSpellEffects().getRuneEffect() != -10L)
            time = Math.max(minimumUnitActionTime, time * (1 - RuneUtilities.getModifier(activeTool.getSpellEffects().getRuneEffect(), RuneUtilities.ModifierEffect.ENCH_USESPEED)));
        return time;
    }
}
