public class BaseVersionFromGitRef {

    private static final String RELEASE_REF = "refs/heads/release/";

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("""
                Parse the git ref and extract the base version to use

                Usage:
                  java VersionComparator.java $GIT_REF $COMMIT_HASH
                Where:
                  - $GIT_REF is the git ref.
                  - $COMMIT_HASH is the current commit hash""");
            System.exit(1);
        }
        System.out.println(extractBaseVersion(args[0], args[1]));
    }

    private static String extractBaseVersion(String ref, String commitHash) {
        if (ref.startsWith(RELEASE_REF) && ref.length() > RELEASE_REF.length()) {
            return ref.substring(RELEASE_REF.length());
        }
        return String.format("0-dev+%.7s%n", commitHash);
    }
}
