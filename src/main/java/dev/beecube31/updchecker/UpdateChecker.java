package dev.beecube31.updchecker;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class UpdateChecker {
    private final String modId;
    private final String modName;
    private final String updateUrl;
    private final String currentVersion;

    private UpdateInfo updateInfo = null;
    private boolean checkFinished = false;
    private boolean updateAvailable = false;
    private boolean notified = false;
    private int tickDelay = 60;

    public UpdateChecker(String modId, String modName, String updateUrl) {
        this.modId = modId;
        this.modName = modName;
        this.updateUrl = updateUrl;

        this.currentVersion = ModList.get().getModContainerById(modId)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("0.0.0");

        MinecraftForge.EVENT_BUS.register(this);
        fetchUpdatesAsync();
    }

    private void fetchUpdatesAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                SimpleUpdateChecker.LOGGER.info("[{}] Checking for updates...", modName);
                URL url = new URL(this.updateUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() != 200) {
                    SimpleUpdateChecker.LOGGER.warn("[{}] Update check failed. HTTP response code {}", modName, conn.getResponseCode());
                    return;
                }

                try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonElement root = JsonParser.parseReader(reader);
                    if (!root.isJsonObject()) return;

                    JsonObject rootObj = root.getAsJsonObject();
                    String mcVersion = FMLLoader.versionInfo().mcVersion();

                    if (rootObj.has(mcVersion) && rootObj.get(mcVersion).isJsonObject()) {
                        this.updateInfo = UpdateInfo.fromJson(rootObj.getAsJsonObject(mcVersion));

                        ComparableVersion remoteVer = new ComparableVersion(this.updateInfo.getVersion());
                        ComparableVersion currentVer = new ComparableVersion(this.currentVersion);

                        if (remoteVer.compareTo(currentVer) > 0) {
                            this.updateAvailable = true;
                            SimpleUpdateChecker.LOGGER.info("[{}] Update found: {} (Current: {})", modName, updateInfo.getVersion(), currentVersion);
                        } else {
                            SimpleUpdateChecker.LOGGER.info("[{}] Mod is up to date.", modName);
                        }
                    } else {
                        SimpleUpdateChecker.LOGGER.warn("[{}] No update data found for Minecraft version {}", modName, mcVersion);
                    }
                }
            } catch (Exception e) {
                SimpleUpdateChecker.LOGGER.error("[{}] Failed to check for updates: {}", modName, e.getMessage());
            } finally {
                this.checkFinished = true;
            }
        });
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || notified) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (tickDelay > 0) {
            tickDelay--;
            return;
        }

        if (checkFinished && updateAvailable && updateInfo != null) {
            notified = true;
            notifyPlayer(mc);

            MinecraftForge.EVENT_BUS.unregister(this);
        }
    }

    private void notifyPlayer(Minecraft mc) {
        String playerLocale = mc.getLanguageManager().getSelected();
        String changelog = updateInfo.getLocalizedChangelog(playerLocale);

        // Title
        Component header = Component.literal("[" + modName + "] ")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.translatable("simple_updates_checker.update_found", updateInfo.getVersion())
                        .withStyle(ChatFormatting.YELLOW));

        mc.player.sendSystemMessage(header);

        // Chgangelog
        if (!changelog.isEmpty()) {
            Component changes = Component.literal("  " + changelog).withStyle(ChatFormatting.GRAY);
            mc.player.sendSystemMessage(changes);
        }

        // Clickable button
        if (updateInfo.getDownloadUrl() != null && !updateInfo.getDownloadUrl().isEmpty()) {
            Component downloadLink = Component.literal("  ")
                    .append(Component.translatable("simple_updates_checker.download")
                            .withStyle(style -> style
                                    .withColor(ChatFormatting.AQUA)
                                    .withUnderlined(true)
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, updateInfo.getDownloadUrl()))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("simple_updates_checker.click_to_open")))
                            ));
            mc.player.sendSystemMessage(downloadLink);
        }
    }
}
