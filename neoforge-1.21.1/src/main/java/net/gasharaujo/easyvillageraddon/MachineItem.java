package net.gasharaujo.easyvillageraddon;

public final class MachineItem extends de.maxhenkel.easyvillagers.corelib.client.CustomRendererBlockItem {
    MachineItem(MachineBlock block, Properties properties) { super(block, properties); }
    @net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
    @Override public de.maxhenkel.easyvillagers.corelib.client.ItemRenderer createItemRenderer() { return new MachineItemRenderer(this::getBlock); }
}
