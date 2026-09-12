package com.ezzo.fluidtranslator.item;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.machine.IItemFluidIdentifier;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.StatCollector;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import java.util.List;

public class ItemFluidIdentifierReset extends Item implements IItemFluidIdentifier {

    public ItemFluidIdentifierReset() {
        this.setMaxStackSize(1);
    }

    @Override
    public FluidType getType(World world, int x, int y, int z, ItemStack stack) {
        return Fluids.NONE;
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean bool) {
        list.add(StatCollector.translateToLocal(getUnlocalizedName() + ".info"));
    }
}