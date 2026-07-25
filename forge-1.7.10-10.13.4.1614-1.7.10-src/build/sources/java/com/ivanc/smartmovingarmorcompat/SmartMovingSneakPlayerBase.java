package com.ivanc.smartmovingarmorcompat;

import api.player.client.ClientPlayerAPI;
import api.player.client.ClientPlayerBase;
import api.player.client.ClientPlayerBaseSorting;
import api.player.server.ServerPlayerAPI;
import api.player.server.ServerPlayerBase;
import api.player.server.ServerPlayerBaseSorting;
import cpw.mods.fml.common.FMLCommonHandler;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class SmartMovingSneakPlayerBase {
    private static final String PLAYER_API_ID = SmartMovingArmorCompat.MODID + ".sneak";
    private static final String SMART_MOVING_PLAYER_API_ID = "Smart Moving";
    private static final String SMART_MOVING_SERVER_PLAYER_BASE = "net.smart.moving.playerapi.SmartMovingServerPlayerBase";
    private static final String SMART_MOVING_CLIENT_PLAYER_BASE = "net.smart.moving.playerapi.SmartMovingPlayerBase";

    private static boolean registered;
    private static boolean serverLookupDone;
    private static Method getServerPlayerBaseMethod;
    private static Method getServerMovingMethod;
    private static Field serverSneakButtonField;
    private static boolean clientLookupDone;
    private static Method getClientPlayerBaseMethod;
    private static Method getClientMovingMethod;
    private static Field clientSneakButtonField;
    private static Field clientButtonPressedField;
    private static boolean clientMovementInputLookupDone;
    private static Field clientMovementInputField;
    private static Field clientMovementSneakField;

    private SmartMovingSneakPlayerBase() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        ServerPlayerBaseSorting serverSorting = new ServerPlayerBaseSorting();
        serverSorting.setOverrideIsSneakingInferiors(new String[] {SMART_MOVING_PLAYER_API_ID});
        ServerPlayerAPI.register(PLAYER_API_ID, Server.class, serverSorting);

        if (FMLCommonHandler.instance().getSide().isClient()) {
            registerClient();
        }

        registered = true;
    }

    private static void registerClient() {
        ClientPlayerBaseSorting clientSorting = new ClientPlayerBaseSorting();
        clientSorting.setOverrideIsSneakingInferiors(new String[] {SMART_MOVING_PLAYER_API_ID});
        ClientPlayerAPI.register(PLAYER_API_ID, Client.class, clientSorting);
    }

    private static boolean isServerSneakButtonPressed(Object player) {
        Object moving = getServerSmartMoving(player);
        if (moving == null || serverSneakButtonField == null) {
            return false;
        }

        try {
            return serverSneakButtonField.getBoolean(moving);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object getServerSmartMoving(Object player) {
        if (player == null || !lookupServerSmartMovingMembers()) {
            return null;
        }

        try {
            Object playerBase = getServerPlayerBaseMethod.invoke(null, player);
            return playerBase == null ? null : getServerMovingMethod.invoke(playerBase);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean lookupServerSmartMovingMembers() {
        if (serverLookupDone) {
            return getServerPlayerBaseMethod != null && getServerMovingMethod != null;
        }

        serverLookupDone = true;
        try {
            Class<?> playerBaseClass = Class.forName(SMART_MOVING_SERVER_PLAYER_BASE);
            getServerPlayerBaseMethod = playerBaseClass.getMethod("getPlayerBase", Object.class);
            getServerMovingMethod = playerBaseClass.getMethod("getMoving");
            Class<?> movingClass = getServerMovingMethod.getReturnType();
            serverSneakButtonField = movingClass.getDeclaredField("isSneakButtonPressed");
            serverSneakButtonField.setAccessible(true);
        } catch (Throwable ignored) {
            getServerPlayerBaseMethod = null;
            getServerMovingMethod = null;
            serverSneakButtonField = null;
        }

        return getServerPlayerBaseMethod != null && getServerMovingMethod != null;
    }

    private static boolean isClientSneakButtonPressed(Object player) {
        return isSmartMovingClientSneakButtonPressed(player) || isClientMovementSneakPressed(player);
    }

    private static boolean isSmartMovingClientSneakButtonPressed(Object player) {
        Object moving = getClientSmartMoving(player);
        if (moving == null || clientSneakButtonField == null || clientButtonPressedField == null) {
            return false;
        }

        try {
            Object button = clientSneakButtonField.get(moving);
            return button != null && clientButtonPressedField.getBoolean(button);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object getClientSmartMoving(Object player) {
        if (player == null || !lookupClientSmartMovingMembers()) {
            return null;
        }

        try {
            Object playerBase = getClientPlayerBaseMethod.invoke(null, player);
            return playerBase == null ? null : getClientMovingMethod.invoke(playerBase);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean lookupClientSmartMovingMembers() {
        if (clientLookupDone) {
            return getClientPlayerBaseMethod != null && getClientMovingMethod != null;
        }

        clientLookupDone = true;
        try {
            Class<?> playerBaseClass = Class.forName(SMART_MOVING_CLIENT_PLAYER_BASE);
            getClientPlayerBaseMethod = findSingleArgumentMethod(playerBaseClass, "getPlayerBase");
            getClientMovingMethod = playerBaseClass.getMethod("getMoving");
            Class<?> movingClass = getClientMovingMethod.getReturnType();
            clientSneakButtonField = movingClass.getField("sneakButton");
            clientButtonPressedField = clientSneakButtonField.getType().getField("Pressed");
        } catch (Throwable ignored) {
            getClientPlayerBaseMethod = null;
            getClientMovingMethod = null;
            clientSneakButtonField = null;
            clientButtonPressedField = null;
        }

        return getClientPlayerBaseMethod != null && getClientMovingMethod != null;
    }

    private static Method findSingleArgumentMethod(Class<?> owner, String name) throws NoSuchMethodException {
        Method[] methods = owner.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (name.equals(method.getName()) && method.getParameterTypes().length == 1) {
                return method;
            }
        }

        throw new NoSuchMethodException(owner.getName() + "." + name);
    }

    private static boolean isClientMovementSneakPressed(Object player) {
        if (player == null || !lookupClientMovementInputMembers()) {
            return false;
        }

        try {
            Object movementInput = clientMovementInputField.get(player);
            return movementInput != null && clientMovementSneakField.getBoolean(movementInput);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean lookupClientMovementInputMembers() {
        if (clientMovementInputLookupDone) {
            return clientMovementInputField != null && clientMovementSneakField != null;
        }

        clientMovementInputLookupDone = true;
        try {
            Class<?> playerClass = Class.forName("net.minecraft.client.entity.EntityPlayerSP");
            clientMovementInputField = playerClass.getField("field_71158_b");
            Class<?> movementInputClass = clientMovementInputField.getType();
            clientMovementSneakField = movementInputClass.getField("field_78899_d");
        } catch (Throwable ignored) {
            clientMovementInputField = null;
            clientMovementSneakField = null;
        }

        return clientMovementInputField != null && clientMovementSneakField != null;
    }

    public static final class Server extends ServerPlayerBase {
        public Server(ServerPlayerAPI playerAPI) {
            super(playerAPI);
        }

        @Override
        public boolean isSneaking() {
            return isServerSneakButtonPressed(this.player) || super.isSneaking();
        }
    }

    public static final class Client extends ClientPlayerBase {
        public Client(ClientPlayerAPI playerAPI) {
            super(playerAPI);
        }

        @Override
        public boolean isSneaking() {
            return isClientSneakButtonPressed(this.player) || super.isSneaking();
        }
    }
}
