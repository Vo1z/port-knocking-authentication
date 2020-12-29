package Server;

import Utils.Constants;
import Utils.KnockUtils;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;

public class AuthenticationServer
{
    public final int numberOfAuthenticationSockets;

    private AuthenticationSocket[] authenticationSockets;
    private volatile HashSet<String>[] authenticationList;
    private boolean isWorking = false;

    public AuthenticationServer(int numberOfAuthenticationSockets)
    {
        this.numberOfAuthenticationSockets = numberOfAuthenticationSockets;
    }

    public void startServer()
    {
        if (!isWorking)
        {
            isWorking = true;
            initializeServer();

            Arrays.stream(this.authenticationSockets)
                    .forEach(Thread::start);
        }
    }

    public void stopServer()
    {
        if(this.isWorking)
        {
            this.isWorking = false;
            Arrays.stream(this.authenticationSockets)
                    .forEach(AuthenticationSocket::stopListening);

            this.authenticationList = null;
            this.authenticationSockets = null;
        }
    }

    private void initializeServer()
    {
        try
        {
            this.authenticationSockets = new AuthenticationSocket[this.numberOfAuthenticationSockets];
            this.authenticationList = new HashSet[this.numberOfAuthenticationSockets];
            for (int i = 0; i < this.authenticationSockets.length; i++)
                this.authenticationSockets[i] = new AuthenticationSocket(this, i);
        }
        catch (SocketException e)
        {
            e.printStackTrace();
        }
    }

    public void openSocketForSuchAddress(String remoteAddress, int remotePort)
    {
        try
        {
            ServerSocket serverSocket = new ServerSocket();
            serverSocket.setSoTimeout(Constants.CONNECTION_TIMEOUT);
            KnockUtils.sendDatagramMessage(serverSocket.getInetAddress().toString(), remoteAddress, remotePort);

            Socket openedSocket = serverSocket.accept();

            //Checks if received socket address is corresponding to address that has passed authentication
            if (((InetSocketAddress)openedSocket.getRemoteSocketAddress()).getHostName().equals(remoteAddress))
                (new ServerProcessing(serverSocket.accept(), "You were authenticated")).start();
            else
                openedSocket.close();
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
        }
    }

    //Checks if given address was trying to access previous sockets
    public boolean checkAuthentication(String addressOfRequester, int authenticationSocketNumber)
    {
        for(int i = 0; i < authenticationSocketNumber; i++)
            if (!this.authenticationList[i].contains(addressOfRequester))
                return false;

        return true;
    }

    public synchronized void addAddressToAuthenticationList(String address, int socketAuthenticationNumber)
    {
        this.authenticationList[socketAuthenticationNumber].add(address);
    }

    public synchronized void removeAddressFromAuthenticationList(String address, int socketAuthenticationNumber)
    {
        for(int i = socketAuthenticationNumber; i >= 0; i--)
            this.authenticationList[i].remove(address);
    }

    //Getters
    public boolean isWorking()
    {
        return this.isWorking;
    }

    public int[] getAuthenticationPorts()
    {
        if(authenticationSockets == null)
            return null;

        return Arrays.stream(this.authenticationSockets)
                .filter(Objects::nonNull)
                .mapToInt(AuthenticationSocket::getPort)
                .toArray();
    }
}