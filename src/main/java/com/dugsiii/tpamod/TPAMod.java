package com.dugsiii.tpamod;

import com.dugsiii.tpamod.commands.TPACommands;
import com.dugsiii.tpamod.manager.TPAManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(TPAMod.MOD_ID)
public class TPAMod {
    public static final String MOD_ID = "tpamod";
    public static final Logger LOGGER = LogManager.getLogger();
    
    private static TPAManager tpaManager;
    
    public TPAMod(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        
        LOGGER.info("TPA Mod initialized!");
    }
    
    private void onServerStarting(ServerStartingEvent event) {
        tpaManager = new TPAManager();
        LOGGER.info("TPA Manager initialized!");
    }
    
    private void onRegisterCommands(RegisterCommandsEvent event) {
        TPACommands.register(event.getDispatcher());
        LOGGER.info("TPA Commands registered!");
    }
    
    public static TPAManager getTPAManager() {
        return tpaManager;
    }
}