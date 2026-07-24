package com.ivanc.smartmovingarmorcompat;

import com.artur114.armoredarms.client.modelrender.armor.ArmModelManagerArmor;
import com.artur114.armoredarms.client.util.ItemStackAA;
import com.artur114.armoredarms.core.api.IPriority;
import com.artur114.armoredarms.core.api.Priority;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;

public class ArmoredArmsArmorModelManager extends ArmModelManagerArmor {
    @Override
    public ModelBiped model(AbstractClientPlayer player, ItemStackAA stack) {
        ModelBiped model = super.model(player, stack);
        ArmorModelSynchronizer.restore(model);
        return model;
    }

    @Override
    public Class<ArmModelManagerArmor> clazz() {
        return ArmModelManagerArmor.class;
    }

    @Override
    public IPriority priority() {
        return Priority.HIGH;
    }
}
