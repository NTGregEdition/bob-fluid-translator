package com.ezzo.fluidtranslator.asm;

import api.hbm.fluidmk2.IFluidProviderMK2;
import api.hbm.fluidmk2.IFluidReceiverMK2;
import com.ezzo.fluidtranslator.ModFluidRegistry;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.tank.FluidTank;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

public final class ForeignFluidPort implements IFluidReceiverMK2, IFluidProviderMK2 {

    public final TileEntity target;
    public final ForgeDirection sideOnForeign;
    private final World world;
    private final int x, y, z;

    public ForeignFluidPort(TileEntity target, ForgeDirection sideOnForeign) {
        this.target = target;
        this.world = target.getWorldObj();
        this.x = target.xCoord;
        this.y = target.yCoord;
        this.z = target.zCoord;
        this.sideOnForeign = sideOnForeign;
    }

    private IFluidHandler handler() {
        if (target == null || target.isInvalid()) return null;
        Object resolved = AE2PartHostCompat.resolveFluidHandler(target, sideOnForeign);
        return resolved instanceof IFluidHandler ? (IFluidHandler) resolved : null;
    }

    // ------------------------------------------------------------------
    // ILoadedTile / IFluidUserMK2
    // ------------------------------------------------------------------

    @Override
    public boolean isLoaded() {
        return target != null && !target.isInvalid() && world != null && world.blockExists(x, y, z);
    }

    @Override
    public FluidTank[] getAllTanks() {
        return new FluidTank[0];
    }

    // ------------------------------------------------------------------
    // IFluidReceiverMK2 - pushing fluid FROM the NTM network INTO the foreign tile
    // ------------------------------------------------------------------

    @Override
    public long getDemand(FluidType type, int pressure) {
        IFluidHandler h = handler();
        if (h == null) return 0;
        Fluid forgeFluid = ModFluidRegistry.getForgeFluid(type);
        if (forgeFluid == null) return 0;

        try {
            if (!h.canFill(sideOnForeign, forgeFluid)) return 0;

            FluidTankInfo[] infos = h.getTankInfo(sideOnForeign);
            if (infos == null) return 0;

            long space = 0;
            for (FluidTankInfo info : infos) {
                if (info == null) continue;
                FluidStack current = info.fluid;
                if (current == null || current.getFluid() == forgeFluid) {
                    int free = info.capacity - (current == null ? 0 : current.amount);
                    if (free > 0) space += free;
                }
            }
            return space;
        } catch (Throwable t) {
            return 0;
        }
    }

    @Override
    public long transferFluid(FluidType type, int pressure, long amount) {
        IFluidHandler h = handler();
        if (h == null || amount <= 0) return amount;
        Fluid forgeFluid = ModFluidRegistry.getForgeFluid(type);
        if (forgeFluid == null) return amount;

        try {
            int toOffer = (int) Math.min(amount, Integer.MAX_VALUE);
            int accepted = h.fill(sideOnForeign, new FluidStack(forgeFluid, toOffer), true);
            return amount - accepted;
        } catch (Throwable t) {
            return amount;
        }
    }

    // ------------------------------------------------------------------
    // IFluidProviderMK2 - pulling fluid FROM the foreign tile INTO the NTM network
    // ------------------------------------------------------------------

    @Override
    public long getFluidAvailable(FluidType type, int pressure) {
        IFluidHandler h = handler();
        if (h == null) return 0;
        Fluid forgeFluid = ModFluidRegistry.getForgeFluid(type);
        if (forgeFluid == null) return 0;

        try {
            if (!h.canDrain(sideOnForeign, forgeFluid)) return 0;

            FluidTankInfo[] infos = h.getTankInfo(sideOnForeign);
            if (infos == null) return 0;

            long amount = 0;
            for (FluidTankInfo info : infos) {
                if (info == null || info.fluid == null) continue;
                if (info.fluid.getFluid() == forgeFluid) amount += info.fluid.amount;
            }
            return amount;
        } catch (Throwable t) {
            return 0;
        }
    }

    @Override
    public void useUpFluid(FluidType type, int pressure, long amount) {
        IFluidHandler h = handler();
        if (h == null || amount <= 0) return;
        Fluid forgeFluid = ModFluidRegistry.getForgeFluid(type);
        if (forgeFluid == null) return;

        try {
            int toDrain = (int) Math.min(amount, Integer.MAX_VALUE);
            h.drain(sideOnForeign, new FluidStack(forgeFluid, toDrain), true);
        } catch (Throwable t) {
            // bruh nothing
        }
    }
}