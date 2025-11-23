package com.example.bowhorsecontrol.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public class AbstractHorseEntityMixin {
    
    /**
     * Modifies the movement input in travel() to prevent horses from following camera direction
     * when the player is drawing a bow, allowing free camera movement.
     */
    @ModifyVariable(
        method = "travel",
        at = @At("HEAD"),
        ordinal = 0,
        argsOnly = true
    )
    private Vec3d modifyTravelInput(Vec3d movementInput) {
        LivingEntity entity = (LivingEntity) (Object) this;
        
        // Only apply to horses
        if (entity instanceof AbstractHorseEntity horse) {
            // Check if the horse has a passenger
            if (horse.getFirstPassenger() instanceof PlayerEntity player) {
                // Check if the player is using a bow (drawing)
                if (player.isUsingItem() && player.getActiveItem().getItem() instanceof net.minecraft.item.BowItem) {
                    // Return only the Y component to prevent horizontal rotation-based movement
                    // This allows free camera movement while drawing the bow
                    // The horse will still respond to explicit movement key presses (WASD)
                    return new Vec3d(0, movementInput.y, 0);
                }
            }
        }
        
        // Normal behavior: return the original movement input
        return movementInput;
    }
}

