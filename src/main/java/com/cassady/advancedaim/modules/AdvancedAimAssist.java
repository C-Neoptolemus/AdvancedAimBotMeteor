package com.cassady.advancedaim.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AdvancedAimAssist extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> fov = sgGeneral.add(new DoubleSetting.Builder()
        .name("fov")
        .description("Max FOV to target.")
        .defaultValue(90.0)
        .min(30.0)
        .max(180.0)
        .sliderRange(30, 180)
        .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("Max target range.")
        .defaultValue(4.5)
        .min(3.0)
        .max(6.0)
        .sliderRange(3, 6)
        .build()
    );

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed")
        .description("Rotation speed multiplier.")
        .defaultValue(1.0)
        .min(0.1)
        .max(5.0)
        .sliderRange(0.1, 5)
        .build()
    );

    private final Setting<Double> hitboxPercent = sgGeneral.add(new DoubleSetting.Builder()
        .name("hitbox-percent")
        .description("Middle % of hitbox for random aim (100=full).")
        .defaultValue(50.0)
        .min(10.0)
        .max(100.0)
        .sliderRange(10, 100)
        .build()
    );

    private final Setting<Keybind> disableKey = sgGeneral.add(new KeybindSetting.Builder()
        .name("disable-key")
        .description("Hold to temp disable aim assist.")
        .defaultValue(Keybind.none())
        .build()
    );

    private final Setting<Boolean> autoTrigger = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-trigger")
        .description("Auto attack on aim complete.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> predict = sgGeneral.add(new BoolSetting.Builder()
        .name("predict")
        .description("Predict target velocity.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> stutterChance = sgGeneral.add(new DoubleSetting.Builder()
        .name("stutter-chance")
        .description("Chance to micro-stutter (skip tick).")
        .defaultValue(0.05)
        .min(0.0)
        .max(0.2)
        .sliderRange(0, 0.2)
        .build()
    );

    private final Setting<Double> jitterAmount = sgGeneral.add(new DoubleSetting.Builder()
        .name("jitter-amount")
        .description("Fake micro jitter degrees (less closer).")
        .defaultValue(1.5)
        .min(0.0)
        .max(5.0)
        .sliderRange(0, 5)
        .build()
    );

    private PlayerEntity target;
    private int snapTicks = 0;

    public AdvancedAimAssist() {
        super(Categories.Combat, "advanced-aim-assist", "Legit aim: random hitbox, stutters/jitter, stronger closer, friends ignore, players only.");
    }

    @Override
    public void onActivate() {
        target = null;
        snapTicks = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (disableKey.get().isPressed()) {
            snapTicks = 0;
            return;
        }

        if (snapTicks > 0) {
            snapTicks--;
            return;
        }

        target = findTarget();
        if (target == null) {
            snapTicks = 2;
            return;
        }

        if (Math.random() < stutterChance.get()) return;

        Vec3d aimPos = getRandomHitboxPos(target);

        if (predict.get()) {
            aimPos = aimPos.add(target.getVelocity().multiply(0.8));
        }

        double yaw = Rotations.getYaw(aimPos);
        double pitch = Rotations.getPitch(aimPos);

        double distFactor = Math.max(0.1, target.distanceTo(mc.player) / range.get());

        double jitterYaw = (Math.random() - 0.5) * jitterAmount.get() * distFactor;
        double jitterPitch = (Math.random() - 0.5) * jitterAmount.get() * distFactor * 0.6;

        yaw += jitterYaw;
        pitch += jitterPitch;
        pitch = MathHelper.clamp(pitch, -90, 90);

        mc.player.setYaw((float) yaw);
        mc.player.setPitch((float) pitch);

        if (autoTrigger.get() && mc.player.distanceTo(target) < range.get()) {
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    private PlayerEntity findTarget() {
        List<PlayerEntity> targets = new ArrayList<>();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || Friends.get().isFriend(player)) continue;

            double dist = player.distanceTo(mc.player);
            if (dist > range.get()) continue;

            double yawDiff = MathHelper.wrapDegrees(Rotations.getYaw(player.getEyePos()) - mc.player.getYaw());
            if (Math.abs(yawDiff) > fov.get() / 2) continue;

            targets.add(player);
        }

        targets.sort(Comparator.comparingDouble(e -> e.distanceTo(mc.player)));
        return targets.isEmpty() ? null : targets.get(0);
    }

    private Vec3d getRandomHitboxPos(PlayerEntity player) {
        Box bb = player.getBoundingBox();
        double percent = hitboxPercent.get() / 100.0;
        double halfW = bb.getLengthX() * percent / 2;
        double halfH = bb.getLengthY() * percent / 2;
        double halfD = bb.getLengthZ() * percent / 2;

        double cx = bb.getCenter().x + (Math.random() - 0.5) * halfW * 2;
        double cy = bb.minY + 1.2 + (Math.random() - 0.5) * halfH * 1.2;
        double cz = bb.getCenter().z + (Math.random() - 0.5) * halfD * 2;

        return new Vec3d(cx, cy, cz);
    }
}
