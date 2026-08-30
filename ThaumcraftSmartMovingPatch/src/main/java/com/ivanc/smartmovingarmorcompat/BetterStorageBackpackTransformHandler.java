package com.ivanc.smartmovingarmorcompat;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.lang.reflect.Method;
import net.mcft.copy.betterstorage.client.model.ModelBackpackArmor;
import net.mcft.copy.betterstorage.item.ItemBackpack;
import net.mcft.copy.betterstorage.misc.PropertiesBackpack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderPlayerEvent;
import org.lwjgl.opengl.GL11;

public class BetterStorageBackpackTransformHandler {
    private static final String ORIGINAL_HANDLER = "net.mcft.copy.betterstorage.proxy.ClientProxy";
    private static final String ORIGINAL_METHOD = "onRenderPlayerSpecialsPre";
    private static final ResourceLocation ENCHANTED_EFFECT =
        new ResourceLocation("textures/misc/enchanted_item_glint.png");

    private static boolean originalHandlerRemoved;
    private static boolean swingLookupDone;
    private static Method renderSwingProgressMethod;

    public static void unregisterOriginalHandlers() {
        if (originalHandlerRemoved) {
            return;
        }

        originalHandlerRemoved =
            ForgeEventBusUtil.unregisterHandlerMethodByClassName(ORIGINAL_HANDLER, ORIGINAL_METHOD);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderPlayerSpecialsPre(RenderPlayerEvent.Specials.Pre event) {
        unregisterOriginalHandlers();

        EntityPlayer player = event.entityPlayer;
        if (!SmartRenderTransformHelper.isRealClientPlayer(player)) {
            return;
        }

        ItemStack backpack = getStoredBackpack(player);
        ItemStack anyBackpack = backpack == null ? getAnyBackpack(player) : backpack;
        if (anyBackpack != null) {
            event.renderCape = false;
        }

        if (backpack == null || !(backpack.getItem() instanceof ItemBackpack)) {
            return;
        }

        ModelBiped source = SmartRenderTransformHelper.getSmartModel(event.renderer);
        if (source == null) {
            return;
        }

        ItemBackpack backpackType = (ItemBackpack) backpack.getItem();
        ModelBiped armorModel = backpackType.getArmorModel(player, backpack, 0);
        if (!(armorModel instanceof ModelBackpackArmor)) {
            return;
        }

        ModelBackpackArmor model = (ModelBackpackArmor) armorModel;
        model.onGround = getRenderSwingProgress(event.renderer, player, event.partialRenderTick);
        model.setLivingAnimations(player, 0.0F, 0.0F, event.partialRenderTick);

        SmartRenderTransformHelper.Transform transform =
            SmartRenderTransformHelper.begin(source, SmartRenderTransformHelper.BODY_PART);
        if (transform == null) {
            renderOriginalStyle(backpackType, backpack, model, player, event.partialRenderTick);
            return;
        }

        try {
            renderBodyAttached(backpackType, backpack, model, player, event.partialRenderTick);
        } finally {
            transform.end();
        }
    }

    private static ItemStack getStoredBackpack(EntityPlayer player) {
        try {
            PropertiesBackpack data = ItemBackpack.getBackpackData(player);
            return data == null ? null : data.backpack;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static ItemStack getAnyBackpack(EntityPlayer player) {
        try {
            return ItemBackpack.getBackpack(player);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void renderBodyAttached(
        ItemBackpack backpackType,
        ItemStack backpack,
        ModelBackpackArmor model,
        EntityPlayer player,
        float partialTicks) {
        GL11.glPushMatrix();
        try {
            renderLayers(backpackType, backpack, model, player, partialTicks, true);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private static void renderOriginalStyle(
        ItemBackpack backpackType,
        ItemStack backpack,
        ModelBackpackArmor model,
        EntityPlayer player,
        float partialTicks) {
        GL11.glPushMatrix();
        try {
            renderLayers(backpackType, backpack, model, player, partialTicks, false);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private static void renderLayers(
        ItemBackpack backpackType,
        ItemStack backpack,
        ModelBackpackArmor model,
        EntityPlayer player,
        float partialTicks,
        boolean bodyOnly) {
        int color = backpackType.func_82814_b(backpack);
        renderLayer(backpackType, backpack, model, player, color >= 0 ? color : 0xFFFFFF, null, bodyOnly);

        if (color >= 0) {
            renderLayer(backpackType, backpack, model, player, 0xFFFFFF, "overlay", bodyOnly);
        }

        if (backpack.isItemEnchanted()) {
            renderEnchantedEffect(model, player, partialTicks, bodyOnly);
        }

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void renderLayer(
        ItemBackpack backpackType,
        ItemStack backpack,
        ModelBackpackArmor model,
        EntityPlayer player,
        int color,
        String type,
        boolean bodyOnly) {
        String texture = backpackType.getArmorTexture(backpack, player, 0, type);
        if (texture == null || texture.length() == 0) {
            return;
        }

        Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation(texture));
        setColorFromInt(color);
        renderModel(model, player, bodyOnly);
    }

    private static void renderEnchantedEffect(
        ModelBackpackArmor model,
        EntityPlayer player,
        float partialTicks,
        boolean bodyOnly) {
        float ticks = (float) player.ticksExisted + partialTicks;
        Minecraft.getMinecraft().getTextureManager().bindTexture(ENCHANTED_EFFECT);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glColor4f(0.5F, 0.5F, 0.5F, 1.0F);
        GL11.glDepthFunc(GL11.GL_EQUAL);
        GL11.glDepthMask(false);

        for (int pass = 0; pass < 2; pass++) {
            GL11.glDisable(GL11.GL_LIGHTING);
            float intensity = 0.76F;
            GL11.glColor4f(0.5F * intensity, 0.25F * intensity, 0.8F * intensity, 1.0F);
            GL11.glBlendFunc(GL11.GL_SRC_COLOR, GL11.GL_ONE);
            GL11.glMatrixMode(GL11.GL_TEXTURE);
            GL11.glLoadIdentity();
            GL11.glScalef(0.33333334F, 0.33333334F, 0.33333334F);
            GL11.glRotatef(30.0F - (float) pass * 60.0F, 0.0F, 0.0F, 1.0F);
            GL11.glTranslatef(0.0F, ticks * (0.001F + (float) pass * 0.003F) * 20.0F, 0.0F);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            renderModel(model, player, bodyOnly);
        }

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glDepthMask(true);
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
    }

    private static void renderModel(ModelBackpackArmor model, EntityPlayer player, boolean bodyOnly) {
        if (bodyOnly && model.bipedBody != null) {
            model.bipedBody.render(0.05F);
        } else {
            model.render(player, 0.0F, 0.0F, (float) player.ticksExisted, 0.0F, 0.0F, 0.0F);
        }
    }

    private static float getRenderSwingProgress(RenderPlayer renderer, EntityLivingBase entity, float partialTicks) {
        Method method = getRenderSwingProgressMethod();
        if (method == null) {
            return 0.0F;
        }

        try {
            Object value = method.invoke(renderer, entity, Float.valueOf(partialTicks));
            return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
        } catch (Throwable ignored) {
            return 0.0F;
        }
    }

    private static Method getRenderSwingProgressMethod() {
        if (swingLookupDone) {
            return renderSwingProgressMethod;
        }

        swingLookupDone = true;
        renderSwingProgressMethod = findMethod(RendererLivingEntity.class, "renderSwingProgress");
        if (renderSwingProgressMethod == null) {
            renderSwingProgressMethod = findMethod(RendererLivingEntity.class, "func_77040_d");
        }
        return renderSwingProgressMethod;
    }

    private static Method findMethod(Class<?> owner, String name) {
        try {
            Method method = owner.getDeclaredMethod(name, EntityLivingBase.class, Float.TYPE);
            method.setAccessible(true);
            return method;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void setColorFromInt(int color) {
        float red = (float) (color >> 16 & 255) / 255.0F;
        float green = (float) (color >> 8 & 255) / 255.0F;
        float blue = (float) (color & 255) / 255.0F;
        GL11.glColor4f(red, green, blue, 1.0F);
    }
}
