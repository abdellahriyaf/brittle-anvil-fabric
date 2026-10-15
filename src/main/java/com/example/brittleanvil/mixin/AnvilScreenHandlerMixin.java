package com.example.brittleanvil.mixin;

import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ForgingScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla: every anvil use has a 12% chance to degrade the anvil
 * (anvil -> chipped -> damaged -> destroyed).
 *
 * This mixin makes a *damaged* anvil (the "very damaged" one) break
 * with 100% certainty the next time something is taken from its output slot.
 */
@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin extends ForgingScreenHandler {

    /** Was the anvil already a damaged anvil BEFORE this use started? */
    @Unique
    private boolean brittleanvil$wasDamaged;

    // Required because the superclass has no default constructor. Mixin discards this.
    public AnvilScreenHandlerMixin(ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(type, syncId, playerInventory, context);
    }

    @Inject(method = "onTakeOutput", at = @At("HEAD"))
    private void brittleanvil$rememberState(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        // Creative players never damage anvils in vanilla, so keep that behaviour.
        // On the client the context is EMPTY, so this safely falls back to false.
        this.brittleanvil$wasDamaged = !player.getAbilities().creativeMode
                && this.context.get((world, pos) -> world.getBlockState(pos).isOf(Blocks.DAMAGED_ANVIL), false);
    }

    @Inject(method = "onTakeOutput", at = @At("TAIL"))
    private void brittleanvil$breakDamagedAnvil(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        if (!this.brittleanvil$wasDamaged) {
            return;
        }
        this.brittleanvil$wasDamaged = false;

        this.context.run((world, pos) -> {
            // If vanilla's 12% roll already destroyed it, the block is gone and we do nothing.
            if (world.getBlockState(pos).isOf(Blocks.DAMAGED_ANVIL)) {
                world.removeBlock(pos, false);
                world.syncWorldEvent(1029, pos, 0); // 1029 = anvil destroyed sound/effect
            }
        });
    }
}
