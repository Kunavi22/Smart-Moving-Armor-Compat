package com.ivanc.smartmovingarmorcompat;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.smart.render.ModelRotationRenderer;
import org.lwjgl.opengl.GL11;

final class FixedTransformModelRenderer extends ModelRenderer {
    private ModelBiped sourceModel;
    private final String[] sourcePartNames;
    private final ModelRenderer original;
    private final RendererTransformSnapshot baseTransform;

    FixedTransformModelRenderer(
        ModelBase owner,
        ModelBiped sourceModel,
        String[] sourcePartNames,
        ModelRenderer original) {
        super(owner);
        this.sourceModel = sourceModel;
        this.sourcePartNames = sourcePartNames;
        this.original = original;
        this.baseTransform = RendererTransformSnapshot.capture(original);
        ArmorModelSynchronizer.copyRendererTransform(original, this);
        ArmorModelSynchronizer.copyRendererVisibility(original, this);
    }

    void setSource(ModelBiped sourceModel) {
        this.sourceModel = sourceModel;
    }

    ModelRenderer getOriginal() {
        return this.original;
    }

    @Override
    public void render(float scale) {
        ModelRenderer source = ArmorModelSynchronizer.getPart(this.sourceModel, this.sourcePartNames);
        if (!(source instanceof ModelRotationRenderer)) {
            renderFallback(scale);
            return;
        }

        RendererTransformSnapshot currentTransform = RendererTransformSnapshot.capture(this.original);
        boolean wrapperMatrixPushed = false;
        boolean sourceTransformsPushed = false;
        try {
            this.baseTransform.restore(this.original);
            ArmorModelSynchronizer.copyRendererVisibility(this, this.original);

            GL11.glPushMatrix();
            wrapperMatrixPushed = true;
            ((ModelRotationRenderer) source).preTransforms(scale, true, true);
            sourceTransformsPushed = true;
            this.original.render(scale);
            ((ModelRotationRenderer) source).postTransforms(scale, true, true);
            sourceTransformsPushed = false;
            GL11.glPopMatrix();
            wrapperMatrixPushed = false;
        } finally {
            if (sourceTransformsPushed) {
                ((ModelRotationRenderer) source).postTransforms(scale, true, true);
            }
            if (wrapperMatrixPushed) {
                GL11.glPopMatrix();
            }
            currentTransform.restore(this.original);
        }
    }

    @Override
    public void addChild(ModelRenderer child) {
        this.original.addChild(child);
    }

    private void renderFallback(float scale) {
        RendererTransformSnapshot currentTransform = RendererTransformSnapshot.capture(this.original);
        try {
            ArmorModelSynchronizer.copyRendererTransform(this, this.original);
            ArmorModelSynchronizer.copyRendererVisibility(this, this.original);
            this.original.render(scale);
        } finally {
            currentTransform.restore(this.original);
        }
    }
}
