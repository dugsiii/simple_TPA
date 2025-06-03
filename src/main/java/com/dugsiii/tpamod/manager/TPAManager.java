package com.dugsiii.tpamod.manager;

import com.dugsiii.tpamod.data.TPARequest;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TPAManager {
    private final Map<UUID, TPARequest> pendingRequests = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastTeleportTime = new ConcurrentHashMap<>();
    private final Set<UUID> toggledPlayers = ConcurrentHashMap.newKeySet();
    
    // Configuration
    private static final long REQUEST_TIMEOUT_MS = 60000; // 60 seconds
    private static final long TELEPORT_COOLDOWN_MS = 5000; // 5 seconds
    private static final double MAX_TELEPORT_DISTANCE = 1000000; // 1 million blocks
    
    public boolean sendTPARequest(ServerPlayer requester, ServerPlayer target, boolean isTPAHere) {
        // Check if target has TPA toggled off
        if (toggledPlayers.contains(target.getUUID())) {
            requester.sendSystemMessage(Component.literal("§c" + target.getName().getString() + " has TPA requests disabled."));
            return false;
        }
        
        // Check if there's already a pending request from this player to target
        UUID requestKey = requester.getUUID();
        if (pendingRequests.containsKey(requestKey)) {
            requester.sendSystemMessage(Component.literal("§cYou already have a pending TPA request. Please wait for it to expire or be answered."));
            return false;
        }
        
        // Check distance for safety (prevent teleporting across extreme distances)
        double distance = requester.position().distanceTo(target.position());
        if (distance > MAX_TELEPORT_DISTANCE) {
            requester.sendSystemMessage(Component.literal("§cTeleport distance is too far for safety reasons."));
            return false;
        }
        
        // Create and store the request
        TPARequest request = new TPARequest(requester, target, isTPAHere);
        pendingRequests.put(requestKey, request);
        
        // Send messages
        if (isTPAHere) {
            requester.sendSystemMessage(Component.literal("§aTPA here request sent to " + target.getName().getString()));
            target.sendSystemMessage(Component.literal("§e" + requester.getName().getString() + " wants you to teleport to them."));
            target.sendSystemMessage(Component.literal("§e/tpaccept to accept or /tpdeny to deny"));
        } else {
            requester.sendSystemMessage(Component.literal("§aTPA request sent to " + target.getName().getString()));
            target.sendSystemMessage(Component.literal("§e" + requester.getName().getString() + " wants to teleport to you."));
            target.sendSystemMessage(Component.literal("§e/tpaccept to accept or /tpdeny to deny"));
        }
        
        return true;
    }
    
    public boolean acceptTPARequest(ServerPlayer accepter) {
        // Find request where accepter is the target
        TPARequest request = findRequestByTarget(accepter.getUUID());
        if (request == null) {
            accepter.sendSystemMessage(Component.literal("§cYou have no pending TPA requests."));
            return false;
        }
        
        // Check if request is expired
        if (request.isExpired(System.currentTimeMillis(), REQUEST_TIMEOUT_MS)) {
            pendingRequests.remove(request.getRequesterId());
            accepter.sendSystemMessage(Component.literal("§cThe TPA request has expired."));
            return false;
        }
        
        // Find the requester player
        ServerPlayer requester = accepter.getServer().getPlayerList().getPlayer(request.getRequesterId());
        if (requester == null) {
            pendingRequests.remove(request.getRequesterId());
            accepter.sendSystemMessage(Component.literal("§cThe player who sent the request is no longer online."));
            return false;
        }
        
        // Check teleport cooldown
        long currentTime = System.currentTimeMillis();
        UUID teleportingPlayerId = request.isTPAHere() ? accepter.getUUID() : requester.getUUID();
        
        if (lastTeleportTime.containsKey(teleportingPlayerId)) {
            long timeSinceLastTeleport = currentTime - lastTeleportTime.get(teleportingPlayerId);
            if (timeSinceLastTeleport < TELEPORT_COOLDOWN_MS) {
                long remainingCooldown = (TELEPORT_COOLDOWN_MS - timeSinceLastTeleport) / 1000;
                ServerPlayer teleportingPlayer = request.isTPAHere() ? accepter : requester;
                teleportingPlayer.sendSystemMessage(Component.literal("§cTeleport cooldown: " + remainingCooldown + " seconds remaining."));
                return false;
            }
        }
        
        // Perform teleportation
        boolean teleportSuccess;
        if (request.isTPAHere()) {
            // TPAHere: accepter teleports to requester
            teleportSuccess = teleportPlayer(accepter, requester.position(), requester.serverLevel());
            lastTeleportTime.put(accepter.getUUID(), currentTime);
        } else {
            // TPA: requester teleports to accepter
            teleportSuccess = teleportPlayer(requester, accepter.position(), accepter.serverLevel());
            lastTeleportTime.put(requester.getUUID(), currentTime);
        }
        
        if (teleportSuccess) {
            // Send success messages
            accepter.sendSystemMessage(Component.literal("§aTeleport request accepted!"));
            requester.sendSystemMessage(Component.literal("§a" + accepter.getName().getString() + " accepted your teleport request!"));
            
            // Remove the request
            pendingRequests.remove(request.getRequesterId());
            return true;
        } else {
            accepter.sendSystemMessage(Component.literal("§cTeleportation failed due to an error."));
            return false;
        }
    }
    
    public boolean denyTPARequest(ServerPlayer denier) {
        TPARequest request = findRequestByTarget(denier.getUUID());
        if (request == null) {
            denier.sendSystemMessage(Component.literal("§cYou have no pending TPA requests."));
            return false;
        }
        
        // Find the requester player
        ServerPlayer requester = denier.getServer().getPlayerList().getPlayer(request.getRequesterId());
        
        // Send messages
        denier.sendSystemMessage(Component.literal("§cTeleport request denied."));
        if (requester != null) {
            requester.sendSystemMessage(Component.literal("§c" + denier.getName().getString() + " denied your teleport request."));
        }
        
        // Remove the request
        pendingRequests.remove(request.getRequesterId());
        return true;
    }
    
    public void toggleTPARequests(ServerPlayer player) {
        UUID playerId = player.getUUID();
        if (toggledPlayers.contains(playerId)) {
            toggledPlayers.remove(playerId);
            player.sendSystemMessage(Component.literal("§aTPA requests are now enabled."));
        } else {
            toggledPlayers.add(playerId);
            player.sendSystemMessage(Component.literal("§cTPA requests are now disabled."));
        }
    }
    
    private boolean teleportPlayer(ServerPlayer player, Vec3 position, net.minecraft.server.level.ServerLevel level) {
        try {
            // Find a safe position near the target location
            Vec3 safePos = findSafePosition(level, position);
            if (safePos == null) {
                safePos = position; // Fallback to original position
            }
            
            // Use the correct teleportTo method signature for 1.21.4
            player.teleportTo(level, safePos.x, safePos.y, safePos.z, 
                            java.util.Set.of(), player.getYRot(), player.getXRot(), true);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private Vec3 findSafePosition(net.minecraft.server.level.ServerLevel level, Vec3 position) {
        // Try to find a safe position near the target
        int x = (int) Math.floor(position.x);
        int z = (int) Math.floor(position.z);
        
        // Check positions in a small radius
        for (int dy = 0; dy <= 3; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int checkX = x + dx;
                    int checkZ = z + dz;
                    int y = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, 
                                                 new net.minecraft.core.BlockPos(checkX, 0, checkZ)).getY() + dy;
                    
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(checkX, y, checkZ);
                    net.minecraft.core.BlockPos above = pos.above();
                    
                    // Check if position is safe (solid ground, air above)
                    if (!level.getBlockState(pos).isAir() && 
                        level.getBlockState(above).isAir() && 
                        level.getBlockState(above.above()).isAir()) {
                        return new Vec3(checkX + 0.5, y + 1, checkZ + 0.5);
                    }
                }
            }
        }
        
        return null; // No safe position found
    }
    
    private TPARequest findRequestByTarget(UUID targetId) {
        return pendingRequests.values().stream()
                .filter(request -> request.getTargetId().equals(targetId))
                .findFirst()
                .orElse(null);
    }
    
    public void cleanupExpiredRequests() {
        long currentTime = System.currentTimeMillis();
        pendingRequests.entrySet().removeIf(entry -> 
            entry.getValue().isExpired(currentTime, REQUEST_TIMEOUT_MS));
    }
    
    public void onPlayerLogout(UUID playerId) {
        // Remove any pending requests involving this player
        pendingRequests.entrySet().removeIf(entry -> 
            entry.getValue().getRequesterId().equals(playerId) || 
            entry.getValue().getTargetId().equals(playerId));
        
        // Remove from cooldown tracking
        lastTeleportTime.remove(playerId);
        
        // Keep toggle state (player preference should persist)
    }
}