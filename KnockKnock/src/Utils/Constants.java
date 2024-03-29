package Utils;

public class Constants
{
    private static final int MIN_MTU = 576;
    private static final int MAX_IP_HEADER_SIZE = 60;
    private static final int UDP_HEADER_SIZE = 8;
    public static final int MAX_DATAGRAM_SIZE = MIN_MTU - MAX_IP_HEADER_SIZE - UDP_HEADER_SIZE;
    public static final int SERVER_SOCKET_CONNECTION_TIMEOUT = 0;
    public static final int AUTHENTICATION_SOCKET_TIMEOUT = 0;
    public static final int CLIENT_SOCKET_TIMEOUT = 5000;
    public static final int MESSAGE_DELAY_MILLISECONDS = 30;
    public static final String PORT_REGEX = ":\\d+|/";
    public static final String ADDRESS_REGEX = "(\\d+(\\.|:)+)|/";
    public static final String ADDRESS_REGEX_ON_ARGUMENTS = "(\\d+\\.)+(\\d+)?";
    public static boolean IS_RUNTIME_IN_DEBUG_MODE = false;
    public static boolean IS_CLIENT_HAS_INFINITE_LIFETIME = false;
}