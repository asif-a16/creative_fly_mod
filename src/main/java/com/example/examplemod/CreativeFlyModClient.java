package com.example.examplemod;

import java.util.Locale;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = CreativeFlyMod.MODID, dist = Dist.CLIENT)
public class CreativeFlyModClient {
    private static final float DEFAULT_FLIGHT_SPEED = FlyProfileManager.DEFAULT_FLIGHT_SPEED;
    private static final float MIN_FLIGHT_SPEED = FlyProfileManager.MIN_FLIGHT_SPEED;
    private static final float MAX_FLIGHT_SPEED = FlyProfileManager.MAX_FLIGHT_SPEED;
    private static final float SPEED_STEP = DEFAULT_FLIGHT_SPEED * 0.10F;
    private static final long DOUBLE_TAP_WINDOW_MS = 300L;
    private static final long STATUS_MESSAGE_DURATION_MS = 1500L;
    private static final int AUTO_ARM_WARNING_DELAY_TICKS = 40;
    private static final double BASE_DISTANCE_PER_TICK = 7.0D;
    private static final double SPRINT_MULTIPLIER = 1.75D;
    private static final float SPEED_COMPARE_EPSILON = 0.0001F;

    private static final KeyMapping TOGGLE_FLIGHT = new KeyMapping(
            "key.creativeflymod.toggle_flight",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.creativeflymod");

    private static final KeyMapping SPEED_DOWN = new KeyMapping(
            "key.creativeflymod.speed_down",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.creativeflymod");

    private static final KeyMapping SPEED_UP = new KeyMapping(
            "key.creativeflymod.speed_up",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.creativeflymod");

    private static final KeyMapping SPEED_RESET = new KeyMapping(
            "key.creativeflymod.speed_reset",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.creativeflymod");

    private static final KeyMapping OPEN_PROFILES = new KeyMapping(
            "key.creativeflymod.open_profiles",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "key.categories.creativeflymod");

        private static final KeyMapping PROFILE_1 = new KeyMapping(
            "key.creativeflymod.profile_1",
            InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.creativeflymod");

        private static final KeyMapping PROFILE_2 = new KeyMapping(
            "key.creativeflymod.profile_2",
            InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.creativeflymod");

        private static final KeyMapping PROFILE_3 = new KeyMapping(
            "key.creativeflymod.profile_3",
            InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.creativeflymod");

            private static final KeyMapping CYCLE_PROFILE_FORWARD = new KeyMapping(
                "key.creativeflymod.cycle_profile_forward",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "key.categories.creativeflymod");

            private static final KeyMapping CYCLE_PROFILE_BACKWARD = new KeyMapping(
                "key.creativeflymod.cycle_profile_backward",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "key.categories.creativeflymod");

    private static boolean flyModEnabled = false;
    private static boolean creativeFlightEnabled = false;
    private static float currentFlightSpeed = DEFAULT_FLIGHT_SPEED;
    private static long lastJumpTapTimeMs = 0L;
    private static boolean jumpKeyWasDown = false;
    private static long statusMessageShownAtMs = 0L;
    private static String statusMessageKey = "";
    private static String statusMessageText = "";
    private static boolean pendingJoinInitialization = true;
    private static boolean pendingAutoArm = false;
    private static boolean hasStoredSpeedBeforeReset = false;
    private static float storedSpeedBeforeReset = DEFAULT_FLIGHT_SPEED;
    private static boolean autoArmBlockedMessageShown = false;
    private static int autoArmBlockedTicks = 0;

    public CreativeFlyModClient(ModContainer container, IEventBus modEventBus) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class,
            (modContainer, parentScreen) -> new CreativeFlyProfilesScreen(parentScreen));

        modEventBus.addListener(CreativeFlyModClient::onClientSetup);
        modEventBus.addListener(CreativeFlyModClient::onRegisterKeyMappings);

        NeoForge.EVENT_BUS.addListener(CreativeFlyModClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(CreativeFlyModClient::onRenderGui);
    }

    static void onClientSetup(FMLClientSetupEvent event) {
        FlyProfileManager.load();
        refreshSpeedFromSelectedProfile();
        CreativeFlyMod.LOGGER.info("CreativeFlyMod client initialized for {}", Minecraft.getInstance().getUser().getName());
    }

    static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_FLIGHT);
        event.register(SPEED_DOWN);
        event.register(SPEED_UP);
        event.register(SPEED_RESET);
        event.register(OPEN_PROFILES);
        event.register(PROFILE_1);
        event.register(PROFILE_2);
        event.register(PROFILE_3);
        event.register(CYCLE_PROFILE_FORWARD);
        event.register(CYCLE_PROFILE_BACKWARD);
    }

    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null) {
            flyModEnabled = false;
            creativeFlightEnabled = false;
            jumpKeyWasDown = false;
            lastJumpTapTimeMs = 0L;
            pendingJoinInitialization = true;
            pendingAutoArm = false;
            autoArmBlockedMessageShown = false;
            autoArmBlockedTicks = 0;
            hasStoredSpeedBeforeReset = false;
            ServerOptInState.setOptedIn(false);
            return;
        }

        boolean flightAllowed = isFlightAllowed(minecraft);

        if (pendingJoinInitialization) {
            pendingAutoArm = FlyProfileManager.isAutoArmOnJoin();
            flyModEnabled = false;
            creativeFlightEnabled = false;
            pendingJoinInitialization = false;
            autoArmBlockedTicks = 0;
        }

        if (pendingAutoArm && flightAllowed) {
            flyModEnabled = true;
            pendingAutoArm = false;
            autoArmBlockedMessageShown = false;
            autoArmBlockedTicks = 0;
        }

        if (pendingAutoArm && !flightAllowed) {
            if (autoArmBlockedTicks < AUTO_ARM_WARNING_DELAY_TICKS) {
                autoArmBlockedTicks++;
            } else if (!autoArmBlockedMessageShown) {
                showServerOptInRequiredMessage();
                autoArmBlockedMessageShown = true;
            }
        }

        if (!flightAllowed) {
            flyModEnabled = false;
            creativeFlightEnabled = false;
        }

        handleJumpDoubleTapToggle(minecraft, flightAllowed);

        while (TOGGLE_FLIGHT.consumeClick()) {
            if (!flightAllowed) {
                showServerOptInRequiredMessage();
                continue;
            }

            flyModEnabled = !flyModEnabled;
            statusMessageKey = flyModEnabled ? "hud.creativeflymod.fly_mod_armed" : "hud.creativeflymod.fly_mod_disarmed";
            statusMessageText = "";
            statusMessageShownAtMs = System.currentTimeMillis();

            if (!flyModEnabled) {
                creativeFlightEnabled = false;
                lastJumpTapTimeMs = 0L;
            }
        }

        while (SPEED_DOWN.consumeClick()) {
            currentFlightSpeed = Mth.clamp(currentFlightSpeed - SPEED_STEP, MIN_FLIGHT_SPEED, MAX_FLIGHT_SPEED);
            hasStoredSpeedBeforeReset = false;
            FlyProfileManager.setProfileSpeed(FlyProfileManager.getSelectedProfileIndex(), currentFlightSpeed);
            FlyProfileManager.save();
        }

        while (SPEED_UP.consumeClick()) {
            currentFlightSpeed = Mth.clamp(currentFlightSpeed + SPEED_STEP, MIN_FLIGHT_SPEED, MAX_FLIGHT_SPEED);
            hasStoredSpeedBeforeReset = false;
            FlyProfileManager.setProfileSpeed(FlyProfileManager.getSelectedProfileIndex(), currentFlightSpeed);
            FlyProfileManager.save();
        }

        while (SPEED_RESET.consumeClick()) {
            boolean atDefaultSpeed = Math.abs(currentFlightSpeed - DEFAULT_FLIGHT_SPEED) <= SPEED_COMPARE_EPSILON;
            if (hasStoredSpeedBeforeReset && atDefaultSpeed) {
                currentFlightSpeed = Mth.clamp(storedSpeedBeforeReset, MIN_FLIGHT_SPEED, MAX_FLIGHT_SPEED);
                hasStoredSpeedBeforeReset = false;
            } else {
                if (!atDefaultSpeed) {
                    storedSpeedBeforeReset = currentFlightSpeed;
                    hasStoredSpeedBeforeReset = true;
                }
                currentFlightSpeed = DEFAULT_FLIGHT_SPEED;
            }

            FlyProfileManager.setProfileSpeed(FlyProfileManager.getSelectedProfileIndex(), currentFlightSpeed);
            FlyProfileManager.save();
        }

        while (OPEN_PROFILES.consumeClick()) {
            minecraft.setScreen(new CreativeFlyProfilesScreen(minecraft.screen));
        }

        while (PROFILE_1.consumeClick()) {
            activateProfile(0);
        }

        while (PROFILE_2.consumeClick()) {
            activateProfile(1);
        }

        while (PROFILE_3.consumeClick()) {
            activateProfile(2);
        }

        while (CYCLE_PROFILE_FORWARD.consumeClick()) {
            cycleProfile(1);
        }

        while (CYCLE_PROFILE_BACKWARD.consumeClick()) {
            cycleProfile(-1);
        }

        applyFlyHackState(minecraft, player);
    }

    private static void handleJumpDoubleTapToggle(Minecraft minecraft, boolean flightAllowed) {
        if (minecraft.screen != null) {
            jumpKeyWasDown = minecraft.options.keyJump.isDown();
            return;
        }

        boolean jumpDown = minecraft.options.keyJump.isDown();

        if (!flightAllowed) {
            if (jumpDown && !jumpKeyWasDown) {
                long now = System.currentTimeMillis();
                if (now - lastJumpTapTimeMs <= DOUBLE_TAP_WINDOW_MS) {
                    showServerOptInRequiredMessage();
                    lastJumpTapTimeMs = 0L;
                    jumpKeyWasDown = jumpDown;
                    return;
                }
                lastJumpTapTimeMs = now;
            }

            jumpKeyWasDown = jumpDown;
            return;
        }

        if (!flyModEnabled) {
            jumpKeyWasDown = jumpDown;
            lastJumpTapTimeMs = 0L;
            return;
        }

        if (jumpDown && !jumpKeyWasDown) {
            long now = System.currentTimeMillis();
            if (now - lastJumpTapTimeMs <= DOUBLE_TAP_WINDOW_MS) {
                creativeFlightEnabled = !creativeFlightEnabled;
                lastJumpTapTimeMs = 0L;
            } else {
                lastJumpTapTimeMs = now;
            }
        }

        jumpKeyWasDown = jumpDown;
    }

    static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null) {
            return;
        }

        boolean showStatusMessage = (!statusMessageKey.isEmpty() || !statusMessageText.isEmpty())
                && (System.currentTimeMillis() - statusMessageShownAtMs) <= STATUS_MESSAGE_DURATION_MS;

        int speedTextY = 6;
        if (showStatusMessage) {
            String statusText = !statusMessageText.isEmpty()
                    ? statusMessageText
                    : net.minecraft.network.chat.Component.translatable(statusMessageKey).getString();
            event.getGuiGraphics().drawString(minecraft.font, statusText, 6, 6, 0xFFFFFF, true);
            speedTextY = 16;
        }

        if (creativeFlightEnabled) {
            String speedPercent = String.format(Locale.ROOT, "%.0f%%", (currentFlightSpeed / DEFAULT_FLIGHT_SPEED) * 100.0F);
            String text = net.minecraft.network.chat.Component.translatable("hud.creativeflymod.flight_speed", speedPercent).getString();
            event.getGuiGraphics().drawString(minecraft.font, text, 6, speedTextY, 0xFFFFFF, true);
        }
    }

    private static void applyFlyHackState(Minecraft minecraft, LocalPlayer player) {
        if (!creativeFlightEnabled) {
            if (player.isNoGravity()) {
                player.setNoGravity(false);
            }
            return;
        }

        if (!player.isNoGravity()) {
            player.setNoGravity(true);
            player.setPos(player.getX(), player.getY() + 0.1D, player.getZ());
        }

        boolean jump = minecraft.options.keyJump.isDown();
        boolean sneak = minecraft.options.keyShift.isDown();
        boolean forward = minecraft.options.keyUp.isDown();
        boolean back = minecraft.options.keyDown.isDown();
        boolean left = minecraft.options.keyLeft.isDown();
        boolean right = minecraft.options.keyRight.isDown();
        boolean sprint = minecraft.options.keySprint.isDown();

        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        Vec3 upDirection = Vec3.directionFromRotation(-90.0F, 0.0F);
        Vec3 downDirection = Vec3.directionFromRotation(90.0F, 0.0F);
        Vec3 forwardDirection = Vec3.directionFromRotation(0.0F, player.getYRot());
        Vec3 strafeDirection = new Vec3(-forwardDirection.z, 0.0D, forwardDirection.x);
        Vec3 movement = Vec3.ZERO;

        if (back) {
            movement = movement.subtract(forwardDirection.x, 0.0D, forwardDirection.z);
        }

        if (right) {
            movement = movement.add(strafeDirection);
        }

        if (left) {
            movement = movement.subtract(strafeDirection);
        }

        if (forward) {
            movement = movement.add(forwardDirection.x, 0.0D, forwardDirection.z);
        }

        if (jump) {
            movement = movement.add(upDirection);
        }

        if (sneak) {
            movement = movement.add(downDirection);
        }

        if (movement.lengthSqr() > 0.0D) {
            double speed = currentFlightSpeed * BASE_DISTANCE_PER_TICK;
            if (sprint) {
                speed *= SPRINT_MULTIPLIER;
            }

            Vec3 step = movement.normalize().scale(speed);
            x += step.x;
            y += step.y;
            z += step.z;
        }

        player.setPos(x, y, z);
        player.setDeltaMovement(Vec3.ZERO);
    }

    static void refreshSpeedFromSelectedProfile() {
        currentFlightSpeed = Mth.clamp(FlyProfileManager.getSelectedProfileSpeed(), MIN_FLIGHT_SPEED, MAX_FLIGHT_SPEED);
        hasStoredSpeedBeforeReset = false;
    }

    private static boolean isFlightAllowed(Minecraft minecraft) {
        return minecraft.hasSingleplayerServer() || ServerOptInState.isOptedIn();
    }

        private static void showServerOptInRequiredMessage() {
            statusMessageKey = "hud.creativeflymod.server_opt_in_required";
            statusMessageText = "";
            statusMessageShownAtMs = System.currentTimeMillis();
        }
    private static void activateProfile(int profileIndex) {
        FlyProfileManager.setSelectedProfileIndex(profileIndex);
        refreshSpeedFromSelectedProfile();
        showProfileSwitchedMessage(profileIndex);
        FlyProfileManager.save();
    }

    private static void showProfileSwitchedMessage(int profileIndex) {
        FlyProfileManager.FlyProfile profile = FlyProfileManager.getProfile(profileIndex);
        int speedPercent = Math.round((profile.speed() / DEFAULT_FLIGHT_SPEED) * 100.0F);
        statusMessageText = net.minecraft.network.chat.Component
                .translatable("hud.creativeflymod.profile_switched", profile.name(), speedPercent)
                .getString();
        statusMessageKey = "";
        statusMessageShownAtMs = System.currentTimeMillis();
    }

    private static void cycleProfile(int direction) {
        int current = FlyProfileManager.getSelectedProfileIndex();
        int wrapped = Math.floorMod(current + direction, FlyProfileManager.PROFILE_COUNT);
        activateProfile(wrapped);
    }
}
