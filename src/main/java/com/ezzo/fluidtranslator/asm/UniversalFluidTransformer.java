package com.ezzo.fluidtranslator.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.FrameNode;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UniversalFluidTransformer implements IClassTransformer {

    private static final Logger LOG = Logger.getLogger("BobFluidTranslatorCore");

    private static final String RECEIVER = "api/hbm/fluidmk2/IFluidStandardReceiverMK2";
    private static final String SENDER = "api/hbm/fluidmk2/IFluidStandardSenderMK2";
    private static final String TRANSCEIVER = "api/hbm/fluidmk2/IFluidStandardTransceiverMK2";
    private static final String BASE_RECEIVER = "api/hbm/fluidmk2/IFluidReceiverMK2";
    private static final String BASE_PROVIDER = "api/hbm/fluidmk2/IFluidProviderMK2";
    private static final String PROXY_BASE = "com/hbm/tileentity/TileEntityProxyBase";
    private static final String PIPE_BASE = "com/hbm/tileentity/network/TileEntityPipeBaseNT";
    private static final String EXHAUST_PIPE = "com/hbm/tileentity/network/TileEntityPipeExhaust";
    private static final String LIBRARY = "com/hbm/lib/Library";

    private static final String LEGACY_TRANSCEIVER = "api/hbm/fluid/IFluidStandardTransceiver";

    private static final String TICK_DEOBF = "updateEntity";
    private static final String TICK_SRG = "func_145845_h";

    private static final String FLUID_HANDLER = "net/minecraftforge/fluids/IFluidHandler";
    private static final String BRIDGE = "com/ezzo/fluidtranslator/asm/UniversalFluidBridge";

    private static final String DIR = "Lnet/minecraftforge/common/util/ForgeDirection;";
    private static final String FLUID = "Lnet/minecraftforge/fluids/Fluid;";
    private static final String STACK = "Lnet/minecraftforge/fluids/FluidStack;";
    private static final String TANKINFO_ARR = "[Lnet/minecraftforge/fluids/FluidTankInfo;";

    private static final String TRY_PROVIDE_DESC =
            "(Lcom/hbm/inventory/fluid/FluidType;ILnet/minecraft/world/World;IIILnet/minecraftforge/common/util/ForgeDirection;)V";

    private static final String CAN_CONNECT_FLUID_DESC =
            "(Lnet/minecraft/world/IBlockAccess;III" + DIR + "Lcom/hbm/inventory/fluid/FluidType;)Z";

    private static final String FLUID_TANK = "com/hbm/inventory/fluid/tank/FluidTank";
    private static final String ITEM_STACK = "Lnet/minecraft/item/ItemStack;";
    private static final String SET_TYPE_DESC = "(II[" + ITEM_STACK + ")Z";
    private static final String SHOULD_BLOCK_RESET_DESC = "(L" + FLUID_TANK + ";I[" + ITEM_STACK + ")Z";

    private static final String COMPRESSOR_BASE = "com/hbm/tileentity/machine/TileEntityMachineCompressorBase";
    private static final String SETUP_TANKS_DESC = "()V";
    private static final String SHOULD_SKIP_COMPRESSOR_DESC = "(L" + COMPRESSOR_BASE + ";)Z";

    private static final String FLUID_TYPE = "Lcom/hbm/inventory/fluid/FluidType;";
    private static final String SET_TANK_TYPE_DESC = "(" + FLUID_TYPE + ")V";
    private static final String SHOULD_BLOCK_TYPE_CHANGE_DESC = "(L" + FLUID_TANK + ";" + FLUID_TYPE + ")Z";
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || transformedName == null) return basicClass;

        try {
            if (transformedName.equals("api.hbm.fluidmk2.IFluidStandardReceiverMK2")) {
                return patchReceiver(basicClass);
            }
            if (transformedName.equals("api.hbm.fluidmk2.IFluidStandardSenderMK2")) {
                return patchSender(basicClass);
            }
            if (transformedName.equals("api.hbm.fluidmk2.IFluidStandardTransceiverMK2")) {
                return patchTransceiver(basicClass);
            }
            if (transformedName.equals("api.hbm.fluid.IFluidStandardTransceiver")) {
                return patchLegacyTransceiver(basicClass);
            }
            if (transformedName.equals("api.hbm.fluidmk2.IFluidReceiverMK2")) {
                return patchBaseReceiver(basicClass);
            }
            if (transformedName.equals("api.hbm.fluidmk2.IFluidProviderMK2")) {
                return patchBaseProvider(basicClass);
            }
            if (transformedName.equals("com.hbm.tileentity.TileEntityProxyBase")) {
                return patchProxyBase(basicClass);
            }
            if (transformedName.equals("com.hbm.tileentity.network.TileEntityPipeBaseNT")) {
                return patchPipeBase(basicClass);
            }
            if (transformedName.equals("com.hbm.tileentity.network.TileEntityPipeExhaust")) {
                return patchPipeExhaust(basicClass);
            }
            if (transformedName.equals("com.hbm.lib.Library")) {
                return patchLibrary(basicClass);
            }

            if (transformedName.startsWith("com.hbm.tileentity.")) {
                return patchDiamondConflict(basicClass, transformedName);
            }
            if (transformedName.equals("com.hbm.inventory.fluid.tank.FluidTank")) {
                return patchFluidTank(basicClass);
            }
            if (transformedName.equals("com.hbm.tileentity.machine.TileEntityMachineCompressorBase")) {
                return patchCompressorBase(basicClass);
            }
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "Failed to patch " + transformedName + " for universal fluid ports", t);
            return basicClass;
        }

        return basicClass;
    }

    // ------------------------------------------------------------------
    // IFluidStandardReceiverMK2
    // ------------------------------------------------------------------

    private byte[] patchReceiver(byte[] basicClass) {
        ClassNode cn = readClass(basicClass);
        if (cn.interfaces.contains(FLUID_HANDLER)) return basicClass; // already patched

        addInterface(cn, FLUID_HANDLER);

        String self = "L" + RECEIVER + ";";
        addTrampoline(cn, "fill", "(" + DIR + STACK + "Z)I",
                "receiverFill", "(" + self + DIR + STACK + "Z)I");
        addTrampoline(cn, "drain", "(" + DIR + STACK + "Z)" + STACK,
                "receiverDrain", "(" + self + DIR + STACK + "Z)" + STACK);
        addTrampoline(cn, "drain", "(" + DIR + "IZ)" + STACK,
                "receiverDrainAmount", "(" + self + DIR + "IZ)" + STACK);
        addTrampoline(cn, "canFill", "(" + DIR + FLUID + ")Z",
                "receiverCanFill", "(" + self + DIR + FLUID + ")Z");
        addTrampoline(cn, "canDrain", "(" + DIR + FLUID + ")Z",
                "receiverCanDrain", "(" + self + DIR + FLUID + ")Z");
        addTrampoline(cn, "getTankInfo", "(" + DIR + ")" + TANKINFO_ARR,
                "receiverTankInfo", "(" + self + DIR + ")" + TANKINFO_ARR);

        LOG.info("Patched " + RECEIVER + " with a universal Forge IFluidHandler bridge");
        return writeClass(cn);
    }

    // ------------------------------------------------------------------
    // IFluidStandardSenderMK2 (+ autonomous push into foreign handlers)
    // ------------------------------------------------------------------

    private byte[] patchSender(byte[] basicClass) {
        ClassNode cn = readClass(basicClass);
        boolean alreadyHandler = cn.interfaces.contains(FLUID_HANDLER);

        if (!alreadyHandler) {
            addInterface(cn, FLUID_HANDLER);

            String self = "L" + SENDER + ";";
            addTrampoline(cn, "fill", "(" + DIR + STACK + "Z)I",
                    "senderFill", "(" + self + DIR + STACK + "Z)I");
            addTrampoline(cn, "drain", "(" + DIR + STACK + "Z)" + STACK,
                    "senderDrain", "(" + self + DIR + STACK + "Z)" + STACK);
            addTrampoline(cn, "drain", "(" + DIR + "IZ)" + STACK,
                    "senderDrainAmount", "(" + self + DIR + "IZ)" + STACK);
            addTrampoline(cn, "canFill", "(" + DIR + FLUID + ")Z",
                    "senderCanFill", "(" + self + DIR + FLUID + ")Z");
            addTrampoline(cn, "canDrain", "(" + DIR + FLUID + ")Z",
                    "senderCanDrain", "(" + self + DIR + FLUID + ")Z");
            addTrampoline(cn, "getTankInfo", "(" + DIR + ")" + TANKINFO_ARR,
                    "senderTankInfo", "(" + self + DIR + ")" + TANKINFO_ARR);

            LOG.info("Patched " + SENDER + " with a universal Forge IFluidHandler bridge");
        }

        wrapTryProvide(cn);

        return writeClass(cn);
    }

    private void wrapTryProvide(ClassNode cn) {
        MethodNode original = findMethod(cn, "tryProvide", TRY_PROVIDE_DESC);
        if (original == null) {
            LOG.warning(SENDER + "#tryProvide" + TRY_PROVIDE_DESC + " not found - "
                    + "the installed NTM version may not match what this coremod expects. "
                    + "Skipping the autonomous-push patch (universal ports still work passively).");
            return;
        }

        if (findMethod(cn, "tryProvide$hbm", TRY_PROVIDE_DESC) != null) return; // already patched

        original.name = "tryProvide$hbm";

        MethodNode wrapper = new MethodNode(Opcodes.ACC_PUBLIC, "tryProvide", TRY_PROVIDE_DESC, null, null);
        InsnList il = new InsnList();

        // this.tryProvide$hbm(type, pressure, world, x, y, z, dir)
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new VarInsnNode(Opcodes.ALOAD, 1));
        il.add(new VarInsnNode(Opcodes.ILOAD, 2));
        il.add(new VarInsnNode(Opcodes.ALOAD, 3));
        il.add(new VarInsnNode(Opcodes.ILOAD, 4));
        il.add(new VarInsnNode(Opcodes.ILOAD, 5));
        il.add(new VarInsnNode(Opcodes.ILOAD, 6));
        il.add(new VarInsnNode(Opcodes.ALOAD, 7));
        il.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, SENDER, "tryProvide$hbm", TRY_PROVIDE_DESC, true));

        // UniversalFluidBridge.tryProvideToForge(this, type, pressure, world, x, y, z, dir)
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new VarInsnNode(Opcodes.ALOAD, 1));
        il.add(new VarInsnNode(Opcodes.ILOAD, 2));
        il.add(new VarInsnNode(Opcodes.ALOAD, 3));
        il.add(new VarInsnNode(Opcodes.ILOAD, 4));
        il.add(new VarInsnNode(Opcodes.ILOAD, 5));
        il.add(new VarInsnNode(Opcodes.ILOAD, 6));
        il.add(new VarInsnNode(Opcodes.ALOAD, 7));
        String bridgeDesc = "(L" + SENDER + ";Lcom/hbm/inventory/fluid/FluidType;ILnet/minecraft/world/World;IIILnet/minecraftforge/common/util/ForgeDirection;)V";
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BRIDGE, "tryProvideToForge", bridgeDesc, false));

        il.add(new InsnNode(Opcodes.RETURN));

        wrapper.instructions = il;
        cn.methods.add(wrapper);

        LOG.info("Patched " + SENDER + "#tryProvide to also push fluid into foreign IFluidHandlers");
    }

    // ------------------------------------------------------------------
    // IFluidStandardTransceiverMK2
    // ------------------------------------------------------------------

    private byte[] patchTransceiver(byte[] basicClass) {
        ClassNode cn = readClass(basicClass);
        if (cn.interfaces.contains(FLUID_HANDLER)) return basicClass; // already patched

        addInterface(cn, FLUID_HANDLER);

        String self = "L" + TRANSCEIVER + ";";
        addTrampoline(cn, "fill", "(" + DIR + STACK + "Z)I",
                "transceiverFill", "(" + self + DIR + STACK + "Z)I");
        addTrampoline(cn, "drain", "(" + DIR + STACK + "Z)" + STACK,
                "transceiverDrain", "(" + self + DIR + STACK + "Z)" + STACK);
        addTrampoline(cn, "drain", "(" + DIR + "IZ)" + STACK,
                "transceiverDrainAmount", "(" + self + DIR + "IZ)" + STACK);
        addTrampoline(cn, "canFill", "(" + DIR + FLUID + ")Z",
                "transceiverCanFill", "(" + self + DIR + FLUID + ")Z");
        addTrampoline(cn, "canDrain", "(" + DIR + FLUID + ")Z",
                "transceiverCanDrain", "(" + self + DIR + FLUID + ")Z");
        addTrampoline(cn, "getTankInfo", "(" + DIR + ")" + TANKINFO_ARR,
                "transceiverTankInfo", "(" + self + DIR + ")" + TANKINFO_ARR);

        LOG.info("Patched " + TRANSCEIVER + " with a universal Forge IFluidHandler bridge");
        return writeClass(cn);
    }

    // ------------------------------------------------------------------
    // api.hbm.fluid.IFluidStandardTransceiver (deprecated legacy interface)
    // ------------------------------------------------------------------

    private byte[] patchLegacyTransceiver(byte[] basicClass) {
        ClassNode cn = readClass(basicClass);
        if (cn.interfaces.contains(FLUID_HANDLER)) return basicClass; // already patched

        addInterface(cn, FLUID_HANDLER);

        String self = "L" + LEGACY_TRANSCEIVER + ";";
        addTrampoline(cn, "fill", "(" + DIR + STACK + "Z)I",
                "legacyTransceiverFill", "(" + self + DIR + STACK + "Z)I");
        addTrampoline(cn, "drain", "(" + DIR + STACK + "Z)" + STACK,
                "legacyTransceiverDrain", "(" + self + DIR + STACK + "Z)" + STACK);
        addTrampoline(cn, "drain", "(" + DIR + "IZ)" + STACK,
                "legacyTransceiverDrainAmount", "(" + self + DIR + "IZ)" + STACK);
        addTrampoline(cn, "canFill", "(" + DIR + FLUID + ")Z",
                "legacyTransceiverCanFill", "(" + self + DIR + FLUID + ")Z");
        addTrampoline(cn, "canDrain", "(" + DIR + FLUID + ")Z",
                "legacyTransceiverCanDrain", "(" + self + DIR + FLUID + ")Z");
        addTrampoline(cn, "getTankInfo", "(" + DIR + ")" + TANKINFO_ARR,
                "legacyTransceiverTankInfo", "(" + self + DIR + ")" + TANKINFO_ARR);

        LOG.info("Patched " + LEGACY_TRANSCEIVER + " with a universal Forge IFluidHandler bridge");
        return writeClass(cn);
    }

    // ------------------------------------------------------------------
    // IFluidReceiverMK2 / IFluidProviderMK2 (base interfaces)
    // ------------------------------------------------------------------

    private byte[] patchBaseReceiver(byte[] basicClass) {
        ClassNode cn = readClass(basicClass);
        if (cn.interfaces.contains(FLUID_HANDLER)) return basicClass;

        addInterface(cn, FLUID_HANDLER);

        String self = "L" + BASE_RECEIVER + ";";
        addTrampoline(cn, "fill", "(" + DIR + STACK + "Z)I",
                "baseReceiverFill", "(" + self + DIR + STACK + "Z)I");
        addTrampoline(cn, "drain", "(" + DIR + STACK + "Z)" + STACK,
                "baseReceiverDrain", "(" + self + DIR + STACK + "Z)" + STACK);
        addTrampoline(cn, "drain", "(" + DIR + "IZ)" + STACK,
                "baseReceiverDrainAmount", "(" + self + DIR + "IZ)" + STACK);
        addTrampoline(cn, "canFill", "(" + DIR + FLUID + ")Z",
                "baseReceiverCanFill", "(" + self + DIR + FLUID + ")Z");
        addTrampoline(cn, "canDrain", "(" + DIR + FLUID + ")Z",
                "baseReceiverCanDrain", "(" + self + DIR + FLUID + ")Z");
        addTrampoline(cn, "getTankInfo", "(" + DIR + ")" + TANKINFO_ARR,
                "baseReceiverTankInfo", "(" + self + DIR + ")" + TANKINFO_ARR);

        LOG.info("Patched " + BASE_RECEIVER + " with a universal Forge IFluidHandler bridge");
        return writeClass(cn);
    }

    private byte[] patchBaseProvider(byte[] basicClass) {
        ClassNode cn = readClass(basicClass);
        if (cn.interfaces.contains(FLUID_HANDLER)) return basicClass;

        addInterface(cn, FLUID_HANDLER);

        String self = "L" + BASE_PROVIDER + ";";
        addTrampoline(cn, "fill", "(" + DIR + STACK + "Z)I",
                "baseProviderFill", "(" + self + DIR + STACK + "Z)I");
        addTrampoline(cn, "drain", "(" + DIR + STACK + "Z)" + STACK,
                "baseProviderDrain", "(" + self + DIR + STACK + "Z)" + STACK);
        addTrampoline(cn, "drain", "(" + DIR + "IZ)" + STACK,
                "baseProviderDrainAmount", "(" + self + DIR + "IZ)" + STACK);
        addTrampoline(cn, "canFill", "(" + DIR + FLUID + ")Z",
                "baseProviderCanFill", "(" + self + DIR + FLUID + ")Z");
        addTrampoline(cn, "canDrain", "(" + DIR + FLUID + ")Z",
                "baseProviderCanDrain", "(" + self + DIR + FLUID + ")Z");
        addTrampoline(cn, "getTankInfo", "(" + DIR + ")" + TANKINFO_ARR,
                "baseProviderTankInfo", "(" + self + DIR + ")" + TANKINFO_ARR);

        LOG.info("Patched " + BASE_PROVIDER + " with a universal Forge IFluidHandler bridge");
        return writeClass(cn);
    }

    // ------------------------------------------------------------------
    // TileEntityProxyBase (multiblock dummy segments)
    // ------------------------------------------------------------------

    private byte[] patchProxyBase(byte[] basicClass) {
        ClassNode cn = readClass(basicClass);
        if (cn.interfaces.contains(FLUID_HANDLER)) return basicClass;

        addInterface(cn, FLUID_HANDLER);

        String self = "L" + PROXY_BASE + ";";
        addTrampoline(cn, "fill", "(" + DIR + STACK + "Z)I",
                "proxyFill", "(" + self + DIR + STACK + "Z)I");
        addTrampoline(cn, "drain", "(" + DIR + STACK + "Z)" + STACK,
                "proxyDrain", "(" + self + DIR + STACK + "Z)" + STACK);
        addTrampoline(cn, "drain", "(" + DIR + "IZ)" + STACK,
                "proxyDrainAmount", "(" + self + DIR + "IZ)" + STACK);
        addTrampoline(cn, "canFill", "(" + DIR + FLUID + ")Z",
                "proxyCanFill", "(" + self + DIR + FLUID + ")Z");
        addTrampoline(cn, "canDrain", "(" + DIR + FLUID + ")Z",
                "proxyCanDrain", "(" + self + DIR + FLUID + ")Z");
        addTrampoline(cn, "getTankInfo", "(" + DIR + ")" + TANKINFO_ARR,
                "proxyTankInfo", "(" + self + DIR + ")" + TANKINFO_ARR);

        LOG.info("Patched " + PROXY_BASE + " with a universal Forge IFluidHandler bridge (delegates to the real core)");
        return writeClass(cn);
    }

    // ------------------------------------------------------------------
    // TileEntityPipeBaseNT (pipes/ducts/valves/gauges - anything that routes fluid)
    // ------------------------------------------------------------------

    private byte[] patchPipeBase(byte[] basicClass) {
        ClassNode cn = readClass(basicClass);

        // Give the pipe/duct itself a Forge IFluidHandler face, so an ACTIVE
        // foreign component (like an AE2FluidCraft-Rework fluid import/export
        // bus, which reaches into the world on its own rather than waiting to
        // be discovered) can fill/drain a bare duct directly.
        if (!cn.interfaces.contains(FLUID_HANDLER)) {
            addInterface(cn, FLUID_HANDLER);

            String self = "L" + PIPE_BASE + ";";
            addTrampoline(cn, "fill", "(" + DIR + STACK + "Z)I",
                    "pipeFill", "(" + self + DIR + STACK + "Z)I");
            addTrampoline(cn, "drain", "(" + DIR + STACK + "Z)" + STACK,
                    "pipeDrain", "(" + self + DIR + STACK + "Z)" + STACK);
            addTrampoline(cn, "drain", "(" + DIR + "IZ)" + STACK,
                    "pipeDrainAmount", "(" + self + DIR + "IZ)" + STACK);
            addTrampoline(cn, "canFill", "(" + DIR + FLUID + ")Z",
                    "pipeCanFill", "(" + self + DIR + FLUID + ")Z");
            addTrampoline(cn, "canDrain", "(" + DIR + FLUID + ")Z",
                    "pipeCanDrain", "(" + self + DIR + FLUID + ")Z");
            addTrampoline(cn, "getTankInfo", "(" + DIR + ")" + TANKINFO_ARR,
                    "pipeTankInfo", "(" + self + DIR + ")" + TANKINFO_ARR);

            LOG.info("Patched " + PIPE_BASE + " with a universal Forge IFluidHandler bridge "
                    + "(lets AE2FluidCraft-Rework and other active foreign fluid busses fill/drain ducts directly)");
        }

        String tickName = TICK_DEOBF;
        MethodNode original = findMethod(cn, tickName, "()V");
        if (original == null) {
            tickName = TICK_SRG;
            original = findMethod(cn, tickName, "()V");
        }

        if (original == null) {
            LOG.warning(PIPE_BASE + "#updateEntity()V not found under either the deobfuscated or SRG "
                    + "name - the installed NTM version may not match what this coremod expects. "
                    + "Skipping the pipe auto-connect-to-foreign-neighbor patch (Forge calling "
                    + "fill/drain/etc. directly on the duct itself still works).");
            return writeClass(cn);
        }

        String renamed = tickName + "$hbm";
        if (findMethod(cn, renamed, "()V") != null) return writeClass(cn); // already patched

        original.name = renamed;

        MethodNode wrapper = new MethodNode(Opcodes.ACC_PUBLIC, tickName, "()V", null, null);
        InsnList il = new InsnList();

        // this.<tickName>$hbm()
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, PIPE_BASE, renamed, "()V", false));

        // UniversalFluidBridge.pipeDiscoverForeignNeighbors(this)
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BRIDGE, "pipeDiscoverForeignNeighbors",
                "(L" + PIPE_BASE + ";)V", false));

        il.add(new InsnNode(Opcodes.RETURN));

        wrapper.instructions = il;
        cn.methods.add(wrapper);

        LOG.info("Patched " + PIPE_BASE + "#" + tickName + " to also auto-discover foreign IFluidHandler neighbors");
        return writeClass(cn);
    }

    // ------------------------------------------------------------------
    // TileEntityPipeExhaust
    // ------------------------------------------------------------------

    private byte[] patchPipeExhaust(byte[] basicClass) {
        ClassNode cn = readClass(basicClass);

        if (!cn.interfaces.contains(FLUID_HANDLER)) {
            addInterface(cn, FLUID_HANDLER);

            String self = "L" + EXHAUST_PIPE + ";";
            addTrampoline(cn, "fill", "(" + DIR + STACK + "Z)I",
                    "exhaustFill", "(" + self + DIR + STACK + "Z)I");
            addTrampoline(cn, "drain", "(" + DIR + STACK + "Z)" + STACK,
                    "exhaustDrain", "(" + self + DIR + STACK + "Z)" + STACK);
            addTrampoline(cn, "drain", "(" + DIR + "IZ)" + STACK,
                    "exhaustDrainAmount", "(" + self + DIR + "IZ)" + STACK);
            addTrampoline(cn, "canFill", "(" + DIR + FLUID + ")Z",
                    "exhaustCanFill", "(" + self + DIR + FLUID + ")Z");
            addTrampoline(cn, "canDrain", "(" + DIR + FLUID + ")Z",
                    "exhaustCanDrain", "(" + self + DIR + FLUID + ")Z");
            addTrampoline(cn, "getTankInfo", "(" + DIR + ")" + TANKINFO_ARR,
                    "exhaustTankInfo", "(" + self + DIR + ")" + TANKINFO_ARR);

            LOG.info("Patched " + EXHAUST_PIPE + " with a universal Forge IFluidHandler bridge "
                    + "(fixed to the SMOKE/SMOKE_LEADED/SMOKE_POISON networks it actually carries)");
        }

        String tickName = TICK_DEOBF;
        MethodNode original = findMethod(cn, tickName, "()V");
        if (original == null) {
            tickName = TICK_SRG;
            original = findMethod(cn, tickName, "()V");
        }

        if (original == null) {
            LOG.warning(EXHAUST_PIPE + "#updateEntity()V not found under either the deobfuscated or SRG "
                    + "name - the installed NTM version may not match what this coremod expects. "
                    + "Skipping the exhaust-duct auto-connect-to-foreign-neighbor patch (Forge calling "
                    + "fill/drain/etc. directly on the duct itself still works).");
            return writeClass(cn);
        }

        String renamed = tickName + "$hbm";
        if (findMethod(cn, renamed, "()V") != null) return writeClass(cn);

        original.name = renamed;

        MethodNode wrapper = new MethodNode(Opcodes.ACC_PUBLIC, tickName, "()V", null, null);
        InsnList il = new InsnList();

        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, EXHAUST_PIPE, renamed, "()V", false));

        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BRIDGE, "exhaustDiscoverForeignNeighbors",
                "(L" + EXHAUST_PIPE + ";)V", false));

        il.add(new InsnNode(Opcodes.RETURN));

        wrapper.instructions = il;
        cn.methods.add(wrapper);

        LOG.info("Patched " + EXHAUST_PIPE + "#" + tickName + " to also auto-discover foreign IFluidHandler neighbors");
        return writeClass(cn);
    }

    // ------------------------------------------------------------------
    // com.hbm.lib.Library#canConnectFluid
    // ------------------------------------------------------------------

    private byte[] patchLibrary(byte[] basicClass) {
        ClassNode cn = readClass(basicClass);

        MethodNode original = findMethod(cn, "canConnectFluid", CAN_CONNECT_FLUID_DESC);
        if (original == null) {
            LOG.warning(LIBRARY + "#canConnectFluid" + CAN_CONNECT_FLUID_DESC + " not found - the "
                    + "installed NTM version may not match what this coremod expects. Skipping the "
                    + "pipe visual-connection patch (fluid still flows to foreign handlers, they "
                    + "just won't render as connected).");
            return basicClass;
        }

        String renamed = "canConnectFluid$hbm";
        if (findMethod(cn, renamed, CAN_CONNECT_FLUID_DESC) != null) return basicClass; // already patched

        original.name = renamed;

        MethodNode wrapper = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "canConnectFluid", CAN_CONNECT_FLUID_DESC, null, null);
        InsnList il = new InsnList();

        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new VarInsnNode(Opcodes.ILOAD, 1));
        il.add(new VarInsnNode(Opcodes.ILOAD, 2));
        il.add(new VarInsnNode(Opcodes.ILOAD, 3));
        il.add(new VarInsnNode(Opcodes.ALOAD, 4));
        il.add(new VarInsnNode(Opcodes.ALOAD, 5));
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, LIBRARY, renamed, CAN_CONNECT_FLUID_DESC, false));

        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new VarInsnNode(Opcodes.ILOAD, 1));
        il.add(new VarInsnNode(Opcodes.ILOAD, 2));
        il.add(new VarInsnNode(Opcodes.ILOAD, 3));
        il.add(new VarInsnNode(Opcodes.ALOAD, 4));
        il.add(new VarInsnNode(Opcodes.ALOAD, 5));
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BRIDGE, "canConnectForeign", CAN_CONNECT_FLUID_DESC, false));

        il.add(new InsnNode(Opcodes.IOR));
        il.add(new InsnNode(Opcodes.IRETURN));

        wrapper.instructions = il;
        cn.methods.add(wrapper);

        LOG.info("Patched " + LIBRARY + "#canConnectFluid to also visually connect to foreign IFluidHandler neighbors");
        return writeClass(cn);
    }

    // ------------------------------------------------------------------
    // com.hbm.inventory.fluid.tank.FluidTank#setType
    // ------------------------------------------------------------------

    private byte[] patchFluidTank(byte[] basicClass) {
        ClassNode cn = readClass(basicClass);
        boolean changed = false;

        // --- setType(int, int, ItemStack[]) - reset-identifier slot behavior (existing) ---
        MethodNode setType = findMethod(cn, "setType", SET_TYPE_DESC);
        if (setType != null && findMethod(cn, "setType$hbm", SET_TYPE_DESC) == null) {
            setType.name = "setType$hbm";

            MethodNode wrapper = new MethodNode(Opcodes.ACC_PUBLIC, "setType", SET_TYPE_DESC, null, null);
            InsnList il = new InsnList();
            LabelNode checkInPlace = new LabelNode();
            LabelNode proceed = new LabelNode();

            il.add(new VarInsnNode(Opcodes.ALOAD, 0));
            il.add(new VarInsnNode(Opcodes.ILOAD, 1));
            il.add(new VarInsnNode(Opcodes.ALOAD, 3));
            il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BRIDGE, "shouldBlockReset", SHOULD_BLOCK_RESET_DESC, false));
            il.add(new JumpInsnNode(Opcodes.IFEQ, checkInPlace));
            il.add(new InsnNode(Opcodes.ICONST_0));
            il.add(new InsnNode(Opcodes.IRETURN));

            il.add(checkInPlace);
            il.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
            il.add(new VarInsnNode(Opcodes.ALOAD, 0));
            il.add(new VarInsnNode(Opcodes.ILOAD, 1));
            il.add(new VarInsnNode(Opcodes.ALOAD, 3));
            il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BRIDGE, "isInPlaceReset", SHOULD_BLOCK_RESET_DESC, false));
            il.add(new JumpInsnNode(Opcodes.IFEQ, proceed));
            il.add(new InsnNode(Opcodes.ICONST_1));
            il.add(new InsnNode(Opcodes.IRETURN));

            il.add(proceed);
            il.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
            il.add(new VarInsnNode(Opcodes.ALOAD, 0));
            il.add(new VarInsnNode(Opcodes.ILOAD, 1));
            il.add(new VarInsnNode(Opcodes.ILOAD, 2));
            il.add(new VarInsnNode(Opcodes.ALOAD, 3));
            il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, FLUID_TANK, "setType$hbm", SET_TYPE_DESC, false));
            il.add(new InsnNode(Opcodes.IRETURN));

            wrapper.instructions = il;
            cn.methods.add(wrapper);
            changed = true;
        }

        // --- setTankType(FluidType) - universal guard: never collapse a full tank to NONE ---
        MethodNode setTankType = findMethod(cn, "setTankType", SET_TANK_TYPE_DESC);
        if (setTankType != null && findMethod(cn, "setTankType$hbm", SET_TANK_TYPE_DESC) == null) {
            setTankType.name = "setTankType$hbm";

            MethodNode wrapper = new MethodNode(Opcodes.ACC_PUBLIC, "setTankType", SET_TANK_TYPE_DESC, null, null);
            InsnList il = new InsnList();
            LabelNode proceed = new LabelNode();

            il.add(new VarInsnNode(Opcodes.ALOAD, 0));
            il.add(new VarInsnNode(Opcodes.ALOAD, 1));
            il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BRIDGE, "shouldBlockTypeChange", SHOULD_BLOCK_TYPE_CHANGE_DESC, false));
            il.add(new JumpInsnNode(Opcodes.IFEQ, proceed));
            il.add(new InsnNode(Opcodes.RETURN));

            il.add(proceed);
            il.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
            il.add(new VarInsnNode(Opcodes.ALOAD, 0));
            il.add(new VarInsnNode(Opcodes.ALOAD, 1));
            il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, FLUID_TANK, "setTankType$hbm", SET_TANK_TYPE_DESC, false));
            il.add(new InsnNode(Opcodes.RETURN));

            wrapper.instructions = il;
            cn.methods.add(wrapper);
            changed = true;
        }

        if (!changed) return basicClass;

        LOG.info("Patched " + FLUID_TANK + ": reset-identifier slot behavior + universal setTankType(NONE) guard");
        return writeClass(cn);
    }

    // ------------------------------------------------------------------
    // com.hbm.tileentity.machine.TileEntityMachineCompressorBase#setupTanks
    // ------------------------------------------------------------------

    private byte[] patchCompressorBase(byte[] basicClass) {
        ClassNode cn = readClass(basicClass);

        MethodNode original = findMethod(cn, "setupTanks", SETUP_TANKS_DESC);
        if (original == null) {
            LOG.warning(COMPRESSOR_BASE + "#setupTanks" + SETUP_TANKS_DESC + " not found - skipping the "
                    + "compressor output-protection patch.");
            return basicClass;
        }

        String renamed = "setupTanks$hbm";
        if (findMethod(cn, renamed, SETUP_TANKS_DESC) != null) return basicClass;

        original.name = renamed;

        MethodNode wrapper = new MethodNode(Opcodes.ACC_PROTECTED, "setupTanks", SETUP_TANKS_DESC, null, null);
        InsnList il = new InsnList();
        LabelNode proceed = new LabelNode();

        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BRIDGE, "shouldSkipCompressorSync", SHOULD_SKIP_COMPRESSOR_DESC, false));
        il.add(new JumpInsnNode(Opcodes.IFEQ, proceed));
        il.add(new InsnNode(Opcodes.RETURN));

        il.add(proceed);
        il.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, COMPRESSOR_BASE, renamed, SETUP_TANKS_DESC, false));
        il.add(new InsnNode(Opcodes.RETURN));

        wrapper.instructions = il;
        cn.methods.add(wrapper);

        LOG.info("Patched " + COMPRESSOR_BASE + "#setupTanks to skip the no-recipe sync while the output tank still holds fluid");
        return writeClass(cn);
    }

    // ------------------------------------------------------------------
    // Diamond conflict
    // ------------------------------------------------------------------

    private byte[] patchDiamondConflict(byte[] basicClass, String transformedName) {
        ClassNode cn = readClass(basicClass);

        boolean standardReceiver = cn.interfaces.contains(RECEIVER);
        boolean standardSender = cn.interfaces.contains(SENDER);
        boolean baseReceiver = cn.interfaces.contains(BASE_RECEIVER);
        boolean baseProvider = cn.interfaces.contains(BASE_PROVIDER);

        boolean isReceiver = standardReceiver || baseReceiver;
        boolean isSender = standardSender || baseProvider;

        if (!isReceiver || !isSender) return basicClass;
        if (cn.interfaces.contains(FLUID_HANDLER)) return basicClass; // already resolved somehow

        addInterface(cn, FLUID_HANDLER);

        String receiverType = standardReceiver ? RECEIVER : BASE_RECEIVER;
        String senderType = standardSender ? SENDER : BASE_PROVIDER;
        String receiverSelf = "L" + receiverType + ";";
        String senderSelf = "L" + senderType + ";";

        String fillMethod = standardReceiver ? "receiverFill" : "baseReceiverFill";
        String canFillMethod = standardReceiver ? "receiverCanFill" : "baseReceiverCanFill";
        String drainMethod = standardSender ? "senderDrain" : "baseProviderDrain";
        String drainAmountMethod = standardSender ? "senderDrainAmount" : "baseProviderDrainAmount";
        String canDrainMethod = standardSender ? "senderCanDrain" : "baseProviderCanDrain";

        addTrampoline(cn, "fill", "(" + DIR + STACK + "Z)I",
                fillMethod, "(" + receiverSelf + DIR + STACK + "Z)I");
        addTrampoline(cn, "drain", "(" + DIR + STACK + "Z)" + STACK,
                drainMethod, "(" + senderSelf + DIR + STACK + "Z)" + STACK);
        addTrampoline(cn, "drain", "(" + DIR + "IZ)" + STACK,
                drainAmountMethod, "(" + senderSelf + DIR + "IZ)" + STACK);
        addTrampoline(cn, "canFill", "(" + DIR + FLUID + ")Z",
                canFillMethod, "(" + receiverSelf + DIR + FLUID + ")Z");
        addTrampoline(cn, "canDrain", "(" + DIR + FLUID + ")Z",
                canDrainMethod, "(" + senderSelf + DIR + FLUID + ")Z");
        String baseReceiverSelf = "L" + BASE_RECEIVER + ";";
        addTrampoline(cn, "getTankInfo", "(" + DIR + ")" + TANKINFO_ARR,
                "combinedTankInfo", "(" + baseReceiverSelf + DIR + ")" + TANKINFO_ARR);

        LOG.info("Patched " + transformedName + " directly (implements a receiver-family and "
                + "sender-family MK2 interface at once, needed its own diamond-resolving overrides)");
        return writeClass(cn);
    }

    // ------------------------------------------------------------------
    // ASM plumbing
    // ------------------------------------------------------------------

    private ClassNode readClass(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassNode node = new ClassNode();
        reader.accept(node, ClassReader.EXPAND_FRAMES);
        return node;
    }

    private byte[] writeClass(ClassNode node) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private MethodNode findMethod(ClassNode cn, String name, String desc) {
        List<MethodNode> methods = cn.methods;
        for (MethodNode m : methods) {
            if (m.name.equals(name) && m.desc.equals(desc)) return m;
        }
        return null;
    }

    private void addInterface(ClassNode cn, String iface) {
        if (!cn.interfaces.contains(iface)) {
            cn.interfaces.add(iface);
        }
    }

    private void addTrampoline(ClassNode cn, String name, String desc, String bridgeMethod, String bridgeDesc) {
        if (findMethod(cn, name, desc) != null) return;

        MethodNode mn = new MethodNode(Opcodes.ACC_PUBLIC, name, desc, null, null);
        InsnList il = new InsnList();

        Type[] argTypes = Type.getArgumentTypes(desc);
        Type returnType = Type.getReturnType(desc);

        int slot = 0;
        il.add(new VarInsnNode(Opcodes.ALOAD, slot)); // this
        slot += 1;
        for (Type t : argTypes) {
            il.add(new VarInsnNode(t.getOpcode(Opcodes.ILOAD), slot));
            slot += t.getSize();
        }
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BRIDGE, bridgeMethod, bridgeDesc, false));
        il.add(new InsnNode(returnType.getOpcode(Opcodes.IRETURN)));

        mn.instructions = il;
        cn.methods.add(mn);
    }
}