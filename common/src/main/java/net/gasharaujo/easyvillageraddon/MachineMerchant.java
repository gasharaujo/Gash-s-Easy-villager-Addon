package net.gasharaujo.easyvillageraddon;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/** The vanilla merchant inventory trades directly with the machine's current villager. */
final class MachineMerchant implements Merchant {
    private final MachineEntity machine;
    private final Player player;
    private final MerchantOffers empty = new MerchantOffers();
    MachineMerchant(MachineEntity machine, Player player) { this.machine = machine; this.player = player; }
    @Override public Player getTradingPlayer() { return player; }
    @Override public void setTradingPlayer(Player player) { }
    @Override public MerchantOffers getOffers() {
        var villager = machine.getVillagerEntity();
        return villager == null || villager.isBaby() ? empty : villager.getOffers();
    }
    @Override public void overrideOffers(MerchantOffers offers) { }
    @Override public void notifyTrade(MerchantOffer offer) {
        var villager = machine.getVillagerEntity();
        if (villager == null) return;
        villager.setTradingPlayer(player);
        try { villager.notifyTrade(offer); }
        finally { villager.setTradingPlayer(null); }
        machine.saveVillagerEntity(); machine.setChanged();
    }
    @Override public void notifyTradeUpdated(ItemStack stack) { }
    @Override public int getVillagerXp() {
        var v = machine.getVillagerEntity(); return v == null ? 0 : v.getVillagerXp();
    }
    @Override public void overrideXp(int xp) {
        // Server-side Villager.notifyTrade already grants its XP. This method is
        // a client synchronization hook, just as with the vanilla server merchant.
    }
    @Override public boolean showProgressBar() { return true; }
    @Override public SoundEvent getNotifyTradeSound() { return SoundEvents.VILLAGER_YES; }
    @Override public boolean isClientSide() { return false; }
    public boolean stillValid(Player player) { return machine.accessible(player); }
}
