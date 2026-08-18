package com.ezzo.fluidtranslator;


import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.hbm.inventory.fluid.Fluids;
import net.minecraftforge.common.config.Configuration;

import java.util.*;

public class ModConfig {

    public static Configuration config;

    /** This list contains NTM fluids for which we should
     * not register a Forge fluid.
     */
    public static Set<String> fluidBlacklist;

    public static BiMap<String, String> customMappings;

    public static String suffix;

    /**
     * Auto converting liquid to forge's or to hbm's fluid systems
     */
    public static boolean enableUniversalFluidPorts;

    /**
     * Trying to push liquid into forge every sec
     */
    public static boolean enableAutoPushToForge;

    public static boolean enablePipeForeignConnect;

    public static boolean debugPipeForeignConnect;

    /**
     * Lets NTM pipes/ducts themselves be filled/drained directly
     */
    public static boolean enablePipeExternalPort;

    /**
     * Loads configs from file and sets their values in game.
     * Finally, saves the configs if they have changed.
     */
    public static void syncConfig() {
        config.load();

        String[] blacklist = config.getStringList("fluidBlacklist",
                "conversion",
                new String[] {
                        Fluids.NONE.getName(), "CUSTOM_DEMO"
                },
                "Fluids in the blacklist do not receive an automatic mapping.\n" +
                        "For more info visit https://github.com/Ezzocorbi/bob-fluid-translator/wiki/Configs\n");
        ModConfig.fluidBlacklist = new HashSet<String>(Arrays.asList(blacklist));

        String[] stringMappings = config.getStringList(
                "mappings",
                "conversion",
                new String[] {
                        "WATZ=mud_fluid", "WATER=water", "LAVA=lava"
                },
                "Overrides the automatic mapping by defining custom NTM to Forge fluid associations.\n" +
                        "These take precedence over automatic mapping.\n" +
                        "For more info visit https://github.com/Ezzocorbi/bob-fluid-translator/wiki/Configs\n"
        );
        ModConfig.customMappings = parseMappings(stringMappings);

        ModConfig.suffix = config.getString(
                "suffix",
                "conversion",
                "_fluid",
                "Suffix used for automatically generated Forge fluid IDs.\n" +
                        "Set to empty string for better compatibility with other mods.\n" +
                        "Default value \"_fluid\" is kept for backward compatibility with\n" +
                        "older versions of this mod, to avoid breaking existing worlds."
        );

        ModConfig.enableUniversalFluidPorts = config.getBoolean(
                "enableUniversalFluidPorts",
                "universalPorts",
                true,
                "If true, every NTM fluid receiver/sender/transceiver also directly implements\n" +
                        "Forge's IFluidHandler, so fluid can be moved straight between NTM machines/tanks\n" +
                        "and any Forge-based system (pipes, AE2 fluid interfaces, hoppers, etc.) with no\n" +
                        "Adapter block in between. Set to false to fall back to requiring the Adapter block.\n"
        );

        ModConfig.enableAutoPushToForge = config.getBoolean(
                "enableAutoPushToForge",
                "universalPorts",
                true,
                "EXPERIMENTAL. If true, NTM fluid senders also actively try to push fluid into a\n" +
                        "neighboring plain-Forge fluid handler every tick, instead of only reacting when\n" +
                        "something else calls them. Disable this if fluid seems to leak somewhere unexpected;\n" +
                        "everything initiated from the Forge side keeps working regardless of this setting.\n"
        );

        ModConfig.enablePipeForeignConnect = config.getBoolean(
                "enablePipeForeignConnect",
                "universalPorts",
                true,
                "If true, NTM fluid pipes (ducts, valves, gauges, etc.) automatically look for plain-Forge\n" +
                        "fluid handlers touching other mods' tanks, and so on -\n" +
                        "and connect to them directly, exactly as if they were native NTM machines. This lets a\n" +
                        "pipe run end at a foreign tank with no NTM machine or Adapter block at the boundary.\n" +
                        "Requires enableUniversalFluidPorts to also be true.\n"
        );

        ModConfig.debugPipeForeignConnect = config.getBoolean(
                "debugPipeForeignConnect",
                "universalPorts",
                false,
                "TEMPORARY DIAGNOSTIC. If true, spams the log every tick explaining exactly why each pipe\n" +
                        "did or didn't wire up to a foreign neighbor. Only turn on while debugging, then off again.\n"
        );

        ModConfig.enablePipeExternalPort = config.getBoolean(
                "enablePipeExternalPort",
                "universalPorts",
                true,
                "If true, NTM pipes/ducts also directly implement Forge's IFluidHandler themselves, not just\n" +
                        "their machine/tank endpoints. This is what lets an ACTIVE foreign component that reaches\n" +
                        "into the world on its own - such as an AE2FluidCraft-Rework fluid import/export bus placed\n" +
                        "on an ME cable next to a bare duct - fill or drain that duct directly, with no NTM machine\n" +
                        "or tank required at the boundary. The duct has no buffer of its own: it relays straight\n" +
                        "into whatever's actually on its network, so it never throttles below real network\n" +
                        "throughput. Requires enableUniversalFluidPorts to also be true.\n"
        );

        if (config.hasChanged()) config.save();
    }

    /**
     * Parses an array of fluid mapping definitions and converts them into a key-value map.
     * <p>
     * Each element of the {@code mappings} array must be in the format:
     * <pre>
     * "NTM Fluid Name=Forge Fluid Name"
     * </pre>
     * For example:
     * <pre>
     * {"WATER=water", "HELIUM=helium"}
     * </pre>
     * would produce a map where:
     * <ul>
     *   <li>{@code "WATER"} maps to {@code "water"}</li>
     *   <li>{@code "HELIUM"} maps to {@code "helium"}</li>
     * </ul>
     * <p>
     * The method performs basic validation and will throw an exception if any entry does not match
     * the expected format (i.e. if it doesn't split into exactly two parts separated by {@code "="}).
     *
     * @param raw an array of strings representing fluid name mappings in the format
     *                 {@code "NTM Fluid=Forge Fluid"}
     * @return a {@link Map} where the key is the NTM fluid name and the value is the Forge fluid name
     * @throws RuntimeException if any mapping string is invalid or malformed
     */
    private static BiMap<String, String> parseMappings(String[] raw) {
        BiMap<String, String> customMappings = HashBiMap.create();
        for(String s: raw) {
            String[] parts = s.split("=", 2);
            if (parts.length != 2) throw new RuntimeException("Invalid mapping in config: " + s);
            customMappings.put(parts[0].trim(), parts[1].trim());
        }
        return customMappings;
    }
}