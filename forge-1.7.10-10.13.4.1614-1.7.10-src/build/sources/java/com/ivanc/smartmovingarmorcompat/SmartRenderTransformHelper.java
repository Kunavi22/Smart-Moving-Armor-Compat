package com.ivanc.smartmovingarmorcompat;

import api.player.render.IRenderPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.smart.render.ModelRotationRenderer;
import org.lwjgl.opengl.GL11;

public final class SmartRenderTransformHelper {
    public static final float MODEL_SCALE = 0.0625F;
    public static final String[] HEAD_PART = new String[] {"bipedHead", "field_78116_c"};
    public static final String[] BODY_PART = new String[] {"bipedBody", "field_78115_e"};

    private SmartRenderTransformHelper() {
    }

    public static ModelBiped getSmartModel(RenderPlayer renderer) {
        if (!(renderer instanceof IRenderPlayer)) {
            return null;
        }

        try {
            return ((IRenderPlayer) renderer).getModelBipedMainField();
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean isRealClientPlayer(EntityPlayer player) {
        if (player == null) {
            return false;
        }

        return player.getClass().getName().startsWith("net.minecraft.client.entity.");
    }

    public static Transform begin(ModelBiped sourceModel, String[] partNames) {
        ModelRenderer source = ArmorModelSynchronizer.getPart(sourceModel, partNames);
        if (!(source instanceof ModelRotationRenderer)) {
            return null;
        }

        return new Transform((ModelRotationRenderer) source);
    }

    public static final class Transform {
        private final ModelRotationRenderer source;
        private boolean active;

        private Transform(ModelRotationRenderer source) {
            this.source = source;
            GL11.glPushMatrix();
            this.active = true;
            this.source.preTransforms(MODEL_SCALE, true, true);
        }

        public void end() {
            if (!this.active) {
                return;
            }

            this.source.postTransforms(MODEL_SCALE, true, true);
            GL11.glPopMatrix();
            this.active = false;
        }
    }
}
