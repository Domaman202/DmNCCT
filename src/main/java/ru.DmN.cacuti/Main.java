package ru.DmN.cacuti;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import sun.misc.Unsafe;

import java.io.*;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class Main implements ModInitializer {
    public static Set<Permission> permissions = new LinkedHashSet<>();
    public static Map<UUID, String> prefixes = new HashMap<>();
    public static Set<String> logList = new LinkedHashSet<>();
    public static Map<UUID, PrintStream> streamHash = new HashMap<>();

    @Override
    public void onInitialize() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            try {
                if (!Files.exists(Paths.get("./logs/break/")))
                    Files.createDirectory(Paths.get("./logs/break/"));

                PrintStream out;
                if (streamHash.containsKey(player.getUuid()))
                    out = streamHash.get(player.getUuid());
                else out = new PrintStream(new FileOutputStream("./logs/break/" + player.getGameProfile().getName() + '_' + Files.list(new File("./logs/break/").toPath()).count()));
                out.println("world -> " + world.getRegistryKey().getValue());
                out.println("pos -> " + pos);
                out.println("ppos -> " + player.getPos());
                out.println(state);
                out.println(blockEntity);
                out.println();
                out.flush();
                streamHash.put(player.getUuid(), out);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

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

            dispatcher.register(literal("dmn_fake_test").then(argument("player", EntityArgumentType.player()).executes(context -> {
                try {
                    var f_ = Unsafe.class.getDeclaredField("theUnsafe");
                    f_.setAccessible(true);
                    var unsafe = (Unsafe) f_.get(null);
                    //
                    var player0 = context.getSource().getPlayer();
                    var player1 = EntityArgumentType.getPlayer(context, "player");
                    //
                    var p0 = unsafe.allocateInstance(ServerPlayerEntity.class);
                    var p1 = unsafe.allocateInstance(ServerPlayerEntity.class);
                    //
                    copy(unsafe, ServerPlayerEntity.class, player1, p1);
                    copy(unsafe, ServerPlayerEntity.class, player0, p0);
                    //
                    copy(unsafe, ServerPlayerEntity.class, p1, player0);
                    copy(unsafe, ServerPlayerEntity.class, p0, player1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return 1;
            })));

            dispatcher.register(literal("log")
                    .then(literal("add").then(argument("player", EntityArgumentType.player()).executes(context -> {
                        logList.add(EntityArgumentType.getPlayer(context, "player").getGameProfile().getName());
                        save(null);
                        return 1;
                    })))
                    .then(literal("del").then(argument("player", EntityArgumentType.player()).executes(context -> {
                        logList.remove(EntityArgumentType.getPlayer(context, "player").getGameProfile().getName());
                        save(null);
                        return 1;
                    })))
            );

            dispatcher.register(literal("ping").executes(context -> {
                var player = context.getSource().getPlayer();
                player.sendMessage(new LiteralText("§l§aВаш пинг - §o§c" + player.pingMilliseconds), false);
                return 1;
            }));

            dispatcher.register(literal("prefix")
                    .then(argument("player", EntityArgumentType.player()).then(argument("prefix", StringArgumentType.greedyString()).executes(context -> {
                        prefixes.put(context.getArgument("player", EntitySelector.class).getPlayer(context.getSource()).getUuid(), context.getArgument("prefix", String.class).replace('#', '§'));
                        save(context.getSource().getServer().getPlayerManager());
                        return 1;
                    }))).then(literal("del").then(argument("player", EntityArgumentType.player()).executes(context -> {
                        prefixes.remove(context.getArgument("player", EntitySelector.class).getPlayer(context.getSource()).getUuid());
                        save(context.getSource().getServer().getPlayerManager());
                        return 1;
                    }))));

            dispatcher.register(literal("permission")
                    .then(literal("add").then(argument("name", StringArgumentType.word()).then(argument("parent", StringArgumentType.word()).then(argument("prefix", StringArgumentType.greedyString()).executes(context -> {
                        addPermission(context.getArgument("name", String.class), context.getArgument("parent", String.class), context.getArgument("prefix", String.class).replace('#', '§'));
                        save(null);
                        return 1;
                    })))))
                    .then(literal("del").then(argument("name", StringArgumentType.word()).executes(context -> {
                        for (var permission : permissions)
                            if (permission.name.equals(context.getArgument("name", String.class))) {
                                permissions.remove(permission);
                                break;
                            }
                        save(context.getSource().getServer().getPlayerManager());
                        return 1;
                    })))
                    .then(literal("prefix").then(argument("name", StringArgumentType.word()).then(argument("prefix", StringArgumentType.greedyString()).executes(context -> {
                        permissions.forEach(permission -> {
                            if (permission.name.equals(context.getArgument("name", String.class)))
                                permission.prefix = context.getArgument("prefix", String.class).replace('#', '§');
                        });
                        save(context.getSource().getServer().getPlayerManager());
                        return 1;
                    }))))
                    .then(literal("addusr").then(argument("name", StringArgumentType.word()).then(argument("user", EntityArgumentType.player()).executes(context -> {
                        for (var permission : permissions)
                            if (permission.name.equals(context.getArgument("name", String.class))) {
                                permission.players.add(EntityArgumentType.getPlayer(context, "user").getGameProfile().getName());
                                break;
                            }
                        save(context.getSource().getServer().getPlayerManager());
                        return 1;
                    }))))
                    .then(literal("delusr").then(argument("name", StringArgumentType.word()).then(argument("user", EntityArgumentType.player()).executes(context -> {
                        for (var permission : permissions)
                            if (permission.name.equals(context.getArgument("name", String.class))) {
                                permission.players.remove(EntityArgumentType.getPlayer(context, "user").getGameProfile().getName());
                                break;
                            }
                        save(context.getSource().getServer().getPlayerManager());
                        return 1;
                    }))))
                    .then(literal("addcmd").then(argument("name", StringArgumentType.word()).then(argument("command", StringArgumentType.greedyString()).executes(context -> {
                        for (var permission : permissions)
                            if (permission.name.equals(context.getArgument("name", String.class))) {
                                permission.commands.add(context.getArgument("command", String.class));
                                break;
                            }
                        save(null);
                        return 1;
                    }))))
                    .then(literal("delcmd").then(argument("name", StringArgumentType.word()).then(argument("command", StringArgumentType.greedyString()).executes(context -> {
                        for (var permission : permissions)
                            if (permission.name.equals(context.getArgument("name", String.class))) {
                                permission.commands.remove(context.getArgument("command", String.class));
                                break;
                            }
                        save(null);
                        return 1;
                    }))))
                    .then(literal("list").executes(context -> {
                        var src = context.getSource();
                        src.sendFeedback(new LiteralText("§CPerms list:"), false);
                        for (var permission : permissions) {
                            src.sendFeedback(new LiteralText("§1>").append("§2" + permission.name + "\n§a<parent>§4" + permission.parent + "\n§e<prefix>§7" + permission.prefix), false);
                            var sb = new StringBuilder();
                            for (var user : permission.players)
                                sb.append("§3<usr>§7").append(user).append('\n');
                            for (var cmd : permission.commands)
                                sb.append("§3<cmd>§6").append(cmd).append('\n');
                            src.sendFeedback(new LiteralText(sb.toString()), false);
                        }
                        return 1;
                    })));
        });
    }

    public long getAddressOfObject(sun.misc.Unsafe unsafe, Object obj) {
        Object helperArray[]    = new Object[1];
        helperArray[0]          = obj;
        long baseOffset         = unsafe.arrayBaseOffset(Object[].class);
        long addressOfObject    = unsafe.getLong(helperArray, baseOffset);
        return addressOfObject;
    }

    public void copy(Unsafe unsafe, Class<?> clazz, Object in, Object out) {
        for (var f : clazz.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers())) {
                var off = unsafe.objectFieldOffset(f);
                unsafe.putObject(out, off, unsafe.getObject(in, off));
            }
        }
        if (clazz.getSuperclass() != Object.class)
            copy(unsafe, clazz.getSuperclass(), in, out);
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

        if (manager != null) {
            manager.sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME, manager.getPlayerList()));
        }
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

/*
//
                    var x = (DataTracker) unsafe.getObject(context.getSource().getPlayer(), unsafe.objectFieldOffset(Entity.class.getDeclaredField("dataTracker")));
                    //
                    var entity = new PigEntity(EntityType.PIG, context.getSource().getWorld());
                    entity.setPos(0, -59, 0);
                    context.getSource().getWorld().spawnEntity(entity);
                    x = new Fake.FakeDataTracker(x, entity);
                    copy(unsafe, LivingEntity.class, entity, context.getSource().getPlayer());
                    //
                    unsafe.putObject(context.getSource().getPlayer(), unsafe.objectFieldOffset(Entity.class.getDeclaredField("dataTracker")), x);
 */