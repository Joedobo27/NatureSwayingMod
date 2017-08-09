package com.joedobo27.nsm;


import com.sun.istack.internal.Nullable;
import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.items.Item;

import java.util.logging.Logger;

class Totem {

    Item itemWU;
    static int totemTemplateId;
    static final private String BITE_MASK_DESCRIPTOR =
            "R=radius, E=empty, F=fuel, I=Id: 0b RRRR EEEF FFFF FFFF FFFF FEEI IIII IIII";

    Totem(Item itemWU) {
        this.itemWU = itemWU;
    }

    static boolean isFueled(Item totem) {
        return decodeZoneFuel(totem) >= 0;
    }

    boolean isPlanted() {
        return this.itemWU.isPlanted();
    }

    static int getTotemActionId(Item totem) {
        return NatureZoneData.natureZoneDatas.get(decodePlantId(totem)).actionId;
    }

    static long getTotemFuelEfficiency(Item totem) {
        return NatureZoneData.natureZoneDatas.get(decodePlantId(totem)).fuelUseFactor;
    }

    static byte decodeZoneRadius(Item totem) {
        return (byte)((totem.getData1() & 0xF0000000) >>> 28);
    }

    /**
     * Decode custom serialized data from the Item.data1(). Custom defined by the mask: 0xF0000000
     * This gives 16 potential values, 0 for one tile all the way up to 15 for 961 tiles (31x31 square).
     *
     * @return byte value, radius value for influence area.
     */
    byte decodeZoneRadius() {
        return (byte)((this.itemWU.getData1() & 0xF0000000) >>> 28);
    }

    static void encodeZoneRadius(Item totem, int radius) {
        int preservedData = totem.getData1() & ~0xF0000000;
        totem.setData1( (radius << 28) + preservedData);
    }

    /**
     * Encode custom serialized data into Item.data1() at 0xF0000000. zone area's radius.
     * This gives 16 potential values, 0 for one tile all the way up to 15 for 961 tiles (31x31 square).
     *
     * @param radius int primitive, zone area's radius.
     */
    void encodeZoneRadius(int radius) {
        int preservedData = this.itemWU.getData1() & ~0xF0000000;
        this.itemWU.setData1( (radius << 28) + preservedData);
    }

    static int decodeZoneFuel(Item totem) {
        return (byte)((totem.getData1() & 0x01FFF800) >>> 7);
    }

    /**
     * Decode custom serialized data from the Item.data1() of size 0x3FFF. Custom defined by the mask: 0x01FFF800
     * This gives 16,383 potential values counting 0 value.
     *
     * @return byte value, radius value for sow square.
     */
    int decodeZoneFuel() {
        return (byte)((this.itemWU.getData1() & 0x01FFF800) >>> 7);
    }

    static void encodeZoneFuel(Item item, int fuel) {
        int preservedData = item.getData1() & ~0x01FFF800;
        item.setData1( (fuel << 7) + preservedData);
    }

    /**
     * Encode custom serialized data into Item.data1() of size 0x3FFF at at 0x0007FFE0. This is the totem's fuel level.
     * This gives 16,383 potential values counting 0 value. Bitwise complement of ~0x01FFF800 masks by 0xFE0007FF;
     *
     * @param fuel int primitive, fuel level.
     */
    void encodeZoneFuel(int fuel) {
        int preservedData = this.itemWU.getData1() & ~0x01FFF800;
        this.itemWU.setData1( (fuel << 7) + preservedData);
    }

    static int decodePlantId(Item totem) {
        return totem.getData1() & 0x000001FF;
    }

    /**
     * Decode custom serialized data from Item.data1(). Custom defined by the mask: 0x000001FF
     * This gives 511 potential values counting 0 value.
     *
     * @return int value, plantId.
     */
    int decodePlantId() {
        return (this.itemWU.getData1() & 0x000001FF);
    }

    static void encodePlantId(Item totem, int plantId) {
        int preservedData = totem.getData1() & ~0x000001FF;
        totem.setData1( (plantId) + preservedData);
    }

    /**
     * Encode custom serialized data into Item.data1() at 0x000001FF. Id from PlantList.
     * This gives 511 potential values counting 0 value. Bitwise complement of ~0x000001FF masks by 0xFFFFFE00
     *
     * @param plantId int primitive, Id from PlantList.
     */
    void encodePlantId(int plantId) {
        int preservedData = this.itemWU.getData1() & ~0x000001FF;
        this.itemWU.setData1( (plantId) + preservedData);
    }

    static void decreaseOneFuel(Item totem) {
        int newFuel = (int)(decodeZoneFuel(totem) - getTotemFuelEfficiency(totem));
        if (newFuel <= 0) {
            NatureZone.removeNatureZone(totem);
        }
        encodeZoneFuel(totem, newFuel);
    }

    static void increaseZoneFuel(Item totem, int itemCount) {
        int currentFuel = decodeZoneFuel(totem);
        int additionalFuel = (int)(itemCount * getTotemFuelEfficiency(totem));
        int newFuel = Math.min(16383, currentFuel + additionalFuel);
        encodeZoneFuel(totem, newFuel);
        if (newFuel > 1 && !NatureZone.natureZones.containsKey(totem))
            new NatureZone(totem.getTilePos(), totem);
    }

    @Nullable
    Item getItemFromWurmId(long wurmId) {
        Item item = null;
        try {
            item = Items.getItem(wurmId);
        } catch (NoSuchItemException e){
            logger.fine("NoSuchItemException for id " + wurmId);
            return null;

        }
        return item;
    }
    
    Item getItemWU() {
        return this.itemWU;
    }
    

    private static final Logger logger = Logger.getLogger(NatureSwayingMod.class.getName());
}
