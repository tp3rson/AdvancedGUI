package libs.austeretony.advancedgui.guicontainer.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import libs.austeretony.advancedgui.guicontainer.button.GUIButton;
import libs.austeretony.advancedgui.guicontainer.framework.GUIFramework;
import libs.austeretony.advancedgui.guicontainer.framework.GUIFramework.GUIPosition;
import libs.austeretony.advancedgui.guicontainer.framework.GUISorter;
import libs.austeretony.advancedgui.guicontainer.panel.GUIButtonPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;

/**
 * Модернизированный GuiContainer. Должен быть унаследован вашим ГПИ.
 */
@SideOnly(Side.CLIENT)
public abstract class AdvancedGUIContainer extends GuiScreen {
	
	/**
	 *  Данный класс представляет собой изменённую версию GuiContainer. 
	 * Все оригинальные механики сохранены, дополнительные элементы работают 
	 * параллельно. Изменению подвергся принцип отрисовки и получения 
	 * "кликнутого" слота. 
	 *	Данному классу в конструктор передаётся контейнер,
	 * содержащий массив слотов (Container#inventorySlots). Оригинальный класс
	 * GuiContainer работал с этим массивом напрямую. Текущая реализация 
	 * использует объекты GUIFramework для перехвата определённого набора слотов
	 * и обеспечения работы всех дополнительных элементов. Массив GUIFramework#slots 
	 * содержит копии слотов из Container#inventorySlots, GUIFramework#slotsIndexes
	 * содержит индексы этих слотов, соответствующие позициям слотов в нём. Когда
	 * производится действие со слотом (клик), вызывается метод
	 * AdvancedGUIContainer#getSlotAtPosition(), который должен вернуть реальный слот. 
	 * Оригинальный метод перебирал слоты из Container#inventorySlots, 
	 * текущий метод перебирает GUIFramework#slots, и возвращает слот из 
	 * оригинального массива Container#inventorySlots, используя индекс из 
	 * GUIFramework#slotsIndexes. Таким образом любые действия производятся с
	 * оригинальными слотами, а GUIFramework лишь предоставляет возможности
	 * по контролю отображаемого контента, т.к. все внутренние действия производятся
	 * с собственными массивами слотов.
	 * 
	 *  Для работы с новыми элементами данный класс должен быть унаследован ГПИ контейнера.
	 * Для того что бы добавить слоты в ГПИ создайте новый GUIFramework. Используйте
	 * {@link AdvancedGUIContainer#addFramework()} в {@link AdvancedGUIContainer#initFrameworks()} 
	 * для его регистрации и вызовите {@link AdvancedGUIContainer#updateFramework()} 
	 * передав в него созданый фреймворк и {@link GUIFramework#BASE_SORTER} для 
	 * отображения оригинального содержимого слотов.
	 */
	
    protected int xSize = 176;
    protected int ySize = 166;
    
    public Container mainContainer;
    
    protected int guiLeft;
    protected int guiTop;
    
    protected Slot theSlot;
    
    private Slot clickedSlot;
    protected boolean isRightMouseClick;
    protected ItemStack draggedStack;
    protected int touchUpX;
    protected int touchUpY;
    protected Slot returningStackDestSlot;
    protected long returningStackTime;

    protected ItemStack returningStack;   
    private Slot currentDragTargetSlot;   
    private long dragItemDropDelay;
    protected final Set dragSplittingSlots = new HashSet();
    protected boolean dragSplitting;
    private int dragSplittingLimit;
    private int dragSplittingButton;
    private boolean ignoreMouseUp;
    protected int dragSplittingRemnant;
    private long lastClickTime;
    private Slot lastClickSlot;
    private int lastClickButton;
    private boolean doubleClick;
    private ItemStack shiftClickedSlot;
    
    /** Рабочее пространство */
    private GUIContainerWorkspace workspace;
	
    /** Объекты GUIFramework для слотов */
	private final List<GUIFramework> frameworks = new ArrayList<GUIFramework>();
	
	private int righClickedFramework, openedMenuFramework, yPrevMouse;
	
	private boolean menuOpened;
	
	private ItemStack dragged;
	
    public AdvancedGUIContainer(Container container) {
    	
        this.mainContainer = container;
        this.ignoreMouseUp = true;
    }
    
    public void initGui() {
    	
        super.initGui();
                
        this.mc.thePlayer.openContainer = this.mainContainer;
        
        this.workspace = this.initWorkspace();
        
        this.xSize = this.workspace.getWidth();
        this.ySize = this.workspace.getHeight();
                
        this.guiLeft = this.workspace.getXPosition();
        this.guiTop = this.workspace.getYPosition();
                
        this.initFrameworks();
    }
    
    /**
     * Используется для инициализпции рабочего пространства ГПИ. 
     * 
     * @return настроенный GUIContainerWorkspace для этого ГПИ.
     */
    protected abstract GUIContainerWorkspace initWorkspace();
    
    /**
     * Используется для инициализации объектов GUIFramework.
     */
    protected abstract void initFrameworks();
    
    /**
     * Используется для добавления фреймворка в рабочий массив. Данный метод
     * должен быть вызван для каждого созданного объекта GUIFramework.
     * 
     * @param framework содержащий набор слотов и управляющих элементов
     */
    protected final void addFramework(GUIFramework framework) {
    	
    	if (!this.frameworks.contains(framework)) this.frameworks.add(framework);
    }
    
    /**
     * Метод для обновления содержимого фреймворка. Должен быть вызван при инициализации ГПИ
     * для созданного фреймворка после его регистрации. Вторым аргументом принимает сортировщик
     * GUISorter. Для отображения без сортировки использовать {@link GUIFramework#BASE_SORTER}.
     * 
     * @param framework фреймворк, требующий обновления
     * @param sorter сортировщик
     */
    protected void updateFramework(GUIFramework framework, GUISorter sorter) {
    	
		int i, k, size;
    	
    	Slot slot, slotCopy;
    	
    	Item itemInSlot;
    		
    	framework.slots.clear();
    	framework.slotsIndexes.clear();
    		
    	framework.slotsBuffer.clear();
    	framework.indexesBuffer.clear();
    	
    	framework.searchSlots.clear();
    	framework.searchIndexes.clear();
    		
    	framework.items.clear();
    	
    	framework.setCurrentSorter(sorter);
    	
    	if (framework.getSlider() != null) {
    		
    		framework.getSlider().reset();
    	}
    		
        for (i = framework.firstSlotIndex; i <= framework.lastSlotIndex; i++) {
    		
    		slot = (Slot) this.mainContainer.inventorySlots.get(i);   
        	
    		slotCopy = this.copySlot(slot);  
    		
			size = framework.slots.size();			
			    			
    		if (sorter == framework.BASE_SORTER) {    
    				
        	    if (slotCopy.getHasStack()) {
        	    		
        	    	framework.items.put(size, slotCopy.getStack());
        	    }
        	    
				if (framework.slotsPosition == GUIPosition.CUSTOM) {
					
					k = size / framework.columns;
					
					slotCopy.xDisplayPosition = framework.getXPosition() + size * (framework.getSlotWidth() + framework.getSlotDistanceHorizontal()) - k * ((framework.getSlotWidth() + framework.getSlotDistanceHorizontal()) * framework.columns);
					slotCopy.yDisplayPosition = framework.getYPosition() + k * (framework.getSlotHeight() + framework.getSlotDistanceVertical()) - (size / framework.visibleSlots) * (framework.rows * (framework.getSlotHeight() + framework.getSlotDistanceVertical()));
				}
        	            			
        	    framework.slots.add(slotCopy);
        	    framework.slotsIndexes.add(i);  
        	    	
        	    framework.slotsBuffer.add(slotCopy);
        	    framework.indexesBuffer.add(i);          	    
    		}  		   		
    			
    		else {
    		
    			if (slotCopy.getHasStack()) {
	    		
    				framework.items.put(size, slotCopy.getStack());
    	    	
    				if (sorter.isSlotValid(slotCopy, Minecraft.getMinecraft().thePlayer)) {   
        			
    					if (framework.slotsPosition == GUIPosition.CUSTOM) {
    						
    						k = size / framework.columns;
    						
    						slotCopy.xDisplayPosition = framework.getXPosition() + size * (framework.getSlotWidth() + framework.getSlotDistanceHorizontal()) - k * ((framework.getSlotWidth() + framework.getSlotDistanceHorizontal()) * framework.columns);
    						slotCopy.yDisplayPosition = framework.getYPosition() + k * (framework.getSlotHeight() + framework.getSlotDistanceVertical()) - (size / framework.visibleSlots) * (framework.rows * (framework.getSlotHeight() + framework.getSlotDistanceVertical()));
    					}
    					
    					framework.slots.add(slotCopy);
    					framework.slotsIndexes.add(i);  
            	    	
    					framework.slotsBuffer.add(slotCopy);
    					framework.indexesBuffer.add(i);     					
    				}
    			}
    		}
    	}
    	
		if (sorter != framework.BASE_SORTER && sorter.shouldAddEmptySlotsAfter()) {
    	
			i = framework.firstSlotIndex;
    	
	        for (i = framework.firstSlotIndex; i <= framework.lastSlotIndex; i++) {
        		
				slot = (Slot) this.mainContainer.inventorySlots.get(i);   
        	
				slotCopy = this.copySlot(slot);  
    		
				if (!slotCopy.getHasStack()) {
					
					size = framework.slots.size();
					    				
					if (framework.slotsPosition == GUIPosition.CUSTOM) {
						
						k = size / framework.columns;
						
						slotCopy.xDisplayPosition = framework.getXPosition() + size * (framework.getSlotWidth() + framework.getSlotDistanceHorizontal()) - k * ((framework.getSlotWidth() + framework.getSlotDistanceHorizontal()) * framework.columns);
						slotCopy.yDisplayPosition = framework.getYPosition() + k * (framework.getSlotHeight() + framework.getSlotDistanceVertical()) - (size / framework.visibleSlots) * (framework.rows * (framework.getSlotHeight() + framework.getSlotDistanceVertical()));
					}
					
					framework.slots.add(slotCopy);
					framework.slotsIndexes.add(i);  
        	    	
					framework.slotsBuffer.add(slotCopy);
					framework.indexesBuffer.add(i); 
				}
			}
    	}
    }
    	
    protected final Slot copySlot(Slot slot) {
        	
    	return new Slot(slot.inventory, slot.getSlotIndex(), slot.xDisplayPosition, slot.yDisplayPosition);	
    }
    
    public void drawDefaultBackground() {
    	
        this.drawWorldBackground(0);
    }

    //TODO drawScreen()
    public void drawScreen(int mouseX, int mouseY, float renderPartialTicks) {
    	
		this.drawDefaultBackground();

        int k = this.guiLeft;
        int l = this.guiTop;
        
        if (this.workspace != null) {
        	
        	this.workspace.drawTexture();
        }
        
        this.drawGuiContainerBackgroundLayer(renderPartialTicks, mouseX, mouseY); 
                
        RenderHelper.disableStandardItemLighting();
        
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        
        GL11.glPushMatrix();
        
        GL11.glTranslatef((float) k, (float) l, 0.0F);
                
		for (GUIFramework framework : this.frameworks) {
			
			framework.mouseOver(mouseX, mouseY, this.guiLeft, this.guiTop);
			
	        for (GUIButton button : framework.getButtonsList()) {
	        	
	            button.mouseOver(mouseX, mouseY, this.guiLeft, this.guiTop);
	            
	            button.draw();	            
	        }
	        
	        for (GUIButtonPanel buttonPanel : framework.getButtonPanelsList()) {
	        	
	            buttonPanel.mouseOver(mouseX, mouseY, this.guiLeft, this.guiTop);
	            
	            buttonPanel.draw();	            
	        }
		}
		
        GL11.glPopMatrix();
		
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_LIGHTING);
                
        RenderHelper.enableGUIStandardItemLighting();
        
        GL11.glPushMatrix();
        
        GL11.glTranslatef((float) k, (float) l, 0.0F);
        
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        
        this.theSlot = null;
        
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) 240 / 1.0F, (float) 240 / 1.0F);
        
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        
        this.drawInventorySlots(mouseX, mouseY);
        
        RenderHelper.disableStandardItemLighting();  
        
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);    
        
        int slidebarOffset;
        
        float sliderActiveHeight, currentPosition;
		
		for (GUIFramework framework : this.frameworks) {
        	
			if (framework.getSlider() != null) {
				
        		if ((framework.getSearchField() != null && !framework.getSearchField().isFocused()) || framework.getSearchField() == null) {
        			
        			framework.getSlider().mouseOver(mouseX, mouseY, this.guiLeft, this.guiTop);
        			
        			framework.getSlider().mouseClicked();
        			        			
        			if (framework.getSlider().isSlidebarDragged()) {           			
        				        				        						
        				slidebarOffset = mouseY - framework.getSlider().getYSlidebarPosition() - this.guiTop;
        				
        				sliderActiveHeight = framework.getSlider().sliderHeight - framework.getSlider().getSlidebarHeight();
        				
        				currentPosition = mouseY - slidebarOffset - this.yPrevMouse + mouseY - this.guiTop;
        				        				
        				framework.getScroller().setPosition((int) ((float) framework.getScroller().getMaxPosition() * ((currentPosition - framework.getSlider().yPosition) / sliderActiveHeight)));       				
        					
        				framework.getSlider().setYSlidebarPosition((int) (sliderActiveHeight * (currentPosition / sliderActiveHeight)));
        				
        				this.scrollSlots(framework);    				
        			}
        		}
        		
				framework.getSlider().draw();				
			}	     
						 
	        if (framework.getSearchField() != null) {
	        	
	        	framework.getSearchField().draw();
	        }	
	        
	        for (GUIButton button : framework.getButtonsList()) {
	            
	            button.drawPopup(mouseX, mouseY, guiLeft, guiTop);
	        }
	        
	        for (GUIButtonPanel buttonPanel : framework.getButtonPanelsList()) {
	            
	            buttonPanel.drawPopup(mouseX, mouseY, guiLeft, guiTop);
	        }
	        
			if (framework.getContextMenu() != null) {
				
				framework.getContextMenu().mouseOver(mouseX, mouseY, this.guiLeft, this.guiTop);
				
				framework.getContextMenu().draw();
			}
		}
				
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_LIGHTING);
                
        RenderHelper.enableGUIStandardItemLighting();
                
        this.drawGuiContainerForegroundLayer(mouseX, mouseY);
        
        InventoryPlayer inventoryPlayer = this.mc.thePlayer.inventory;
        
        ItemStack itemStack = this.draggedStack == null ? inventoryPlayer.getItemStack() : this.draggedStack;       
        
        this.dragged = itemStack;

        int k1;
        
        if (itemStack != null) {
        	
            byte b0 = 8;
            k1 = this.draggedStack == null ? 8 : 16;
            
            String s = null;

            if (this.draggedStack != null && this.isRightMouseClick) {
            	
                itemStack = itemStack.copy();
                itemStack.stackSize = MathHelper.ceiling_float_int((float)itemStack.stackSize / 2.0F);
            }
            
            else if (this.dragSplitting && this.dragSplittingSlots.size() > 1) {
            	
                itemStack = itemStack.copy();
                itemStack.stackSize = this.dragSplittingRemnant;

                if (itemStack.stackSize == 0) {
                	
                    s = "" + EnumChatFormatting.YELLOW + "0";
                }
            }

            this.drawItemStack(itemStack, mouseX - k - b0, mouseY - l - k1, s);
        }

        if (this.returningStack != null) {
        	
            float f1 = (float) (Minecraft.getSystemTime() - this.returningStackTime) / 100.0F;

            if (f1 >= 1.0F) {
            	
                f1 = 1.0F;
                this.returningStack = null;
            }

            k1 = this.returningStackDestSlot.xDisplayPosition - this.touchUpX;
            
            int j2 = this.returningStackDestSlot.yDisplayPosition - this.touchUpY;
            int l1 = this.touchUpX + (int)((float)k1 * f1);
            int i2 = this.touchUpY + (int)((float)j2 * f1);
            
            this.drawItemStack(this.returningStack, l1, i2, (String) null);
        }

        GL11.glPopMatrix();

        if (inventoryPlayer.getItemStack() == null && this.theSlot != null && this.theSlot.getHasStack()) {
        	
            ItemStack itemstack1 = this.theSlot.getStack();
            
            this.renderToolTip(itemstack1, mouseX, mouseY);
        }

        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        
        RenderHelper.enableStandardItemLighting();
        
		this.yPrevMouse = mouseY;
    }    
    
    //TODO drawInventorySlots()
    protected void drawInventorySlots(int mouseX, int mouseY) {
		
		int i;
		
		Slot slot;
				
		for (GUIFramework framework : this.frameworks) {
			
    		for (i = 0; i < framework.visibleSlots; i++) {        	
				
				if (i < framework.slots.size()) {
					
					slot = framework.slots.get(i);
					
					this.drawSlotBottomLayer(slot, framework);
					            
					if (framework.getSlotRenderer() != null) {
						
						framework.getSlotRenderer().drawSlotBottomLayer(slot);

						framework.getSlotRenderer().drawSlot(slot, this.itemRender);
					}
					
					else {
												
						this.drawSlot(slot, framework);
					}
						
					if (this.isMouseOverSlot(slot, mouseX, mouseY, framework) && slot.func_111238_b()) {
						
						this.drawSlotHighlighting(slot, framework);
					}
				}
			}
		}		
	}
    
    protected void drawSlotBottomLayer(Slot slot, GUIFramework framework) {

        this.drawRect(slot.xDisplayPosition, slot.yDisplayPosition, slot.xDisplayPosition + framework.getSlotWidth(), slot.yDisplayPosition + framework.getSlotHeight(), framework.getSlotBottomLayerColor());
    }
    
    protected void drawSlot(Slot slot, GUIFramework framework) {
    	
        int i = slot.xDisplayPosition;
        int j = slot.yDisplayPosition;
        ItemStack itemStackInSlot = slot.getStack();
        boolean flag = false;
        boolean flag1 = slot == this.clickedSlot && this.draggedStack != null && !this.isRightMouseClick;
        ItemStack itemstack1 = this.mc.thePlayer.inventory.getItemStack();
        String s = null;

        if (slot == this.clickedSlot && this.draggedStack != null && this.isRightMouseClick && itemStackInSlot != null) {
        	
            itemStackInSlot = itemStackInSlot.copy();
            itemStackInSlot.stackSize /= 2;
        }
        
        else if (this.dragSplitting && this.dragSplittingSlots.contains(slot) && itemstack1 != null) {
        	
            if (this.dragSplittingSlots.size() == 1) {
            	
                return;
            }

            if (Container.func_94527_a(slot, itemstack1, true) && this.mainContainer.canDragIntoSlot(slot)) {
            	
                itemStackInSlot = itemstack1.copy();
                flag = true;
                Container.func_94525_a(this.dragSplittingSlots, this.dragSplittingLimit, itemStackInSlot, slot.getStack() == null ? 0 : slot.getStack().stackSize);

                if (itemStackInSlot.stackSize > itemStackInSlot.getMaxStackSize()) {
                	
                    s = EnumChatFormatting.YELLOW + "" + itemStackInSlot.getMaxStackSize();
                    itemStackInSlot.stackSize = itemStackInSlot.getMaxStackSize();
                }

                if (itemStackInSlot.stackSize > slot.getSlotStackLimit()) {
                	
                    s = EnumChatFormatting.YELLOW + "" + slot.getSlotStackLimit();
                    itemStackInSlot.stackSize = slot.getSlotStackLimit();
                }
            }
            
            else {
            	
                this.dragSplittingSlots.remove(slot);
                this.func_146980_g();
            }
        }

        this.zLevel = 100.0F;
        this.itemRender.zLevel = 100.0F;    

        if (itemStackInSlot == null) {
        	
            IIcon iicon = slot.getBackgroundIconIndex();

            if (iicon != null) {
            	
                GL11.glDisable(GL11.GL_LIGHTING);
                GL11.glEnable(GL11.GL_BLEND); // Forge: Blending needs to be enabled for this.
                
                this.mc.getTextureManager().bindTexture(TextureMap.locationItemsTexture);
                this.drawTexturedModelRectFromIcon(i, j, iicon, 16, 16);
                
                GL11.glDisable(GL11.GL_BLEND); // Forge: And clean that up
                GL11.glEnable(GL11.GL_LIGHTING);
                
                flag1 = true;
            }
        }

        if (!flag1) {
        	
            if (flag) {
            	
                this.drawRect(i, j, i + 16, j + 16, - 2130706433);
            }

            GL11.glEnable(GL11.GL_DEPTH_TEST);
            
            this.itemRender.renderItemAndEffectIntoGUI(this.fontRendererObj, this.mc.getTextureManager(), itemStackInSlot, i, j);
            this.itemRender.renderItemOverlayIntoGUI(this.fontRendererObj, this.mc.getTextureManager(), itemStackInSlot, i, j, s);
        }

        this.itemRender.zLevel = 0.0F;
        this.zLevel = 0.0F;
    }
    
    protected void drawSlotHighlighting(Slot slot, GUIFramework framework) {
		
		if (!this.menuOpened) {
			
			this.theSlot = slot;
			
			if (framework.getSlotRenderer() != null) {
			
				framework.getSlotRenderer().drawSlotHighlighting(slot);
			}
			
			else {
				
				int j, k;	       
			
				GL11.glDisable(GL11.GL_DEPTH_TEST);
				GL11.glDisable(GL11.GL_LIGHTING);
        
				j = slot.xDisplayPosition;
				k = slot.yDisplayPosition;            
            
				GL11.glColorMask(true, true, true, false);
        
				this.drawGradientRect(j, k, j + 16, k + 16, - 2130706433, - 2130706433);
        	
				GL11.glColorMask(true, true, true, true);
        
				GL11.glEnable(GL11.GL_LIGHTING);
				GL11.glEnable(GL11.GL_DEPTH_TEST);
			}
		}
	}

    protected void drawItemStack(ItemStack itemStack, int x, int y, String displayString) {
    	
        GL11.glTranslatef(0.0F, 0.0F, 32.0F);
        this.zLevel = 200.0F;
        itemRender.zLevel = 200.0F;
        FontRenderer font = null;
        if (itemStack != null) font = itemStack.getItem().getFontRenderer(itemStack);
        if (font == null) font = fontRendererObj;
        itemRender.renderItemAndEffectIntoGUI(font, this.mc.getTextureManager(), itemStack, x, y);
        itemRender.renderItemOverlayIntoGUI(font, this.mc.getTextureManager(), itemStack, x, y - (this.draggedStack == null ? 0 : 8), displayString);
        this.zLevel = 0.0F;
        itemRender.zLevel = 0.0F;
    }

    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {}

    protected void drawGuiContainerBackgroundLayer(float renderPartialTicks, int mouseX, int mouseY) {}
    
    //TODO handleMouseInput()
    public void handleMouseInput() {
    	
    	super.handleMouseInput();
    	
    	if (Mouse.getDWheel() != 0) {
    		    		
    		for (GUIFramework framework : this.frameworks) {

    			if (framework.getScroller() != null) {   
    					
    	    		if (framework.isHovered() || framework.getScroller().shouldIgnoreBorders()) {
    				    					
    	    			if (framework.getScroller().handleScroller()) {
        					
    	    				this.scrollSlots(framework);
        	    			
    	    				if (framework.getSlider() != null) {
        							
    	    					framework.getSlider().handleSlidebarPosition();
    	    				}	
    	    			}
    				}
    			}
    		}
    	}
    	
    	if (!Mouse.isButtonDown(0)) {
    			
    	  	for (GUIFramework framework : this.frameworks) {
    	  			
				if (framework.getSlider() != null) {
					
					if (framework.getSlider().isSlidebarDragged()) {
    	  			
						framework.getSlider().setSlidebarNotDragged(); 	  		
    	  			}
				}
    		}
    	}
    }
    
    //TODO scrollSlots()
    protected void scrollSlots(GUIFramework framework) {
    	
		int i = 0, size, k;
		
		boolean scrollingSearch = !framework.searchSlots.isEmpty();
    	
    	Slot slot, slotCopy;
    	    	
    	framework.slots.clear();
    	framework.slotsIndexes.clear();
    	
    	for (i = framework.getScroller().getPosition() * framework.columns; i < framework.getScroller().getPosition() * framework.columns + framework.visibleSlots; i++) {
    		
    		if ((!scrollingSearch && i < framework.slotsBuffer.size()) || (scrollingSearch && i < framework.searchSlots.size())) {
    			
    			slot = scrollingSearch ? framework.searchSlots.get(i) : framework.slotsBuffer.get(i);  
    			
            	slotCopy = this.copySlot(slot); 
            	            	
    			size = framework.slots.size();
    			    			       		
				if (framework.slotsPosition == GUIPosition.CUSTOM) {
					
					k = size / framework.columns;
					
					slotCopy.xDisplayPosition = framework.getXPosition() + size * (framework.getSlotWidth() + framework.getSlotDistanceHorizontal()) - k * ((framework.getSlotWidth() + framework.getSlotDistanceHorizontal()) * framework.columns);
					slotCopy.yDisplayPosition = framework.getYPosition() + k * (framework.getSlotHeight() + framework.getSlotDistanceVertical()) - (size / framework.visibleSlots) * (framework.rows * (framework.getSlotHeight() + framework.getSlotDistanceVertical()));
				}
				
    			framework.slots.add(slotCopy);
    			framework.slotsIndexes.add(scrollingSearch ? framework.searchIndexes.get(i) : framework.indexesBuffer.get(i));
    		}
    	}
    }
       
    private void func_146980_g() {
    	
        ItemStack itemstack = this.mc.thePlayer.inventory.getItemStack();

        if (itemstack != null && this.dragSplitting) {
        	
            this.dragSplittingRemnant = itemstack.stackSize;
            ItemStack itemstack1;
            int i;

            for (Iterator iterator = this.dragSplittingSlots.iterator(); iterator.hasNext(); this.dragSplittingRemnant -= itemstack1.stackSize - i) {
            	
                Slot slot = (Slot)iterator.next();
                
                itemstack1 = itemstack.copy();
                i = slot.getStack() == null ? 0 : slot.getStack().stackSize;
                
                Container.func_94525_a(this.dragSplittingSlots, this.dragSplittingLimit, itemstack1, i);

                if (itemstack1.stackSize > itemstack1.getMaxStackSize()) {
                	
                    itemstack1.stackSize = itemstack1.getMaxStackSize();
                }

                if (itemstack1.stackSize > slot.getSlotStackLimit()) {
                	
                    itemstack1.stackSize = slot.getSlotStackLimit();
                }
            }
        }
    }

    //TODO getSlotAtPosition()
    protected Slot getSlotAtPosition(int mouseX, int mouseY, boolean flag) {
    	
    	int i;
    	
    	Slot slot;	
    	
    	for (GUIFramework framework : this.frameworks) {
    		
    		if (!this.menuOpened || flag) {
    			
        		for (i = 0; i < framework.visibleSlots; i++) {        	
    				
        			if (i < framework.slots.size()) {
        				
        				slot = framework.slots.get(i);    
    				
        				if (this.isMouseOverSlot(slot, mouseX, mouseY, framework)) {
    					
        					if (flag) {
        						
        						this.righClickedFramework = this.frameworks.indexOf(framework);
        					}
    		        	
        					return (Slot) this.mainContainer.inventorySlots.get(framework.slotsIndexes.get(i));
        				} 
        			}
    			}
    		}
    	}
    	
        return null;
    }
    
    //TODO handleRightClick()
    protected boolean handleRightClick(int mouseX, int mouseY) {
    	
    	Slot slot = this.getSlotAtPosition(mouseX, mouseY, true);
			
		if (this.dragged != null) {
    		
			return false;
		}
    	    	
    	if (slot != null && slot.getHasStack()) {

    		GUIFramework righClickedFramework = this.frameworks.get(this.righClickedFramework);
    		
    		if (righClickedFramework.getContextMenu() != null) {
    			
    			if (!this.menuOpened) {
    				
    				righClickedFramework.getContextMenu().open(righClickedFramework.inventory, mouseX, mouseY, this.guiLeft, this.guiTop, slot);    				
    			}  
    			
    			else {
    				
    				this.frameworks.get(this.openedMenuFramework).getContextMenu().close();
    				
    				righClickedFramework.getContextMenu().open(righClickedFramework.inventory, mouseX, mouseY, this.guiLeft, this.guiTop, slot);   
    			}
    			    			
				this.openedMenuFramework = this.righClickedFramework;
				
    			this.menuOpened = true;
    			
 				return true;
    		}  
    	}   
    	
    	return false;
    }

    //TODO mouseClicked()
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    	
        super.mouseClicked(mouseX, mouseY, mouseButton);
        
        boolean flag = mouseButton == this.mc.gameSettings.keyBindPickBlock.getKeyCode() + 100;
        
        Slot slot = this.getSlotAtPosition(mouseX, mouseY, false);
        
        long l = Minecraft.getSystemTime();
        
        this.doubleClick = this.lastClickSlot == slot && l - this.lastClickTime < 250L && this.lastClickButton == mouseButton;
        this.ignoreMouseUp = false;
        
        int rightButton = 0;
        
        if (mouseButton == 1) {
        	
        	if (!this.handleRightClick(mouseX, mouseY)) {
        		
        		rightButton = 1;
        	}
        }

        if (mouseButton == 0 || mouseButton == rightButton || flag) {
        	
            int i1 = this.guiLeft;
            int j1 = this.guiTop;
            
            boolean flag1 = mouseX < i1 || mouseY < j1 || mouseX >= i1 + this.xSize || mouseY >= j1 + this.ySize;
            
            int k1 = - 1;

            if (slot != null) {
            	
                k1 = slot.slotNumber;
            }

            if (flag1) {
            	
                k1 = - 999;
            }

            if (this.mc.gameSettings.touchscreen && flag1 && this.mc.thePlayer.inventory.getItemStack() == null) {
            	
                this.mc.displayGuiScreen((GuiScreen) null);
                return;
            }

            if (k1 != - 1) {
            	
                if (this.mc.gameSettings.touchscreen) {
                	
                    if (slot != null && slot.getHasStack()) {
                    	
                        this.clickedSlot = slot;
                        this.draggedStack = null;
                        this.isRightMouseClick = mouseButton == 1;
                    }
                    
                    else {
                    	
                        this.clickedSlot = null;
                    }
                }
                
                else if (!this.dragSplitting) {
                	
                    if (this.mc.thePlayer.inventory.getItemStack() == null) {
                    	
                        if (mouseButton == this.mc.gameSettings.keyBindPickBlock.getKeyCode() + 100) {
                        	
                            this.handleMouseClick(slot, k1, mouseButton, 3);
                        }
                        
                        else {
                        	
                            boolean flag2 = k1 != - 999 && (Keyboard.isKeyDown(42) || Keyboard.isKeyDown(54));
                            
                            byte b0 = 0;

                            if (flag2) {
                            	
                                this.shiftClickedSlot = slot != null && slot.getHasStack() ? slot.getStack() : null;
                                b0 = 1;
                            }
                            
                            else if (k1 == - 999) {
                            	
                                b0 = 4;
                            }

                            this.handleMouseClick(slot, k1, mouseButton, b0);
                        }

                        this.ignoreMouseUp = true;
                    }
                    
                    else {
                    	
                        this.dragSplitting = true;
                        this.dragSplittingButton = mouseButton;
                        this.dragSplittingSlots.clear();

                        if (mouseButton == 0) {
                        	
                            this.dragSplittingLimit = 0;
                        }
                        
                        else if (mouseButton == 1) {
                        	
                            this.dragSplittingLimit = 1;
                        }
                    }
                }
            }
        }

        this.lastClickSlot = slot;
        this.lastClickTime = l;
        this.lastClickButton = mouseButton;
        
        if (mouseButton == 0) {
        	
        	for (GUIFramework framework : this.frameworks) {
        	      	
        		if (framework.getSearchField() != null) {       	
        		
        			framework.getSearchField().mouseClicked(mouseX, mouseY, this.guiLeft, this.guiTop);
                }        
            	
            	if (framework.getContextMenu() != null && !framework.getContextMenu().mousePressed(framework.inventory, mouseX, mouseY)) {
            		
            		framework.getContextMenu().close();
            		
            		this.menuOpened = false;
            	}
            	
            	for (GUIButton button : framework.getButtonsList()) {
            		
            		if (button.mouseClicked()) this.handleButtonPress(framework, button);
            	}
            	
            	for (GUIButtonPanel buttonPanel : framework.getButtonPanelsList()) {
            		
            		for (GUIButton button : buttonPanel.buttons) {
            			
                		if (button.mouseClicked()) this.handleButtonPress(framework, button);
            		}
            	}
        	}
        }
    }
    
    /**
     * Метод для управления активностью кнопок.
     * 
     * @param framework фреймворк, к которому относится активированная кнопка.
     * @param button кнопка, которая была активирована.
     */
    protected abstract void handleButtonPress(GUIFramework framework, GUIButton button);

    protected void mouseClickMove(int mouseX, int mouseY, int lastButton, long timeSinceLastClick) {
    	
        Slot slot = this.getSlotAtPosition(mouseX, mouseY, false);
        ItemStack itemstack = this.mc.thePlayer.inventory.getItemStack();

        if (this.clickedSlot != null && this.mc.gameSettings.touchscreen) {
        	
            if (lastButton == 0 || lastButton == 1) {
            	
                if (this.draggedStack == null) {
                	
                    if (slot != this.clickedSlot) {
                    	
                        this.draggedStack = this.clickedSlot.getStack().copy();
                    }
                }
                
                else if (this.draggedStack.stackSize > 1 && slot != null && Container.func_94527_a(slot, this.draggedStack, false)) {
                	
                    long i1 = Minecraft.getSystemTime();

                    if (this.currentDragTargetSlot == slot) {
                    	
                        if (i1 - this.dragItemDropDelay > 500L) {
                        	
                            this.handleMouseClick(this.clickedSlot, this.clickedSlot.slotNumber, 0, 0);
                            this.handleMouseClick(slot, slot.slotNumber, 1, 0);
                            this.handleMouseClick(this.clickedSlot, this.clickedSlot.slotNumber, 0, 0);
                            this.dragItemDropDelay = i1 + 750L;
                            --this.draggedStack.stackSize;
                        }
                    }
                    
                    else {
                    	
                        this.currentDragTargetSlot = slot;
                        this.dragItemDropDelay = i1;
                    }
                }
            }
        }
        
        else if (this.dragSplitting && slot != null && itemstack != null && itemstack.stackSize > this.dragSplittingSlots.size() && Container.func_94527_a(slot, itemstack, true) && slot.isItemValid(itemstack) && this.mainContainer.canDragIntoSlot(slot)) {
            
        	this.dragSplittingSlots.add(slot);
            this.func_146980_g();
        }
    }

    protected void mouseMovedOrUp(int mouseX, int mouseY, int mouseState) {
    	
        super.mouseMovedOrUp(mouseX, mouseY, mouseState); //Forge, Call parent to release buttons
        
        Slot slot = this.getSlotAtPosition(mouseX, mouseY, false);
        
        int l = this.guiLeft;
        int i1 = this.guiTop;
        
        boolean flag = mouseX < l || mouseY < i1 || mouseX >= l + this.xSize || mouseY >= i1 + this.ySize;
        
        int j1 = - 1;

        if (slot != null) {
        	
            j1 = slot.slotNumber;
        }

        if (flag) {
        	
            j1 = - 999;
        }

        Slot slot1;
        Iterator iterator;

        if (this.doubleClick && slot != null && mouseState == 0 && this.mainContainer.func_94530_a((ItemStack)null, slot)) {
        	
            if (isShiftKeyDown()) {
            	
                if (slot != null && slot.inventory != null && this.shiftClickedSlot != null) {
                	
                    iterator = this.mainContainer.inventorySlots.iterator();

                    while (iterator.hasNext()) {
                    	
                        slot1 = (Slot)iterator.next();

                        if (slot1 != null && slot1.canTakeStack(this.mc.thePlayer) && slot1.getHasStack() && slot1.inventory == slot.inventory && Container.func_94527_a(slot1, this.shiftClickedSlot, true)) {
                        	
                            this.handleMouseClick(slot1, slot1.slotNumber, mouseState, 1);
                        }
                    }
                }
            }
            
            else {
            	
                this.handleMouseClick(slot, j1, mouseState, 6);
            }

            this.doubleClick = false;
            this.lastClickTime = 0L;
        }
        
        else {
        	
            if (this.dragSplitting && this.dragSplittingButton != mouseState) {
            	
                this.dragSplitting = false;
                this.dragSplittingSlots.clear();
                this.ignoreMouseUp = true;
                return;
            }

            if (this.ignoreMouseUp) {
            	
                this.ignoreMouseUp = false;
                return;
            }

            boolean flag1;

            if (this.clickedSlot != null && this.mc.gameSettings.touchscreen) {
            	
                if (mouseState == 0 || mouseState == 1) {
                	
                    if (this.draggedStack == null && slot != this.clickedSlot) {
                    	
                        this.draggedStack = this.clickedSlot.getStack();
                    }

                    flag1 = Container.func_94527_a(slot, this.draggedStack, false);

                    if (j1 != - 1 && this.draggedStack != null && flag1) {
                    	
                        this.handleMouseClick(this.clickedSlot, this.clickedSlot.slotNumber, mouseState, 0);
                        this.handleMouseClick(slot, j1, 0, 0);

                        if (this.mc.thePlayer.inventory.getItemStack() != null) {
                        	
                            this.handleMouseClick(this.clickedSlot, this.clickedSlot.slotNumber, mouseState, 0);
                            this.touchUpX = mouseX - l;
                            this.touchUpY = mouseY - i1;
                            this.returningStackDestSlot = this.clickedSlot;
                            this.returningStack = this.draggedStack;
                            this.returningStackTime = Minecraft.getSystemTime();
                        }
                        
                        else {
                        	
                            this.returningStack = null;
                        }
                    }
                    
                    else if (this.draggedStack != null) {
                    	
                        this.touchUpX = mouseX - l;
                        this.touchUpY = mouseY - i1;
                        this.returningStackDestSlot = this.clickedSlot;
                        this.returningStack = this.draggedStack;
                        this.returningStackTime = Minecraft.getSystemTime();
                    }

                    this.draggedStack = null;
                    this.clickedSlot = null;
                }
            }
            
            else if (this.dragSplitting && !this.dragSplittingSlots.isEmpty()) {
            	
                this.handleMouseClick((Slot)null, - 999, Container.func_94534_d(0, this.dragSplittingLimit), 5);
                iterator = this.dragSplittingSlots.iterator();

                while (iterator.hasNext()) {
                	
                    slot1 = (Slot)iterator.next();
                    this.handleMouseClick(slot1, slot1.slotNumber, Container.func_94534_d(1, this.dragSplittingLimit), 5);
                }

                this.handleMouseClick((Slot)null, - 999, Container.func_94534_d(2, this.dragSplittingLimit), 5);
            }
            
            else if (this.mc.thePlayer.inventory.getItemStack() != null) {
            	
                if (mouseState == this.mc.gameSettings.keyBindPickBlock.getKeyCode() + 100) {
                	
                    this.handleMouseClick(slot, j1, mouseState, 3);
                }
                
                else {
                	
                    flag1 = j1 != - 999 && (Keyboard.isKeyDown(42) || Keyboard.isKeyDown(54));

                    if (flag1) {
                    	
                        this.shiftClickedSlot = slot != null && slot.getHasStack() ? slot.getStack() : null;
                    }

                    this.handleMouseClick(slot, j1, mouseState, flag1 ? 1 : 0);
                }
            }
        }

        if (this.mc.thePlayer.inventory.getItemStack() == null) {
        	
            this.lastClickTime = 0L;
        }

        this.dragSplitting = false;
    }

    protected boolean isMouseOverSlot(Slot slot, int mouseX, int mouseY, GUIFramework framework) {
    	
        return this.isHovered(slot.xDisplayPosition, slot.yDisplayPosition, framework.getSlotWidth(), framework.getSlotHeight(), mouseX, mouseY);
    }

    protected boolean isHovered(int xSlotPosition, int ySlotPosition, int hoverWidth, int howerHeight, int mouseX, int mouseY) {
    	
        int guiLeft = this.guiLeft;
        int guiTop = this.guiTop;
        
        mouseX -= guiLeft;
        mouseY -= guiTop;
        
        return mouseX >= xSlotPosition - 1 && mouseX < xSlotPosition + hoverWidth + 1 && mouseY >= ySlotPosition - 1 && mouseY < ySlotPosition + howerHeight + 1;
    }

    protected void handleMouseClick(Slot slot, int mouseX, int mouseY, int button) {
    	
        if (slot != null) {
        	
            mouseX = slot.slotNumber;
        }

        this.mc.playerController.windowClick(this.mainContainer.windowId, mouseX, mouseY, button, this.mc.thePlayer);
    }

    //TODO keyTyped()
    protected void keyTyped(char keyChar, int keyCode) {
    	
        if (keyCode == 1) {
        	
            this.mc.thePlayer.closeScreen();
        }

        this.checkHotbarKeys(keyCode);

        if (this.theSlot != null && this.theSlot.getHasStack()) {
        	
            if (keyCode == this.mc.gameSettings.keyBindPickBlock.getKeyCode()) {
            	
                this.handleMouseClick(this.theSlot, this.theSlot.slotNumber, 0, 3);
            }
            
            else if (keyCode == this.mc.gameSettings.keyBindDrop.getKeyCode()) {
            	
                this.handleMouseClick(this.theSlot, this.theSlot.slotNumber, isCtrlKeyDown() ? 1 : 0, 4);
            }
        }
        
        for (GUIFramework framework : this.frameworks) {     
        	
        	if (framework.getSearchField() != null) {       	      
        		
            	if (framework.getSearchField().keyTyped(keyChar, keyCode)) {
            		
            		if (framework.getSearchField().getTypedText().length() > 0) {
        			
            			this.updateSearchResult(framework);
            		}
        		
            		else {
            			
            			framework.searchSlots.clear();
            			framework.searchIndexes.clear();
        			
            			this.updateFramework(framework, framework.getCurrentSorter());
            		}
        		
            		if (framework.getSlider() != null) {
            			
            			framework.getSlider().reset();
            		}
            	}
        	}
        }
    }
    
    //TODO updateSearchResult()
    protected void updateSearchResult(GUIFramework framework) {
    	
        int slotIndex, size, k;
        
        Slot slotCopy;
        
        ItemStack itemStack;
        
        String itemName;
        
    	String typedText = framework.getSearchField().getTypedText();
        
    	Iterator itemsIterator = framework.items.keySet().iterator();
    	
    	framework.slots.clear();
    	framework.slotsIndexes.clear();   
    	
    	framework.searchSlots.clear();
    	framework.searchIndexes.clear();   
        
        while (itemsIterator.hasNext()) {
        	
        	slotIndex = (Integer) itemsIterator.next();
        	
        	itemStack = framework.items.get(slotIndex);
        	
        	if (itemStack != null) {
        	        	
        		itemName = itemStack.getDisplayName().toLowerCase();
        	
        		if (itemName.startsWith(typedText) || itemName.contains(" " + typedText)) {
        		        					        			
        			size = framework.searchSlots.size();
        					
        			slotCopy = this.copySlot(framework.slotsBuffer.get(slotIndex));
        					        					
					if (framework.slotsPosition == GUIPosition.CUSTOM) {
						
						k = size / framework.columns;
						
						slotCopy.xDisplayPosition = framework.getXPosition() + size * (framework.getSlotWidth() + framework.getSlotDistanceHorizontal()) - k * ((framework.getSlotWidth() + framework.getSlotDistanceHorizontal()) * framework.columns);
						slotCopy.yDisplayPosition = framework.getYPosition() + k * (framework.getSlotHeight() + framework.getSlotDistanceVertical()) - (size / framework.visibleSlots) * (framework.rows * (framework.getSlotHeight() + framework.getSlotDistanceVertical()));
					}
					
        			framework.slots.add(slotCopy);
        			framework.slotsIndexes.add(framework.indexesBuffer.get(slotIndex));  
					
        			framework.searchSlots.add(slotCopy);
        			framework.searchIndexes.add(framework.indexesBuffer.get(slotIndex));  
        		}
        	}
        }
    }
    
    protected boolean checkHotbarKeys(int index) {
    	
        if (this.mc.thePlayer.inventory.getItemStack() == null && this.theSlot != null) {
        	
            for (int j = 0; j < 9; ++j) {
            	
                if (index == this.mc.gameSettings.keyBindsHotbar[j].getKeyCode()) {
                	
                    this.handleMouseClick(this.theSlot, this.theSlot.slotNumber, j, 2);
                    return true;
                }
            }
        }

        return false;
    }

    public void onGuiClosed() {
    	
        if (this.mc.thePlayer != null) {
        	
            this.mainContainer.onContainerClosed(this.mc.thePlayer);
        }
    }

    public boolean doesGuiPauseGame() {
    	
        return false;
    }

    //TODO updateScreen()
    public void updateScreen() {
    	  	   	
        super.updateScreen();

        if (!this.mc.thePlayer.isEntityAlive() || this.mc.thePlayer.isDead) {
        	
            this.mc.thePlayer.closeScreen();
        }
	    
	    for (GUIFramework framework : this.frameworks) {
	    	
	    	if (framework.getSearchField() != null) {
	    		
	    		framework.getSearchField().updateCursorCounter();
	    	}
	    }	    
    }
}
