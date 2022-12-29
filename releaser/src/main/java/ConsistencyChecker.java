import java.util.regex.Pattern;

public class ConsistencyChecker {

    private static final Pattern RELEASE_VERSION_PATTERN = Pattern.compile("^[0-9]+(?:\\.[0-9]+)*$");
    private static final Pattern DEV_VERSION_PATTERN = Pattern.compile("^[0-9]+(?:\\.[0-9]+)*-dev$");

    public void check(GitRepository repo, Application application) {
        Version version = application.version();
        if (repo.isReleaseBranch()) {
            checkVersionBranchConsistency(RELEASE_VERSION_PATTERN, version, "release");
        } else {
            checkVersionBranchConsistency(DEV_VERSION_PATTERN, version, "dev");
        }
    }

    private static void checkVersionBranchConsistency(Pattern versionPattern, Version version, String branchType) {
        String versionStr = version.toString();
        if (!versionPattern.matcher(versionStr).matches()) {
            throw new ReleaseException(
                    """
                    Invalid version number for %s branch %s.
                    Version numbers must follow pattern %s"""
                            .formatted(branchType, versionStr, versionPattern.pattern()));
        }
    }
}
