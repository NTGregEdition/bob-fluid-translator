package com.ezzo.fluidtranslator.item;

import com.ezzo.fluidtranslator.blocks.BlockUniversalTank;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import java.util.List;

public class UniversalTankItemBlock extends ItemBlock {

    public UniversalTankItemBlock(Block block) {
        super(block);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        int capacity = ((BlockUniversalTank) field_150939_a).getCapacity();
        list.add(StatCollector.translateToLocalFormatted("tooltip.universal_tank.capacity", capacity));

        list.add(EnumChatFormatting.YELLOW + "" + EnumChatFormatting.ITALIC + StatCollector.translateToLocal("tooltip.hold_shift"));

        if (GuiScreen.isShiftKeyDown()) {
            String formatting = EnumChatFormatting.GRAY + "" + EnumChatFormatting.ITALIC;

            list.add(formatting + StatCollector.translateToLocal("tooltip.universal_tank.desc1"));
            list.add(formatting + StatCollector.translateToLocal("tooltip.universal_tank.desc2"));
            list.add(formatting + StatCollector.translateToLocal("tooltip.universal_tank.desc3"));
        }
    }
}
