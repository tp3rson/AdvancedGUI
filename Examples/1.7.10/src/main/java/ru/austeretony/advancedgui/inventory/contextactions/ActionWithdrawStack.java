package ru.austeretony.advancedgui.inventory.contextactions;

import libs.austeretony.advancedgui.guicontainer.button.GUIButtonSound;
import libs.austeretony.advancedgui.guicontainer.contextmenu.GUIPresetAction;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import ru.austeretony.advancedgui.inventory.ContainerAdvancedStorage;
import ru.austeretony.advancedgui.network.NetworkHandler;
import ru.austeretony.advancedgui.network.server.SPContextActionWithdrawStack;

public class ActionWithdrawStack extends GUIPresetAction {

	public ActionWithdrawStack(String name) {
		
		super(name);
	}

	@Override
	public boolean isValidAction(IInventory inventory, Slot slot, EntityPlayer player) {
		
		return ((ContainerAdvancedStorage) player.openContainer).playerInventory.getFirstEmptyStack() != - 1;
	}

	@Override
	public void performAction(IInventory inventory, Slot slot, EntityPlayer player) {
		
		slot.inventory.setInventorySlotContents(slot.getSlotIndex(), null);
		
		NetworkHandler.sendToServer(new SPContextActionWithdrawStack(slot.getSlotIndex()));
	}

	@Override
	public GUIButtonSound getSound() {
		
		return null;
	}
}
