package releaser;

class ReleaseFailure extends RuntimeException {

    public ReleaseFailure(String message) {
        super(message);
    }

    public ReleaseFailure(String message, Throwable cause) {
        super(message, cause);
    }
}
