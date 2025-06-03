package com.dugsiii.tpamod.events;

import com.dugsiii.tpamod.TPAMod;
import com.dugsiii.tpamod.manager.TPAManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = TPAMod.MOD_ID)
public class TPAEventHandler {
    
    private static int tickCounter = 0;
    private static final int CLEANUP_INTERVAL = 1200; // Clean up every 60 seconds (20 ticks/sec * 60)
    
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TPAManager manager = TPAMod.getTPAManager();
            if (manager != null) {
                manager.onPlayerLogout(player.getUUID());
            }
        }
    }
    
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        if (tickCounter >= CLEANUP_INTERVAL) {
            TPAManager manager = TPAMod.getTPAManager();
            if (manager != null) {
                manager.cleanupExpiredRequests();
            }
            tickCounter = 0;
        }
    }
}