package com.ezzo.fluidtranslator.asm;

import com.ezzo.fluidtranslator.FluidTranslator;
import com.ezzo.fluidtranslator.ModConfig;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.IFluidHandler;

import java.lang.reflect.Method;

final class AE2PartHostCompat {

    private AE2PartHostCompat() {
    }

    private static final String PART_HOST_CLASS = "appeng.api.parts.IPartHost";

    private static boolean resolved = false;
    private static Class<?> partHostClass;
    private static Method getPartMethod;

    private static void resolve() {
        if (resolved) return;
        resolved = true;
        try {
            partHostClass = Class.forName(PART_HOST_CLASS);
            getPartMethod = partHostClass.getMethod("getPart", ForgeDirection.class);
            FluidTranslator.logger.info("AE2PartHostCompat: found " + PART_HOST_CLASS
                    + "#getPart(ForgeDirection) via reflection - AE2/ExtraCells2 fluid buses can be bridged.");
        } catch (Throwable t) {
            partHostClass = null;
            getPartMethod = null;
            FluidTranslator.logger.warn("AE2PartHostCompat: could not resolve " + PART_HOST_CLASS
                    + "#getPart(ForgeDirection) via reflection (" + t.getClass().getSimpleName()
                    + ": " + t.getMessage() + "). AE2/ExtraCells2 fluid buses will NOT be bridged. "
                    + "This is expected if AE2 isn't installed; if it IS installed, this AE2 build's "
                    + "part API doesn't match what this coremod expects.");
        }
    }

    static Object resolveFluidHandler(TileEntity te, ForgeDirection sideOnHost) {
        if (te == null) return null;

        boolean debug = ModConfig.debugPipeForeignConnect;

        resolve();
        if (partHostClass != null && getPartMethod != null && partHostClass.isInstance(te)) {
            try {
                Object part = getPartMethod.invoke(te, sideOnHost);
                if (debug) {
                    FluidTranslator.logger.info("AE2PartHostCompat: " + te.getClass().getName()
                            + " is an IPartHost, part on side " + sideOnHost + " = "
                            + (part == null ? "null (nothing attached on that face)" : part.getClass().getName())
                            + (part != null ? (", isFluidHandler=" + (part instanceof IFluidHandler)) : ""));
                }
                if (part instanceof IFluidHandler) return part;
            } catch (Throwable t) {
                if (debug) {
                    FluidTranslator.logger.warn("AE2PartHostCompat: getPart(" + sideOnHost + ") threw on "
                            + te.getClass().getName(), t);
                }
            }
        }

        if (te instanceof IFluidHandler) {
            if (debug) {
                FluidTranslator.logger.info("AE2PartHostCompat: " + te.getClass().getName()
                        + " has no usable AE2 part on side " + sideOnHost
                        + " - falling back to the tile itself, which directly implements IFluidHandler.");
            }
            return te;
        }

        return null;
    }
}