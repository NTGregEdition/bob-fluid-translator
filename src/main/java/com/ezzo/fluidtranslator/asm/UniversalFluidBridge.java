package com.ezzo.fluidtranslator.asm;

import api.hbm.fluid.IFluidStandardTransceiver;
import api.hbm.fluidmk2.FluidNetMK2;
import api.hbm.fluidmk2.IFluidConnectorMK2;
import api.hbm.fluidmk2.IFluidProviderMK2;
import api.hbm.fluidmk2.IFluidReceiverMK2;
import api.hbm.fluidmk2.IFluidStandardReceiverMK2;
import api.hbm.fluidmk2.IFluidStandardSenderMK2;
import api.hbm.fluidmk2.IFluidStandardTransceiverMK2;
import com.ezzo.fluidtranslator.FluidTranslator;
import com.ezzo.fluidtranslator.ModConfig;
import com.ezzo.fluidtranslator.ModFluidRegistry;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.tileentity.TileEntityProxyBase;
import com.hbm.tileentity.network.TileEntityPipeBaseNT;
import com.hbm.uninos.GenNode;
import com.hbm.uninos.UniNodespace;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class UniversalFluidBridge {

    private UniversalFluidBridge() {
    }

    // ======================================================================
    // IFluidStandardReceiverMK2 -> IFluidHandler
    // ======================================================================

    public static int receiverFill(IFluidStandardReceiverMK2 self, ForgeDirection from, FluidStack resource, boolean doFill) {
        if (!ModConfig.enableUniversalFluidPorts) return 0;
        try {
            return fillTanks(self.getReceivingTanks(), resource, doFill);
        } catch (Throwable t) {
            logError("receiverFill", self, t);
            return 0;
        }
    }

    public static FluidStack receiverDrain(IFluidStandardReceiverMK2 self, ForgeDirection from, FluidStack resource, boolean doDrain) {
        return null;
    }

    public static FluidStack receiverDrainAmount(IFluidStandardReceiverMK2 self, ForgeDirection from, int maxDrain, boolean doDrain) {
        return null;
    }

    public static boolean receiverCanFill(IFluidStandardReceiverMK2 self, ForgeDirection from, Fluid fluid) {
        if (!ModConfig.enableUniversalFluidPorts) return false;
        try {
            return canFillTanks(self.getReceivingTanks(), fluid);
        } catch (Throwable t) {
            logError("receiverCanFill", self, t);
            return false;
        }
    }

    public static boolean receiverCanDrain(IFluidStandardReceiverMK2 self, ForgeDirection from, Fluid fluid) {
        return false;
    }

    public static FluidTankInfo[] receiverTankInfo(IFluidStandardReceiverMK2 self, ForgeDirection from) {
        if (!ModConfig.enableUniversalFluidPorts) return emptyInfo();
        try {
            return tankInfo(self.getReceivingTanks());
        } catch (Throwable t) {
            logError("receiverTankInfo", self, t);
            return emptyInfo();
        }
    }

    // ======================================================================
    // IFluidStandardSenderMK2 -> IFluidHandler
    // ======================================================================

    public static int senderFill(IFluidStandardSenderMK2 self, ForgeDirection from, FluidStack resource, boolean doFill) {
        return 0;
    }

    public static FluidStack senderDrain(IFluidStandardSenderMK2 self, ForgeDirection from, FluidStack resource, boolean doDrain) {
        if (!ModConfig.enableUniversalFluidPorts) return null;
        try {
            return drainTanksByType(self.getSendingTanks(), resource, doDrain);
        } catch (Throwable t) {
            logError("senderDrain", self, t);
            return null;
        }
    }

    public static FluidStack senderDrainAmount(IFluidStandardSenderMK2 self, ForgeDirection from, int maxDrain, boolean doDrain) {
        if (!ModConfig.enableUniversalFluidPorts) return null;
        try {
            return drainTanksAny(self.getSendingTanks(), maxDrain, doDrain);
        } catch (Throwable t) {
            logError("senderDrainAmount", self, t);
            return null;
        }
    }

    public static boolean senderCanFill(IFluidStandardSenderMK2 self, ForgeDirection from, Fluid fluid) {
        return false;
    }

    public static boolean senderCanDrain(IFluidStandardSenderMK2 self, ForgeDirection from, Fluid fluid) {
        if (!ModConfig.enableUniversalFluidPorts) return false;
        try {
            return canDrainTanks(self.getSendingTanks(), fluid);
        } catch (Throwable t) {
            logError("senderCanDrain", self, t);
            return false;
        }
    }

    public static FluidTankInfo[] senderTankInfo(IFluidStandardSenderMK2 self, ForgeDirection from) {
        if (!ModConfig.enableUniversalFluidPorts) return emptyInfo();
        try {
            return tankInfo(self.getSendingTanks());
        } catch (Throwable t) {
            logError("senderTankInfo", self, t);
            return emptyInfo();
        }
    }

    // ======================================================================
    // IFluidStandardTransceiverMK2 -> IFluidHandler
    // ======================================================================

    public static int transceiverFill(IFluidStandardTransceiverMK2 self, ForgeDirection from, FluidStack resource, boolean doFill) {
        if (!ModConfig.enableUniversalFluidPorts) return 0;
        try {
            return fillTanks(self.getReceivingTanks(), resource, doFill);
        } catch (Throwable t) {
            logError("transceiverFill", self, t);
            return 0;
        }
    }

    public static FluidStack transceiverDrain(IFluidStandardTransceiverMK2 self, ForgeDirection from, FluidStack resource, boolean doDrain) {
        if (!ModConfig.enableUniversalFluidPorts) return null;
        try {
            return drainTanksByType(self.getSendingTanks(), resource, doDrain);
        } catch (Throwable t) {
            logError("transceiverDrain", self, t);
            return null;
        }
    }

    public static FluidStack transceiverDrainAmount(IFluidStandardTransceiverMK2 self, ForgeDirection from, int maxDrain, boolean doDrain) {
        if (!ModConfig.enableUniversalFluidPorts) return null;
        try {
            return drainTanksAny(self.getSendingTanks(), maxDrain, doDrain);
        } catch (Throwable t) {
            logError("transceiverDrainAmount", self, t);
            return null;
        }
    }

    public static boolean transceiverCanFill(IFluidStandardTransceiverMK2 self, ForgeDirection from, Fluid fluid) {
        if (!ModConfig.enableUniversalFluidPorts) return false;
        try {
            return canFillTanks(self.getReceivingTanks(), fluid);
        } catch (Throwable t) {
            logError("transceiverCanFill", self, t);
            return false;
        }
    }

    public static boolean transceiverCanDrain(IFluidStandardTransceiverMK2 self, ForgeDirection from, Fluid fluid) {
        if (!ModConfig.enableUniversalFluidPorts) return false;
        try {
            return canDrainTanks(self.getSendingTanks(), fluid);
        } catch (Throwable t) {
            logError("transceiverCanDrain", self, t);
            return false;
        }
    }

    public static FluidTankInfo[] transceiverTankInfo(IFluidStandardTransceiverMK2 self, ForgeDirection from) {
        if (!ModConfig.enableUniversalFluidPorts) return emptyInfo();
        try {
            return tankInfo(self.getAllTanks());
        } catch (Throwable t) {
            logError("transceiverTankInfo", self, t);
            return emptyInfo();
        }
    }

    // ======================================================================
    // api.hbm.fluid.IFluidStandardTransceiver (deprecated legacy interface)
    // ======================================================================

    @SuppressWarnings("deprecation")
    public static int legacyTransceiverFill(IFluidStandardTransceiver self, ForgeDirection from, FluidStack resource, boolean doFill) {
        if (!ModConfig.enableUniversalFluidPorts) return 0;
        try {
            return fillTanks(self.getReceivingTanks(), resource, doFill);
        } catch (Throwable t) {
            logError("legacyTransceiverFill", self, t);
            return 0;
        }
    }

    @SuppressWarnings("deprecation")
    public static FluidStack legacyTransceiverDrain(IFluidStandardTransceiver self, ForgeDirection from, FluidStack resource, boolean doDrain) {
        if (!ModConfig.enableUniversalFluidPorts) return null;
        try {
            return drainTanksByType(self.getSendingTanks(), resource, doDrain);
        } catch (Throwable t) {
            logError("legacyTransceiverDrain", self, t);
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    public static FluidStack legacyTransceiverDrainAmount(IFluidStandardTransceiver self, ForgeDirection from, int maxDrain, boolean doDrain) {
        if (!ModConfig.enableUniversalFluidPorts) return null;
        try {
            return drainTanksAny(self.getSendingTanks(), maxDrain, doDrain);
        } catch (Throwable t) {
            logError("legacyTransceiverDrainAmount", self, t);
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    public static boolean legacyTransceiverCanFill(IFluidStandardTransceiver self, ForgeDirection from, Fluid fluid) {
        if (!ModConfig.enableUniversalFluidPorts) return false;
        try {
            return canFillTanks(self.getReceivingTanks(), fluid);
        } catch (Throwable t) {
            logError("legacyTransceiverCanFill", self, t);
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    public static boolean legacyTransceiverCanDrain(IFluidStandardTransceiver self, ForgeDirection from, Fluid fluid) {
        if (!ModConfig.enableUniversalFluidPorts) return false;
        try {
            return canDrainTanks(self.getSendingTanks(), fluid);
        } catch (Throwable t) {
            logError("legacyTransceiverCanDrain", self, t);
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    public static FluidTankInfo[] legacyTransceiverTankInfo(IFluidStandardTransceiver self, ForgeDirection from) {
        if (!ModConfig.enableUniversalFluidPorts) return emptyInfo();
        try {
            return tankInfo(self.getAllTanks());
        } catch (Throwable t) {
            logError("legacyTransceiverTankInfo", self, t);
            return emptyInfo();
        }
    }

    // ======================================================================
    // Diamond-conflict
    // ======================================================================

    public static FluidTankInfo[] combinedTankInfo(IFluidReceiverMK2 self, ForgeDirection from) {
        if (!ModConfig.enableUniversalFluidPorts) return emptyInfo();
        try {
            return tankInfo(self.getAllTanks());
        } catch (Throwable t) {
            logError("combinedTankInfo", self, t);
            return emptyInfo();
        }
    }

    // ======================================================================
    // IFluidReceiverMK2 / IFluidProviderMK2 (base interfaces)
    // ======================================================================

    public static int baseReceiverFill(IFluidReceiverMK2 self, ForgeDirection from, FluidStack resource, boolean doFill) {
        if (!ModConfig.enableUniversalFluidPorts) return 0;
        try {
            if (resource == null || resource.getFluid() == null || resource.amount <= 0) return 0;

            FluidType incoming = ModFluidRegistry.getHBMFluid(resource.getFluid());
            if (incoming == null || incoming.getID() == Fluids.NONE.getID()) return 0;

            long demand = self.getDemand(incoming, 0);
            if (demand <= 0) return 0;

            long toOffer = Math.min(resource.amount, demand);
            if (toOffer <= 0) return 0;

            if (!doFill) {
                return (int) Math.min(toOffer, Integer.MAX_VALUE);
            }

            long remainder = self.transferFluid(incoming, 0, toOffer);
            long accepted = toOffer - remainder;
            return (int) Math.max(0, Math.min(accepted, Integer.MAX_VALUE));
        } catch (Throwable t) {
            logError("baseReceiverFill", self, t);
            return 0;
        }
    }

    public static FluidStack baseReceiverDrain(IFluidReceiverMK2 self, ForgeDirection from, FluidStack resource, boolean doDrain) {
        return null;
    }

    public static FluidStack baseReceiverDrainAmount(IFluidReceiverMK2 self, ForgeDirection from, int maxDrain, boolean doDrain) {
        return null;
    }

    public static boolean baseReceiverCanFill(IFluidReceiverMK2 self, ForgeDirection from, Fluid fluid) {
        if (!ModConfig.enableUniversalFluidPorts) return false;
        try {
            if (fluid == null) return false;
            FluidType incoming = ModFluidRegistry.getHBMFluid(fluid);
            if (incoming == null || incoming.getID() == Fluids.NONE.getID()) return false;
            return self.getDemand(incoming, 0) > 0;
        } catch (Throwable t) {
            logError("baseReceiverCanFill", self, t);
            return false;
        }
    }

    public static boolean baseReceiverCanDrain(IFluidReceiverMK2 self, ForgeDirection from, Fluid fluid) {
        return false;
    }

    public static FluidTankInfo[] baseReceiverTankInfo(IFluidReceiverMK2 self, ForgeDirection from) {
        if (!ModConfig.enableUniversalFluidPorts) return emptyInfo();
        try {
            return tankInfo(self.getAllTanks());
        } catch (Throwable t) {
            logError("baseReceiverTankInfo", self, t);
            return emptyInfo();
        }
    }

    public static int baseProviderFill(IFluidProviderMK2 self, ForgeDirection from, FluidStack resource, boolean doFill) {
        return 0; // a pure provider has nowhere to accept fluid into
    }

    public static FluidStack baseProviderDrain(IFluidProviderMK2 self, ForgeDirection from, FluidStack resource, boolean doDrain) {
        if (!ModConfig.enableUniversalFluidPorts) return null;
        try {
            if (resource == null || resource.getFluid() == null) return null;

            FluidType wanted = ModFluidRegistry.getHBMFluid(resource.getFluid());
            if (wanted == null || wanted.getID() == Fluids.NONE.getID()) return null;

            long available = self.getFluidAvailable(wanted, 0);
            if (available <= 0) return null;

            long toDrain = Math.min(resource.amount, available);
            if (toDrain <= 0) return null;

            Fluid forgeFluid = ModFluidRegistry.getForgeFluid(wanted);
            if (forgeFluid == null) return null;

            if (doDrain) self.useUpFluid(wanted, 0, toDrain);
            return new FluidStack(forgeFluid, (int) Math.min(toDrain, Integer.MAX_VALUE));
        } catch (Throwable t) {
            logError("baseProviderDrain", self, t);
            return null;
        }
    }

    public static FluidStack baseProviderDrainAmount(IFluidProviderMK2 self, ForgeDirection from, int maxDrain, boolean doDrain) {
        if (!ModConfig.enableUniversalFluidPorts) return null;
        try {
            FluidTank[] tanks = self.getAllTanks();
            if (tanks == null) return null;

            for (FluidTank tank : tanks) {
                FluidType type = tank.getTankType();
                if (type == null || type.getID() == Fluids.NONE.getID()) continue;

                long available = self.getFluidAvailable(type, 0);
                if (available <= 0) continue;

                Fluid forgeFluid = ModFluidRegistry.getForgeFluid(type);
                if (forgeFluid == null) continue;

                long toDrain = Math.min(maxDrain, available);
                if (toDrain <= 0) continue;

                if (doDrain) self.useUpFluid(type, 0, toDrain);
                return new FluidStack(forgeFluid, (int) Math.min(toDrain, Integer.MAX_VALUE));
            }
            return null;
        } catch (Throwable t) {
            logError("baseProviderDrainAmount", self, t);
            return null;
        }
    }

    public static boolean baseProviderCanFill(IFluidProviderMK2 self, ForgeDirection from, Fluid fluid) {
        return false;
    }

    public static boolean baseProviderCanDrain(IFluidProviderMK2 self, ForgeDirection from, Fluid fluid) {
        if (!ModConfig.enableUniversalFluidPorts) return false;
        try {
            if (fluid == null) return false;
            FluidType wanted = ModFluidRegistry.getHBMFluid(fluid);
            if (wanted == null || wanted.getID() == Fluids.NONE.getID()) return false;
            return self.getFluidAvailable(wanted, 0) > 0;
        } catch (Throwable t) {
            logError("baseProviderCanDrain", self, t);
            return false;
        }
    }

    public static FluidTankInfo[] baseProviderTankInfo(IFluidProviderMK2 self, ForgeDirection from) {
        if (!ModConfig.enableUniversalFluidPorts) return emptyInfo();
        try {
            return tankInfo(self.getAllTanks());
        } catch (Throwable t) {
            logError("baseProviderTankInfo", self, t);
            return emptyInfo();
        }
    }

    // ======================================================================
    // TileEntityProxyBase (multiblock "dummy" segments)
    // ======================================================================

    public static int proxyFill(TileEntityProxyBase self, ForgeDirection from, FluidStack resource, boolean doFill) {
        if (!ModConfig.enableUniversalFluidPorts) return 0;
        try {
            TileEntity core = resolveProxyCore(self);
            return core instanceof IFluidHandler ? ((IFluidHandler) core).fill(from, resource, doFill) : 0;
        } catch (Throwable t) {
            logError("proxyFill", self, t);
            return 0;
        }
    }

    public static FluidStack proxyDrain(TileEntityProxyBase self, ForgeDirection from, FluidStack resource, boolean doDrain) {
        if (!ModConfig.enableUniversalFluidPorts) return null;
        try {
            TileEntity core = resolveProxyCore(self);
            return core instanceof IFluidHandler ? ((IFluidHandler) core).drain(from, resource, doDrain) : null;
        } catch (Throwable t) {
            logError("proxyDrain", self, t);
            return null;
        }
    }

    public static FluidStack proxyDrainAmount(TileEntityProxyBase self, ForgeDirection from, int maxDrain, boolean doDrain) {
        if (!ModConfig.enableUniversalFluidPorts) return null;
        try {
            TileEntity core = resolveProxyCore(self);
            return core instanceof IFluidHandler ? ((IFluidHandler) core).drain(from, maxDrain, doDrain) : null;
        } catch (Throwable t) {
            logError("proxyDrainAmount", self, t);
            return null;
        }
    }

    public static boolean proxyCanFill(TileEntityProxyBase self, ForgeDirection from, Fluid fluid) {
        if (!ModConfig.enableUniversalFluidPorts) return false;
        try {
            TileEntity core = resolveProxyCore(self);
            return core instanceof IFluidHandler && ((IFluidHandler) core).canFill(from, fluid);
        } catch (Throwable t) {
            logError("proxyCanFill", self, t);
            return false;
        }
    }

    public static boolean proxyCanDrain(TileEntityProxyBase self, ForgeDirection from, Fluid fluid) {
        if (!ModConfig.enableUniversalFluidPorts) return false;
        try {
            TileEntity core = resolveProxyCore(self);
            return core instanceof IFluidHandler && ((IFluidHandler) core).canDrain(from, fluid);
        } catch (Throwable t) {
            logError("proxyCanDrain", self, t);
            return false;
        }
    }

    public static FluidTankInfo[] proxyTankInfo(TileEntityProxyBase self, ForgeDirection from) {
        if (!ModConfig.enableUniversalFluidPorts) return emptyInfo();
        try {
            TileEntity core = resolveProxyCore(self);
            return core instanceof IFluidHandler ? ((IFluidHandler) core).getTankInfo(from) : emptyInfo();
        } catch (Throwable t) {
            logError("proxyTankInfo", self, t);
            return emptyInfo();
        }
    }

    private static TileEntity resolveProxyCore(TileEntityProxyBase proxy) {
        try {
            TileEntity core = proxy.getTE();
            return core == proxy ? null : core; // guard against any accidental self-reference
        } catch (Throwable t) {
            return null;
        }
    }

    // ======================================================================

    // ======================================================================

    public static void tryProvideToForge(IFluidStandardSenderMK2 self, FluidType type, int pressure,
                                         World world, int x, int y, int z, ForgeDirection dir) {
        if (!ModConfig.enableUniversalFluidPorts || !ModConfig.enableAutoPushToForge) return;
        if (world == null || type == null || type.getID() == Fluids.NONE.getID()) return;
        if (pressure != 0) return; // pressurized output must never be auto-pushed into a foreign Forge/AE2 handler

        try {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te == null || te == self) return;

            // If the neighbour is already a native HBM receiver, the untouched
            // original tryProvide() call already handled it - don't double dip.
            if (te instanceof api.hbm.fluidmk2.IFluidReceiverMK2) return;

            ForgeDirection sideOnNeighbor = dir.getOpposite();
            Object resolved = AE2PartHostCompat.resolveFluidHandler(te, sideOnNeighbor);
            if (!(resolved instanceof IFluidHandler)) return;
            IFluidHandler handler = (IFluidHandler) resolved;

            Fluid forgeFluid = ModFluidRegistry.getForgeFluid(type);
            if (forgeFluid == null) return; // fluid has no Forge-side counterpart (blacklisted etc.)

            long available = self.getFluidAvailable(type, pressure);
            if (available <= 0) return;

            int toOffer = (int) Math.min(available, Integer.MAX_VALUE);

            if (!handler.canFill(sideOnNeighbor, forgeFluid)) return;

            int accepted = handler.fill(sideOnNeighbor, new FluidStack(forgeFluid, toOffer), true);
            if (accepted > 0) {
                self.useUpFluid(type, pressure, accepted);
            }
        } catch (Throwable t) {
            FluidTranslator.logger.error("UniversalFluidBridge: error auto-pushing fluid into a foreign IFluidHandler at "
                    + x + "," + y + "," + z, t);
        }
    }

    // ======================================================================
    // Pipe -> foreign IFluidHandler auto-connect
    // ======================================================================

    private static final Map<TileEntityPipeBaseNT, ForeignFluidPort[]> PIPE_FOREIGN_PORTS =
            new WeakHashMap<TileEntityPipeBaseNT, ForeignFluidPort[]>();

    @SuppressWarnings("unchecked")
    public static void pipeDiscoverForeignNeighbors(TileEntityPipeBaseNT self) {
        if (!ModConfig.enableUniversalFluidPorts || !ModConfig.enablePipeForeignConnect) return;

        boolean debug = ModConfig.debugPipeForeignConnect;
        String posTag = null;

        try {
            World world = self.getWorldObj();
            if (world == null || world.isRemote) return;

            if (debug) {
                posTag = self.xCoord + "," + self.yCoord + "," + self.zCoord;
            }

            FluidType type = self.getType();
            if (type == null || type.getID() == Fluids.NONE.getID()) {
                if (debug) {
                    FluidTranslator.logger.info("PipeDebug " + posTag + ": no fluid type set on this pipe (Fluids.NONE) - "
                            + "right-click it with a bucket of the fluid you want to route, or connect it to a "
                            + "native NTM machine/tank first.");
                }
                return;
            }

            Fluid forgeFluid = ModFluidRegistry.getForgeFluid(type);
            if (forgeFluid == null) {
                if (debug) {
                    FluidTranslator.logger.info("PipeDebug " + posTag + ": type=" + type.getName()
                            + " has no Forge fluid mapping (blacklisted or unregistered) - nothing to bridge.");
                }
                return;
            }

            GenNode node = UniNodespace.getNode(world, self.xCoord, self.yCoord, self.zCoord, type.getNetworkProvider());
            if (node == null || !node.hasValidNet()) {
                if (debug) {
                    FluidTranslator.logger.info("PipeDebug " + posTag + ": type=" + type.getName()
                            + " has no established network node yet (node=" + node + ") - skipping this tick.");
                }
                return;
            }
            FluidNetMK2 net = (FluidNetMK2) node.net;

            ForeignFluidPort[] ports = PIPE_FOREIGN_PORTS.get(self);
            if (ports == null) {
                ports = new ForeignFluidPort[6];
                PIPE_FOREIGN_PORTS.put(self, ports);
            }

            int foundThisTick = 0;

            for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                int idx = dir.ordinal();
                int nx = self.xCoord + dir.offsetX;
                int ny = self.yCoord + dir.offsetY;
                int nz = self.zCoord + dir.offsetZ;
                TileEntity te = world.getTileEntity(nx, ny, nz);

                if (te == null || te == self || te instanceof IFluidConnectorMK2) {
                    ports[idx] = null;
                    continue;
                }

                if (AE2PartHostCompat.resolveFluidHandler(te, dir.getOpposite()) == null) {
                    if (debug) {
                        FluidTranslator.logger.info("PipeDebug " + posTag + " side=" + dir
                                + ": neighbor " + te.getClass().getName()
                                + " is not a bridgeable Forge IFluidHandler and has no fluid-handling "
                                + "AE2 part on this face - skipped.");
                    }
                    ports[idx] = null;
                    continue;
                }

                ForeignFluidPort port = ports[idx];
                if (port == null || port.target != te) {
                    port = new ForeignFluidPort(te, dir.getOpposite());
                    ports[idx] = port;
                }

                net.addProvider(port);
                net.addReceiver(port);
                foundThisTick++;

                if (debug) {
                    FluidTranslator.logger.info("PipeDebug " + posTag + " side=" + dir
                            + ": wired up foreign handler " + te.getClass().getName()
                            + " at " + nx + "," + ny + "," + nz + " for type=" + type.getName());
                }
            }

            if (debug && foundThisTick == 0) {
                FluidTranslator.logger.info("PipeDebug " + posTag + ": network is valid (type=" + type.getName()
                        + ") but no foreign IFluidHandler neighbors found on any of the 6 sides this tick.");
            }
        } catch (Throwable t) {
            FluidTranslator.logger.error("UniversalFluidBridge: error discovering foreign fluid neighbors for the pipe at "
                    + self.xCoord + "," + self.yCoord + "," + self.zCoord, t);
        }
    }

    // ======================================================================
    // Pipe's own forge fluid
    // ======================================================================

    private static FluidNetMK2 liveNetwork(TileEntityPipeBaseNT self, FluidType type) {
        if (type == null || type.getID() == Fluids.NONE.getID()) return null;
        World world = self.getWorldObj();
        if (world == null) return null;

        GenNode node = UniNodespace.getNode(world, self.xCoord, self.yCoord, self.zCoord, type.getNetworkProvider());
        if (node == null || !node.hasValidNet()) return null;

        return (FluidNetMK2) node.net;
    }

    /** Pushes {@code amount} of {@code type} straight at the network's real receivers, right now. */
    private static long relayToReceivers(FluidNetMK2 net, FluidType type, long amount, boolean doTransfer) {
        if (amount <= 0 || net.receiverEntries.isEmpty()) return 0;

        if (!doTransfer) {
            long demand = 0;
            for (IFluidReceiverMK2 receiver : net.receiverEntries.keySet()) {
                demand += Math.max(0, receiver.getDemand(type, 0));
                if (demand >= amount) return amount;
            }
            return Math.min(amount, demand);
        }

        long remaining = amount;
        for (IFluidReceiverMK2 receiver : net.receiverEntries.keySet()) {
            if (remaining <= 0) break;
            remaining = receiver.transferFluid(type, 0, remaining); // leftover not accepted, per the MK2 contract
        }
        return amount - remaining;
    }

    /** Pulls up to {@code amount} of {@code type} straight from the network's real providers, right now. */
    private static long relayFromProviders(FluidNetMK2 net, FluidType type, long amount, boolean doTransfer) {
        if (amount <= 0 || net.providerEntries.isEmpty()) return 0;

        if (!doTransfer) {
            long available = 0;
            for (IFluidProviderMK2 provider : net.providerEntries.keySet()) {
                available += Math.max(0, provider.getFluidAvailable(type, 0));
                if (available >= amount) return amount;
            }
            return Math.min(amount, available);
        }

        long remaining = amount;
        for (IFluidProviderMK2 provider : net.providerEntries.keySet()) {
            if (remaining <= 0) break;
            long take = Math.min(remaining, Math.max(0, provider.getFluidAvailable(type, 0)));
            if (take <= 0) continue;
            provider.useUpFluid(type, 0, take);
            remaining -= take;
        }
        return amount - remaining;
    }

    public static int pipeFill(TileEntityPipeBaseNT self, ForgeDirection from, FluidStack resource, boolean doFill) {
        if (!ModConfig.enableUniversalFluidPorts || !ModConfig.enablePipeExternalPort) return 0;
        try {
            if (resource == null || resource.getFluid() == null || resource.amount <= 0) return 0;

            FluidType incoming = ModFluidRegistry.getHBMFluid(resource.getFluid());
            if (incoming == null || incoming.getID() == Fluids.NONE.getID()) return 0;

            FluidType pipeType = self.getType();
            boolean priming = pipeType == null || pipeType.getID() == Fluids.NONE.getID();
            if (!priming && pipeType.getID() != incoming.getID()) return 0; // wrong fluid for this duct

            FluidNetMK2 net = liveNetwork(self, priming ? incoming : pipeType);
            if (net == null) {
                if (doFill && priming) self.setType(incoming);
                return 0;
            }

            long accepted = relayToReceivers(net, incoming, resource.amount, doFill);
            if (accepted > 0 && doFill && priming) self.setType(incoming);
            return (int) accepted;
        } catch (Throwable t) {
            logError("pipeFill", self, t);
            return 0;
        }
    }

    public static FluidStack pipeDrain(TileEntityPipeBaseNT self, ForgeDirection from, FluidStack resource, boolean doDrain) {
        if (!ModConfig.enableUniversalFluidPorts || !ModConfig.enablePipeExternalPort) return null;
        try {
            if (resource == null || resource.getFluid() == null) return null;

            FluidType pipeType = self.getType();
            if (pipeType == null || pipeType.getID() == Fluids.NONE.getID()) return null; // nothing flowing through yet

            FluidType requested = ModFluidRegistry.getHBMFluid(resource.getFluid());
            if (requested == null || requested.getID() != pipeType.getID()) return null;

            return drainInternal(self, pipeType, resource.amount, doDrain);
        } catch (Throwable t) {
            logError("pipeDrain", self, t);
            return null;
        }
    }

    public static FluidStack pipeDrainAmount(TileEntityPipeBaseNT self, ForgeDirection from, int maxDrain, boolean doDrain) {
        if (!ModConfig.enableUniversalFluidPorts || !ModConfig.enablePipeExternalPort) return null;
        try {
            if (maxDrain <= 0) return null;

            FluidType pipeType = self.getType();
            if (pipeType == null || pipeType.getID() == Fluids.NONE.getID()) return null;

            return drainInternal(self, pipeType, maxDrain, doDrain);
        } catch (Throwable t) {
            logError("pipeDrainAmount", self, t);
            return null;
        }
    }

    private static FluidStack drainInternal(TileEntityPipeBaseNT self, FluidType pipeType, int maxDrain, boolean doDrain) {
        FluidNetMK2 net = liveNetwork(self, pipeType);
        if (net == null) return null;

        long drained = relayFromProviders(net, pipeType, maxDrain, doDrain);
        if (drained <= 0) return null;

        Fluid forgeFluid = ModFluidRegistry.getForgeFluid(pipeType);
        if (forgeFluid == null) return null;

        return new FluidStack(forgeFluid, (int) drained);
    }

    public static boolean pipeCanFill(TileEntityPipeBaseNT self, ForgeDirection from, Fluid fluid) {
        if (!ModConfig.enableUniversalFluidPorts || !ModConfig.enablePipeExternalPort) return false;
        try {
            FluidType incoming = ModFluidRegistry.getHBMFluid(fluid);
            if (incoming == null || incoming.getID() == Fluids.NONE.getID()) return false;

            FluidType pipeType = self.getType();
            return pipeType == null || pipeType.getID() == Fluids.NONE.getID() || pipeType.getID() == incoming.getID();
        } catch (Throwable t) {
            logError("pipeCanFill", self, t);
            return false;
        }
    }

    public static boolean pipeCanDrain(TileEntityPipeBaseNT self, ForgeDirection from, Fluid fluid) {
        if (!ModConfig.enableUniversalFluidPorts || !ModConfig.enablePipeExternalPort) return false;
        try {
            FluidType pipeType = self.getType();
            if (pipeType == null || pipeType.getID() == Fluids.NONE.getID()) return false;
            if (fluid == null) return true;

            FluidType requested = ModFluidRegistry.getHBMFluid(fluid);
            return requested != null && requested.getID() == pipeType.getID();
        } catch (Throwable t) {
            logError("pipeCanDrain", self, t);
            return false;
        }
    }

    public static FluidTankInfo[] pipeTankInfo(TileEntityPipeBaseNT self, ForgeDirection from) {
        if (!ModConfig.enableUniversalFluidPorts || !ModConfig.enablePipeExternalPort) return emptyInfo();
        try {
            FluidType pipeType = self.getType();
            if (pipeType == null || pipeType.getID() == Fluids.NONE.getID()) {
                return new FluidTankInfo[]{new FluidTankInfo((FluidStack) null, Integer.MAX_VALUE)};
            }

            Fluid forgeFluid = ModFluidRegistry.getForgeFluid(pipeType);
            if (forgeFluid == null) return emptyInfo();

            long available = 0;
            FluidNetMK2 net = liveNetwork(self, pipeType);
            if (net != null) {
                available = relayFromProviders(net, pipeType, Integer.MAX_VALUE, false);
            }
            int reportedAmount = (int) Math.max(0, Math.min(Integer.MAX_VALUE, available));

            return new FluidTankInfo[]{new FluidTankInfo(new FluidStack(forgeFluid, reportedAmount), Integer.MAX_VALUE)};
        } catch (Throwable t) {
            logError("pipeTankInfo", self, t);
            return emptyInfo();
        }
    }

    // ======================================================================
    // Pipe -> foreign IFluidHandler visual connection (block bounds / collision / model)
    // ======================================================================

    public static boolean canConnectForeign(IBlockAccess world, int x, int y, int z, ForgeDirection dir, FluidType type) {
        if (!ModConfig.enableUniversalFluidPorts || !ModConfig.enablePipeForeignConnect) return false;
        if (world == null || type == null || type.getID() == Fluids.NONE.getID()) return false;

        try {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te == null) return false;

            Fluid forgeFluid = ModFluidRegistry.getForgeFluid(type);
            if (forgeFluid == null) return false;

            ForgeDirection sideOnNeighbor = dir.getOpposite();

            if (AE2PartHostCompat.isActiveForeignFluidPart(te, sideOnNeighbor)) return true;

            Object resolved = AE2PartHostCompat.resolveFluidHandler(te, sideOnNeighbor);
            if (!(resolved instanceof IFluidHandler)) return false;

            IFluidHandler handler = (IFluidHandler) resolved;
            return handler.canFill(sideOnNeighbor, forgeFluid) || handler.canDrain(sideOnNeighbor, forgeFluid);
        } catch (Throwable t) {
            return false;
        }
    }

    // ======================================================================
    // Shared translation core
    // ======================================================================

    private static int fillTanks(FluidTank[] tanks, FluidStack resource, boolean doFill) {
        if (tanks == null || tanks.length == 0) return 0;
        if (resource == null || resource.getFluid() == null || resource.amount <= 0) return 0;

        FluidType incoming = ModFluidRegistry.getHBMFluid(resource.getFluid());
        if (incoming == null || incoming.getID() == Fluids.NONE.getID()) return 0;

        FluidTank target = null;

        // Forge/AE2 fluid stacks carry no pressure value, so they may only ever land in an
        // unpressurized (pressure == 0) tank. Pressurized tanks are invisible to this bridge;
        // the only legitimate way to fill them is through an actual in-world compressor.
        for (FluidTank tank : tanks) {
            if (tank.getPressure() == 0 && tank.getTankType().getID() == incoming.getID()) {
                target = tank;
                break;
            }
        }

        if (target == null) {
            for (FluidTank tank : tanks) {
                if (tank.getPressure() == 0 && tank.getTankType().getID() == Fluids.NONE.getID()) {
                    target = tank;
                    break;
                }
            }
        }

        if (target == null) return 0;

        int space = target.getMaxFill() - target.getFill();
        int toFill = Math.min(resource.amount, space);
        if (toFill <= 0) return 0;

        if (doFill) {
            if (target.getTankType().getID() == Fluids.NONE.getID()) {
                target.setTankType(incoming);
            }
            target.setFill(target.getFill() + toFill);
        }

        return toFill;
    }

    private static FluidStack drainTanksByType(FluidTank[] tanks, FluidStack resource, boolean doDrain) {
        if (tanks == null || resource == null || resource.getFluid() == null) return null;

        FluidType wanted = ModFluidRegistry.getHBMFluid(resource.getFluid());
        if (wanted == null) return null;

        for (FluidTank tank : tanks) {
            if (tank.getPressure() == 0 && tank.getTankType().getID() == wanted.getID() && tank.getFill() > 0) {
                Fluid forgeFluid = ModFluidRegistry.getForgeFluid(tank.getTankType());
                if (forgeFluid == null) return null;

                int drained = Math.min(resource.amount, tank.getFill());
                if (drained <= 0) return null;

                if (doDrain) {
                    tank.setFill(tank.getFill() - drained);
                }

                return new FluidStack(forgeFluid, drained);
            }
        }
        return null;
    }

    private static FluidStack drainTanksAny(FluidTank[] tanks, int maxDrain, boolean doDrain) {
        if (tanks == null || maxDrain <= 0) return null;

        for (FluidTank tank : tanks) {
            if (tank.getPressure() != 0) continue; // never let a pressurized tank drain out as plain Forge fluid
            if (tank.getFill() <= 0 || tank.getTankType().getID() == Fluids.NONE.getID()) continue;

            Fluid forgeFluid = ModFluidRegistry.getForgeFluid(tank.getTankType());
            if (forgeFluid == null) continue; // no Forge-side counterpart, try the next tank

            int drained = Math.min(maxDrain, tank.getFill());
            if (drained <= 0) continue;

            if (doDrain) {
                tank.setFill(tank.getFill() - drained);
            }

            return new FluidStack(forgeFluid, drained);
        }
        return null;
    }

    private static boolean canFillTanks(FluidTank[] tanks, Fluid fluid) {
        if (tanks == null || fluid == null) return false;

        FluidType incoming = ModFluidRegistry.getHBMFluid(fluid);
        if (incoming == null || incoming.getID() == Fluids.NONE.getID()) return false;

        for (FluidTank tank : tanks) {
            if (tank.getPressure() != 0) continue;
            if (tank.getTankType().getID() == Fluids.NONE.getID()
                    || tank.getTankType().getID() == incoming.getID()) {
                return true;
            }
        }
        return false;
    }

    private static boolean canDrainTanks(FluidTank[] tanks, Fluid fluid) {
        if (tanks == null || fluid == null) return false;

        FluidType wanted = ModFluidRegistry.getHBMFluid(fluid);
        if (wanted == null) return false;

        for (FluidTank tank : tanks) {
            if (tank.getPressure() == 0 && tank.getTankType().getID() == wanted.getID() && tank.getFill() > 0) return true;
        }
        return false;
    }

    private static FluidTankInfo[] tankInfo(FluidTank[] tanks) {
        if (tanks == null || tanks.length == 0) return emptyInfo();

        List<FluidTankInfo> infos = new ArrayList<FluidTankInfo>(tanks.length);
        for (FluidTank tank : tanks) {
            if (tank.getPressure() != 0) {
                infos.add(new FluidTankInfo(null, 0));
                continue;
            }
            Fluid forgeFluid = tank.getTankType().getID() == Fluids.NONE.getID()
                    ? null
                    : ModFluidRegistry.getForgeFluid(tank.getTankType());
            FluidStack stack = forgeFluid == null ? null : new FluidStack(forgeFluid, tank.getFill());
            infos.add(new FluidTankInfo(stack, tank.getMaxFill()));
        }
        return infos.toArray(new FluidTankInfo[0]);
    }

    private static FluidTankInfo[] emptyInfo() {
        return new FluidTankInfo[]{new FluidTankInfo(null, 0)};
    }

    private static void logError(String method, Object self, Throwable t) {
        FluidTranslator.logger.error("UniversalFluidBridge#" + method + " failed for "
                + (self == null ? "null" : self.getClass().getName()), t);
    }
}