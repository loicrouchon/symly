package releaser;

import java.util.Objects;
import java.util.regex.Pattern;

public record Version(String version, String suffix) {

    private static final String VERSION_SEPARATOR = ".";
    private static final String SUFFIX_SEPARATOR = "-";
    private static final String VERSION_SPLIT = "\\.";
    private static final Pattern VERSION_PATTERN = Pattern.compile("^[0-9]+(:?\\.[0-9]+)*(:?-[a-zA-Z0-9]+)?$");

    public Version subversion(String subversion) {
        return new Version(version + VERSION_SEPARATOR + subversion, suffix);
    }

    public boolean isSubVersion(Version baseVersion) {
        return Objects.equals(version, baseVersion.version) || version.startsWith(baseVersion.version + ".");
    }

    public Version suffix(String suffix) {
        return new Version(version, suffix);
    }

    public Version withoutSuffix() {
        return new Version(version, null);
    }

    public Version increment() {
        String[] parts = version.split(VERSION_SPLIT);
        parts[parts.length - 1] = Integer.toString(Integer.parseInt(parts[parts.length - 1]) + 1);
        return new Version(String.join(VERSION_SEPARATOR, parts), suffix);
    }

    @Override
    public String toString() {
        if (suffix != null) {
            return version + SUFFIX_SEPARATOR + suffix;
        }
        return version;
    }

    public static Version parse(String fullVersion) {
        fullVersion = fullVersion.trim();
        if (!VERSION_PATTERN.matcher(fullVersion).matches()) {
            throw new ReleaseException(
                    """
                    Invalid version number: %s
                    Version numbers must follow the following pattern: %s"""
                            .formatted(fullVersion, VERSION_PATTERN.pattern()));
        }
        int suffixIndex = fullVersion.indexOf(SUFFIX_SEPARATOR);
        if (suffixIndex >= 0) {
            return new Version(fullVersion.substring(0, suffixIndex), fullVersion.substring(suffixIndex + 1));
        }
        return new Version(fullVersion, null);
    }

    public static int compare(Version a, Version b) {
        return compare(a.version, b.version);
    }

    public static int compare(String a, String b) {
        String[] av = a.split(VERSION_SPLIT);
        String[] bv = b.split(VERSION_SPLIT);
        for (int i = 0; i < Math.min(av.length, bv.length); i++) {
            if (!Objects.equals(av[i], bv[i])) {
                return Integer.compare(Integer.parseInt(av[i]), Integer.parseInt(bv[i]));
            }
        }
        return Integer.compare(av.length, bv.length);
    }
}
