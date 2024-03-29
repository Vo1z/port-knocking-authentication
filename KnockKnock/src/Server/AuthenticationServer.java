package Server;

import Utils.Constants;
import Utils.KnockUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

public class AuthenticationServer
{
    public final int numberOfAuthenticationSockets;
    public final int[] customUserPorts;

    private AuthenticationSocket[] authenticationSockets;
    private volatile Set<String>[] authenticationAddresses;
    private boolean isWorking = false;

    private final Map<String, String> messagesFromAuthorisedClients;
    private final String messageToClients;

    public AuthenticationServer(String messageToClients, int numberOfAuthenticationSockets)
    {
        if(numberOfAuthenticationSockets < 0)
            throw new IllegalArgumentException("Number of authentication sockets can not be smaller then one");

        this.customUserPorts = null;
        this.numberOfAuthenticationSockets = numberOfAuthenticationSockets;
        this.messagesFromAuthorisedClients = new HashMap<>();
        this.messageToClients = messageToClients;
    }

    public AuthenticationServer(int numberOfAuthenticationSockets)
    {
        if(numberOfAuthenticationSockets < 0)
            throw new IllegalArgumentException("Number of authentication sockets can not be smaller then one");

        this.customUserPorts = null;
        this.numberOfAuthenticationSockets = numberOfAuthenticationSockets;
        this.messagesFromAuthorisedClients = new HashMap<>();
        this.messageToClients = "Hello client!";
    }

    public AuthenticationServer(int... customUserPorts)
    {
        if (Arrays.stream(customUserPorts).anyMatch(port -> port < 0))
            throw new IllegalArgumentException("Port < 0");

        this.customUserPorts = customUserPorts;
        this.numberOfAuthenticationSockets = customUserPorts.length;
        this.messagesFromAuthorisedClients = new HashMap<>();
        this.messageToClients = "Hello client!";
    }

    public AuthenticationServer(String messageToClients, int... customUserPorts)
    {
        if (Arrays.stream(customUserPorts).anyMatch(port -> port < 0))
            throw new IllegalArgumentException("Port < 0");

        this.customUserPorts = customUserPorts;
        this.numberOfAuthenticationSockets = customUserPorts.length;
        this.messagesFromAuthorisedClients = new HashMap<>();
        this.messageToClients = messageToClients;
    }

    public void startServer()
    {
        if (!this.isWorking)
        {
            this.isWorking = true;

            try
            {
                this.authenticationSockets = new AuthenticationSocket[this.numberOfAuthenticationSockets];
                this.authenticationAddresses = new HashSet[this.numberOfAuthenticationSockets];

                if(this.customUserPorts == null)
                {
                    for (int i = 0; i < this.authenticationSockets.length; i++)
                    {
                        this.authenticationSockets[i] = new AuthenticationSocket(this, i);
                        this.authenticationSockets[i].start();
                        this.authenticationAddresses[i] = new HashSet<>();
                    }
                }
                else
                {
                    for (int i = 0; i < this.authenticationSockets.length; i++)
                    {
                        this.authenticationSockets[i] = new AuthenticationSocket(this, this.customUserPorts[i], i);
                        this.authenticationSockets[i].start();
                        this.authenticationAddresses[i] = new HashSet<>();
                    }
                }
            }
            catch (SocketException e)
            {
                e.printStackTrace();
            }
        }
    }

    public void stopServer()
    {
        if(this.isWorking)
        {
            this.isWorking = false;
            Arrays.stream(this.authenticationSockets)
                    .forEach(AuthenticationSocket::stopListening);

            this.authenticationAddresses = null;
            this.authenticationSockets = null;

            if(!Constants.IS_RUNTIME_IN_DEBUG_MODE)
                this.notifyAll();
        }
    }

    public void openSocketForSuchAddress(String remoteAddress, int remotePort)
    {
        try
        {
            ServerSocket openedSocketForClient = new ServerSocket(0);
            openedSocketForClient.setSoTimeout(Constants.SERVER_SOCKET_CONNECTION_TIMEOUT);

            KnockUtils.sendDatagramMessage(openedSocketForClient.getLocalPort() + "", remoteAddress, remotePort);

            Socket openedSocket = openedSocketForClient.accept();
            String clientAddress = openedSocket.getRemoteSocketAddress().toString().replaceAll(Constants.PORT_REGEX, "");

            if (clientAddress.equals(remoteAddress))
                (new ServerProcessing(openedSocket,this)).start();
            else
                openedSocket.close();
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
        }
    }

    public synchronized boolean checkAuthentication(String addressOfRequester, int authenticationSocketNumber)
    {
        if (this.authenticationAddresses[authenticationSocketNumber].contains(addressOfRequester))
            return false;

        for(int i = 0; i < authenticationSocketNumber; i++)
            if (!this.authenticationAddresses[i].contains(addressOfRequester))
                return false;

        return true;
    }

    public synchronized void addAddressToAuthenticationList(String address, int socketAuthenticationNumber)
    {
        this.authenticationAddresses[socketAuthenticationNumber].add(address);
    }

    public synchronized void removeAddressFromAuthenticationList(String address, int socketAuthenticationNumber)
    {
        for(int i = 0; i <= socketAuthenticationNumber; i++)
            this.authenticationAddresses[i].remove(address);
    }

    //Getters
    public boolean isWorking()
    {
        return this.isWorking;
    }

    public int[] getAuthenticationPorts()
    {
        if(!this.isWorking)
            throw new RuntimeException("Server was not started yet");
        if(this.authenticationSockets == null)
            return null;


        return Arrays.stream(this.authenticationSockets)
                .filter(Objects::nonNull)
                .mapToInt(AuthenticationSocket::getPort)
                .toArray();
    }

    public void addMessage(String address, String message)
    {
        this.messagesFromAuthorisedClients.put(address, message);
        //todo replace
        System.out.println("Client " + address + " responded with " + "\"" + message + "\"");
    }

    public Map<String, String> getMessagesFromAuthorisedClients()
    {
        return this.messagesFromAuthorisedClients;
    }

    public String getMessageToClients()
    {
        return this.messageToClients;
    }

    public Set<String>[] getAuthenticationAddresses()
    {
        return this.authenticationAddresses;
    }
}