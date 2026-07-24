package com.ivanc.smartmovingarmorcompat;

import net.minecraft.client.model.ModelRenderer;

final class RendererTransformSnapshot {
    private final float rotationPointX;
    private final float rotationPointY;
    private final float rotationPointZ;
    private final float rotateAngleX;
    private final float rotateAngleY;
    private final float rotateAngleZ;
    private final float offsetX;
    private final float offsetY;
    private final float offsetZ;

    private RendererTransformSnapshot(ModelRenderer renderer) {
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

    static RendererTransformSnapshot capture(ModelRenderer renderer) {
        return new RendererTransformSnapshot(renderer);
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
