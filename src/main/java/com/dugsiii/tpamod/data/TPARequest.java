package com.dugsiii.tpamod.data;

import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

public class TPARequest {
    private final UUID requesterId;
    private final UUID targetId;
    private final String requesterName;
    private final String targetName;
    private final long timestamp;
    private final boolean isTPAHere; // true for tpahere, false for tpa
    
    public TPARequest(ServerPlayer requester, ServerPlayer target, boolean isTPAHere) {
        this.requesterId = requester.getUUID();
        this.targetId = target.getUUID();
        this.requesterName = requester.getName().getString();
        this.targetName = target.getName().getString();
        this.timestamp = System.currentTimeMillis();
        this.isTPAHere = isTPAHere;
    }
    
    public UUID getRequesterId() {
        return requesterId;
    }
    
    public UUID getTargetId() {
        return targetId;
    }
    
    public String getRequesterName() {
        return requesterName;
    }
    
    public String getTargetName() {
        return targetName;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public boolean isTPAHere() {
        return isTPAHere;
    }
    
    public boolean isExpired(long currentTime, long timeoutMs) {
        return (currentTime - timestamp) > timeoutMs;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof TPARequest)) return false;
        TPARequest other = (TPARequest) obj;
        return requesterId.equals(other.requesterId) && targetId.equals(other.targetId);
    }
    
    @Override
    public int hashCode() {
        return requesterId.hashCode() + targetId.hashCode();
    }
}