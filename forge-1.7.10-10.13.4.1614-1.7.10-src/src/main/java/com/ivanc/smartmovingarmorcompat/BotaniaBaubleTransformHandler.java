package com.ivanc.smartmovingarmorcompat;

import baubles.common.container.InventoryBaubles;
import baubles.common.lib.PlayerHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.awt.Color;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.event.RenderPlayerEvent;
import org.lwjgl.opengl.GL11;
import vazkii.botania.api.item.IBaubleRender;
import vazkii.botania.api.item.ICosmeticAttachable;
import vazkii.botania.api.item.IPhantomInkable;
import vazkii.botania.client.core.handler.ContributorFancinessHandler;
import vazkii.botania.common.core.handler.ConfigHandler;
import vazkii.botania.common.item.ModItems;

public class BotaniaBaubleTransformHandler {
    private static final String ORIGINAL_HANDLER = "vazkii.botania.client.core.handler.BaubleRenderHandler";
    private static boolean originalHandlersRemoved;

    public static void unregisterOriginalHandlers() {
        if (originalHandlersRemoved) {
            return;
        }

        originalHandlersRemoved = ForgeEventBusUtil.unregisterHandlersByClassName(ORIGINAL_HANDLER);
    }

    @SubscribeEvent
    public void onPlayerRender(RenderPlayerEvent.Specials.Post event) {
        unregisterOriginalHandlers();

        if (!SmartRenderTransformHelper.isRealClientPlayer(event.entityPlayer)) {
            return;
        }

        if (!ConfigHandler.renderBaubles || event.entityLiving.getActivePotionEffect(Potion.invisibility) != null) {
            return;
        }

        ModelBiped source = SmartRenderTransformHelper.getSmartModel(event.renderer);
        if (source == null) {
            return;
        }

        EntityPlayer player = event.entityPlayer;
        InventoryBaubles inv = PlayerHandler.getPlayerBaubles(player);
        renderBodyBaubles(source, inv, event);
        renderHeadBaubles(source, player, inv, event);
    }

    private static void renderBodyBaubles(ModelBiped source, InventoryBaubles inv, RenderPlayerEvent.Specials.Post event) {
        SmartRenderTransformHelper.Transform transform =
            SmartRenderTransformHelper.begin(source, SmartRenderTransformHelper.BODY_PART);
        if (transform == null) {
            dispatchRenders(inv, event, IBaubleRender.RenderType.BODY);
            renderManaTablet(inv, event);
            return;
        }

        try {
            GL11.glPushMatrix();
            try {
                if (event.entityPlayer.isSneaking()) {
                    GL11.glRotatef(-28.64789F, 1.0F, 0.0F, 0.0F);
                }
                dispatchRenders(inv, event, IBaubleRender.RenderType.BODY);
                renderManaTablet(inv, event);
            } finally {
                GL11.glPopMatrix();
            }
        } finally {
            transform.end();
        }
    }

    private static void renderHeadBaubles(ModelBiped source, EntityPlayer player, InventoryBaubles inv, RenderPlayerEvent.Specials.Post event) {
        SmartRenderTransformHelper.Transform transform =
            SmartRenderTransformHelper.begin(source, SmartRenderTransformHelper.HEAD_PART);
        if (transform == null) {
            dispatchRenders(inv, event, IBaubleRender.RenderType.HEAD);
            renderTerrasteelAndContributorHead(player, event);
            return;
        }

        try {
            GL11.glPushMatrix();
            try {
                GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
                dispatchRenders(inv, event, IBaubleRender.RenderType.HEAD);
                renderTerrasteelAndContributorHead(player, event);
            } finally {
                GL11.glPopMatrix();
            }
        } finally {
            transform.end();
        }
    }

    private static void renderTerrasteelAndContributorHead(EntityPlayer player, RenderPlayerEvent.Specials.Post event) {
        ItemStack helm = player.inventory.armorItemInSlot(3);
        if (isTerrasteelHelm(helm)) {
            renderTerrasteelHelm(helm, event);
        }
        ContributorFancinessHandler.render(event);
    }

    private static void dispatchRenders(InventoryBaubles inv, RenderPlayerEvent event, IBaubleRender.RenderType type) {
        for (int i = 0; i < inv.func_70302_i_(); i++) {
            ICosmeticAttachable attachable;
            ItemStack cosmetic;
            IPhantomInkable inkable;
            ItemStack stack = inv.func_70301_a(i);
            if (stack == null) {
                continue;
            }

            Item item = stack.getItem();
            if (item instanceof IPhantomInkable && (inkable = (IPhantomInkable) item).hasPhantomInk(stack)) {
                continue;
            }

            if (item instanceof ICosmeticAttachable && (cosmetic = (attachable = (ICosmeticAttachable) item).getCosmeticItem(stack)) != null) {
                renderBauble(cosmetic, event, type);
            } else if (item instanceof IBaubleRender) {
                renderBauble(stack, event, type);
            }
        }
    }

    private static void renderBauble(ItemStack stack, RenderPlayerEvent event, IBaubleRender.RenderType type) {
        GL11.glPushMatrix();
        try {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            ((IBaubleRender) stack.getItem()).onPlayerBaubleRender(stack, event, type);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private static void renderManaTablet(InventoryBaubles inv, RenderPlayerEvent event) {
        if (inv.func_70301_a(3) == null) {
            return;
        }

        EntityPlayer player = event.entityPlayer;
        boolean renderedOne = false;
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (stack == null || stack.getItem() != ModItems.manaTablet) {
                continue;
            }

            Item item = stack.getItem();
            GL11.glPushMatrix();
            try {
                Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.locationItemsTexture);
                IBaubleRender.Helper.rotateIfSneaking(event.entityPlayer);
                boolean armor = event.entityPlayer.getCurrentArmor(1) != null;
                GL11.glRotatef(180.0F, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
                GL11.glTranslatef(-0.25F, -0.85F, renderedOne ? (armor ? 0.2F : 0.28F) : (armor ? -0.3F : -0.25F));
                GL11.glScalef(0.5F, 0.5F, 0.5F);
                GL11.glColor3f(1.0F, 1.0F, 1.0F);
                int light = 0xF000F0;
                int lightmapX = light % 65536;
                int lightmapY = light / 65536;
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightmapX, lightmapY);
                for (int j = 0; j < 2; j++) {
                    IIcon icon = item.getIcon(stack, j);
                    ItemRenderer.renderItemIn2D(
                        Tessellator.instance,
                        icon.getMaxU(),
                        icon.getMinV(),
                        icon.getMinU(),
                        icon.getMaxV(),
                        icon.getIconWidth(),
                        icon.getIconHeight(),
                        0.0625F);
                    Color color = new Color(item.getColorFromItemStack(stack, 1));
                    GL11.glColor3ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue());
                }
            } finally {
                GL11.glPopMatrix();
            }

            if (renderedOne) {
                return;
            }
            renderedOne = true;
        }
    }

    private static boolean isTerrasteelHelm(ItemStack stack) {
        if (stack == null) {
            return false;
        }

        try {
            Class<?> type = Class.forName("vazkii.botania.common.item.equipment.armor.terrasteel.ItemTerrasteelHelm");
            return type.isInstance(stack.getItem());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void renderTerrasteelHelm(ItemStack helm, RenderPlayerEvent.Specials.Post event) {
        try {
            Class<?> type = Class.forName("vazkii.botania.common.item.equipment.armor.terrasteel.ItemTerrasteelHelm");
            Method method = type.getMethod("renderOnPlayer", ItemStack.class, RenderPlayerEvent.class);
            method.invoke(null, helm, event);
        } catch (Throwable ignored) {
        }
    }
}
