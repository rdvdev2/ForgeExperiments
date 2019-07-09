package tk.rdvdev2.experiments.common.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;

import static tk.rdvdev2.experiments.Experiments.MODID;

public class MarkerItem extends Item {

    public enum SelectionTarget {
        POS1, POS2
    }

    public static SelectionTarget target = SelectionTarget.POS1;
    public static BlockPos pos1 = null;
    public static BlockPos pos2 = null;

    public MarkerItem() {
        super(new Properties().maxStackSize(1).group(ItemGroup.TOOLS));
        setRegistryName(MODID, "marker");
    }
    @Override
    public ActionResultType onItemUseFirst(ItemStack itemStack, ItemUseContext context) {

        if (context.isPlacerSneaking()) {
            switch (target) {
                case POS1:
                    target = SelectionTarget.POS2;
                    context.getPlayer().sendStatusMessage(new StringTextComponent("Modifiying pos2"), true);
                    break;
                case POS2:
                    target = SelectionTarget.POS1;
                    context.getPlayer().sendStatusMessage(new StringTextComponent("Modifiying pos1"), true);
                    break;
            }
        } else {
            switch (target) {
                case POS1:
                    pos1 = context.getPos();
                    context.getPlayer().sendStatusMessage(new StringTextComponent("Pos1 = "+pos1.toString()), true);
                    break;
                case POS2:
                    pos2 = context.getPos();
                    context.getPlayer().sendStatusMessage(new StringTextComponent("Pos2 = "+pos2.toString()), true);
                    break;
            }
        }

        return ActionResultType.SUCCESS;
    }
}
