package com.joedobo27.nsm;


import com.opencsv.CSVReader;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

class NatureZoneData {

    int zoneId;
    int actionId;
    int fuelItemId;
    byte fuelItemMaterial;
    long fuelUseFactor;
    byte tileTypeId;
    static HashMap<Integer, NatureZoneData> natureZoneDatas = new HashMap<>(100);
    static final String csvFilePath = "C:\\Users\\Jason\\IdeaProjects\\NatureSwayingMod\\src\\main\\resources\\NatureZoneData - Sheet1.csv";

    private NatureZoneData(int zoneId, int actionId, int fuelItemId, byte fuelItemMaterial, long fuelUseFactor, byte tileTypeId){
        this.zoneId = zoneId;
        this.actionId = actionId;
        this.fuelItemId = fuelItemId;
        this.fuelItemMaterial = fuelItemMaterial;
        this.fuelUseFactor = fuelUseFactor;
        this.tileTypeId = tileTypeId;
        natureZoneDatas.put(zoneId, this);
    }

    static void initializeEntries() {
        try {
            CSVReader csvReader = new CSVReader(new FileReader(csvFilePath));
            List<String[]> csvLines = csvReader.readAll();
            csvLines.forEach(strings -> new NatureZoneData(Integer.parseInt(strings[0]), Integer.parseInt(strings[1]),
                    Integer.parseInt(strings[2]), Byte.parseByte(strings[3]), Long.parseLong(strings[4]), Byte.parseByte(strings[5])));
        }catch (IOException e){
            NatureSwayingMod.logger.warning(e.getMessage());
        }
    }

    static {
        initializeEntries();
    }
}
