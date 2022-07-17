import java.io.IOException;
import java.io.InputStreamReader;

import java.io.BufferedReader;
import java.util.Objects;

public class VersionBumper {

    private static final String VERSION_SEPARATOR = ".";
    private static final String VERSION_SPLIT = "\\.";

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("""
                Finds the highest existing version and increment the last digit by one.

                Usage:
                  echo $VERSIONS | java VersionComparator.java $BASE_VERSION
                Where:
                  - $VERSIONS is a list of existing versions, one per line.
                  - $BASE_VERSION is the current version we want to bump""");
            System.exit(1);
        }
        Version baseVersion = Version.parse(args[0]);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            String subVersion = br.lines()
                .map(version -> version.replaceFirst("^v", ""))
                .filter(version -> version.startsWith(baseVersion.version() + "."))
                .map(version -> version.substring(baseVersion.version().length() + 1))
                .max(VersionBumper::compareVersion)
                .map(VersionBumper::increment)
                .orElse("1");
            System.out.println(baseVersion.subversion(subVersion));
        }
    }

    private static int compareVersion(String a, String b) {
        String[] av = a.split(VERSION_SPLIT);
        String[] bv = b.split(VERSION_SPLIT);
        for (int i = 0; i < Math.min(av.length, bv.length); i++) {
            if (!Objects.equals(av[i], bv[i])) {
                return Integer.compare(Integer.parseInt(av[i]), Integer.parseInt(bv[i]));
            }
        }
        return Integer.compare(av.length, bv.length);
    }

    private static String increment(String version) {
        String[] parts = version.split(VERSION_SPLIT);
        parts[parts.length - 1] = Integer.toString(Integer.parseInt(parts[parts.length - 1]) + 1);
        return String.join(VERSION_SEPARATOR, parts);
    }

    private static record Version (String version, String suffix) {

        public Version subversion(String subversion) {
            return new Version(version + "." + subversion, suffix);
        }

        @Override
        public String toString() {
            if (suffix != null) {
                return version + "-" + suffix;
            }
            return version;
        }

        public static Version parse(String fullVersion) {
            int suffixIndex = fullVersion.indexOf('-');
            if (suffixIndex >= 0) {
                return new Version(fullVersion.substring(0, suffixIndex), fullVersion.substring(suffixIndex + 1));
            }
            return new Version(fullVersion, null);
        }
    }
}
