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
        RendererTransformSnapshot originalBodyTransform = body == null ? null : RendererTransformSnapshot.capture(body);
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
}
