package ru.austeretony.advancedgui.network.server;

import java.io.IOException;

import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import ru.austeretony.advancedgui.inventory.ContainerAdvancedStorage;
import ru.austeretony.advancedgui.network.AbstractMessage.AbstractServerMessage;

public class SPContextActionWithdrawStack extends AbstractServerMessage<SPContextActionWithdrawStack> {
	
	private short slotIndex;
	
	public SPContextActionWithdrawStack() {}
	
	public SPContextActionWithdrawStack(int slotIndex) {
		
		this.slotIndex = (short) slotIndex;
	}

	@Override
	protected void writeData(PacketBuffer buffer) throws IOException {
		
		buffer.writeShort(this.slotIndex);
	}

	@Override
	protected void readData(PacketBuffer buffer) throws IOException {
		
		this.slotIndex = buffer.readShort();
	}

	@Override
	public void performProcess(EntityPlayer player, Side side) {
		
		ContainerAdvancedStorage storage = ((ContainerAdvancedStorage) player.openContainer);
		
		ItemStack itemStack = storage.slorageInventory.getStackInSlot(this.slotIndex).copy();
		
		storage.slorageInventory.setInventorySlotContents(this.slotIndex, null);
		
		storage.playerInventory.setInventorySlotContents(storage.playerInventory.getFirstEmptyStack(), itemStack);
	}
}
