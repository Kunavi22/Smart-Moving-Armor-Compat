package com.ivanc.smartmovingarmorcompat;

import net.minecraft.client.model.ModelBiped;

public final class VanillaModelFactory {
    private VanillaModelFactory() {
    }

    public static ModelBiped createBiped(float modelSize) {
        return new ModelBiped(modelSize);
    }
}
