package com.ezzo.fluidtranslator;

import com.ezzo.fluidtranslator.blocks.CustomFluidBlock;
import com.ezzo.fluidtranslator.item.CustomFluidItemBlock;
import com.ezzo.fluidtranslator.item.GenericBucket;
import com.hbm.inventory.FluidContainer;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModItems;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class manages the registration and translation of fluids from the HBM mod
 * into the Forge fluid system.
 * <p>
 * Given an instance of {@link com.hbm.inventory.fluid.FluidType} (an HBM-defined fluid),
 * the class handles the registration of the corresponding {@link net.minecraftforge.fluids.Fluid}
 * through the Forge API, automatically creating the associated block and bucket.
 * </p>
 *
 * <p>
 * The conversion (lookup) between HBM fluids and Forge fluids is based on a naming convention:
 * <ul>
 *   <li><b>HBM</b> uses the format: <code>"FLUID_NAME"</code> (uppercase, no suffix)</li>
 *   <li><b>Forge</b> uses the format: <code>"fluid_name_fluid"</code> (lowercase, with a <code>_fluid</code> suffix)</li>
 * </ul>
 * Custom exceptions to this naming rule are handled through an internal lookup table.
 *
 */
public class ModFluidRegistry {

    public ModFluidRegistry() {

    }

    /**
     * Given a {@link FluidType} from HBM, this method registers a corresponding Forge Fluid ({@link Fluid})
     * @param fluidType HBM fluid
     * @return Returns the fluid block associated to the ForgeFluid
     */
    public CustomFluidBlock registerFluidType(FluidType fluidType) {
        String name = ModConfig.customMappings.getOrDefault(
                fluidType.getName(),
                fluidType.getName().toLowerCase() + ModConfig.suffix);

        Fluid forgeFluid = new Fluid(name);
        FluidRegistry.registerFluid(forgeFluid);

        CustomFluidBlock block = new CustomFluidBlock(forgeFluid, Material.water, name);
        GameRegistry.registerBlock(block, CustomFluidItemBlock.class, name + "_block");
        forgeFluid.setBlock(block);

        GenericBucket genericBucket = new GenericBucket(forgeFluid, block);
        GameRegistry.registerItem(genericBucket, name + "_bucket");

        FluidContainerRegistry.registerFluidContainer(
                new FluidStack(forgeFluid, FluidContainerRegistry.BUCKET_VOLUME),
                new ItemStack(genericBucket),
                new ItemStack(Items.bucket)
        );

        return block;
    }

    /**
     * Returns the corresponding HBM fluid
     * @param fluid Forge fluid
     * @return returns NONE {@link FluidType} if there is no correspondence
     */
    public static FluidType getHBMFluid(Fluid fluid) {
        String fluidName = ModConfig.customMappings.inverse().getOrDefault(
                fluid.getName(),
                fluid.getName()
                        .replaceFirst(ModConfig.suffix + "$", "")
                        .toUpperCase());
        return Fluids.fromName(fluidName);
    }

    /**
     * Returns the corresponding Forge fluid
     * @param fluidType HBM fluid
     * @return returns null if there is no correspondence (like for black-listed fluids)
     */
    public static Fluid getForgeFluid(FluidType fluidType) {
        String fluidName = ModConfig.customMappings.getOrDefault(
                fluidType.getName(),
                fluidType.getName().toLowerCase() + ModConfig.suffix
        );
        return FluidRegistry.getFluid(fluidName);
    }

    /**
     *
     * @param fluidType Fluid to check
     * @return Returns true if the fluid should not be registered
     */
    public static boolean isBlackListed(FluidType fluidType) {
        return ModConfig.fluidBlacklist.contains(fluidType.getName());
    }

    /**
     * Returns all registered {@link FluidType}s that are not blacklisted.
     *
     * @return an {@link Iterable} of valid, non-blacklisted {@link FluidType}s
     */
    public static Iterable<FluidType> validFluids() {
        List<FluidType> result = new ArrayList<>();

        Arrays.stream(Fluids.getAll())
                .filter(f -> !ModFluidRegistry.isBlackListed(f))
                .forEach(result::add);

        return result;
    }

    public static void bridgeHBMContainersToForge() {
        if (!ModConfig.enableHBMContainerBridge) return;

        int registered = 0;
        int skipped = 0;

        for (FluidContainer con : com.hbm.inventory.FluidContainerRegistry.allContainers) {

            if (con == null || con.fullContainer == null || con.emptyContainer == null || con.content <= 0) {
                skipped++;
                continue;
            }

            Fluid forgeFluid = getForgeFluid(con.type);
            if (forgeFluid == null) {
                skipped++;
                continue;
            }

            ItemStack full = con.fullContainer.copy();
            full.stackSize = 1;

            try {
                if (FluidContainerRegistry.isFilledContainer(full)) {
                    skipped++;
                    continue;
                }

                ItemStack empty = con.emptyContainer.copy();
                empty.stackSize = 1;

                FluidContainerRegistry.registerFluidContainer(new FluidStack(forgeFluid, con.content), full, empty);
                registered++;
            } catch (Throwable t) {
                skipped++;
                FluidTranslator.logger.warn("ModFluidRegistry: couldn't bridge HBM container "
                        + full.getItem().getUnlocalizedName() + ":" + full.getItemDamage()
                        + " (fluid " + con.type.getName() + ") into Forge's FluidContainerRegistry", t);
            }
        }

        FluidType[] order = Fluids.getInNiceOrder();
        for (int i = 1; i < order.length; i++) {
            FluidType type = order[i];

            Fluid forgeFluid = getForgeFluid(type);
            if (forgeFluid == null) {
                skipped++;
                continue;
            }

            if (!type.hasNoContainer()) {
                ItemStack full = new ItemStack(ModItems.fluid_pack_full, 1, type.getID());
                try {
                    if (!FluidContainerRegistry.isFilledContainer(full)) {
                        FluidContainerRegistry.registerFluidContainer(new FluidStack(forgeFluid, 32000),
                                full, new ItemStack(ModItems.fluid_pack_empty));
                        registered++;
                    } else skipped++;
                } catch (Throwable t) {
                    skipped++;
                    FluidTranslator.logger.warn("ModFluidRegistry: couldn't bridge fluid pack for " + type.getName(), t);
                }
            }

            ItemStack fullId = new ItemStack(ModItems.fluid_identifier_multi, 1, type.getID());
            try {
                if (!FluidContainerRegistry.isFilledContainer(fullId)) {
                    FluidContainerRegistry.registerFluidContainer(new FluidStack(forgeFluid, FluidContainerRegistry.BUCKET_VOLUME),
                            fullId, new ItemStack(ModItems.fluid_identifier_multi, 1, 0));
                    registered++;
                } else skipped++;
            } catch (Throwable t) {
                skipped++;
                FluidTranslator.logger.warn("ModFluidRegistry: couldn't bridge fluid identifier for " + type.getName(), t);
            }

            ItemStack fullIcon = new ItemStack(ModItems.fluid_icon, 1, type.getID());
            try {
                if (!FluidContainerRegistry.isFilledContainer(fullIcon)) {
                    FluidContainerRegistry.registerFluidContainer(new FluidStack(forgeFluid, FluidContainerRegistry.BUCKET_VOLUME),
                            fullIcon, new ItemStack(ModItems.fluid_icon, 1, 0));
                    registered++;
                } else skipped++;
            } catch (Throwable t) {
                skipped++;
                FluidTranslator.logger.warn("ModFluidRegistry: couldn't bridge fluid icon for " + type.getName(), t);
            }
        }

        FluidTranslator.logger.info("ModFluidRegistry: bridged " + registered
                + " HBM fluid container(s) into Forge's FluidContainerRegistry (" + skipped
                + " skipped - no Forge fluid mapping, already registered, or invalid entry). "
                + "These now behave like plain Forge fluid containers anywhere that honors the "
                + "standard convention, including AE2FluidCraft-Rework's fluid terminal/pattern UIs.");
    }

    /**
     *
     * @param fluid Fluid to check
     * @return True if a texture exists for the fluid {@code fluid}, false otherwise
     */
    public static boolean textureExists(FluidType fluid) {
        try {
            Minecraft.getMinecraft()
                    .getResourceManager()
                    .getResource(fluid.getTexture());
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
