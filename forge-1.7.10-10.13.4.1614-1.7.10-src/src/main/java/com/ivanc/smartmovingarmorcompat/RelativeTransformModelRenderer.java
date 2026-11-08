package com.ivanc.smartmovingarmorcompat;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.smart.render.ModelRotationRenderer;
import org.lwjgl.opengl.GL11;

final class RelativeTransformModelRenderer extends ModelRenderer {
    private ModelBiped sourceModel;
    private final String[] sourcePartNames;
    private final ModelRenderer original;
    private final RendererTransformSnapshot baseTransform;
    private final float neutralPivotX;
    private final float neutralPivotY;
    private final float neutralPivotZ;
    private final float parentScaleX;
    private final float parentScaleY;
    private final float parentScaleZ;
    private final float parentTranslateX;
    private final float parentTranslateY;
    private final float parentTranslateZ;

    RelativeTransformModelRenderer(
        ModelBase owner,
        ModelBiped sourceModel,
        String[] sourcePartNames,
        ModelRenderer original,
        float neutralPivotX,
        float neutralPivotY,
        float neutralPivotZ) {
        this(
            owner,
            sourceModel,
            sourcePartNames,
            original,
            neutralPivotX,
            neutralPivotY,
            neutralPivotZ,
            1.0F,
            1.0F,
            1.0F,
            0.0F,
            0.0F,
            0.0F);
    }

    RelativeTransformModelRenderer(
        ModelBase owner,
        ModelBiped sourceModel,
        String[] sourcePartNames,
        ModelRenderer original,
        float neutralPivotX,
        float neutralPivotY,
        float neutralPivotZ,
        float parentScaleX,
        float parentScaleY,
        float parentScaleZ,
        float parentTranslateX,
        float parentTranslateY,
        float parentTranslateZ) {
        super(owner);
        this.sourceModel = sourceModel;
        this.sourcePartNames = sourcePartNames;
        this.original = original;
        this.baseTransform = RendererTransformSnapshot.capture(original);
        this.neutralPivotX = neutralPivotX;
        this.neutralPivotY = neutralPivotY;
        this.neutralPivotZ = neutralPivotZ;
        this.parentScaleX = parentScaleX;
        this.parentScaleY = parentScaleY;
        this.parentScaleZ = parentScaleZ;
        this.parentTranslateX = parentTranslateX;
        this.parentTranslateY = parentTranslateY;
        this.parentTranslateZ = parentTranslateZ;
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
        boolean matrixPushed = false;
        boolean sourceTransformsPushed = false;
        try {
            this.baseTransform.restore(this.original);
            ArmorModelSynchronizer.copyRendererVisibility(this, this.original);
            this.original.rotationPointX -= this.neutralPivotX;
            this.original.rotationPointY -= this.neutralPivotY;
            this.original.rotationPointZ -= this.neutralPivotZ;

            GL11.glPushMatrix();
            matrixPushed = true;
            undoParentTransform();
            ((ModelRotationRenderer) source).preTransforms(scale, true, true);
            sourceTransformsPushed = true;
            applyParentTransform();
            this.original.render(scale);
            ((ModelRotationRenderer) source).postTransforms(scale, true, true);
            sourceTransformsPushed = false;
            GL11.glPopMatrix();
            matrixPushed = false;
        } finally {
            if (sourceTransformsPushed) {
                ((ModelRotationRenderer) source).postTransforms(scale, true, true);
            }
            if (matrixPushed) {
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

    private void undoParentTransform() {
        GL11.glTranslatef(-this.parentTranslateX, -this.parentTranslateY, -this.parentTranslateZ);
        GL11.glScalef(
            1.0F / this.parentScaleX,
            1.0F / this.parentScaleY,
            1.0F / this.parentScaleZ);
    }

    private void applyParentTransform() {
        GL11.glScalef(this.parentScaleX, this.parentScaleY, this.parentScaleZ);
        GL11.glTranslatef(this.parentTranslateX, this.parentTranslateY, this.parentTranslateZ);
    }
}
