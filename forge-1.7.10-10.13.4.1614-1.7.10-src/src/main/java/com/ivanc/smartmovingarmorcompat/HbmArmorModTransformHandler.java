package com.ivanc.smartmovingarmorcompat;

import com.hbm.handler.ArmorModHandler;
import com.hbm.items.armor.JetpackBase;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderPlayerEvent;

public class HbmArmorModTransformHandler {
    private static final String ITEM_MOD_GASMASK = "com.hbm.items.armor.ItemModGasmask";
    private static final String ITEM_MOD_TESLA = "com.hbm.items.armor.ItemModTesla";
    private static final String MODEL_M65 = "com.hbm.render.model.ModelM65";
    private static final String MODEL_BACK_TESLA = "com.hbm.render.model.ModelBackTesla";
    private static final String RENDER_ACCESSORY_UTILITY = "com.hbm.render.util.RenderAccessoryUtility";

    private ActiveState activeState;

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void prepareArmorMods(RenderPlayerEvent.SetArmorModel event) {
        if (!SmartRenderTransformHelper.isRealClientPlayer(event.entityPlayer)) {
            return;
        }

        ModelBiped source = SmartRenderTransformHelper.getSmartModel(event.renderer);
        if (source == null) {
            return;
        }

        ActiveState state = new ActiveState(event.entityPlayer, source);
        for (int i = 0; i < 4; i++) {
            ItemStack armor = event.entityPlayer.getEquipmentInSlot(i + 1);
            if (armor == null) {
                continue;
            }

            prepareInstalledMods(event, armor, state);
            prepareStandaloneArmor(event, armor, state);
        }

        prepareStaticAccessoryModels(state);

        if (state.hasWork()) {
            this.activeState = state;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void restoreArmorMods(RenderPlayerEvent.SetArmorModel event) {
        ActiveState state = this.activeState;
        this.activeState = null;
        if (state != null) {
            state.restore();
        }
    }

    private static void prepareInstalledMods(RenderPlayerEvent.SetArmorModel event, ItemStack armor, ActiveState state) {
        try {
            if (!ArmorModHandler.hasMods(armor)) {
                return;
            }

            ItemStack[] mods = ArmorModHandler.pryMods(armor);
            for (int i = 0; i < mods.length; i++) {
                if (mods[i] != null) {
                    prepareArmorMod(event, mods[i], state);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void prepareStandaloneArmor(RenderPlayerEvent.SetArmorModel event, ItemStack armor, ActiveState state) {
        Item item = armor.getItem();
        if (item instanceof JetpackBase) {
            prepareJetpackModel(event, (JetpackBase) item, state);
        }
    }

    private static void prepareArmorMod(RenderPlayerEvent.SetArmorModel event, ItemStack mod, ActiveState state) {
        Item item = mod.getItem();
        if (item == null) {
            return;
        }

        String className = item.getClass().getName();
        if (ITEM_MOD_GASMASK.equals(className)) {
            prepareFieldModel(item, "modelM65", MODEL_M65, state);
        } else if (ITEM_MOD_TESLA.equals(className)) {
            prepareFieldModel(item, "modelTesla", MODEL_BACK_TESLA, state);
        } else if (item instanceof JetpackBase) {
            prepareJetpackModel(event, (JetpackBase) item, state);
        }
    }

    private static void prepareJetpackModel(RenderPlayerEvent.SetArmorModel event, JetpackBase jetpack, ActiveState state) {
        try {
            ModelBiped model = jetpack.getArmorModel(event.entityLiving, null, 1);
            if (HbmArmorModelWrapper.isManualBodyModel(model)) {
                state.wrapModelField(jetpack, "cachedModel", model);
                return;
            }
            state.prepareModel(model);
        } catch (Throwable ignored) {
        }
    }

    private static void prepareFieldModel(Object owner, String fieldName, String modelClassName, ActiveState state) {
        try {
            Field field = findField(owner.getClass(), fieldName);
            if (field == null) {
                return;
            }

            Object model = field.get(owner);
            if (model == null) {
                model = newModel(modelClassName);
                if (model != null) {
                    field.set(owner, model);
                }
            }

            if (model instanceof ModelBiped) {
                state.prepareModel((ModelBiped) model);
            }
        } catch (Throwable ignored) {
        }
    }

    private static Object newModel(String modelClassName) {
        try {
            Class<?> type = Class.forName(modelClassName);
            Constructor<?> constructor = type.getConstructor(new Class[0]);
            return constructor.newInstance(new Object[0]);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void prepareStaticAccessoryModels(ActiveState state) {
        try {
            Class<?> utility = Class.forName(RENDER_ACCESSORY_UTILITY);
            Field wingModelsField = findField(utility, "wingModels");
            if (wingModelsField != null) {
                Object value = wingModelsField.get(null);
                if (value instanceof Object[]) {
                    Object[] models = (Object[]) value;
                    for (int i = 0; i < models.length; i++) {
                        if (models[i] instanceof ModelBiped) {
                            ModelBiped model = (ModelBiped) models[i];
                            if (HbmArmorModelWrapper.isManualBodyModel(model)) {
                                state.wrapArrayModel(models, i, model);
                            } else {
                                state.prepareModel(model);
                            }
                        }
                    }
                }
            }

            prepareStaticModelField(utility, "axePackModel", state);
            prepareStaticModelField(utility, "tailModel", state);
        } catch (Throwable ignored) {
        }
    }

    private static void prepareStaticModelField(Class<?> owner, String fieldName, ActiveState state) {
        try {
            Field field = findField(owner, fieldName);
            Object value = field == null ? null : field.get(null);
            if (value instanceof ModelBiped) {
                state.prepareModel((ModelBiped) value);
            }
        } catch (Throwable ignored) {
        }
    }

    private static Field findField(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            } catch (Throwable ignored) {
                return null;
            }
        }
        return null;
    }

    private static final class ActiveState {
        private final Entity entity;
        private final ModelBiped source;
        private final List genericModels = new ArrayList();
        private final List hbmModels = new ArrayList();
        private final List hbmProxyInstalls = new ArrayList();
        private final List wrappedModels = new ArrayList();
        private HbmArmorModelWrapper.SmartRenderContext smartContext;

        private ActiveState(Entity entity, ModelBiped source) {
            this.entity = entity;
            this.source = source;
        }

        private void prepareModel(ModelBiped model) {
            if (model == null) {
                return;
            }

            if (HbmArmorModelWrapper.isHbmArmorModel(model)) {
                if (this.hbmModels.contains(model)) {
                    return;
                }
                if (this.smartContext == null) {
                    this.smartContext = HbmArmorModelWrapper.beginSmartRenderContext(this.entity, this.source);
                }
                List installed = HbmArmorModelWrapper.prepareHbmPartProxies(model, this.source);
                if (!installed.isEmpty()) {
                    this.hbmModels.add(model);
                    this.hbmProxyInstalls.add(installed);
                }
            } else if (!this.genericModels.contains(model)) {
                ArmorModelSynchronizer.prepare(model, this.source);
                this.genericModels.add(model);
            }
        }

        private void wrapModelField(Object owner, String fieldName, ModelBiped original) {
            try {
                Field field = findField(owner.getClass(), fieldName);
                if (field == null) {
                    return;
                }

                Object current = field.get(owner);
                if (current instanceof HbmArmorModelWrapper) {
                    ((HbmArmorModelWrapper) current).setSourceModel(this.source);
                    return;
                }

                if (current != original) {
                    original = current instanceof ModelBiped ? (ModelBiped) current : original;
                }

                field.set(owner, new HbmArmorModelWrapper(original, this.source));
                this.wrappedModels.add(new FieldWrap(owner, field, original));
            } catch (Throwable ignored) {
            }
        }

        private void wrapArrayModel(Object[] array, int index, ModelBiped original) {
            try {
                Object current = array[index];
                if (current instanceof HbmArmorModelWrapper) {
                    ((HbmArmorModelWrapper) current).setSourceModel(this.source);
                    return;
                }

                if (current != original) {
                    original = current instanceof ModelBiped ? (ModelBiped) current : original;
                }

                array[index] = new HbmArmorModelWrapper(original, this.source);
                this.wrappedModels.add(new ArrayWrap(array, index, original));
            } catch (Throwable ignored) {
            }
        }

        private boolean hasWork() {
            return this.smartContext != null
                || !this.genericModels.isEmpty()
                || !this.hbmProxyInstalls.isEmpty()
                || !this.wrappedModels.isEmpty();
        }

        private void restore() {
            for (int i = this.wrappedModels.size() - 1; i >= 0; i--) {
                ((ModelWrap) this.wrappedModels.get(i)).restore();
            }

            for (int i = this.hbmProxyInstalls.size() - 1; i >= 0; i--) {
                HbmArmorModelWrapper.restorePreparedHbmPartProxies((List) this.hbmProxyInstalls.get(i));
            }

            for (int i = this.genericModels.size() - 1; i >= 0; i--) {
                ArmorModelSynchronizer.restore((ModelBiped) this.genericModels.get(i));
            }

            if (this.smartContext != null) {
                this.smartContext.end();
            }
        }
    }

    private interface ModelWrap {
        void restore();
    }

    private static final class FieldWrap implements ModelWrap {
        private final Object owner;
        private final Field field;
        private final Object original;

        private FieldWrap(Object owner, Field field, Object original) {
            this.owner = owner;
            this.field = field;
            this.original = original;
        }

        public void restore() {
            try {
                this.field.set(this.owner, this.original);
            } catch (Throwable ignored) {
            }
        }
    }

    private static final class ArrayWrap implements ModelWrap {
        private final Object[] array;
        private final int index;
        private final Object original;

        private ArrayWrap(Object[] array, int index, Object original) {
            this.array = array;
            this.index = index;
            this.original = original;
        }

        public void restore() {
            try {
                this.array[this.index] = this.original;
            } catch (Throwable ignored) {
            }
        }
    }
}
