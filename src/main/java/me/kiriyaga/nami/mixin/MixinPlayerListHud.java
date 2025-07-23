package me.kiriyaga.nami.mixin;

import me.kiriyaga.nami.feature.module.impl.misc.BetterTabModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static me.kiriyaga.nami.Nami.FRIEND_MANAGER;
import static me.kiriyaga.nami.Nami.MODULE_MANAGER;

@Mixin(PlayerListHud.class)
public abstract class MixinPlayerListHud {

    @Shadow @Final private MinecraftClient client;

    @Shadow @Final private static Comparator<PlayerListEntry> ENTRY_ORDERING;

    @Shadow protected abstract Text applyGameModeFormatting(PlayerListEntry entry, MutableText name);

    @Inject(method = "collectPlayerEntries", at = @At("HEAD"), cancellable = true)
    private void collectPlayerEntries(CallbackInfoReturnable<List<PlayerListEntry>> info) {
        BetterTabModule betterTab = MODULE_MANAGER.getStorage() != null ? MODULE_MANAGER.getStorage().getByClass(BetterTabModule.class) : null;
        if (betterTab == null || !betterTab.isEnabled()) return;

        if (client == null || client.player == null || client.player.networkHandler == null) return;

        List<PlayerListEntry> entries = client.player.networkHandler.getListedPlayerListEntries().stream()
                .sorted(ENTRY_ORDERING)
                .filter(entry -> !betterTab.friendsOnly.get() || FRIEND_MANAGER.isFriend(entry.getProfile().getName()))
                .limit(betterTab.limit.get())
                .collect(Collectors.toList());

        info.setReturnValue(entries);
    }

    @Inject(method = "getPlayerName", at = @At("HEAD"), cancellable = true)
    private void getPlayerName(PlayerListEntry entry, CallbackInfoReturnable<Text> info) {
        BetterTabModule betterTab = MODULE_MANAGER.getStorage() != null ? MODULE_MANAGER.getStorage().getByClass(BetterTabModule.class) : null;
        if (betterTab == null || !betterTab.isEnabled()) return;

        boolean highlightFriends = betterTab.highlighFriends.get();
        String playerName = entry.getProfile().getName();
        boolean isFriend = FRIEND_MANAGER.isFriend(playerName);

        if (highlightFriends && isFriend) {
            MutableText formattedName = Text.empty();

            if (entry.getDisplayName() != null) {
                for (Text sibling : entry.getDisplayName().getSiblings()) {
                    String str = sibling.getString();
                    if (str.equals(playerName)) {
                        formattedName.append(Text.literal(playerName).formatted(Formatting.AQUA));
                    } else if (str.equals("] " + playerName)) {
                        formattedName.append(Text.literal("] ").formatted(Formatting.WHITE))
                                .append(Text.literal(playerName).formatted(Formatting.AQUA));
                    } else {
                        formattedName.append(sibling);
                    }
                }
            } else {
                formattedName = Team.decorateName(entry.getScoreboardTeam(), Text.literal(playerName).formatted(Formatting.AQUA));
            }

            info.setReturnValue(applyGameModeFormatting(entry, formattedName));
        }
    }
}
