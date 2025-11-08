import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppUsageReader {

    public static class AppInfo {
        public String name;
        public long usageCount;

        public AppInfo(String name, long usageCount) {
            this.name = name;
            this.usageCount = usageCount;
        }

        @Override
        public String toString() {
            return name + " (used " + usageCount + " times)";
        }
    }

    public static List<AppInfo> readAppUsage(String filePath) {
        List<AppInfo> appInfos = new ArrayList<>();

        try {
            Path resolvedPath = resolveUsagePath(filePath);
            if (resolvedPath == null) {
                System.out.println("No usage data found. Run the tracker to create " + filePath);
                return appInfos;
            }

            String json = Files.readString(resolvedPath);
            Pattern appPattern = Pattern.compile(
                    "\\{\\s*\"name\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"usage_count\"\\s*:\\s*(\\d+)\\s*}",
                    Pattern.MULTILINE);
            Matcher matcher = appPattern.matcher(json);
            while (matcher.find()) {
                String name = matcher.group(1);
                long usageCount = Long.parseLong(matcher.group(2));
                appInfos.add(new AppInfo(name, usageCount));
            }
        } catch (IOException e) {
            System.out.println("Error reading app info file: " + e.getMessage());
        }
        return appInfos;
    }

    private static Path resolveUsagePath(String filePath) {
        Path requested = Paths.get(filePath);
        if (Files.exists(requested)) {
            return requested;
        }

        Set<Path> candidates = new LinkedHashSet<>();
        Path cwd = Paths.get(System.getProperty("user.dir"));

        if (!requested.isAbsolute()) {
            candidates.add(cwd.resolve(filePath));
            candidates.add(cwd.resolve("OrgoGoblins").resolve(filePath));
        }

        try {
            Path codeSource = Paths.get(AppUsageReader.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());
            Path base = codeSource.getParent();
            if (base != null) {
                candidates.add(base.resolve(filePath));
                Path parent = base.getParent();
                if (parent != null) {
                    candidates.add(parent.resolve(filePath));
                }
            }
        } catch (URISyntaxException ignored) {
        }

        if (requested.isAbsolute()) {
            candidates.add(requested);
        }

        for (Path candidate : candidates) {
            if (candidate != null && Files.exists(candidate)) {
                return candidate;
            }
        }
        return null;
    }
}
