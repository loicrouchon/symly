package releaser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record BranchingModel(
        String remote, String mainBranch, String releaseBranchPrefix, Pattern releaseBranchPattern) {

    public static BranchingModel of(String remote, String mainBranch, String releaseBranchPrefix) {
        return new BranchingModel(
                remote,
                mainBranch,
                releaseBranchPrefix,
                Pattern.compile("^" + releaseBranchPrefix + "/([0-9]+(:?\\.[0-9]+)*)$"));
    }

    public boolean isAReleaseBranch(String branch) {
        return releaseBranchPattern.matcher(branch).matches();
    }

    public Version releaseBranchNumber(String branch) {
        Matcher matcher = releaseBranchPattern.matcher(branch);
        if (!matcher.matches()) {
            throw new IllegalStateException("%s is not a release branch".formatted(branch));
        }
        return Version.parse(matcher.group(1));
    }
}
