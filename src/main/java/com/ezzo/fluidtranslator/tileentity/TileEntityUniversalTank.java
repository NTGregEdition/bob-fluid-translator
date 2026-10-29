package com.ezzo.fluidtranslator.tileentity;

import api.hbm.fluidmk2.FluidNode;
import api.hbm.fluidmk2.IFluidConnectorMK2;
import api.hbm.fluidmk2.IFluidReceiverMK2;
import api.hbm.fluidmk2.IFluidStandardTransceiverMK2;
import api.hbm.tile.ILoadedTile.TileAccessCache;
import com.ezzo.fluidtranslator.ModFluidRegistry;
import com.ezzo.fluidtranslator.TankModes;
import com.ezzo.fluidtranslator.adapter.UnifiedFluid;
import com.ezzo.fluidtranslator.adapter.UnifiedFluidStack;
import com.ezzo.fluidtranslator.adapter.UnifiedFluidTank;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.items.machine.ItemFluidTank;
import com.hbm.lib.Library;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.uninos.UniNodespace;
import com.hbm.util.fauxpointtwelve.BlockPos;
import com.hbm.util.fauxpointtwelve.DirPos;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.*;

import javax.annotation.Nullable;
import java.util.HashSet;

public class TileEntityUniversalTank extends TileEntityLoadedBase implements IInventory, IFluidStandardTransceiverMK2, IFluidHandler {

    protected FluidNode node;
    protected FluidType lastType = Fluids.NONE;
    public UnifiedFluidTank tank;
    private short mode = (short)TankModes.DISABLED.ordinal;
    private final ItemStack[] slots = new ItemStack[2];

    // Used to add a delay between transfer operations with items
    public int operationDelay = 0;
    public final int OPERATION_TIME_TICKS = 10;

    public TileEntityUniversalTank() {
        this(8000);
    }

    public TileEntityUniversalTank(int capacity) {
        super();
        tank = new UnifiedFluidTank(capacity);
    }

    /// HBM-RELEVANT IMPLEMENTATION ///

    public void setTankMode(short mode) {
        if (mode < 0 || mode > 3) {
            throw new IllegalArgumentException("Invalid tank mode: " + mode);
        }
        this.mode = mode;
        markDirtyAndUpdate();
    }

    public short getTankMode() {
        return this.mode;
    }

    @Override
    public FluidTank[] getSendingTanks() {
        if (mode == TankModes.BUFFER.ordinal || mode == TankModes.SENDER.ordinal)
            return new FluidTank[] {tank.toHBM()};
        else
            return new FluidTank[0];
    }

    @Override
    public FluidTank[] getReceivingTanks() {
        if (mode == TankModes.BUFFER.ordinal || mode == TankModes.RECEIVER.ordinal)
            return new FluidTank[] {tank.toHBM()};
        else
            return new FluidTank[0];
    }

    @Override
    public FluidTank[] getAllTanks() {
        return new FluidTank[] { tank.toHBM() };
    }

    @Override
    public long getDemand(FluidType type, int pressure) {
        if(this.mode == TankModes.SENDER.ordinal || this.mode == TankModes.DISABLED.ordinal) return 0;
        if(tank.toHBM().getPressure() != pressure) return 0;
        return type == tank.toHBM().getTankType() ? tank.getCapacity() - tank.getFill() : 0;
    }

    @Override
    public boolean isLoaded() {
        return true;
    }

    public long transferFluid(FluidType type, int pressure, long amount) {
        int tanks = 0;
        for(FluidTank tank : getReceivingTanks()) {
            if(tank.getTankType() == type && tank.getPressure() == pressure) tanks++;
        }
        if(tanks > 1) {
            int firstRound = (int) Math.floor((double) amount / (double) tanks);
            for(FluidTank tank : getReceivingTanks()) {
                if(tank.getTankType() == type && tank.getPressure() == pressure) {
                    int toAdd = Math.min(firstRound, tank.getMaxFill() - tank.getFill());
                    tank.setFill(tank.getFill() + toAdd);
                    amount -= toAdd;
                }
            }
            this.markDirtyAndUpdate();
        }
        if(amount > 0) for(FluidTank tank : getReceivingTanks()) {
            if(tank.getTankType() == type && tank.getPressure() == pressure) {
                int toAdd = (int) Math.min(amount, tank.getMaxFill() - tank.getFill());
                tank.setFill(tank.getFill() + toAdd);
                amount -= toAdd;
                this.markDirtyAndUpdate();
            }
        }
        return amount;
    }

    public long getFluidAvailable(FluidType type, int pressure) {
        long amount = 0;
        for(FluidTank tank : getSendingTanks()) {
            if(tank.getTankType() == type && tank.getPressure() == pressure) amount += tank.getFill();
        }
        return amount;
    }

    public void useUpFluid(FluidType type, int pressure, long amount) {
        int tanks = 0;
        for(FluidTank tank : getSendingTanks()) {
            if(tank.getTankType() == type && tank.getPressure() == pressure) tanks++;
        }
        if(tanks > 1) {
            int firstRound = (int) Math.floor((double) amount / (double) tanks);
            for(FluidTank tank : getSendingTanks()) {
                if(tank.getTankType() == type && tank.getPressure() == pressure) {
                    int toRem = Math.min(firstRound, tank.getFill());
                    tank.setFill(tank.getFill() - toRem);
                    amount -= toRem;
                }
            }
            this.markDirtyAndUpdate();
        }
        if(amount > 0) for(FluidTank tank : getSendingTanks()) {
            if(tank.getTankType() == type && tank.getPressure() == pressure) {
                int toRem = (int) Math.min(amount, tank.getFill());
                tank.setFill(tank.getFill() - toRem);
                amount -= toRem;
                this.markDirtyAndUpdate();
            }
        }
    }

    protected DirPos[] getConPos() {
        return new DirPos[] {
                new DirPos(xCoord + 1, yCoord, zCoord, Library.POS_X),
                new DirPos(xCoord - 1, yCoord, zCoord, Library.NEG_X),
                new DirPos(xCoord, yCoord + 1, zCoord, Library.POS_Y),
                new DirPos(xCoord, yCoord - 1, zCoord, Library.NEG_Y),
                new DirPos(xCoord, yCoord, zCoord + 1, Library.POS_Z),
                new DirPos(xCoord, yCoord, zCoord - 1, Library.NEG_Z)
        };
    }

    /**
     * This method handles the behavior of this node in a fluid network from HBM
     */
    private void handleHBMNode() {
        if(mode == TankModes.DISABLED.ordinal && this.node != null) {
            UniNodespace.destroyNode(worldObj, xCoord, yCoord, zCoord, tank.toHBM().getTankType().getNetworkProvider());
            this.node = null;
            return;
        }

        if(!worldObj.isRemote) {
            if(mode == TankModes.BUFFER.ordinal) {
                if(this.node == null || this.node.expired || tank.toHBM().getTankType() != lastType) {

                    this.node = (FluidNode) UniNodespace.getNode(worldObj, xCoord, yCoord, zCoord, tank.toHBM().getTankType().getNetworkProvider());

                    if(this.node == null || this.node.expired || tank.toHBM().getTankType() != lastType) {
                        this.node = this.createNode(tank.toHBM().getTankType());
                        UniNodespace.createNode(worldObj, this.node);
                        lastType = tank.toHBM().getTankType();
                    }
                }

                if(node != null && node.hasValidNet()) {
                    node.net.addProvider(this);
                    node.net.addReceiver(this);
                }
            } else {
                if(this.node != null) {
                    UniNodespace.destroyNode(worldObj, xCoord, yCoord, zCoord, tank.toHBM().getTankType().getNetworkProvider());
                    this.node = null;
                }

                for(DirPos pos : getConPos()) {
                    FluidNode dirNode = (FluidNode) UniNodespace.getNode(worldObj, pos.getX(), pos.getY(), pos.getZ(), tank.toHBM().getTankType().getNetworkProvider());

                    if(mode == TankModes.SENDER.ordinal) {
                        tryProvide(tank.toHBM(), dirNode, pos);
                    } else {
                        if(dirNode != null && dirNode.hasValidNet()) dirNode.net.removeProvider(this);
                    }

                    if(mode == TankModes.RECEIVER.ordinal) {
                        if(dirNode != null && dirNode.hasValidNet()) dirNode.net.addReceiver(this);
                    } else {
                        if(dirNode != null && dirNode.hasValidNet()) dirNode.net.removeReceiver(this);
                    }
                }
            }
            this.networkPackNT(50);
        }
    }

    private void tryProvide(FluidTank sending, FluidNode dirNode, DirPos pos) {
        FluidType type = sending.getTankType();
        int pressure = sending.getPressure();
        ForgeDirection fromNeighbor = pos.getDir().getOpposite();
        TileEntity te = TileAccessCache.getTileOrCache(worldObj, pos.getX(), pos.getY(), pos.getZ());

        if (te instanceof IFluidConnectorMK2 && ((IFluidConnectorMK2) te).canConnect(type, fromNeighbor)
                && dirNode != null && dirNode.hasValidNet()) {
            dirNode.net.addProvider(this);
        }

        if (te == this || !(te instanceof IFluidReceiverMK2)) return;
        IFluidReceiverMK2 receiver = (IFluidReceiverMK2) te;
        if (!receiver.canConnect(type, fromNeighbor)) return;

        long provides = Math.min(getFluidAvailable(type, pressure), getProviderSpeed(type, pressure));
        long receives = Math.min(receiver.getDemand(type, pressure), receiver.getReceiverSpeed(type, pressure));
        long toTransfer = Math.min(provides, receives);
        if (toTransfer <= 0) return;

        toTransfer -= receiver.transferFluid(type, pressure, toTransfer);
        useUpFluid(type, pressure, toTransfer);
    }

    protected FluidNode createNode(FluidType type) {
        DirPos[] conPos = getConPos();

        HashSet<BlockPos> posSet = new HashSet<BlockPos>();
        posSet.add(new BlockPos(this));
        for(DirPos pos : conPos) {
            ForgeDirection dir = pos.getDir();
            posSet.add(new BlockPos(pos.getX() - dir.offsetX, pos.getY() - dir.offsetY, pos.getZ() - dir.offsetZ));
        }

        return new FluidNode(type.getNetworkProvider(), posSet.toArray(new BlockPos[posSet.size()])).setConnections(conPos);
    }

    @Override
    public void updateEntity() {
        handleHBMNode();
        handleItemInput();
    }

    private void handleItemInput() {
        ItemStack stackIn = this.getStackInSlot(0);

        if (stackIn == null){
            operationDelay = OPERATION_TIME_TICKS;
            return;
        }

        if (operationDelay <= 0) {
            if (worldObj.isRemote) return;
            if (FluidContainerRegistry.isBucket(stackIn)) {
                handleBuckets(stackIn);
            } else if (stackIn.getItem() instanceof IFluidContainerItem) {
                handleFluidContainer(stackIn);
            } else if (stackIn.getItem() instanceof ItemFluidTank) {
                handleFullHBMTank(stackIn);
            } else if (com.hbm.inventory.FluidContainerRegistry.getFullContainer(stackIn, tank.toHBM().getTankType()) != null) {
                // Case of an empty container from HBM
                handleEmptyHBMTank(stackIn);
            } else if (stackIn.getItem() instanceof IItemFluidIdentifier) {
                handleFluidIdentifier(stackIn);
            }
        } else {
            operationDelay--;
        }
    }

    ///  FLUID HANDLING ///

    private void handleFluidIdentifier(ItemStack stackIn) {
        assert stackIn.getItem() instanceof IItemFluidIdentifier;
        FluidType type = ((IItemFluidIdentifier)stackIn.getItem()).getType(null, 0, 0, 0, stackIn);
        tank.toHBM().setTankType(type);
        setInventorySlotContents(0, null);
        setInventorySlotContents(1, stackIn);
        markDirtyAndUpdate();
    }

    private void handleBuckets(ItemStack buckets) {
        FluidStack fluidStack = FluidContainerRegistry.getFluidForFilledItem(buckets);
        if (fluidStack != null) {
            // Full bucket -> tank
            if (!canFill(ForgeDirection.UP, fluidStack.getFluid())) return;
            transferBucketToTank(fluidStack);
        } else {
            // Empty bucket <- tank
            transferTankToBucket(buckets);
        }
    }

    private void transferBucketToTank(FluidStack fluidStack) {
        ItemStack out = getStackInSlot(1);
        if (out != null && out.stackSize >= out.getMaxStackSize()) return;

        int filled = tank.fill(UnifiedFluidStack.fromForge(fluidStack), true);
        if (filled <= 0) return;

        this.setInventorySlotContents(0, null);
        if (out == null) {
            setInventorySlotContents(1, new ItemStack(Items.bucket));
        } else {
            out.stackSize++;
            setInventorySlotContents(1, out);
        }
        markDirtyAndUpdate();
    }

    private void transferTankToBucket(ItemStack buckets) {
        if (getStackInSlot(1) != null) return;
        if (tank.getFill() < FluidContainerRegistry.BUCKET_VOLUME) return;

        ItemStack bucket = new ItemStack(Items.bucket);
        UnifiedFluidStack drained = tank.drain(FluidContainerRegistry.BUCKET_VOLUME, true);
        if (drained == null || drained.amount() <= 0) return;

        ItemStack filledBucket = FluidContainerRegistry.fillFluidContainer(drained.toForge(), bucket);
        if (filledBucket == null) return;

        buckets.stackSize--;
        if (buckets.stackSize <= 0) this.setInventorySlotContents(0, null);
        else this.setInventorySlotContents(0, buckets);
        this.setInventorySlotContents(1, filledBucket);

        this.operationDelay = this.OPERATION_TIME_TICKS;
        markDirtyAndUpdate();
    }

    private void handleFluidContainer(ItemStack containerIn) {
        assert containerIn.getItem() instanceof IFluidContainerItem;
        if (containerIn.stackSize > 1) return;
        if (getStackInSlot(1) != null) return;

        IFluidContainerItem container = (IFluidContainerItem) containerIn.getItem();
        FluidStack containerFluid = container.getFluid(containerIn);

        if (containerFluid == null) {
            transferTankToContainer(containerIn, container, null);
            return;
        }
        if (tank.getFill() <= 0) { // tank is empty: fill the tank
            if (!canFill(ForgeDirection.UP, containerFluid.getFluid())) return;
            transferContainerToTank(containerIn, container, containerFluid);
        } else if (tank.getFill() < tank.getCapacity()) { // tank has some fluid
            if (container.getCapacity(containerIn) == containerFluid.amount) { // container is full: fill tank
                if (!canFill(ForgeDirection.UP, containerFluid.getFluid())) return;
                transferContainerToTank(containerIn, container, containerFluid);
            } else { // fill container
                if (!canDrain(ForgeDirection.UP, containerFluid.getFluid())) return;
                transferTankToContainer(containerIn, container, containerFluid);
            }
        } else { // tank is full: fill container
            if (!canDrain(ForgeDirection.UP, containerFluid.getFluid())) return;
            transferTankToContainer(containerIn, container, containerFluid);
        }
    }

    private void transferContainerToTank(ItemStack stackIn, IFluidContainerItem container, FluidStack containerFluid) {
        int filled = tank.fill(UnifiedFluidStack.fromForge(containerFluid), true);
        if (filled > 0) {
            container.drain(stackIn, filled, true);
            this.setInventorySlotContents(0, null);
            this.setInventorySlotContents(1, stackIn);
        }
        markDirtyAndUpdate();
    }

    private void transferTankToContainer(ItemStack stackIn, IFluidContainerItem container, @Nullable FluidStack containerFluid) {
        int space = containerFluid == null
                ? container.getCapacity(stackIn)
                : container.getCapacity(stackIn) - containerFluid.amount;

        if (space <= 0 || tank.getFill() <= 0) {
            return;
        }

        int toTransfer = Math.min(tank.getFill(), space);
        UnifiedFluidStack drained = tank.drain(toTransfer, true);
        if (drained == null || drained.amount() <= 0) {
            return;
        }

        int accepted = container.fill(stackIn, drained.toForge(), true);
        if (accepted > 0) {
            this.setInventorySlotContents(0, null);
            this.setInventorySlotContents(1, stackIn);
        }
        markDirtyAndUpdate();
    }

    // Transfer from HBM portable tank to universal tank
    private void handleFullHBMTank(ItemStack tankIn) {
        assert tankIn.getItem() instanceof ItemFluidTank;
        FluidType storedFluid = Fluids.fromID(tankIn.getItemDamage());
        int fluidAmount = com.hbm.inventory.FluidContainerRegistry.getFluidContent(tankIn, storedFluid);
        UnifiedFluidStack stackToTransfer = UnifiedFluidStack.fromHBM(storedFluid, fluidAmount);

        if (!canFill(ForgeDirection.UP, stackToTransfer.toForge().getFluid())) return;
        transferHBMContainerToTank(tankIn, stackToTransfer);
    }

    private void transferHBMContainerToTank(ItemStack stackIn, UnifiedFluidStack toTransfer) {
        ItemStack out = getStackInSlot(1);
        if (out != null && out.stackSize >= out.getMaxStackSize()) return;

        if (toTransfer.amount() > tank.getCapacity() - tank.getFill()) return;
        int filled = tank.fill(toTransfer, true);
        if (filled <= 0) return;

        stackIn.stackSize--;
        if (stackIn.stackSize <= 0) this.setInventorySlotContents(0, null);
        else this.setInventorySlotContents(0, stackIn);

        if (out == null) {
            ItemStack emptyContainer = com.hbm.inventory.FluidContainerRegistry.getEmptyContainer(stackIn);
            setInventorySlotContents(1, emptyContainer);
        } else {
            out.stackSize++;
            setInventorySlotContents(1, out);
        }
        markDirtyAndUpdate();
    }

    // Transfer from universal tank to HBM portable tank
    private void handleEmptyHBMTank(ItemStack stackIn) {
        ItemStack fullContainer = com.hbm.inventory.FluidContainerRegistry.getFullContainer(stackIn, tank.toHBM().getTankType());
        int toTransfer = com.hbm.inventory.FluidContainerRegistry.getFluidContent(fullContainer, tank.toHBM().getTankType());
        FluidType storedFluid = Fluids.fromID(fullContainer.getItemDamage());
        UnifiedFluidStack stackToTransfer = UnifiedFluidStack.fromHBM(storedFluid, toTransfer);

        if (!canDrain(ForgeDirection.UP, stackToTransfer.toForge().getFluid())) return;
        transferTankToHBMContainer(stackIn, stackToTransfer);
    }

    private void transferTankToHBMContainer(ItemStack stackIn, UnifiedFluidStack toTransfer) {
        ItemStack out = getStackInSlot(1);
        if (out != null && out.stackSize >= out.getMaxStackSize()) return;

        if (toTransfer.amount() > tank.getFill()) return;
        UnifiedFluidStack drained = tank.drain(toTransfer.amount(), true);
        if (drained == null || drained.amount() <= 0) return;

        stackIn.stackSize--;
        if (stackIn.stackSize <= 0) this.setInventorySlotContents(0, null);
        else this.setInventorySlotContents(0, stackIn);

        if (out == null) {
            ItemStack fullContainer = com.hbm.inventory.FluidContainerRegistry.getFullContainer(stackIn, tank.toHBM().getTankType());
            setInventorySlotContents(1, fullContainer);
        } else {
            out.stackSize++;
            setInventorySlotContents(1, out);
        }
        markDirtyAndUpdate();
    }

    /// FORGE-RELEVANT IMPLEMENTATION ///

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setShort("tankMode", this.mode);
        NBTTagCompound tankTag = tank.writeToNBT();
        tag.setTag("tank", tankTag);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        if (tag.hasKey("tankMode")) {
            this.mode = tag.getShort("tankMode");
        }
        if (tag.hasKey("tank")) {
            tank.readFromNBT(tag.getCompoundTag("tank"));
        }
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound nbtTag = new NBTTagCompound();
        this.writeToNBT(nbtTag);
        return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 1, nbtTag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        this.readFromNBT(pkt.func_148857_g());
    }

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        if (resource == null) return 0;
        if (!canFill(from, resource.getFluid())) return 0;
        int filled = tank.fill(UnifiedFluidStack.fromForge(resource), doFill);
        if(filled > 0 && doFill) this.markDirtyAndUpdate();
        return filled;
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
        if (resource == null || resource.getFluid() == null) return null;
        if (!canDrain(from, resource.getFluid())) return null;

        UnifiedFluidStack drained = tank.drain(resource.amount, doDrain);
        if (drained == null || drained.isEmpty()) return null;

        if (doDrain) markDirtyAndUpdate();
        return drained.toForge();
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        if (maxDrain <= 0) return null;

        UnifiedFluidStack drained = tank.drain(maxDrain, doDrain);
        if (drained == null || drained.isEmpty()) return null;

        if (doDrain) markDirtyAndUpdate();
        return drained.toForge();
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid) {
        FluidType incomingFluid = ModFluidRegistry.getHBMFluid(fluid);
        FluidType storedFluid = tank.toHBM().getTankType();

        if (incomingFluid == null) return false;
        if (incomingFluid.getID() == Fluids.NONE.getID()) return false;
        if (storedFluid.getID() == Fluids.NONE.getID()) return true;
        return tank.setFluidSafe(UnifiedFluid.fromForge(fluid));
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        if (fluid == null) return false;
        return tank.toHBM().getTankType().getID() == ModFluidRegistry.getHBMFluid(fluid).getID();
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from) {
        return new FluidTankInfo[]{tank.toForge().getInfo()};
    }

    ///  INVENTORY HANDLING ///

    @Override
    public int getSizeInventory() {
        return slots.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot < slots.length) {
            return slots[slot];
        } else {
            return null;
        }
    }

    /**
     * Removes from an inventory slot (first arg) up to a specified number (second arg) of items and returns them in a
     * new stack.
     */
    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        if (this.slots[slot] != null) {
            ItemStack stack;

            if (this.slots[slot].stackSize <= amount) {
                // If the stack is smaller than the requested amount
                stack = this.slots[slot];
                this.slots[slot] = null;
                return stack;
            } else {
                stack = this.slots[slot].splitStack(amount);

                if (this.slots[slot].stackSize == 0) {
                    this.slots[slot] = null;
                }

                return stack;
            }
        } else {
            return null;
        }
    }

    /**
     * When some containers are closed they call this on each slot, then drop whatever it returns as an EntityItem -
     * like when you close a workbench GUI.
     */
    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        if(slots[slot] != null) {
            ItemStack stack = slots[slot];
            slots[slot] = null;
            return stack;
        } else {
            return null;
        }
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack itemStack) {
        slots[slot] = itemStack;
        if(itemStack != null && itemStack.stackSize > this.getInventoryStackLimit()) {
            itemStack.stackSize = this.getInventoryStackLimit();
        }
    }

    @Override
    public String getInventoryName() {
        return "forgeFluidTankInventory";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return true;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer p_70300_1_) {
        return false;
    }

    @Override
    public void openInventory() {

    }

    @Override
    public void closeInventory() {

    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack itemStack) {
        return true;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    public void markDirtyAndUpdate() {
        this.markDirty();
        if (worldObj != null && !worldObj.isRemote) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }
}
