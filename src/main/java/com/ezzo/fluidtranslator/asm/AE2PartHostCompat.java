package com.ezzo.fluidtranslator.asm;

import com.ezzo.fluidtranslator.FluidTranslator;
import com.ezzo.fluidtranslator.ModConfig;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.IFluidHandler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

final class AE2PartHostCompat {

    private AE2PartHostCompat() {
    }

    private static final String PART_HOST_CLASS = "appeng.api.parts.IPartHost";

    // Known AE2FluidCraft-Rework parts that reach OUT to a neighbor's Forge
    // IFluidHandler on their own tick, instead of exposing an IFluidHandler of
    // their own for something else to call into. resolveFluidHandler() below can
    // never find these (by design - they simply aren't IFluidHandlers), so a duct
    // sitting next to one never sees a fillable/drainable target on that face and
    // never visually connects, even though pipeFill/pipeDrain already move fluid
    // through it fine once an AE2 bus reaches in. isActiveForeignFluidPart() below
    // exists purely to fix that visual gap.
    private static final String[] ACTIVE_FLUID_PART_CLASSES = {
            "com.glodblock.github.common.parts.PartFluidImportBus",
            "com.glodblock.github.common.parts.PartFluidExportBus"
    };

    private static boolean resolved = false;
    private static Class<?> partHostClass;
    private static Method getPartMethod;

    private static boolean activePartsResolved = false;
    private static Class<?>[] activeFluidPartClasses = new Class<?>[0];

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

    private static void resolveActiveParts() {
        if (activePartsResolved) return;
        activePartsResolved = true;

        List<Class<?>> found = new ArrayList<Class<?>>();
        for (String name : ACTIVE_FLUID_PART_CLASSES) {
            try {
                found.add(Class.forName(name));
            } catch (Throwable ignored) {
                // AE2FluidCraft-Rework isn't installed, or this particular build doesn't
                // have this class under this name - just skip it. Nothing else depends
                // on this list; it only widens what a duct visually recognizes.
            }
        }

        activeFluidPartClasses = found.toArray(new Class<?>[0]);
        if (activeFluidPartClasses.length > 0) {
            FluidTranslator.logger.info("AE2PartHostCompat: found " + activeFluidPartClasses.length
                    + " AE2FluidCraft-Rework active fluid bus class(es) - ducts will visually connect to them.");
        }
    }

    /**
     * Whether the part on {@code sideOnHost} of {@code te} is a known "active" fluid
     * bus (see {@link #ACTIVE_FLUID_PART_CLASSES}) - one that reaches out and calls
     * fill()/drain() on ITS neighbor by itself, rather than being a passive
     * IFluidHandler something else calls into. Used only to decide whether a duct
     * should visually render/collide as connected on that face; it plays no part in
     * the actual fluid transfer, which already happens via the duct's own
     * IFluidHandler fill()/drain() (see UniversalFluidBridge#pipeFill/pipeDrain).
     */
    static boolean isActiveForeignFluidPart(TileEntity te, ForgeDirection sideOnHost) {
        if (te == null) return false;

        resolve();
        if (partHostClass == null || getPartMethod == null || !partHostClass.isInstance(te)) return false;

        resolveActiveParts();
        if (activeFluidPartClasses.length == 0) return false;

        try {
            Object part = getPartMethod.invoke(te, sideOnHost);
            if (part == null) return false;

            for (Class<?> cls : activeFluidPartClasses) {
                if (cls.isInstance(part)) return true;
            }
        } catch (Throwable t) {
            if (ModConfig.debugPipeForeignConnect) {
                FluidTranslator.logger.warn("AE2PartHostCompat: getPart(" + sideOnHost + ") threw while "
                        + "checking for an active fluid bus part on " + te.getClass().getName(), t);
            }
        }

        return false;
    }
}