package com.example.horsearchery.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class AbstractHorseEntityMixin {
    
    @Unique
    private float bowhorsecontrol$initialYaw = Float.NaN;
    
    @Unique
    private float bowhorsecontrol$targetYaw = Float.NaN;
    
    @Unique
    private int bowhorsecontrol$interpolationStartTick = -1;
    
    @Unique
    private float bowhorsecontrol$initialAngleDiff = Float.NaN;
    
    @Unique
    private static final float ROTATION_SPEED = 0.15f; // Rotation speed for smooth yaw transition
    
    @Unique
    private static final float MAX_INTERPOLATION_TIME_MULTIPLIER = 1.0f; // Allow 2x the expected time before forcing snap
    
    /**
     * Transforms the movement input to be relative to the horse's locked yaw direction
     * instead of the player's camera direction, but only when drawing a bow.
     * Stores the initial yaw when the bow draw starts.
     */
    @ModifyVariable(
        method = "travel",
        at = @At("HEAD"),
        ordinal = 0,
        argsOnly = true
    )
    private Vec3d transformMovementInput(Vec3d movementInput) {
        LivingEntity entity = (LivingEntity) (Object) this;
        
        // Only apply to horses
        if (entity instanceof AbstractHorseEntity horse) {
            // Check if the horse has a passenger
            boolean isMoving = movementInput.x != 0 || movementInput.z != 0;
            if (isMoving && horse.getFirstPassenger() instanceof PlayerEntity player) {
                // Check if the horse is moving
                // Check if the player is drawing a bow
                boolean isDrawingBow = player.isUsingItem() && 
                                      player.getActiveItem().getItem() instanceof BowItem;
                
                if (isDrawingBow || !Float.isNaN(bowhorsecontrol$targetYaw)) {
                    if (isDrawingBow) {
                        bowhorsecontrol$targetYaw = Float.NaN;
                        bowhorsecontrol$interpolationStartTick = -1;
                        bowhorsecontrol$initialAngleDiff = Float.NaN;
                    }

                    // Store the initial yaw when bow draw starts (if not already stored)
                    if (Float.isNaN(bowhorsecontrol$initialYaw)) {
                        bowhorsecontrol$initialYaw = horse.getYaw();
                    }
                    
                    // Transform movement input from player's camera direction to horse's locked direction
                    // Calculate the yaw difference between player's look direction and horse's locked direction
                    float playerYaw = player.getYaw();
                    float yawDifference = bowhorsecontrol$initialYaw - playerYaw;
                    
                    // Convert to radians
                    double yawRad = Math.toRadians(yawDifference);
                    double cos = Math.cos(yawRad);
                    double sin = Math.sin(yawRad);
                    
                    // Rotate the movement vector to be relative to the horse's locked yaw
                    // This transforms the X and Z components from player-relative to horse-relative
                    double newX = movementInput.x * cos - movementInput.z * sin;
                    double newZ = movementInput.x * sin + movementInput.z * cos;
                    
                    return new Vec3d(newX, movementInput.y, newZ);
                } else {
                    // When bow is released (arrow shot or stopped drawing)
                    // Start smooth rotation towards the horse's initial yaw
                    if (!Float.isNaN(bowhorsecontrol$initialYaw)) {
                        bowhorsecontrol$targetYaw = bowhorsecontrol$initialYaw;
                        // Calculate initial angle difference for timeout calculation
                        float currentYaw = player.getYaw();
                        float initialDiff = bowhorsecontrol$initialYaw - currentYaw;
                        // Normalize to -180 to 180 range
                        while (initialDiff > 180.0f) initialDiff -= 360.0f;
                        while (initialDiff < -180.0f) initialDiff += 360.0f;
                        bowhorsecontrol$initialAngleDiff = Math.abs(initialDiff);
                        // Start tracking interpolation time (using entity age as tick counter)
                        bowhorsecontrol$interpolationStartTick = entity.age;
                        // Don't reset initialYaw yet - let the rotation complete first
                    }
                }
            } else {
                // Reset when player dismounts
                bowhorsecontrol$initialYaw = Float.NaN;
                bowhorsecontrol$targetYaw = Float.NaN;
                bowhorsecontrol$interpolationStartTick = -1;
                bowhorsecontrol$initialAngleDiff = Float.NaN;
            }
        }
        
        // Return the original movement input for non-horses or when not drawing a bow
        return movementInput;
    }
    
    /**
     * Prevents the horse from rotating its yaw based on passenger input.
     * This only applies when drawing a bow and runs at the end of the travel method.
     * Also handles smooth rotation of player's yaw when bow is released.
     */
    @Inject(
        method = "travel",
        at = @At("RETURN")
    )
    private void preventYawRotation(Vec3d movementInput, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        
        if (entity instanceof AbstractHorseEntity horse) {
            if (horse.getFirstPassenger() instanceof PlayerEntity player) {
                // Only prevent rotation when drawing a bow
                boolean isDrawingBow = player.isUsingItem() && 
                                      player.getActiveItem().getItem() instanceof BowItem;
                
                if (!Float.isNaN(bowhorsecontrol$initialYaw)) {
                    // Restore the initial yaw to prevent rotation
                    horse.setYaw(bowhorsecontrol$initialYaw);
                    horse.prevYaw = bowhorsecontrol$initialYaw;
                }
                
                // Handle smooth rotation of player's yaw when bow is released
                if (!Float.isNaN(bowhorsecontrol$targetYaw)) {
                    float currentYaw = player.getYaw();
                    float targetYaw = bowhorsecontrol$targetYaw;
                    
                    // Calculate the shortest rotation path (handle 360-degree wrap-around)
                    float yawDiff = targetYaw - currentYaw;
                    // Normalize to -180 to 180 range
                    while (yawDiff > 180.0f) yawDiff -= 360.0f;
                    while (yawDiff < -180.0f) yawDiff += 360.0f;
                    
                    // Check if we should force snap due to timeout
                    boolean shouldForceSnap = false;
                    if (bowhorsecontrol$interpolationStartTick >= 0 && !Float.isNaN(bowhorsecontrol$initialAngleDiff)) {
                        int elapsedTicks = entity.age - bowhorsecontrol$interpolationStartTick;
                        // Calculate expected ticks using exponential decay formula
                        // With exponential interpolation: remaining = initial * (1 - ROTATION_SPEED)^n
                        // To reach threshold (10 degrees): threshold = initial * (1 - ROTATION_SPEED)^n
                        // Solving for n: n = log(threshold/initial) / log(1 - ROTATION_SPEED)
                        float threshold = 10.0f;
                        if (bowhorsecontrol$initialAngleDiff > threshold) {
                            double expectedTicks = Math.log(threshold / bowhorsecontrol$initialAngleDiff) / Math.log(1.0 - ROTATION_SPEED);
                            float maxTicks = (float) (expectedTicks * MAX_INTERPOLATION_TIME_MULTIPLIER);
                            
                            if (elapsedTicks > maxTicks) {
                                shouldForceSnap = true;
                            }
                        }
                    }
                    
                    // If we're close enough or timeout exceeded, snap to target and reset
                    if (Math.abs(yawDiff) < 10.0f || shouldForceSnap) {
                        player.setYaw(targetYaw);
                        player.prevYaw = targetYaw;
                        bowhorsecontrol$targetYaw = Float.NaN;
                        bowhorsecontrol$initialYaw = Float.NaN;
                        bowhorsecontrol$interpolationStartTick = -1;
                        bowhorsecontrol$initialAngleDiff = Float.NaN;
                    } else {
                        // Interpolate towards target
                        float newYaw = currentYaw + yawDiff * ROTATION_SPEED;
                        player.setYaw(newYaw);
                        player.prevYaw = newYaw;
                    }
                }
            }
        }
    }
}

