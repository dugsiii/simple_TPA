package com.dugsiii.tpamod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.dugsiii.tpamod.TPAMod;
import com.dugsiii.tpamod.manager.TPAManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class TPACommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /tpa <player> - Request to teleport to another player
        dispatcher.register(Commands.literal("tpa")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(TPACommands::executeTPA)));
        
        // /tpahere <player> - Request another player to teleport to you
        dispatcher.register(Commands.literal("tpahere")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(TPACommands::executeTPAHere)));
        
        // /tpaccept - Accept a pending TPA request
        dispatcher.register(Commands.literal("tpaccept")
                .executes(TPACommands::executeTPAccept));
        
        // /tpdeny - Deny a pending TPA request
        dispatcher.register(Commands.literal("tpdeny")
                .executes(TPACommands::executeTPDeny));
        
        // /tptoggle - Toggle TPA requests on/off
        dispatcher.register(Commands.literal("tptoggle")
                .executes(TPACommands::executeTPToggle));
        
        // Admin command to reload TPA system
        dispatcher.register(Commands.literal("tpareload")
                .requires(source -> source.hasPermission(2))
                .executes(TPACommands::executeTPAReload));
    }
    
    private static int executeTPA(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer sender = context.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        
        if (sender.equals(target)) {
            sender.sendSystemMessage(Component.literal("§cYou cannot send a TPA request to yourself."));
            return 0;
        }
        
        TPAManager manager = TPAMod.getTPAManager();
        if (manager == null) {
            sender.sendSystemMessage(Component.literal("§cTPA system is not available."));
            return 0;
        }
        
        manager.sendTPARequest(sender, target, false);
        return 1;
    }
    
    private static int executeTPAHere(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer sender = context.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        
        if (sender.equals(target)) {
            sender.sendSystemMessage(Component.literal("§cYou cannot send a TPA request to yourself."));
            return 0;
        }
        
        TPAManager manager = TPAMod.getTPAManager();
        if (manager == null) {
            sender.sendSystemMessage(Component.literal("§cTPA system is not available."));
            return 0;
        }
        
        manager.sendTPARequest(sender, target, true);
        return 1;
    }
    
    private static int executeTPAccept(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        TPAManager manager = TPAMod.getTPAManager();
        if (manager == null) {
            player.sendSystemMessage(Component.literal("§cTPA system is not available."));
            return 0;
        }
        
        boolean success = manager.acceptTPARequest(player);
        return success ? 1 : 0;
    }
    
    private static int executeTPDeny(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        TPAManager manager = TPAMod.getTPAManager();
        if (manager == null) {
            player.sendSystemMessage(Component.literal("§cTPA system is not available."));
            return 0;
        }
        
        boolean success = manager.denyTPARequest(player);
        return success ? 1 : 0;
    }
    
    private static int executeTPToggle(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        TPAManager manager = TPAMod.getTPAManager();
        if (manager == null) {
            player.sendSystemMessage(Component.literal("§cTPA system is not available."));
            return 0;
        }
        
        manager.toggleTPARequests(player);
        return 1;
    }
    
    private static int executeTPAReload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        TPAManager manager = TPAMod.getTPAManager();
        if (manager != null) {
            manager.cleanupExpiredRequests();
            source.sendSuccess(() -> Component.literal("§aTPA system reloaded and expired requests cleaned up."), true);
        } else {
            source.sendFailure(Component.literal("§cTPA system is not available."));
        }
        
        return 1;
    }
}