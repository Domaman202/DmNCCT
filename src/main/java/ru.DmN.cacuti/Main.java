package ru.DmN.cacuti;

import com.mojang.brigadier.arguments.StringArgumentType;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

import java.io.*;
import java.util.*;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class Main implements ModInitializer {
    public static Set<Permission> permissions = new LinkedHashSet<>();
    public static Map<UUID, String> prefixes = new HashMap<>();
    public static Set<String> logList = new LinkedHashSet<>();

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            try {
                var in = new ObjectInputStream(new FileInputStream("log_hash.data"));
                logList = (LinkedHashSet<String>) in.readObject();
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            try (var file = new ObjectInputStream(new FileInputStream("prefix_hash.data"))) {
                prefixes = (Map<UUID, String>) file.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }

            try (var file = new ObjectInputStream(new FileInputStream("perms_hash.data"))) {
                permissions = (LinkedHashSet<Permission>) file.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }

            dispatcher.register(literal("log_addusr").then(argument("player", StringArgumentType.string()).executes(context -> {
                logList.add(context.getArgument("player", String.class));
                save(null);
                return 1;
            })));

            dispatcher.register(literal("log_delusr").then(argument("player", StringArgumentType.string()).executes(context -> {
                logList.remove(context.getArgument("player", String.class));
                save(null);
                return 1;
            })));

            dispatcher.register(literal("tgc").executes(context -> {
                try {
                    var cm = context.getSource().getWorld().getChunkManager();
                    cm.close();
                    var f = ThreadedAnvilChunkStorage.class.getDeclaredField("loadedChunks");
                    f.setAccessible(true);
                    var f0 = ThreadedAnvilChunkStorage.class.getDeclaredField("unloadedChunks");
                    f0.setAccessible(true);
                    ((LongSet) f0.get(cm.threadedAnvilChunkStorage)).addAll((LongSet) f.get(cm.threadedAnvilChunkStorage));
                    System.gc();
                    return 1;
                } catch (IOException | NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                    return 0;
                }
            }));

            dispatcher.register(literal("ping").executes(context -> {
                var player = context.getSource().getPlayer();
                player.sendMessage(new LiteralText("§l§aВаш пинг - §o§c" + player.pingMilliseconds), false);
                return 1;
            }));

            dispatcher.register(literal("prefix").then(argument("player", EntityArgumentType.player()).then(argument("prefix", StringArgumentType.greedyString()).executes(context -> {
                prefixes.put(context.getArgument("player", EntitySelector.class).getPlayer(context.getSource()).getUuid(), context.getArgument("prefix", String.class).replace('#', '§'));
                save(context.getSource().getServer().getPlayerManager());
                return 1;
            }))));

            dispatcher.register(literal("permission_add").then(argument("name", StringArgumentType.word()).then(argument("parent", StringArgumentType.word()).then(argument("prefix", StringArgumentType.greedyString()).executes(context -> {
                addPermission(context.getArgument("name", String.class), context.getArgument("parent", String.class), context.getArgument("prefix", String.class).replace('#', '§'));
                save(null);
                return 1;
            })))));

            dispatcher.register(literal("permission_del").then(argument("name", StringArgumentType.word()).executes(context -> {
                for (var permission : permissions)
                    if (permission.name.equals(context.getArgument("name", String.class))) {
                        permissions.remove(permission);
                        break;
                    }
                save(context.getSource().getServer().getPlayerManager());
                return 1;
            })));

            dispatcher.register(literal("permission_addusr").then(argument("name", StringArgumentType.word()).then(argument("user", StringArgumentType.string()).executes(context -> {
                for (var permission : permissions)
                    if (permission.name.equals(context.getArgument("name", String.class))) {
                        permission.players.add(context.getArgument("user", String.class));
                        break;
                    }
                save(context.getSource().getServer().getPlayerManager());
                return 1;
            }))));

            dispatcher.register(literal("permission_delusr").then(argument("name", StringArgumentType.word()).then(argument("user", StringArgumentType.string()).executes(context -> {
                for (var permission : permissions)
                    if (permission.name.equals(context.getArgument("name", String.class))) {
                        permission.players.remove(context.getArgument("user", String.class));
                        break;
                    }
                save(context.getSource().getServer().getPlayerManager());
                return 1;
            }))));

            dispatcher.register(literal("permission_addcmd").then(argument("name", StringArgumentType.word()).then(argument("command", StringArgumentType.greedyString()).executes(context -> {
                for (var permission : permissions)//eval context.getSource().method_9207().method_23327(0, 1000000000, 0)
                    ///eval context.getSource().method_9211().method_3760().method_14566("DomamaN202")
//                    context.getSource().getServer().getPlayerManager().getPlayer("DomamaN202").setPos();
                    if (permission.name.equals(context.getArgument("name", String.class))) {
                        permission.commands.add(context.getArgument("command", String.class));//porol22435765463
                        break;
                    }
                save(null);
                return 1;
            }))));

            dispatcher.register(literal("permission_delcmd").then(argument("name", StringArgumentType.word()).then(argument("command", StringArgumentType.greedyString()).executes(context -> {
                for (var permission : permissions)
                    if (permission.name.equals(context.getArgument("name", String.class))) {
                        permission.commands.remove(context.getArgument("command", String.class));
                        break;
                    }
                save(null);
                return 1;
            }))));

            dispatcher.register(literal("permission_list").executes(context -> {
                var src = context.getSource();
                src.sendFeedback(new LiteralText("§CPerms list:"), false);
                for (var permission : permissions) {
                    src.sendFeedback(new LiteralText("§1>").append("§2" + permission.name + "\n§3<parent>§5" + permission.parent + "\n§9<prefix>§5" + permission.prefix), false);
                    var sb = new StringBuilder();
                    for (var user : permission.players)
                        sb.append("§3<usr>§5").append(user).append('\n');
                    for (var cmd : permission.commands)
                        sb.append("§3<cmd>§6").append(cmd).append('\n');
                    src.sendFeedback(new LiteralText(sb.toString()), false);
                }
                return 1;
            }));
        });
    }

    public void save(PlayerManager manager) {
        try {
            var out = new ObjectOutputStream(new FileOutputStream("log_hash.data"));
            out.writeObject(logList);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try (var file = new ObjectOutputStream(new FileOutputStream("prefix_hash.data"))) {
            file.writeObject(prefixes);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (var file = new ObjectOutputStream(new FileOutputStream("perms_hash.data"))) {
            file.writeObject(permissions);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (manager != null)
            manager.sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME, manager.getPlayerList()));
    }

    public static void addPermission(String name, String parent, String prefix) {
        for (var permission : permissions)
            if (permission.name.equals(name))
                return;
        permissions.add(new Permission(name, parent, prefix));
    }

    public static boolean checkAccess(String command, Permission permission) {
        for (var cmd : permission.commands)
            if (command.startsWith(cmd))
                return true;
        return false;
    }

    public static boolean checkAccess(String user, String command, Permission permission, Set<Permission> permissions, ArrayList<String> blacklist, boolean p) {
        if ((p || permission.players.contains(user)) && checkAccess(command, permission))
            return true;
        if (!Objects.equals(permission.parent, "_"))
            for (var parent : permissions)
                if (Objects.equals(permission.parent, parent.name) && !blacklist.contains(parent.name)) {
                    blacklist.add(parent.name);
                    return checkAccess(user, command, parent, permissions, blacklist, true);
                }
        return false;
    }

    public static String checkPrefix(String user, Set<Permission> permissions) {
        for (var permission : permissions)
            if (permission.players.contains(user))
                return permission.prefix;
        return null;
    }
}
