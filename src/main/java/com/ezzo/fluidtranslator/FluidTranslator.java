package com.ezzo.fluidtranslator;

import com.ezzo.fluidtranslator.blocks.ModBlocks;
import com.ezzo.fluidtranslator.container.GuiHandler;
import com.ezzo.fluidtranslator.item.HBMAdapterItemBlock;
import com.ezzo.fluidtranslator.item.UniversalTankItemBlock;
import com.ezzo.fluidtranslator.network.ModNetwork;
import com.ezzo.fluidtranslator.tileentity.TileEntityHBMAdapter;
import com.ezzo.fluidtranslator.tileentity.TileEntityUniversalTank;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModItems;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fluids.FluidRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = FluidTranslator.MODID, version = FluidTranslator.VERSION, dependencies = "required-after:hbm")
public class FluidTranslator
{
    public static final String MODID = "bobfluidtranslator";
    public static final String VERSION = "2.1.8";

    @SidedProxy(
            clientSide = "com.ezzo.fluidtranslator.ClientProxy",
            serverSide = "com.ezzo.fluidtranslator.CommonProxy"
    )

    public static CommonProxy proxy;

    @Mod.Instance(FluidTranslator.MODID)
    public static FluidTranslator instance;

    public static Logger logger = LogManager.getLogger(MODID);

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        ModNetwork.init();

        ModConfig.config = new Configuration(event.getSuggestedConfigurationFile());
        ModConfig.syncConfig();

        // Register a Forge fluid for each NTM fluid
        ModFluidRegistry ft = new ModFluidRegistry();
        for(FluidType f : Fluids.getAll()) {
            if (ModFluidRegistry.isBlackListed(f)) continue;

            // Don't register the forge fluid if it's already registered.
            // This may be the case if a mapping was added manually.
            if (ModFluidRegistry.getForgeFluid(f) != null) continue;

            // Don't register the forge fluid if another mod already did it
            // This doesn't always work because other mods might get loaded after this one
            if (FluidRegistry.getFluid(f.getName().toLowerCase()) != null) continue;
            ft.registerFluidType(f);
        }
        proxy.registerTanslations();

        ModBlocks.initBlocks();

        GameRegistry.registerBlock(ModBlocks.universalTank, UniversalTankItemBlock.class, "universalTank");
        GameRegistry.registerBlock(ModBlocks.universalTankLarge, UniversalTankItemBlock.class, "universalTankLarge");
        GameRegistry.registerBlock(ModBlocks.hbmAdapter, HBMAdapterItemBlock.class, "ntmAdapter");

        GameRegistry.registerTileEntity(TileEntityUniversalTank.class, "teUniversalTank");
        GameRegistry.registerTileEntity(TileEntityHBMAdapter.class, "teNTMAdapter");
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
        proxy.registerEvents();
        addRecipes();
    }

    public void addRecipes() {
        GameRegistry.addRecipe(new ItemStack(ModBlocks.universalTank),
                "XLX",
                "XCX",
                "XLX",
                'X', ModItems.plate_polymer,
                'L', ModItems.plate_lead,
                'C', ModItems.fluid_tank_empty
        );

        GameRegistry.addRecipe(new ItemStack(ModBlocks.universalTankLarge),
                "XLX",
                "XCX",
                "XLX",
                'X', ModItems.plate_polymer,
                'L', ModItems.plate_lead,
                'C', ModItems.fluid_barrel_empty
        );

        GameRegistry.addRecipe(new ItemStack(ModBlocks.hbmAdapter),
                "XWX",
                "ZYZ",
                "XWX",
                'X', ModItems.plate_steel,
                'Y', new ItemStack(ModItems.circuit, 1, 8),
                'W', Items.comparator,
                'Z', new ItemStack(com.hbm.blocks.ModBlocks.fluid_duct_neo)
        );
    }
}
