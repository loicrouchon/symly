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
        String baseVersion = args[0];
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            String subVersion = br.lines()
                .filter(version -> version.startsWith(baseVersion + "."))
                .map(version -> version.substring(baseVersion.length() + 1))
                .max(VersionBumper::compareVersion)
                .map(VersionBumper::increment)
                .orElse("1");
            String fullVersion = String.format("%s.%s", baseVersion, subVersion);
            System.out.println(fullVersion);
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
}
