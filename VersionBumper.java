import java.io.*;

import java.io.IOException;
import java.io.InputStreamReader;

import java.io.BufferedReader;
import java.util.Objects;

public class VersionBumper {

    private static final String VERSION_SEPARATOR = ".";
    private static final String VERSION_SPLIT = "\\.";

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Finds the highest existing version and increment the last digit by one.");
            System.out.println("");
            System.out.println("Usage:");
            System.out.println("  echo $VERSIONS > java VersionComparator.java $BASE_VERSION");
            System.out.println("Where:");
            System.out.println("  - $VERSIONS is a list of existing versions, one per line.");
            System.out.println("  - $BASE_VERSION is the current version we want to bump");
            System.exit(1);
        }
        String baseVersion = args[0];
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            String version = br.lines()
                    .filter(tag -> tag.startsWith(baseVersion + "."))
                    .max(VersionBumper::compareVersion)
                    .map(VersionBumper::increment)
                    .orElse(baseVersion + ".1");
            System.out.println(version);
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

    private static String increment(String tag) {
        String[] parts = tag.split(VERSION_SPLIT);
        parts[parts.length - 1] = Integer.toString(Integer.parseInt(parts[parts.length - 1]) + 1);
        return String.join(VERSION_SEPARATOR, parts);
    }
}
