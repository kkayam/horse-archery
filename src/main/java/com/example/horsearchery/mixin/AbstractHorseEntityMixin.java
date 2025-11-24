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
            if (horse.getFirstPassenger() instanceof PlayerEntity player) {
                // Check if the player is drawing a bow
                boolean isDrawingBow = player.isUsingItem() && 
                                      player.getActiveItem().getItem() instanceof BowItem;
                
                if (isDrawingBow) {
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
                    // Set player's yaw to match the horse's initial yaw before resetting
                    if (!Float.isNaN(bowhorsecontrol$initialYaw)) {
                        player.setYaw(bowhorsecontrol$initialYaw);
                        player.prevYaw = bowhorsecontrol$initialYaw;
                        // Reset after setting player yaw
                        bowhorsecontrol$initialYaw = Float.NaN;
                    }
                }
            } else {
                // Reset when player dismounts
                bowhorsecontrol$initialYaw = Float.NaN;
            }
        }
        
        // Return the original movement input for non-horses or when not drawing a bow
        return movementInput;
    }
    
    /**
     * Prevents the horse from rotating its yaw based on passenger input.
     * This only applies when drawing a bow and runs at the end of the travel method.
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
                
                if (isDrawingBow && !Float.isNaN(bowhorsecontrol$initialYaw)) {
                    // Restore the initial yaw to prevent rotation
                    horse.setYaw(bowhorsecontrol$initialYaw);
                    horse.prevYaw = bowhorsecontrol$initialYaw;
                }
            }
        }
    }
}

