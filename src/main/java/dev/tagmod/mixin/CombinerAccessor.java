package dev.tagmod.mixin;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(ItemCombinerMenu.class)
public interface CombinerAccessor {@Accessor("player") Player tagmod$player();}
