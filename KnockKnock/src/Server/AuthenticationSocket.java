package Server;

import Utils.Constants;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class AuthenticationSocket extends Thread
{
    public final int authenticationSocketNumber;

    private boolean isWorking = false;
    private final AuthenticationServer server;
    private DatagramSocket udpSocket;

    public AuthenticationSocket(AuthenticationServer server, int authenticationNumber) throws SocketException
    {
        this.authenticationSocketNumber = authenticationNumber;
        this.server = server;
        this.udpSocket = new DatagramSocket(0);
        this.udpSocket.setSoTimeout(Constants.AUTHENTICATION_SOCKET_TIMEOUT);
    }

    public AuthenticationSocket(AuthenticationServer server, int customUserPort, int authenticationNumber)
    {
        this.authenticationSocketNumber = authenticationNumber;
        this.server = server;
        try
        {
            this.udpSocket = new DatagramSocket(customUserPort);
            this.udpSocket.setSoTimeout(Constants.AUTHENTICATION_SOCKET_TIMEOUT);
        }
        catch (SocketException e)
        {
            System.err.println("Such port can not be opened in AuthenticationSocket: " + customUserPort);
            System.exit(-1);
        }
    }

    public void stopListening()
    {
        this.isWorking = false;
    }

    @Override
    public void run()
    {
        this.isWorking = true;
        try
        {
            byte[] buffer = new byte[Constants.MAX_DATAGRAM_SIZE];
            DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);

            while (this.isWorking)
            {
                this.udpSocket.receive(datagramPacket);
                int incomePort = Integer.parseInt(datagramPacket.getSocketAddress().toString()
                        .replaceAll(Constants.ADDRESS_REGEX, "")
                        .replaceAll(":|/", ""));
                String incomeAddress = datagramPacket.getSocketAddress().toString().replaceAll(Constants.PORT_REGEX, "");

                if (this.server.checkAuthentication(incomeAddress, this.authenticationSocketNumber))
                {
                    if (this.authenticationSocketNumber == this.server.numberOfAuthenticationSockets - 1)
                    {
                        this.server.removeAddressFromAuthenticationList(incomeAddress, authenticationSocketNumber);
                        this.server.openSocketForSuchAddress(incomeAddress, incomePort);
                    }
                    else
                    {
                        this.server.addAddressToAuthenticationList(incomeAddress, this.authenticationSocketNumber);
                    }
                }
                else
                    this.server.removeAddressFromAuthenticationList(incomeAddress, this.authenticationSocketNumber);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public int getPort()
    {
        return this.udpSocket.getLocalPort();
    }
}