package com.ivanc.smartmovingarmorcompat;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderPlayerEvent;
import org.lwjgl.opengl.GL11;
import tombenpotter.icarus.ConfigHandler;
import tombenpotter.icarus.common.items.ItemWing;

public class IcarusWingTransformHandler {
    private static final String ORIGINAL_HANDLER = "tombenpotter.icarus.client.ClientEventHandler";
    private static boolean originalHandlersRemoved;

    public static void unregisterOriginalHandlers() {
        if (originalHandlersRemoved) {
            return;
        }

        originalHandlersRemoved = ForgeEventBusUtil.unregisterHandlersByClassName(ORIGINAL_HANDLER);
    }

    @SubscribeEvent
    public void renderPlayerWings(RenderPlayerEvent.Specials.Post event) {
        unregisterOriginalHandlers();

        if (!SmartRenderTransformHelper.isRealClientPlayer(event.entityPlayer)) {
            return;
        }

        ItemStack stack = event.entityPlayer.inventory.armorInventory[2];
        if (stack == null || !(stack.getItem() instanceof ItemWing)) {
            return;
        }

        ModelBiped source = SmartRenderTransformHelper.getActiveSmartModel(
            SmartRenderTransformHelper.getSmartModel(event.renderer));
        SmartRenderTransformHelper.Transform transform = source == null
            ? null
            : SmartRenderTransformHelper.begin(source, SmartRenderTransformHelper.BODY_PART);

        try {
            renderWings(event.entityPlayer, stack, (ItemWing) stack.getItem(), event.partialRenderTick);
        } finally {
            if (transform != null) {
                transform.end();
            }
        }
    }

    private static void renderWings(EntityPlayer player, ItemStack stack, ItemWing itemWing, float partialTicks) {
        Minecraft minecraft = Minecraft.getMinecraft();
        float renderTime = 2.0F * (float) player.ticksExisted + partialTicks;
        float flap = getFlap(player, renderTime);

        GL11.glPushMatrix();
        try {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            minecraft.getTextureManager().bindTexture(
                new ResourceLocation("icarus:textures/items/wings/" + itemWing.getWing(stack).name + ".png"));

            if (stack.hasTagCompound() && stack.getTagCompound().hasKey("Icarus_ItemStack")) {
                GL11.glTranslatef(0.0F, -0.3125F, 0.225F);
            } else {
                GL11.glTranslatef(0.0F, -0.3125F, 0.125F);
            }

            renderWing(flap, false);
            renderWing(flap, true);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private static float getFlap(EntityPlayer player, float renderTime) {
        if (player.onGround) {
            return 0.0F;
        }
        if (player.motionY < 0.0D) {
            return player.isSneaking() == ConfigHandler.holdKeyToHover
                ? (1.0F + (float) Math.cos(renderTime / 2.0F)) * 13.0F
                : 0.0F;
        }
        return (1.0F + (float) Math.cos(renderTime / 4.0F)) * 13.0F;
    }

    private static void renderWing(float flap, boolean left) {
        float side = left ? -1.0F : 1.0F;
        Tessellator tessellator = Tessellator.instance;

        GL11.glPushMatrix();
        try {
            GL11.glRotatef(side * (-20.0F - flap), 0.0F, 1.0F, 0.0F);
            tessellator.startDrawingQuads();
            tessellator.addVertexWithUV(0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
            tessellator.addVertexWithUV(0.0D, 1.0D, 0.0D, 0.0D, 1.0D);
            tessellator.addVertexWithUV(side, 1.0D, 0.0D, 1.0D, 1.0D);
            tessellator.addVertexWithUV(side, 0.0D, 0.0D, 1.0D, 0.0D);
            tessellator.draw();
        } finally {
            GL11.glPopMatrix();
        }
    }
}
