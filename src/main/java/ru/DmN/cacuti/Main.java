package ru.DmN.cacuti;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.DmN.cacuti.login.GetPlayer;
import ru.DmN.cacuti.login.RegisteredPlayersJson;
import ru.DmN.cacuti.login.commands.LoginCommand;
import ru.DmN.cacuti.login.commands.RegisterCommand;
import ru.DmN.cacuti.permission.Permission;
import sun.misc.Unsafe;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class Main implements ModInitializer {
    public static final Instrumentation instrumentation = ByteBuddyAgent.install();
    public static Unsafe unsafe = getUnsafe();

    public static final GetPlayer getPlayer = new GetPlayer();

    public static Set<Permission> permissions = new LinkedHashSet<>();
    public static Map<UUID, String> prefixes = new HashMap<>();
    public static Map<UUID, String> nicks = new HashMap<>();
    public static Set<String> logList = new LinkedHashSet<>();
    public static Map<UUID, PrintStream> streamHash = new HashMap<>();

    public static final ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
    public static final Map<UUID, Long> coolDownPlayerList = new HashMap<>();
    public static final long sleepThreadLock = unsafe.allocateMemory(1);

    @Override
    public void onInitialize() {
        RegisteredPlayersJson.read();

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            try {
                if (!Files.exists(Paths.get("./logs/break/")))
                    Files.createDirectory(Paths.get("./logs/break/"));

                PrintStream out;
                if (streamHash.containsKey(player.getUuid()))
                    out = streamHash.get(player.getUuid());
                else
                    out = new PrintStream(new FileOutputStream("./logs/break/" + player.getGameProfile().getName() + '_' + Files.list(new File("./logs/break/").toPath()).count()));
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

            try (var file = new ObjectInputStream(new FileInputStream("nickcolor_hash.data"))) {
                nicks = (Map<UUID, String>) file.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }

            LoginCommand.register(dispatcher);
            RegisterCommand.register(dispatcher);

            dispatcher.register(literal("twCent").executes(context -> {
                var stack = new ItemStack(Items.POISONOUS_POTATO);
                stack.setCustomName(new LiteralText("twCent"));
                stack.addEnchantment(Enchantments.MENDING, 999);
                context.getSource().getPlayer().giveItemStack(stack);
                return 1;
            }));

            dispatcher.register(literal("dmn_admin_utils").then(literal("fake_player").then(argument("name", StringArgumentType.word()).executes(context -> {
                try {
                    var caller = context.getSource().getPlayer();
                    var pm = context.getSource().getServer().getPlayerManager();
                    var player = pm.createPlayer(new GameProfile(UUID.randomUUID(), context.getArgument("name", String.class)));
                    player.setWorld(caller.getWorld());
                    player.networkHandler = new ServerPlayNetworkHandler(context.getSource().getServer(), new ClientConnection(NetworkSide.SERVERBOUND), player);
                    var f = PlayerManager.class.getDeclaredField("players");
                    f.setAccessible(true);
                    ((List<ServerPlayerEntity>) f.get(pm)).add(player);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                return 1;
            }))));

            dispatcher.register(literal("nick").then(argument("player", EntityArgumentType.player())
                    .then(literal("del").executes(context -> {
                        nicks.remove(EntityArgumentType.getPlayer(context, "player").getGameProfile().getId());
                        save(context.getSource().getServer().getPlayerManager());
                        return 1;
                    })).then(argument("text", StringArgumentType.greedyString()).executes(context -> {
                        nicks.put(EntityArgumentType.getPlayer(context, "player").getGameProfile().getId(), context.getArgument("text", String.class).replace('#', '§'));
                        save(context.getSource().getServer().getPlayerManager());
                        return 1;
                    }))));

            dispatcher.register(literal("rp")
                    .then(literal("no_author_book").executes(context -> {
                        var stack = context.getSource().getPlayer().getMainHandStack();
                        if (stack.getItem() == Items.WRITTEN_BOOK)
                            stack.getNbt().putString("author", "§k*unknown*");
                        return 1;
                    }))
                    .then(literal("sit").executes(context -> {
                        dispatcher.execute("sit", context.getSource());
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
                player.sendMessage(new LiteralText("§l§aВаш пинг - §o§c" + player.pingMilliseconds + "§l§6 мс"), false);
                return 1;
            }).then(argument("player", EntityArgumentType.player()).executes(context -> {
                var player = EntityArgumentType.getPlayer(context, "player");
                context.getSource().sendFeedback(new LiteralText("§l§aПинг игрока §o§e" + player.getName().asString() + "§l§a - §o§c" + player.pingMilliseconds + "§l§6 мс"), false);
                return 1;
            })));

            dispatcher.register(literal("prefix")
                    .then(literal("update").executes(context -> {
                        save(context.getSource().getServer().getPlayerManager());
                        return 1;
                    }))
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
                    .then(literal("prefix")
                            .then(argument("name", StringArgumentType.word()).then(argument("prefix", StringArgumentType.greedyString()).executes(context -> {
                                permissions.forEach(permission -> {
                                    if (permission.name.equals(context.getArgument("name", String.class)))
                                        permission.prefix = context.getArgument("prefix", String.class).replace('#', '§');
                                });
                                save(context.getSource().getServer().getPlayerManager());
                                return 1;
                            })))
                            .then(literal("del").then(argument("name", StringArgumentType.word()).executes(context -> {
                                permissions.forEach(permission -> {
                                    if (permission.name.equals(context.getArgument("name", String.class)))
                                        permission.prefix = "";
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

    public static String toNormaString(Text text) {
        var sb = new StringBuilder();
        var style = text.getStyle();

        var color = style.getColor();
        if (color != null) {
            sb.append('§');
            for (var format : Formatting.values()) {
                var cv = format.getColorValue();
                if (cv != null && cv == color.getRgb())
                    sb.append(format.getCode());
            }
        }

        if (style.isObfuscated()) {
            sb.append("§k");
        }

        if (style.isBold()) {
            sb.append("§l");
        }

        if (style.isStrikethrough()) {
            sb.append("§m");
        }

        if (style.isUnderlined()) {
            sb.append("§n");
        }

        if (style.isItalic()) {
            sb.append("§o");
        }

        sb.append(text.asString());

        for (var t : text.getSiblings())
            sb.append(toNormaString(t));

        return sb.toString();
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

        try (var file = new ObjectOutputStream(new FileOutputStream("nickcolor_hash.data"))) {
            file.writeObject(nicks);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (manager != null)
            manager.sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME, manager.getPlayerList()));
    }

    public static void runCooldown(ServerPlayerEntity player) {
        long addr;
        if (Main.coolDownPlayerList.containsKey(player.getGameProfile().getId())) {
            addr = Main.coolDownPlayerList.get(player.getGameProfile().getId());
            unsafe.putInt(addr, 16);
            if (unsafe.getInt(addr + 4) != 0)
                return;
        } else {
            addr = unsafe.allocateMemory(8);
            Main.coolDownPlayerList.put(player.getGameProfile().getId(), addr);
            unsafe.putInt(addr, 16);
        }

        var task = new AtomicReference<ScheduledFuture<?>>();
        task.set(pool.scheduleAtFixedRate(() -> {
            unsafe.loadFence();
            if (unsafe.getInt(addr) > 0) {
                unsafe.putInt(addr + 4, Thread.currentThread().hashCode());
                var x = unsafe.getInt(addr) - 1;
                unsafe.putInt(addr, x);
                unsafe.storeFence();
                if (x == 0)
                    player.sendMessage(new LiteralText("§cНе выходите§7, осталось - §e" + 1 + "§7 сек."), false);
                else if (x % 5 == 0)
                    player.sendMessage(new LiteralText("§cНе выходите§7, осталось - §e" + x + "§7 сек."), false);
            } else {
                unsafe.putInt(addr + 4, 0);
                task.get().cancel(false);
                player.sendMessage(new LiteralText("§aМожете§7 выходить!"), false);
            }
        }, 1, 1, TimeUnit.SECONDS));

        unsafe.putInt(addr + 4, 0);
    }

    public static long getAddressOfObject(Object obj) {
        Object[] helperArray = new Object[1];
        helperArray[0] = obj;
        long baseOffset = unsafe.arrayBaseOffset(Object[].class);
        long addressOfObject = unsafe.getLong(helperArray, baseOffset);
        return addressOfObject;
    }

    public static void addPermission(String name, String parent, String prefix) {
        for (var permission : permissions)
            if (permission.name.equals(name))
                return;
        permissions.add(new Permission(name, parent, prefix));
    }

    public static boolean checkAccess(String user, String command) {
        for (var permission : permissions)
            if (checkAccess(user, command, permission, permissions, new ArrayList<>(), false))
                return true;
        return false;
    }

    public static boolean checkAccess(String user, String command, Permission permission, Set<Permission> permissions, ArrayList<String> blacklist, boolean p) {
        if (p || permission.players.contains(user)) {
            if (checkAccess(command, permission))
                return true;
            if (!Objects.equals(permission.parent, "_"))
                for (var parent : permissions)
                    if (Objects.equals(permission.parent, parent.name) && !blacklist.contains(parent.name)) {
                        blacklist.add(parent.name);
                        return checkAccess(user, command, parent, permissions, blacklist, true);
                    }
        }
        return false;
    }

    public static boolean checkAccess(String command, Permission permission) {
        for (var cmd : permission.commands)
            if (command.startsWith(cmd))
                return true;
        return false;
    }

    public static String checkPrefix(String user, Set<Permission> permissions) {
        for (var permission : permissions)
            if (permission.players.contains(user))
                return permission.prefix;
        return null;
    }

    private static Unsafe getUnsafe() {
        try {
            var f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (Unsafe) f.get(null);
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    static {
        unsafe.putByte(sleepThreadLock, (byte) 0);
    }
}