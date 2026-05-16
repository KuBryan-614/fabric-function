package kuku.update;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class UpdateDownloader {
    private static final Logger LOGGER = LoggerFactory.getLogger("function-updater");

    private static final String MOD_FILE_PREFIX = "function-";
    private static final String MOD_FILE_SUFFIX = ".jar";
    private static final String DISABLED_SUFFIX = ".disabled";

    private UpdateDownloader() {
    }

    public static void downloadAndPrepare(Path modsDir, String downloadUrl, String newVersion) {
        Path targetFile = modsDir.resolve(MOD_FILE_PREFIX + newVersion + MOD_FILE_SUFFIX);
        Path tempFile = modsDir.resolve(MOD_FILE_PREFIX + newVersion + MOD_FILE_SUFFIX + ".download");

        try {
            Files.createDirectories(modsDir);

            if (Files.exists(targetFile)) {
                LOGGER.info("Version v{} already exists at {}.", newVersion, targetFile);
                return;
            }

            LOGGER.info("Downloading Function v{} from {}", newVersion, downloadUrl);
            download(downloadUrl, tempFile);

            List<Path> disabledOldJars = disableOldFunctionJars(modsDir, targetFile.getFileName().toString());
            Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING);

            LOGGER.info("Downloaded Function v{} to {}.", newVersion, targetFile);
            if (!disabledOldJars.isEmpty()) {
                LOGGER.info("Disabled old Function jars: {}", disabledOldJars);
            }
            LOGGER.info("Restart the server to load Function v{}.", newVersion);
        } catch (Exception e) {
            cleanupTempFile(tempFile);
            LOGGER.warn("Failed to prepare Function update v{}.", newVersion, e);
        }
    }

    private static void download(String downloadUrl, Path tempFile) throws Exception {
        URL url = URI.create(downloadUrl).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", "function-mod-updater");
        connection.setRequestProperty("Accept", "application/octet-stream");
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(60_000);
        connection.setInstanceFollowRedirects(true);

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IllegalStateException("Download failed. HTTP status: " + responseCode);
        }

        try (InputStream inputStream = connection.getInputStream()) {
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            connection.disconnect();
        }

        if (Files.size(tempFile) == 0) {
            throw new IllegalStateException("Downloaded file is empty.");
        }
    }

    private static List<Path> disableOldFunctionJars(Path modsDir, String keepFileName) throws Exception {
        List<Path> disabled = new ArrayList<>();
        try (Stream<Path> paths = Files.list(modsDir)) {
            for (Path path : paths.toList()) {
                String fileName = path.getFileName().toString();
                if (!Files.isRegularFile(path) || !isFunctionModJar(fileName) || fileName.equals(keepFileName)) {
                    continue;
                }

                Path disabledPath = nextDisabledPath(path);
                Files.move(path, disabledPath, StandardCopyOption.REPLACE_EXISTING);
                disabled.add(disabledPath.getFileName());
            }
        }
        return disabled;
    }

    private static boolean isFunctionModJar(String fileName) {
        String lowerName = fileName.toLowerCase();
        return lowerName.startsWith(MOD_FILE_PREFIX)
                && lowerName.endsWith(MOD_FILE_SUFFIX)
                && !lowerName.endsWith("-sources.jar");
    }

    private static Path nextDisabledPath(Path jarPath) {
        Path candidate = jarPath.resolveSibling(jarPath.getFileName() + DISABLED_SUFFIX);
        int counter = 1;
        while (Files.exists(candidate)) {
            candidate = jarPath.resolveSibling(jarPath.getFileName() + DISABLED_SUFFIX + "." + counter);
            counter++;
        }
        return candidate;
    }

    private static void cleanupTempFile(Path tempFile) {
        try {
            Files.deleteIfExists(tempFile);
        } catch (Exception ignored) {
        }
    }
}
