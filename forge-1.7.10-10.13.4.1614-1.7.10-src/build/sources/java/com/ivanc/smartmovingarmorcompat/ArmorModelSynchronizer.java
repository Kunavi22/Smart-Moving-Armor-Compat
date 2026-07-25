package com.ivanc.smartmovingarmorcompat;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;

public final class ArmorModelSynchronizer {
    private static final String[] BIPED_HEAD = new String[] {"bipedHead", "field_78116_c"};
    private static final String[] BIPED_HEADWEAR = new String[] {"bipedHeadwear", "field_78114_d"};
    private static final String[] BIPED_BODY = new String[] {"bipedBody", "field_78115_e"};
    private static final String[] BIPED_RIGHT_ARM = new String[] {"bipedRightArm", "field_78112_f"};
    private static final String[] BIPED_LEFT_ARM = new String[] {"bipedLeftArm", "field_78113_g"};
    private static final String[] BIPED_RIGHT_LEG = new String[] {"bipedRightLeg", "field_78123_h"};
    private static final String[] BIPED_LEFT_LEG = new String[] {"bipedLeftLeg", "field_78124_i"};

    private static final String[][] BIPED_PARTS = new String[][] {
        BIPED_HEAD,
        BIPED_HEADWEAR,
        BIPED_BODY,
        BIPED_RIGHT_ARM,
        BIPED_LEFT_ARM,
        BIPED_RIGHT_LEG,
        BIPED_LEFT_LEG
    };

    private static final String[][][] CUSTOM_PART_FIELDS = new String[][][] {
        {BIPED_HEAD, new String[] {"helm", "head"}},
        {BIPED_BODY, new String[] {"body", "torso", "cloakMain", "cloakLeft", "cloakRight", "cape", "cloak"}},
        {BIPED_RIGHT_ARM, new String[] {"armR", "armr", "rightArm", "armRight"}},
        {BIPED_LEFT_ARM, new String[] {"armL", "leftArm", "armLeft"}},
        {BIPED_RIGHT_LEG, new String[] {"legR", "bootR", "rightLeg", "legRight"}},
        {BIPED_LEFT_LEG, new String[] {"legL", "bootL", "leftLeg", "legLeft"}}
    };

    private static final String[][] DIRECT_BODY_PART_FIELDS = new String[][] {
        {"emt.client.model.ModelWings", "rightWing", "leftWing", "center"},
        {"emt.client.model.ModelSpecialArmor", "rightWing", "leftWing", "center", "jetpack"},
        {"tconstruct.armor.model.WingModel", "WingBaseRight", "WingBaseLeft"},
        {"com.emoniph.witchery.client.model.ModelVampireArmor", "chest"}
    };

    private static final String[][] DIRECT_HEAD_PART_FIELDS = new String[][] {
        {"com.darkona.adventurebackpack.client.models.ModelAdventureHat", "wing", "top", "thing"}
    };

    private static final String[] DIRECT_BODY_ALL_DECLARED_FIELDS = new String[] {
        "galaxyspace.core.model.ModelJetPack"
    };

    private static final String[][] MODEL_STATE = new String[][] {
        {"heldItemLeft", "field_78119_l"},
        {"heldItemRight", "field_78120_m"},
        {"isSneak", "field_78117_n"},
        {"aimedBow", "field_78118_o"},
        {"isRiding", "field_78093_q"},
        {"onGround", "field_78095_p"},
        {"isChild", "field_78091_s"}
    };

    private static final Map<String, Field> FIELD_CACHE = new HashMap<String, Field>();

    private ArmorModelSynchronizer() {
    }

    public static void prepare(ModelBiped target, ModelBiped source) {
        copyModelState(source, target);

        for (int i = 0; i < BIPED_PARTS.length; i++) {
            installProxy(target, source, BIPED_PARTS[i]);
        }

        for (int i = 0; i < CUSTOM_PART_FIELDS.length; i++) {
            String[] sourcePartNames = CUSTOM_PART_FIELDS[i][0];
            String[] targetFieldNames = CUSTOM_PART_FIELDS[i][1];
            for (int j = 0; j < targetFieldNames.length; j++) {
                installProxy(target, source, sourcePartNames, new String[] {targetFieldNames[j]});
            }
        }

        installDirectPartFields(target, source, DIRECT_BODY_PART_FIELDS, BIPED_BODY);
        installDirectPartFields(target, source, DIRECT_HEAD_PART_FIELDS, BIPED_HEAD);
        installAllDeclaredDirectPartFields(target, source, DIRECT_BODY_ALL_DECLARED_FIELDS, BIPED_BODY);
    }

    public static void restore(ModelBiped target) {
        if (target == null) {
            return;
        }

        for (int i = 0; i < BIPED_PARTS.length; i++) {
            restoreProxy(target, BIPED_PARTS[i]);
        }

        for (int i = 0; i < CUSTOM_PART_FIELDS.length; i++) {
            String[] targetFieldNames = CUSTOM_PART_FIELDS[i][1];
            for (int j = 0; j < targetFieldNames.length; j++) {
                restoreProxy(target, new String[] {targetFieldNames[j]});
            }
        }

        restoreDirectPartFields(target, DIRECT_BODY_PART_FIELDS);
        restoreDirectPartFields(target, DIRECT_HEAD_PART_FIELDS);
        restoreAllDeclaredDirectPartFields(target, DIRECT_BODY_ALL_DECLARED_FIELDS);

        resetVanillaState(target);
    }

    static ModelRenderer getPart(ModelBiped model, String[] names) {
        Object value = get(model, names);
        return value instanceof ModelRenderer ? (ModelRenderer) value : null;
    }

    static void syncAngles(ModelRenderer source, ModelRenderer target) {
        copyFloat(source, target, "rotateAngleX", "field_78795_f");
        copyFloat(source, target, "rotateAngleY", "field_78796_g");
        copyFloat(source, target, "rotateAngleZ", "field_78808_h");
    }

    static void copyRendererTransform(ModelRenderer source, ModelRenderer target) {
        target.rotationPointX = source.rotationPointX;
        target.rotationPointY = source.rotationPointY;
        target.rotationPointZ = source.rotationPointZ;
        target.rotateAngleX = source.rotateAngleX;
        target.rotateAngleY = source.rotateAngleY;
        target.rotateAngleZ = source.rotateAngleZ;
        target.offsetX = source.offsetX;
        target.offsetY = source.offsetY;
        target.offsetZ = source.offsetZ;
    }

    static void copyRendererVisibility(ModelRenderer source, ModelRenderer target) {
        copyBoolean(source, target, "showModel", "field_78806_j");
        copyBoolean(source, target, "isHidden", "field_78807_k");
        copyBoolean(source, target, "mirror", "field_78809_i");
    }

    private static void installProxy(ModelBiped target, ModelBiped source, String[] partNames) {
        installProxy(target, source, partNames, partNames);
    }

    private static void installProxy(ModelBiped target, ModelBiped source, String[] sourcePartNames, String[] targetPartNames) {
        installProxy(target, source, sourcePartNames, targetPartNames, true, false);
    }

    private static void installProxy(
        ModelBiped target,
        ModelBiped source,
        String[] sourcePartNames,
        String[] targetPartNames,
        boolean copyAnglesFromSource,
        boolean preserveLocalTransform) {
        ModelRenderer current = getPart(target, targetPartNames);
        if (current == null) {
            return;
        }

        if (current instanceof SyncingModelRenderer) {
            ((SyncingModelRenderer) current).setSource(source);
            return;
        }

        SyncingModelRenderer proxy =
            new SyncingModelRenderer(target, source, sourcePartNames, current, copyAnglesFromSource, preserveLocalTransform);
        set(target, targetPartNames, proxy);
    }

    private static void installDirectPartFields(ModelBiped target, ModelBiped source, String[][] rules, String[] sourcePartNames) {
        String className = target.getClass().getName();
        for (int i = 0; i < rules.length; i++) {
            if (!rules[i][0].equals(className)) {
                continue;
            }

            for (int j = 1; j < rules[i].length; j++) {
                installProxy(target, source, sourcePartNames, new String[] {rules[i][j]}, false, true);
            }
        }
    }

    private static void installAllDeclaredDirectPartFields(
        ModelBiped target,
        ModelBiped source,
        String[] classNames,
        String[] sourcePartNames) {
        if (!isTargetClass(target, classNames)) {
            return;
        }

        Class<?> current = target.getClass();
        while (current != null && current != ModelBiped.class) {
            Field[] fields = current.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                if (ModelRenderer.class.isAssignableFrom(fields[i].getType())) {
                    installProxy(target, source, sourcePartNames, new String[] {fields[i].getName()}, false, true);
                }
            }
            current = current.getSuperclass();
        }
    }

    private static void restoreProxy(ModelBiped target, String[] partNames) {
        ModelRenderer current = getPart(target, partNames);
        if (current instanceof SyncingModelRenderer) {
            set(target, partNames, ((SyncingModelRenderer) current).getOriginal());
        }
    }

    private static void restoreDirectPartFields(ModelBiped target, String[][] rules) {
        String className = target.getClass().getName();
        for (int i = 0; i < rules.length; i++) {
            if (!rules[i][0].equals(className)) {
                continue;
            }

            for (int j = 1; j < rules[i].length; j++) {
                restoreProxy(target, new String[] {rules[i][j]});
            }
        }
    }

    private static void restoreAllDeclaredDirectPartFields(ModelBiped target, String[] classNames) {
        if (!isTargetClass(target, classNames)) {
            return;
        }

        Class<?> current = target.getClass();
        while (current != null && current != ModelBiped.class) {
            Field[] fields = current.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                if (ModelRenderer.class.isAssignableFrom(fields[i].getType())) {
                    restoreProxy(target, new String[] {fields[i].getName()});
                }
            }
            current = current.getSuperclass();
        }
    }

    private static boolean isTargetClass(ModelBiped target, String[] classNames) {
        String className = target.getClass().getName();
        for (int i = 0; i < classNames.length; i++) {
            if (classNames[i].equals(className)) {
                return true;
            }
        }
        return false;
    }

    private static void resetVanillaState(ModelBiped target) {
        target.heldItemLeft = 0;
        target.heldItemRight = 0;
        target.isSneak = false;
        target.aimedBow = false;
        target.isRiding = false;
        target.onGround = 0.0F;

        resetPart(target.bipedHead, 0.0F, 0.0F, 0.0F);
        resetPart(target.bipedHeadwear, 0.0F, 0.0F, 0.0F);
        resetPart(target.bipedBody, 0.0F, 0.0F, 0.0F);
        resetPart(target.bipedRightArm, -5.0F, 2.0F, 0.0F);
        resetPart(target.bipedLeftArm, 5.0F, 2.0F, 0.0F);
        resetPart(target.bipedRightLeg, -2.0F, 12.0F, 0.0F);
        resetPart(target.bipedLeftLeg, 2.0F, 12.0F, 0.0F);
    }

    private static void resetPart(ModelRenderer renderer, float rotationPointX, float rotationPointY, float rotationPointZ) {
        if (renderer == null) {
            return;
        }

        renderer.rotationPointX = rotationPointX;
        renderer.rotationPointY = rotationPointY;
        renderer.rotationPointZ = rotationPointZ;
        renderer.rotateAngleX = 0.0F;
        renderer.rotateAngleY = 0.0F;
        renderer.rotateAngleZ = 0.0F;
        renderer.offsetX = 0.0F;
        renderer.offsetY = 0.0F;
        renderer.offsetZ = 0.0F;
    }

    private static void copyModelState(ModelBiped source, ModelBiped target) {
        for (int i = 0; i < MODEL_STATE.length; i++) {
            copyValue(source, target, MODEL_STATE[i]);
        }
    }

    private static void copyValue(Object source, Object target, String[] names) {
        Field sourceField = findField(source.getClass(), names);
        Field targetField = findField(target.getClass(), names);
        if (sourceField == null || targetField == null) {
            return;
        }

        try {
            targetField.set(target, sourceField.get(source));
        } catch (Throwable ignored) {
        }
    }

    private static Object get(Object owner, String[] names) {
        Field field = findField(owner.getClass(), names);
        if (field == null) {
            return null;
        }

        try {
            return field.get(owner);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void set(Object owner, String[] names, Object value) {
        Field field = findField(owner.getClass(), names);
        if (field == null) {
            return;
        }

        try {
            field.set(owner, value);
        } catch (Throwable ignored) {
        }
    }

    private static void copyFloat(Object source, Object target, String mcpName, String srgName) {
        Field sourceField = findField(source.getClass(), new String[] {mcpName, srgName});
        Field targetField = findField(target.getClass(), new String[] {mcpName, srgName});
        if (sourceField == null || targetField == null) {
            return;
        }

        try {
            targetField.setFloat(target, sourceField.getFloat(source));
        } catch (Throwable ignored) {
        }
    }

    private static void copyBoolean(Object source, Object target, String mcpName, String srgName) {
        Field sourceField = findField(source.getClass(), new String[] {mcpName, srgName});
        Field targetField = findField(target.getClass(), new String[] {mcpName, srgName});
        if (sourceField == null || targetField == null) {
            return;
        }

        try {
            targetField.setBoolean(target, sourceField.getBoolean(source));
        } catch (Throwable ignored) {
        }
    }

    private static Field findField(Class<?> type, String[] names) {
        for (int i = 0; i < names.length; i++) {
            Field field = findField(type, names[i]);
            if (field != null) {
                return field;
            }
        }
        return null;
    }

    private static Field findField(Class<?> type, String name) {
        String key = type.getName() + "#" + name;
        if (FIELD_CACHE.containsKey(key)) {
            return FIELD_CACHE.get(key);
        }

        Class<?> current = type;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                FIELD_CACHE.put(key, field);
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            } catch (Throwable ignored) {
                break;
            }
        }

        FIELD_CACHE.put(key, null);
        return null;
    }
}
