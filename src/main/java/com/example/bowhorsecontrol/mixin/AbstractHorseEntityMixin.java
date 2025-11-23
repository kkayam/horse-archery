package com.example.bowhorsecontrol.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
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
     * instead of the player's camera direction, and stores the initial yaw.
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
                // Store the initial yaw when player first mounts (if not already stored)
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
                // Reset when player dismounts
                bowhorsecontrol$initialYaw = Float.NaN;
            }
        }
        
        // Return the original movement input for non-horses
        return movementInput;
    }
    
    /**
     * Prevents the horse from rotating its yaw based on passenger input.
     * This runs at the end of the travel method to restore the initial yaw.
     */
    @Inject(
        method = "travel",
        at = @At("RETURN")
    )
    private void preventYawRotation(Vec3d movementInput, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        
        if (entity instanceof AbstractHorseEntity horse) {
            if (horse.getFirstPassenger() instanceof PlayerEntity) {
                // Restore the initial yaw to prevent rotation
                if (!Float.isNaN(bowhorsecontrol$initialYaw)) {
                    horse.setYaw(bowhorsecontrol$initialYaw);
                    horse.prevYaw = bowhorsecontrol$initialYaw;
                }
            }
        }
    }
}

