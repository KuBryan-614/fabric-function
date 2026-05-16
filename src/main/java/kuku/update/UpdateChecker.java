package kuku.update;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UpdateChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger("function-updater");
    private static final Gson GSON = new Gson();

    private static final String MOD_ID = "function";
    private static final String GITHUB_OWNER = "kubryan";
    private static final String GITHUB_REPO = "fabric-function";
    private static final String API_URL =
            "https://api.github.com/repos/" + GITHUB_OWNER + "/" + GITHUB_REPO + "/releases/latest";

    private UpdateChecker() {
    }

    public static void checkForUpdate() {
        Thread updaterThread = new Thread(UpdateChecker::checkForUpdateSafely, "Function-Updater");
        updaterThread.setDaemon(true);
        updaterThread.start();
    }

    private static void checkForUpdateSafely() {
        try {
            Optional<ModContainer> modContainer = FabricLoader.getInstance().getModContainer(MOD_ID);
            if (modContainer.isEmpty()) {
                LOGGER.warn("Cannot check updates because mod '{}' is not loaded.", MOD_ID);
                return;
            }

            String currentVersion = normalizeVersion(
                    modContainer.get().getMetadata().getVersion().getFriendlyString());
            JsonObject latestRelease = fetchLatestRelease();
            if (latestRelease == null) {
                return;
            }

            String tagName = latestRelease.get("tag_name").getAsString();
            String latestVersion = normalizeVersion(tagName);
            LOGGER.info("Current version: v{}, latest GitHub release: v{}", currentVersion, latestVersion);

            if (!isNewer(latestVersion, currentVersion)) {
                LOGGER.info("No update available.");
                return;
            }

            Optional<String> downloadUrl = findJarAssetUrl(latestRelease, latestVersion);
            if (downloadUrl.isEmpty()) {
                LOGGER.warn("Release v{} has no usable mod jar asset.", latestVersion);
                return;
            }

            Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
            UpdateDownloader.downloadAndPrepare(modsDir, downloadUrl.get(), latestVersion);
        } catch (Exception e) {
            LOGGER.warn("Failed to check for updates.", e);
        }
    }

    private static JsonObject fetchLatestRelease() throws Exception {
        URL url = URI.create(API_URL).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", "function-mod-updater");
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(10_000);

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            LOGGER.warn("GitHub release check failed. HTTP status: {}", responseCode);
            return null;
        }

        try (InputStreamReader reader = new InputStreamReader(
                connection.getInputStream(), StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, JsonObject.class);
        } finally {
            connection.disconnect();
        }
    }

    private static Optional<String> findJarAssetUrl(JsonObject release, String version) {
        JsonArray assets = release.getAsJsonArray("assets");
        if (assets == null || assets.isEmpty()) {
            return Optional.empty();
        }

        String versionToken = version.toLowerCase();
        String fallbackUrl = null;
        for (int i = 0; i < assets.size(); i++) {
            JsonObject asset = assets.get(i).getAsJsonObject();
            String name = asset.get("name").getAsString();
            String lowerName = name.toLowerCase();
            if (!lowerName.endsWith(".jar") || lowerName.endsWith("-sources.jar")) {
                continue;
            }
            if (!lowerName.startsWith("function-")) {
                continue;
            }

            String url = asset.get("browser_download_url").getAsString();
            if (lowerName.contains(versionToken)) {
                return Optional.of(url);
            }
            fallbackUrl = url;
        }

        return Optional.ofNullable(fallbackUrl);
    }

    private static String normalizeVersion(String version) {
        String normalized = version.trim();
        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    static boolean isNewer(String candidate, String current) {
        int[] candidateParts = versionParts(candidate);
        int[] currentParts = versionParts(current);
        int length = Math.max(candidateParts.length, currentParts.length);

        for (int i = 0; i < length; i++) {
            int candidatePart = i < candidateParts.length ? candidateParts[i] : 0;
            int currentPart = i < currentParts.length ? currentParts[i] : 0;
            if (candidatePart > currentPart) {
                return true;
            }
            if (candidatePart < currentPart) {
                return false;
            }
        }
        return false;
    }

    private static int[] versionParts(String version) {
        Matcher matcher = Pattern.compile("\\d+").matcher(version);
        return matcher.results()
                .map(result -> result.group())
                .mapToInt(Integer::parseInt)
                .toArray();
    }
}
