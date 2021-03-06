package ru.austeretony.advancedgui.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.client.renderer.Tessellator;

public class AdvancedGUIUtils {

	public static final Logger LOGGER = LogManager.getLogger();

	private AdvancedGUIUtils() {}
	
    public static void drawCustomSizedTexturedRect(int x, int y, int u, int v, int width, int height, int textureWidth, int textureHeight) {
    	
        float f = 1.0F / (float) textureWidth;
        float f1 = 1.0F / (float) textureHeight;
        
        Tessellator tessellator = Tessellator.instance;
        
        tessellator.startDrawingQuads();
        
        tessellator.addVertexWithUV((double) (x), (double) (y + height), 0, (double) ((float) (u) * f), (double) ((float) (v + height) * f1));
        tessellator.addVertexWithUV((double) (x + width), (double) (y + height), 0, (double) ((float) (u + width) * f), (double) ((float) (v + height) * f1));
        tessellator.addVertexWithUV((double) (x + width), (double) (y), 0, (double) ((float) (u + width) * f), (double) ((float) (v) * f1));
        tessellator.addVertexWithUV((double) (x), (double) (y), 0, (double) ((float) (u) * f), (double) ((float) (v) * f1));
        
        tessellator.draw();
    }
}
