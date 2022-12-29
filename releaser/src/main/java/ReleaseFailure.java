class ReleaseFailure extends RuntimeException {

    public ReleaseFailure(String message, Throwable cause) {
        super(message, cause);
    }
}
