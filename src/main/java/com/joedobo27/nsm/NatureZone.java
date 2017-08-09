package com.joedobo27.nsm;


import com.joedobo27.libs.TileUtilities;
import com.wurmonline.math.TilePos;
import com.wurmonline.mesh.GrassData;
import com.wurmonline.mesh.Tiles;
import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.Players;
import com.wurmonline.server.Server;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.zones.NoSuchZoneException;
import com.wurmonline.server.zones.Zone;
import com.wurmonline.server.zones.Zones;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class NatureZone {
    /**
     * zoneTiles is type {@link ArrayList<TilePos>} and is a listing of all tiles for a {@link NatureZone} instance.
     */
    private ArrayList<TilePos> zoneTiles;

    /**
     * It's difficult to store the "Item" object as key in database. The wurmId is a long-value and is unique. The database is
     * the only reason to use this otherwise an Item object reference would work fine within a running java app.
     */
    private long totemId;

    /**
     * natureZones is type {@link HashMap} key {@link Totem}, value {@link NatureZone}. It is a map of all {@link NatureZone}
     * instances and is keyed with each instance's matching Totem item.
     */
    private Item totem;


    static HashMap<Item, NatureZone> natureZones = new HashMap<>();
    static long lastPollTimeNanoSec;
    static Iterator<Map.Entry<Item, NatureZone>> natureZonesIterator = null;


    /**
     * @param centerTile type {@link TilePos}, {@link NatureZone#centerTile}
     * @param totem type {@link Totem}, The Totem Item.
     */
    NatureZone(TilePos centerTile, Item totem) {
        try{
            this.totemId = totem.getWurmId();
            this.totem = Items.getItem(this.totemId);
        }catch(NoSuchItemException nie){
            NatureSwayingMod.logger.log(Level.WARNING,"wurmId for a totem isn't valid", nie.getMessage());
            return;
        }
        int radius = Totem.decodeZoneRadius(totem);
        natureZones.put(totem, this);
        this.zoneTiles = IntStream.range(this.centerTile.x - radius, this.centerTile.x + radius + 1)
                .mapToObj(X -> IntStream.range(this.centerTile.y - radius, this.centerTile.y + radius + 1)
                        .mapToObj(Y -> TilePos.fromXY(X,Y))
                        .collect(Collectors.toCollection(ArrayList::new)))
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(this.zoneTiles);
    }

    static double getSpreadChance(int fuelIntensity) {
        double spreadChance;
        switch (fuelIntensity) {
            case 0:
                spreadChance = 0.2d;
                break;
            case 1:
                spreadChance = 0.4d;
                break;
            case 2:
                spreadChance = 0.6d;
                break;
            case 3:
                spreadChance = 0.8d;
                break;
            case 4:
                spreadChance = 0.9d;
                break;
            default:
                spreadChance = 1.0d;
        }
        return spreadChance;
    }

    static double getWildChance( int fuelIntensity) {
        double wildChance;
        switch ((int)fuelIntensity) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                wildChance = 0.0d;
                break;
            case 6:
                wildChance = 0.2d;
                break;
            case 7:
                wildChance = 0.4d;
                break;
            case 8:
                wildChance = 0.6d;
                break;
            case 9:
                wildChance = 0.8d;
                break;
            default:
                wildChance = 1.0d;
        }
        return wildChance;
    }

    /**
     *
     */
    private void processTerrainSpreadZone(ArrayList<TilePos> tilesToDo) {
        double fuelIntensity = tallyFuelIntensity();
        double spreadChance = getSpreadChance((int)fuelIntensity);
        double wildChance = getWildChance((int)fuelIntensity);

        Iterator<TilePos> iterator = tilesToDo.iterator();
        boolean spread;
        boolean wild;
        switch(Totem.decodePlantId(this.totem)) {
            case PlantList.grass:
                while (iterator.hasNext()) {
                    TilePos tilePos = iterator.next();
                    short tileHeight = TileUtilities.getSurfaceHeight(tilePos);
                    byte tileType = TileUtilities.getSurfaceType(tilePos);
                    spread = Math.random() < spreadChance;
                    wild = Math.random() < wildChance;
                    boolean changeTypeAndData = tileType != Tiles.Tile.TILE_GRASS.id && spread && wild;
                    boolean changeType = tileType != Tiles.Tile.TILE_GRASS.id && spread && !wild;
                    boolean changeData = tileType == Tiles.Tile.TILE_GRASS.id && wild;

                    if (changeTypeAndData) {
                        byte grassTileData = GrassData.encodeGrassTileData(GrassData.GrowthStage.WILD, GrassData.FlowerType.NONE);
                        Server.setSurfaceTile(tilePos.x, tilePos.y, tileHeight, Tiles.Tile.TILE_GRASS.id, grassTileData);
                        TileUtilities.voidWorldResourceEntry(tilePos);
                        Server.modifyFlagsByTileType(tilePos.x, tilePos.y, Tiles.Tile.TILE_GRASS.id);
                        Players.getInstance().sendChangedTile(tilePos.x, tilePos.y, true, true);
                        try {
                            final Zone z = Zones.getZone(tilePos.x, tilePos.y, true);
                            z.changeTile(tilePos.x, tilePos.y);
                        } catch (NoSuchZoneException ignored) {
                        }
                        Totem.decreaseOneFuel(this.totem);
                    }
                    else if (changeType) {
                        byte grassTileData = GrassData.encodeGrassTileData(GrassData.GrowthStage.SHORT, GrassData.FlowerType.NONE);
                        Server.setSurfaceTile(tilePos.x, tilePos.y, tileHeight, Tiles.Tile.TILE_GRASS.id, grassTileData);
                        TileUtilities.voidWorldResourceEntry(tilePos);
                        Server.modifyFlagsByTileType(tilePos.x, tilePos.y, Tiles.Tile.TILE_GRASS.id);
                        Players.getInstance().sendChangedTile(tilePos.x, tilePos.y, true, true);
                        try {
                            final Zone z = Zones.getZone(tilePos.x, tilePos.y, true);
                            z.changeTile(tilePos.x, tilePos.y);
                        } catch (NoSuchZoneException ignored) {
                        }
                        Totem.decreaseOneFuel(this.totem);
                    }
                    else if (changeData) {
                        byte grassTileData = GrassData.encodeGrassTileData(GrassData.GrowthStage.WILD,
                                GrassData.FlowerType.decodeTileData(TileUtilities.getSurfaceData(tilePos)));
                        Server.setSurfaceTile(tilePos.x, tilePos.y, tileHeight, Tiles.Tile.TILE_GRASS.id, grassTileData);
                        Players.getInstance().sendChangedTile(tilePos.x, tilePos.y, true, false);
                        try {
                            final Zone z = Zones.getZone(tilePos.x, tilePos.y, true);
                            z.changeTile(tilePos.x, tilePos.y);
                        } catch (NoSuchZoneException ignored) {
                        }
                        Totem.decreaseOneFuel(this.totem);
                    }
                }
                break;
            case PlantList.steppe:
                break;
            case PlantList.moss:
                break;
        }

    }

    void processHarvestZone(ArrayList<TilePos> tilesToDo) {

    }

    void processFlowerZone(ArrayList<TilePos> tilesToDo) {

    }

    void processForageVeggiesZone(ArrayList<TilePos> tilesToDo) {

    }

    void processForageResourcesZone(ArrayList<TilePos> tilesToDo) {

    }

    void processForageBerriesZone(ArrayList<TilePos> tilesToDo) {

    }

    void processBotanizeSeedsZone(ArrayList<TilePos> tilesToDo) {

    }

    void processBotanizeHerbsZone(ArrayList<TilePos> tilesToDo) {

    }

    void processBotanizePlantsZone(ArrayList<TilePos> tilesToDo) {

    }

    void processBotanizeResourceZone(ArrayList<TilePos> tilesToDo) {

    }

    void processBotanizeSpicesZone(ArrayList<TilePos> tilesToDo) {

    }

    /**
     * @param totem type {@link Totem}, Remove the entry specified from {@link NatureZone#natureZones}.
     */
   static void removeNatureZone(Item totem) {
        if (natureZones.getOrDefault(totem, null) != null)
            natureZones.remove(totem);
    }

    /**
     * Periodically cycle through {@link NatureZone#natureZones} and check that they are still proper Nature Zones. If not remove the zone.
     */
    void verifyZoneIntegraty() {
        HashMap<Item, NatureZone> removeUs = new HashMap<>();
        natureZones.entrySet()
                .stream()
                .filter(map -> map.getKey() == null)
                .forEach(map -> removeUs.put(map.getKey(), map.getValue()));
        natureZones.entrySet()
                .stream()
                .filter(map -> map.getKey() != null && (!map.getKey().isPlanted() || !Totem.isFueled(map.getKey())))
                .forEach(map -> removeUs.put(map.getKey(), map.getValue()));
        if (removeUs.size() > 0)
            removeUs.forEach((key, value) -> natureZones.remove(key));
    }

    /**
     * During WU server start up find totem items, verify they should generate a zone, and finally generate those Nature zones.
     */
    static void startUpInitializeZones(){
        Item[] totems = Arrays.stream(Items.getAllItems())
                .parallel()
                .filter(item -> item.getTemplateId() == Totem.totemTemplateId)
                .toArray(Item[]::new);
        Item[] totems1 = Arrays.stream(totems)
                .filter(totem -> totem != null && totem.isPlanted() && Totem.isFueled(totem))
                .toArray(Item[]::new);
        Arrays.stream(totems1).forEach(totem -> new NatureZone(totem.getTilePos(), totem));
        NatureSwayingMod.logger.info("Nature zones generated for totems, total made: " + totems1.length);
    }

    /**
     * @return type HashMap<Totem, NatureZone>, {@link NatureZone#natureZones}
     */
    public static HashMap<Item, NatureZone> getNatureZones() {
        return natureZones;
    }

    private double tallyFuelIntensity() {
        return this.zoneTiles.size() / (Totem.decodeZoneFuel(this.totem) * Totem.getTotemFuelEfficiency(this.totem));
    }

    private ArrayList<TilePos> tallyZoneTiles() {
        int tilesInZone = this.zoneTiles.size();
        int fuelSupply = (int)(Totem.decodeZoneFuel(this.totem) * Totem.getTotemFuelEfficiency(this.totem));
        int smallestSize = Math.min(tilesInZone, fuelSupply);
        ArrayList<TilePos> toReturn = new ArrayList<>(smallestSize);
        IntStream.range(0, tilesInZone)
                .filter(value -> value <= smallestSize)
                .forEach(value -> toReturn.add(this.zoneTiles.get(value)));
        return toReturn;
    }

    /**
     *
     */
    static void doNatureZonePolling() {
        if (natureZones.size() == 0)
            return;
        if (natureZonesIterator == null)
            natureZonesIterator = natureZones.entrySet().iterator();
        if (!natureZonesIterator.hasNext()) {
            natureZonesIterator = null;
            lastPollTimeNanoSec = System.nanoTime();
            return;
        }
        Map.Entry<Item, NatureZone> totemNatureZoneEntry = natureZonesIterator.next();
        NatureZone natureZone = totemNatureZoneEntry.getValue();
        ArrayList<TilePos> tilesToDo = natureZone.tallyZoneTiles();
        switch (Totem.getTotemActionId(totemNatureZoneEntry.getKey())) {
            case 1:
                natureZone.processTerrainSpreadZone(tilesToDo);
                break;
            case 152:
                natureZone.processHarvestZone(tilesToDo);
                break;
            case 187:
                natureZone.processFlowerZone(tilesToDo);
                break;
            case 569:
                natureZone.processForageVeggiesZone(tilesToDo);
                break;
            case 570:
                natureZone.processForageResourcesZone(tilesToDo);
            case 571:
                natureZone.processForageBerriesZone(tilesToDo);
                break;
            case 572:
                natureZone.processBotanizeSeedsZone(tilesToDo);
                break;
            case 573:
                natureZone.processBotanizeHerbsZone(tilesToDo);
                break;
            case 574:
                natureZone.processBotanizePlantsZone(tilesToDo);
                break;
            case 575:
                natureZone.processBotanizeResourceZone(tilesToDo);
                break;
            case 720:
                natureZone.processBotanizeSpicesZone(tilesToDo);
                break;
        }
    }
}
