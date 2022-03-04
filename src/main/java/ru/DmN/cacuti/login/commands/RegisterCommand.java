package ru.DmN.cacuti.login.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import ru.DmN.cacuti.Main;
import ru.DmN.cacuti.login.RegisteredPlayersJson;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class RegisterCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("register")
                .then(argument("newPassword", StringArgumentType.word())
                    .then(argument("confirmPassword", StringArgumentType.word())
                        .executes(ctx -> {
                            String password = StringArgumentType.getString(ctx, "newPassword");
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            String username = player.getEntityName();
                            if (RegisteredPlayersJson.isPlayerRegistered(username)) {
                                ctx.getSource().sendFeedback(new LiteralText("§cYou're already registered! Use /ru.DmN.cacuti.login instead."), false);
                                return 1;
                            }
                            if (!password.equals(StringArgumentType.getString(ctx, "confirmPassword"))) {
                                ctx.getSource().sendFeedback(new LiteralText("§cPasswords don't match! Repeat it correctly."), false);
                                return 1;
                            }
                            String uuid = ctx.getSource().getPlayer().getUuidAsString();
                            RegisteredPlayersJson.save(uuid, username, password);
                            Main.getPlayer.get(ctx.getSource().getPlayer()).set(true);
                            player.setInvulnerable(false);
                            ctx.getSource().sendFeedback(new LiteralText("§aSuccessfully registered."), false);
                            return 1;
        }))));
    }
}
