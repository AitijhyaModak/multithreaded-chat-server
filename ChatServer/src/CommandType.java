public enum CommandType {
    SEND_PRIVATE_MESSAGE(3),
    BROADCAST_MESSAGE( 2),
    LIST_USERS( 1),
    BLOCK_USER( 2),
    UNBLOCK_USER(2),
    HELP( 1),
    INVALID_COMMAND( 1),
    EXIT(1);


    private final int argCount;

    CommandType( int argCount) {
        this.argCount = argCount;
    }

    public int getArgCount() { return this.argCount; }
}
