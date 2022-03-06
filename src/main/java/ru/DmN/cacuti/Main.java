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
import net.minecraft.util.Pair;
import org.apache.logging.log4j.Logger;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static ru.DmN.cacuti.permission.Permission.addPermission;

public class Main implements ModInitializer {
    public static final Instrumentation instrumentation = ByteBuddyAgent.install();
    public static Unsafe unsafe;

    public static final GetPlayer getPlayer = new GetPlayer();;

    public static Set<Permission> permissions = new LinkedHashSet<>();
    public static Map<UUID, String> prefixes = new HashMap<>();
    public static Set<String> logList = new LinkedHashSet<>();
    public static Map<UUID, PrintStream> streamHash = new HashMap<>();

    public static final Map<String, Pair<AtomicInteger, Thread>> coolDownPlayerList = new HashMap<>();

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
                } catch (Throwable t)  {
                    t.printStackTrace();
                }
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

    public static void runCooldown(ServerPlayerEntity player, Logger logger) {
        var threadRef = new AtomicReference<Thread>();
        var thread = new Thread(() -> {
            synchronized (Main.coolDownPlayerList) {
                var i = new AtomicInteger(15);
                Main.coolDownPlayerList.put(player.getGameProfile().getName(), new Pair<>(i, threadRef.get()));
                while (i.decrementAndGet() > 0 && !threadRef.get().isInterrupted()) {
                    player.sendMessage(new LiteralText("§cНе выходите§7, осталось - §e" + i.get() + "§7 сек."), false);
                    logger.info("Кд (" + player.getName().asString() + ") => " + i.get() + " сек.");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                player.sendMessage(new LiteralText("§aМожете§7 выходить!"), false);
                Main.coolDownPlayerList.remove(player.getGameProfile().getName());
            }
        });
        threadRef.set(thread);
        CompletableFuture.runAsync(thread);
    }

    public static long getAddressOfObject(Object obj) {
        Object[] helperArray    = new Object[1];
        helperArray[0]          = obj;
        long baseOffset         = unsafe.arrayBaseOffset(Object[].class);
        long addressOfObject    = unsafe.getLong(helperArray, baseOffset);
        return addressOfObject;
    }

    static {
        try {
            var f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
        } catch (Throwable t) {
            t.printStackTrace();
        }
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