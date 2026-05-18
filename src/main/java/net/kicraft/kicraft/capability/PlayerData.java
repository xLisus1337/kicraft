package net.kicraft.kicraft.capability;

import net.minecraft.nbt.CompoundTag;

public class PlayerData {
    private boolean isDirty = false;
    private String race = "NONE";
    private boolean hasChosenRace = false;
    private int[] hairStyles = new int[8];
    private int[] eyeTypes = new int[8];
    private int[] hairColors = new int[8];
    private int[] eyeColors = new int[8];
    private int[] skinColors = new int[8];
    private int[] kiColors = new int[8];
    private int[] secondaryColors = new int[8];
    private int tailColor = 0x5C3A21;
    private boolean isFatMajin = false;
    private long level = 1;
    private double ki = 100.0;
    private double maxKi = 100.0;
    private double stamina = 100.0;
    private double maxStamina = 100.0;
    private long strength = 1;
    private long durability = 1;
    private double maxHP = 20.0;
    private long kiControl = 1;
    private double kiXP = 0.0;
    private double strengthXP = 0.0;
    private double durabilityXP = 0.0;
    private double maxHpXP = 0.0;
    private double staminaXP = 0.0;
    private double kiControlXP = 0.0;
    private boolean isChargingKi = false;
    private boolean isTurboMode = false;
    private float visualKi = 0.0f;
    private boolean hasFlight = false;
    private boolean hasKiSense = false;
    private boolean hasInstantTransmission = false;

    public void markDirty() { this.isDirty = true; }
    public boolean isDirty() { return isDirty; }
    public void clearDirty() { this.isDirty = false; }

    public PlayerData() {
        for(int i = 0; i < 8; i++) {
            hairColors[i] = 0x000000; eyeColors[i] = 0x000000; skinColors[i] = 0xFFCC99;
            kiColors[i] = 0xFFFFFF; secondaryColors[i] = 0xFFFFFF;
        }
    }

    public void setRace(String race) { this.race = race; markDirty(); }
    public void setHasChosenRace(boolean has) { this.hasChosenRace = has; markDirty(); }
    public void setLevel(long level) { this.level = level; markDirty(); }
    public void setKi(double ki) { this.ki = Math.max(0, Math.min(ki, maxKi)); markDirty(); }
    public void setMaxKi(double maxKi) { this.maxKi = maxKi; markDirty(); }
    public void setStamina(double stamina) { this.stamina = Math.max(0, Math.min(stamina, maxStamina)); markDirty(); }
    public void setMaxStamina(double max) { this.maxStamina = Math.max(1, max); markDirty(); }
    public void setStrength(long str) { this.strength = str; markDirty(); }
    public void setDurability(long dur) { this.durability = dur; markDirty(); }
    public void setMaxHP(double hp) { this.maxHP = Math.max(1, hp); markDirty(); }
    public void setKiControl(long kc) { this.kiControl = kc; markDirty(); }
    public void setFlight(boolean v) { this.hasFlight = v; markDirty(); }
    public void setKiSense(boolean v) { this.hasKiSense = v; markDirty(); }
    public void setInstantTransmission(boolean v) { this.hasInstantTransmission = v; markDirty(); }
    public void setChargingKi(boolean is) { this.isChargingKi = is; markDirty(); }
    public void setTurboMode(boolean is) { this.isTurboMode = is; markDirty(); }
    public void setKiXP(double xp) { this.kiXP = xp; markDirty(); }
    public void setStrengthXP(double xp) { this.strengthXP = xp; markDirty(); }
    public void setDurabilityXP(double xp) { this.durabilityXP = xp; markDirty(); }
    public void setMaxHpXP(double xp) { this.maxHpXP = xp; markDirty(); }
    public void setStaminaXP(double xp) { this.staminaXP = xp; markDirty(); }
    public void setKiControlXP(double xp) { this.kiControlXP = xp; markDirty(); }

    public String getRace() { return race; }
    public boolean hasChosenRace() { return hasChosenRace; }
    public long getLevel() { return level; }
    public double getKi() { return ki; }
    public double getMaxKi() { return maxKi; }
    public double getStamina() { return stamina; }
    public double getMaxStamina() { return maxStamina; }
    public long getStrength() { return strength; }
    public long getDurability() { return durability; }
    public double getMaxHP() { return maxHP; }
    public long getKiControl() { return kiControl; }
    public double getKiXP() { return kiXP; }
    public double getStrengthXP() { return strengthXP; }
    public double getDurabilityXP() { return durabilityXP; }
    public double getMaxHpXP() { return maxHpXP; }
    public double getStaminaXP() { return staminaXP; }
    public double getKiControlXP() { return kiControlXP; }
    public boolean isChargingKi() { return isChargingKi; }
    public boolean isTurboMode() { return isTurboMode; }
    public boolean hasFlight() { return hasFlight; }
    public boolean hasKiSense() { return hasKiSense; }
    public boolean hasInstantTransmission() { return hasInstantTransmission; }
    public int[] getSkinColors() { return skinColors; }

    public void addKiXP(double amount) { this.kiXP += amount; markDirty(); }
    public void addStrengthXP(double amount) { this.strengthXP += amount; markDirty(); }
    public void addDurabilityXP(double amount) { this.durabilityXP += amount; markDirty(); }
    public void addMaxHpXP(double amount) { this.maxHpXP += amount; markDirty(); }
    public void addStaminaXP(double amount) { this.staminaXP += amount; markDirty(); }
    public void addKiControlXP(double amount) { this.kiControlXP += amount; markDirty(); }
    public void addKi(double amount) { this.setKi(this.ki + amount); }

    public double getReqStrengthXP() { return 1500.0 * Math.pow(this.strength, 1.8); }
    public double getReqMaxHpXP() { return 2000.0 * Math.pow((this.maxHP - 18), 1.7); }
    public double getReqStaminaXP() { return 1200.0 * Math.pow((this.maxStamina / 50.0), 1.8); }
    public double getReqKiXP() { return 2500.0 * Math.pow((this.maxKi / 50.0), 1.9); }
    public double getReqDurabilityXP() { return 1800.0 * Math.pow(this.durability, 1.8); }
    public double getReqKiControlXP() { return 3000.0 * Math.pow(this.kiControl, 1.8); }

    public void saveNBTData(CompoundTag nbt) {
        nbt.putString("Race", race);
        nbt.putBoolean("HasChosenRace", hasChosenRace);
        nbt.putIntArray("HairStyles", hairStyles);
        nbt.putIntArray("EyeTypes", eyeTypes);
        nbt.putIntArray("HairColors", hairColors);
        nbt.putIntArray("EyeColors", eyeColors);
        nbt.putIntArray("SkinColors", skinColors);
        nbt.putIntArray("KiColors", kiColors);
        nbt.putIntArray("SecondaryColors", secondaryColors);
        nbt.putInt("TailColor", tailColor);
        nbt.putBoolean("IsFatMajin", isFatMajin);
        nbt.putLong("Level", level);
        nbt.putDouble("Ki", ki);
        nbt.putDouble("MaxKi", maxKi);
        nbt.putDouble("Stamina", stamina);
        nbt.putDouble("MaxStamina", maxStamina);
        nbt.putLong("Strength", strength);
        nbt.putLong("Durability", durability);
        nbt.putDouble("MaxHP", maxHP);
        nbt.putLong("KiControl", kiControl);
        nbt.putBoolean("IsTurboMode", isTurboMode);
        nbt.putBoolean("IsChargingKi", isChargingKi);
        nbt.putDouble("KiXP", kiXP);
        nbt.putDouble("StrengthXP", strengthXP);
        nbt.putDouble("DurabilityXP", durabilityXP);
        nbt.putDouble("MaxHpXP", maxHpXP);
        nbt.putDouble("StaminaXP", staminaXP);
        nbt.putDouble("KiControlXP", kiControlXP);
        nbt.putBoolean("HasFlight", hasFlight);
        nbt.putBoolean("HasKiSense", hasKiSense);
        nbt.putBoolean("HasIT", hasInstantTransmission);
    }

    public void loadNBTData(CompoundTag nbt) {
        race = nbt.getString("Race");
        hasChosenRace = nbt.getBoolean("HasChosenRace");
        if (nbt.contains("HairStyles")) hairStyles = nbt.getIntArray("HairStyles");
        if (nbt.contains("EyeTypes")) eyeTypes = nbt.getIntArray("EyeTypes");
        if (nbt.contains("HairColors")) hairColors = nbt.getIntArray("HairColors");
        if (nbt.contains("EyeColors")) eyeColors = nbt.getIntArray("EyeColors");
        if (nbt.contains("SkinColors")) skinColors = nbt.getIntArray("SkinColors");
        if (nbt.contains("KiColors")) kiColors = nbt.getIntArray("KiColors");
        if (nbt.contains("SecondaryColors")) secondaryColors = nbt.getIntArray("SecondaryColors");
        tailColor = nbt.getInt("TailColor");
        isFatMajin = nbt.getBoolean("IsFatMajin");
        level = nbt.getLong("Level");
        ki = nbt.getDouble("Ki");
        maxKi = nbt.contains("MaxKi") ? nbt.getDouble("MaxKi") : 100.0;
        stamina = nbt.getDouble("Stamina");
        maxStamina = nbt.contains("MaxStamina") ? nbt.getDouble("MaxStamina") : 100.0;
        strength = nbt.getLong("Strength");
        durability = nbt.getLong("Durability");
        maxHP = nbt.contains("MaxHP") ? nbt.getDouble("MaxHP") : 20.0;
        kiControl = nbt.getLong("KiControl");
        isTurboMode = nbt.getBoolean("IsTurboMode");
        isChargingKi = nbt.getBoolean("IsChargingKi");
        kiXP = nbt.getDouble("KiXP");
        strengthXP = nbt.getDouble("StrengthXP");
        durabilityXP = nbt.getDouble("DurabilityXP");
        maxHpXP = nbt.getDouble("MaxHpXP");
        staminaXP = nbt.getDouble("StaminaXP");
        kiControlXP = nbt.getDouble("KiControlXP");
        hasFlight = nbt.getBoolean("HasFlight");
        hasKiSense = nbt.getBoolean("HasKiSense");
        hasInstantTransmission = nbt.getBoolean("HasIT");
    }

    public void copyFrom(PlayerData source) {
        this.race = source.race;
        this.hasChosenRace = source.hasChosenRace;
        this.hairStyles = source.hairStyles.clone();
        this.eyeTypes = source.eyeTypes.clone();
        this.hairColors = source.hairColors.clone();
        this.eyeColors = source.eyeColors.clone();
        this.skinColors = source.skinColors.clone();
        this.kiColors = source.kiColors.clone();
        this.secondaryColors = source.secondaryColors.clone();
        this.tailColor = source.tailColor;
        this.isFatMajin = source.isFatMajin;
        this.level = source.level;
        this.ki = source.ki;
        this.maxKi = source.maxKi;
        this.stamina = source.stamina;
        this.maxStamina = source.maxStamina;
        this.strength = source.strength;
        this.durability = source.durability;
        this.maxHP = source.maxHP;
        this.kiControl = source.kiControl;
        this.isTurboMode = source.isTurboMode;
        this.isChargingKi = source.isChargingKi;
        this.kiXP = source.kiXP;
        this.strengthXP = source.strengthXP;
        this.durabilityXP = source.durabilityXP;
        this.maxHpXP = source.maxHpXP;
        this.staminaXP = source.staminaXP;
        this.kiControlXP = source.kiControlXP;
        this.hasFlight = source.hasFlight;
        this.hasKiSense = source.hasKiSense;
        this.hasInstantTransmission = source.hasInstantTransmission;
    }
}