package net.gasharaujo.easyvillageraddon;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

public final class UiSnapshot {
    public int menuId, kind, level = 1, xp, targetLevel = 1, bribeTicks, reputation, reputationLimit, attempts;
    public int revision, offset, catalogSize;
    public int targetsRevision;
    public boolean matchAll;
    public List<TradeTarget> targets = List.of();
    public boolean enabled, hasVillager, hasWorkstation, canChooseInitial, catalogComplete;
    public String name = "", profession = "none", status = "";
    public ItemStack target = ItemStack.EMPTY;
    public List<OfferView> entries = List.of();

    public void write(FriendlyByteBuf b) {
        b.writeVarInt(menuId); b.writeVarInt(kind); b.writeVarInt(level); b.writeVarInt(xp);
        b.writeVarInt(targetLevel); b.writeVarInt(bribeTicks); b.writeVarInt(reputation);
        b.writeVarInt(reputationLimit); b.writeVarInt(attempts); b.writeVarInt(revision);
        b.writeVarInt(offset); b.writeVarInt(catalogSize);
        b.writeBoolean(enabled); b.writeBoolean(hasVillager); b.writeBoolean(hasWorkstation);
        b.writeBoolean(canChooseInitial); b.writeBoolean(catalogComplete);
        b.writeUtf(name, 256); b.writeUtf(profession, 256); b.writeUtf(status, 256);
        UiNetwork.writeStack(b, target);
        b.writeBoolean(matchAll); b.writeVarInt(targetsRevision); b.writeVarInt(targets.size());
        targets.forEach(goal -> goal.write(b));
        b.writeVarInt(entries.size());
        entries.forEach(entry -> entry.write(b));
    }
    public static UiSnapshot read(FriendlyByteBuf b) {
        UiSnapshot s = new UiSnapshot();
        s.menuId = b.readVarInt(); s.kind = b.readVarInt(); s.level = b.readVarInt(); s.xp = b.readVarInt();
        s.targetLevel = b.readVarInt(); s.bribeTicks = b.readVarInt(); s.reputation = b.readVarInt();
        s.reputationLimit = b.readVarInt(); s.attempts = b.readVarInt(); s.revision = b.readVarInt();
        s.offset = b.readVarInt(); s.catalogSize = b.readVarInt();
        s.enabled = b.readBoolean(); s.hasVillager = b.readBoolean(); s.hasWorkstation = b.readBoolean();
        s.canChooseInitial = b.readBoolean(); s.catalogComplete = b.readBoolean();
        s.name = b.readUtf(256); s.profession = b.readUtf(256); s.status = b.readUtf(256);
        s.target = UiNetwork.readStack(b);
        s.matchAll = b.readBoolean(); s.targetsRevision = b.readVarInt();
        int goals = b.readVarInt();
        if (goals < 0 || goals > TradeTarget.MAX_TARGETS) throw new IllegalArgumentException("Invalid target count");
        List<TradeTarget> targets = new ArrayList<>(goals);
        for (int i = 0; i < goals; i++) targets.add(TradeTarget.read(b));
        s.targets = targets;
        int size = b.readVarInt();
        if (size < 0 || size > 48) throw new IllegalArgumentException("Invalid catalog batch");
        s.entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) s.entries.add(OfferView.read(b));
        return s;
    }
}
