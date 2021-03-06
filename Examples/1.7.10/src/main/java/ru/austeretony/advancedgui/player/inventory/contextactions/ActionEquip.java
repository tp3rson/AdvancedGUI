package ru.austeretony.advancedgui.player.inventory.contextactions;

import libs.austeretony.advancedgui.guicontainer.button.GUIButtonSound;
import libs.austeretony.advancedgui.guicontainer.contextmenu.GUIPresetAction;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import ru.austeretony.advancedgui.network.NetworkHandler;
import ru.austeretony.advancedgui.network.server.SPContextActionEquip;
import ru.austeretony.advancedgui.player.inventory.PlayerInventory;

public class ActionEquip extends GUIPresetAction {

	public ActionEquip(String name) {
		
		super(name);
	}

	@Override
	public boolean isValidAction(IInventory inventory, Slot slot, EntityPlayer player) {		
		
		int armorType = - 1;
		
		boolean 
		isWeapon = slot.getStack().getItem() instanceof ItemSword || slot.getStack().getItem() instanceof ItemBow || slot.getStack().getItem() instanceof ItemTool,
		isArmor = slot.getStack().getItem() instanceof ItemArmor,
		hasHotbarSpace = player.inventory.getFirstEmptyStack() != - 1 && player.inventory.getFirstEmptyStack() < 9,
		hasValidSpaceArmor = false;
		
		if (isArmor) {
	
			armorType = ((ItemArmor) slot.getStack().getItem()).armorType;
			
			hasValidSpaceArmor = player.inventory.getStackInSlot(player.inventory.getSizeInventory() - 1 - armorType) == null;
		}
		
		return (isWeapon && hasHotbarSpace) || (isArmor && hasValidSpaceArmor);
	}

	@Override
	public void performAction(IInventory inventory, Slot slot, EntityPlayer player) {
		
		int inventoryId = - 1;
		
		if (inventory instanceof PlayerInventory) {
			
			inventoryId = 0;
		}
		
		else if (inventory instanceof InventoryPlayer) {
			
			inventoryId = 2;
		}
		
		NetworkHandler.sendToServer(new SPContextActionEquip(inventoryId, slot.getSlotIndex()));
	}

	@Override
	public GUIButtonSound getSound() {
		
		return null;
	}
}
