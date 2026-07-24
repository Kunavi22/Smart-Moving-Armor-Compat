package com.ivanc.smartmovingarmorcompat;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.smart.render.ModelRotationRenderer;
import org.lwjgl.opengl.GL11;

public class HoverHarnessModelWrapper extends ModelBiped {
    private static final String[] BODY_PART = new String[] {"bipedBody", "field_78115_e"};

    private final ModelBiped original;
    private ModelBiped sourceModel;

    public HoverHarnessModelWrapper(ModelBiped original, ModelBiped sourceModel) {
        this.original = original;
        this.sourceModel = sourceModel;
    }

    public void setSourceModel(ModelBiped sourceModel) {
        this.sourceModel = sourceModel;
    }

    public ModelBiped getOriginal() {
        return this.original;
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        ModelRenderer source = ArmorModelSynchronizer.getPart(this.sourceModel, BODY_PART);
        if (source instanceof ModelRotationRenderer) {
            renderOriginalWithBodyTransform((ModelRotationRenderer) source, entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        } else {
            this.original.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        }
    }

    private void renderOriginalWithBodyTransform(ModelRotationRenderer source, Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        ModelRenderer body = ArmorModelSynchronizer.getPart(this.original, BODY_PART);
        RendererTransform originalBodyTransform = body == null ? null : RendererTransform.capture(body);
        boolean wrapperMatrixPushed = false;

        try {
            if (originalBodyTransform != null) {
                originalBodyTransform.applyIdentity(body);
            }

            GL11.glPushMatrix();
            wrapperMatrixPushed = true;
            source.preTransforms(scale, true, true);
            if (entity != null && entity.isSneaking()) {
                GL11.glRotatef(-28.64789F, 1.0F, 0.0F, 0.0F);
            }
            this.original.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            source.postTransforms(scale, true, true);
            GL11.glPopMatrix();
            wrapperMatrixPushed = false;
        } finally {
            if (wrapperMatrixPushed) {
                GL11.glPopMatrix();
            }
            if (originalBodyTransform != null) {
                originalBodyTransform.restore(body);
            }
        }
    }

    private static final class RendererTransform {
        private final float rotationPointX;
        private final float rotationPointY;
        private final float rotationPointZ;
        private final float rotateAngleX;
        private final float rotateAngleY;
        private final float rotateAngleZ;
        private final float offsetX;
        private final float offsetY;
        private final float offsetZ;

        private RendererTransform(ModelRenderer renderer) {
            this.rotationPointX = renderer.rotationPointX;
            this.rotationPointY = renderer.rotationPointY;
            this.rotationPointZ = renderer.rotationPointZ;
            this.rotateAngleX = renderer.rotateAngleX;
            this.rotateAngleY = renderer.rotateAngleY;
            this.rotateAngleZ = renderer.rotateAngleZ;
            this.offsetX = renderer.offsetX;
            this.offsetY = renderer.offsetY;
            this.offsetZ = renderer.offsetZ;
        }

        static RendererTransform capture(ModelRenderer renderer) {
            return new RendererTransform(renderer);
        }

        void applyIdentity(ModelRenderer renderer) {
            renderer.rotationPointX = 0.0F;
            renderer.rotationPointY = 0.0F;
            renderer.rotationPointZ = 0.0F;
            renderer.rotateAngleX = 0.0F;
            renderer.rotateAngleY = 0.0F;
            renderer.rotateAngleZ = 0.0F;
            renderer.offsetX = 0.0F;
            renderer.offsetY = 0.0F;
            renderer.offsetZ = 0.0F;
        }

        void restore(ModelRenderer renderer) {
            renderer.rotationPointX = this.rotationPointX;
            renderer.rotationPointY = this.rotationPointY;
            renderer.rotationPointZ = this.rotationPointZ;
            renderer.rotateAngleX = this.rotateAngleX;
            renderer.rotateAngleY = this.rotateAngleY;
            renderer.rotateAngleZ = this.rotateAngleZ;
            renderer.offsetX = this.offsetX;
            renderer.offsetY = this.offsetY;
            renderer.offsetZ = this.offsetZ;
        }
    }
}
