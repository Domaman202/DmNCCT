package ru.DmN.cacuti.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.server.command.MessageCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;

@Mixin(MessageCommand.class)
public class MessageCommandMixin {
    /**
     * @author DomamaN202
     */
    @Overwrite
    private static int execute(ServerCommandSource source, Collection<ServerPlayerEntity> targets, Text message) {
        UUID uUID = source.getEntity() == null ? Util.NIL_UUID : source.getEntity().getUuid();
        Entity entity = source.getEntity();
        Consumer<Text> consumer;
        if (entity instanceof ServerPlayerEntity serverPlayerEntity)
            consumer = (playerName) -> serverPlayerEntity.sendSystemMessage(new LiteralText("§eВы прошептали ").append("§c" + playerName.asString()).append("§e: ").append(message.copy().formatted(Formatting.DARK_AQUA, Formatting.ITALIC)), serverPlayerEntity.getUuid());
        else
            consumer = (playerName) -> source.sendFeedback(new LiteralText("§eВы прошептали ").append("§c" + playerName.asString()).append("§e: ").append(message.copy().formatted(Formatting.DARK_AQUA, Formatting.ITALIC)), false);

        for (ServerPlayerEntity serverPlayerEntity2 : targets) {
            consumer.accept(serverPlayerEntity2.getDisplayName());
            serverPlayerEntity2.sendSystemMessage((new LiteralText("§c" + source.getDisplayName().asString() + "§e прошептал: ").append(message.copy().formatted(Formatting.DARK_AQUA, Formatting.ITALIC))), uUID);
        }

        return targets.size();
    }
}
