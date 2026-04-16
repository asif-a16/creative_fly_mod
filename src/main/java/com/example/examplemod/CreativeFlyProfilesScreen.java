package com.example.examplemod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class CreativeFlyProfilesScreen extends Screen {
    private final Screen parent;

    private EditBox profileNameBox;
    private SpeedSlider speedSlider;
    private Button autoArmToggleButton;
    private Button profile1Button;
    private Button profile2Button;
    private Button profile3Button;
    private int selectedProfileIndex;

    protected CreativeFlyProfilesScreen(Screen parent) {
        super(Component.translatable("screen.creativeflymod.profiles.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        selectedProfileIndex = FlyProfileManager.getSelectedProfileIndex();

        int centerX = width / 2;
        int y = 46;
        int buttonWidth = 98;

        profile1Button = addRenderableWidget(Button.builder(Component.empty(), button -> selectProfile(0))
                .bounds(centerX - 154, y, buttonWidth, 20)
                .build());

        profile2Button = addRenderableWidget(Button.builder(Component.empty(), button -> selectProfile(1))
                .bounds(centerX - 50, y, buttonWidth, 20)
                .build());

        profile3Button = addRenderableWidget(Button.builder(Component.empty(), button -> selectProfile(2))
                .bounds(centerX + 54, y, buttonWidth, 20)
                .build());

        profileNameBox = addRenderableWidget(new EditBox(font, centerX - 100, 92, 200, 20,
                Component.translatable("screen.creativeflymod.profiles.name")));
        profileNameBox.setMaxLength(32);
        profileNameBox.setResponder(this::onNameChanged);

        speedSlider = addRenderableWidget(new SpeedSlider(centerX - 100, 126, 200, 20));

        autoArmToggleButton = addRenderableWidget(Button.builder(Component.empty(),
            button -> toggleAutoArmOnJoin())
            .bounds(centerX - 100, 158, 200, 20)
            .build());
        updateAutoArmButtonLabel();

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onDone())
                .bounds(centerX - 100, height - 32, 200, 20)
                .build());

        refreshFromSelectedProfile();
        setInitialFocus(profileNameBox);
    }

    private void selectProfile(int index) {
        selectedProfileIndex = Mth.clamp(index, 0, FlyProfileManager.PROFILE_COUNT - 1);
        FlyProfileManager.setSelectedProfileIndex(selectedProfileIndex);
        refreshFromSelectedProfile();
        CreativeFlyModClient.refreshSpeedFromSelectedProfile();
        FlyProfileManager.save();
    }

    private void onNameChanged(String value) {
        FlyProfileManager.setProfileName(selectedProfileIndex, value);
        updateProfileButtonLabels();
        FlyProfileManager.save();
    }

    private void refreshFromSelectedProfile() {
        FlyProfileManager.FlyProfile profile = FlyProfileManager.getProfile(selectedProfileIndex);
        profileNameBox.setValue(profile.name());
        speedSlider.setSpeed(profile.speed());
        updateProfileButtonLabels();
    }

    private void updateProfileButtonLabels() {
        profile1Button.setMessage(createProfileButtonLabel(0));
        profile2Button.setMessage(createProfileButtonLabel(1));
        profile3Button.setMessage(createProfileButtonLabel(2));
    }

    private Component createProfileButtonLabel(int index) {
        FlyProfileManager.FlyProfile profile = FlyProfileManager.getProfile(index);
        String prefix = index == selectedProfileIndex ? "> " : "";
        return Component.literal(prefix + (index + 1) + ": " + profile.name());
    }

    private void onDone() {
        FlyProfileManager.save();
        Minecraft.getInstance().setScreen(parent);
    }

    private void toggleAutoArmOnJoin() {
        FlyProfileManager.setAutoArmOnJoin(!FlyProfileManager.isAutoArmOnJoin());
        updateAutoArmButtonLabel();
        FlyProfileManager.save();
    }

    private void updateAutoArmButtonLabel() {
        String state = FlyProfileManager.isAutoArmOnJoin() ? "[x]" : "[ ]";
        autoArmToggleButton.setMessage(Component.translatable("screen.creativeflymod.profiles.auto_arm", state));
    }

    @Override
    public void onClose() {
        FlyProfileManager.save();
        super.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = width / 2;
        guiGraphics.drawCenteredString(font, title, centerX, 16, 0xFFFFFF);
        guiGraphics.drawString(font, Component.translatable("screen.creativeflymod.profiles.profile_label"), centerX - 154, 34, 0xA0A0A0);
        guiGraphics.drawString(font, Component.translatable("screen.creativeflymod.profiles.name"), centerX - 100, 80, 0xA0A0A0);
        guiGraphics.drawString(font, Component.translatable("screen.creativeflymod.profiles.speed"), centerX - 100, 114, 0xA0A0A0);
        guiGraphics.drawString(font, Component.translatable("screen.creativeflymod.profiles.auto_arm_hint"), centerX - 100, 146, 0xA0A0A0);
    }

    private final class SpeedSlider extends AbstractSliderButton {
        private SpeedSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), 0.0D);
            updateMessage();
        }

        private void setSpeed(float speed) {
            double normalized = (speed - FlyProfileManager.MIN_FLIGHT_SPEED)
                    / (FlyProfileManager.MAX_FLIGHT_SPEED - FlyProfileManager.MIN_FLIGHT_SPEED);
            value = Mth.clamp(normalized, 0.0D, 1.0D);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            float speed = getCurrentSpeed();
            int percent = Math.round((speed / FlyProfileManager.DEFAULT_FLIGHT_SPEED) * 100.0F);
            setMessage(Component.translatable("screen.creativeflymod.profiles.speed_value", percent));
        }

        @Override
        protected void applyValue() {
            float speed = getCurrentSpeed();
            FlyProfileManager.setProfileSpeed(selectedProfileIndex, speed);
            CreativeFlyModClient.refreshSpeedFromSelectedProfile();
            FlyProfileManager.save();
        }

        private float getCurrentSpeed() {
            return (float) (FlyProfileManager.MIN_FLIGHT_SPEED
                    + value * (FlyProfileManager.MAX_FLIGHT_SPEED - FlyProfileManager.MIN_FLIGHT_SPEED));
        }
    }
}
