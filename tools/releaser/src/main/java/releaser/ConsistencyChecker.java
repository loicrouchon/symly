package releaser;

import java.util.Optional;
import java.util.regex.Pattern;

public class ConsistencyChecker {

    private static final Pattern RELEASE_VERSION_PATTERN = Pattern.compile("^[0-9]+(?:\\.[0-9]+)*$");
    private static final Pattern DEV_VERSION_PATTERN = Pattern.compile("^[0-9]+(?:\\.[0-9]+)*-SNAPSHOT$");

    public void check(GitRepository repository, Application application) {
        Version version = application.version();
        if (repository.isReleaseBranch()) {
            checkVersionBranchConsistency(RELEASE_VERSION_PATTERN, version, "release");
            repository.fetchRemote();
            Version releaseBaseVersion = repository.releaseBranchNumber();
            Optional<Version> latestTag = repository.latestTaggedVersionForBaseVersion(releaseBaseVersion);
            latestTag.ifPresent(tag -> ensureVersionDoesNotAlreadyExist(version, tag));
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

    private void ensureVersionDoesNotAlreadyExist(Version version, Version tag) {
        if (Version.compare(version, tag) < 0) {
            throw new ReleaseException(
                    """
                Invalid release version %s
                Next release version should be greater than the latest released tag (%s)"""
                            .formatted(version, tag));
        }
    }
}
