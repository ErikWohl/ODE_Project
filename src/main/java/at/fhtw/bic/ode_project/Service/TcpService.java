package at.fhtw.bic.ode_project.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TcpService implements Runnable {
    private Socket clientSocket;
    private SocketAddress socketAddress;
    private BufferedReader input;
    private PrintWriter output;

    private ClientObserver clientObserver;
    private boolean started = false;
    public TcpService(String iPAddress, int port) {
        this.socketAddress = new InetSocketAddress(iPAddress, port);
        this.clientSocket = new Socket();
    }

    public Socket getClientSocket() {
        return clientSocket;
    }

    public SocketAddress getSocketAddress() {
        return socketAddress;
    }

    public void setSocketAddress(SocketAddress socketAddress) {
        this.socketAddress = socketAddress;
    }

    public void setClientObserver(ClientObserver clientObserver) {
        this.clientObserver = clientObserver;
    }
    public boolean isStarted() {
        return started;
    }

    public void sendMessage(String message) {
        try {
            output = new PrintWriter(clientSocket.getOutputStream(), true);
            output.println(message);
        } catch (IOException e) {
            System.out.println("Server disconnected, while sending message!");
            disconnect();
            retry();
        }
    }

    @Override
    public void run() {
        ReadWriteLock lock = new ReentrantReadWriteLock();
        lock.writeLock().lock();
        try {
            if(started) {
                System.out.println("Client is already started!");
                return;
            }
            started = true;
        } finally {
            lock.writeLock().unlock();
        }


        do {
            listenFromServer();
        } while (true);
    }

    public void listenFromServer() {
        try {
            if(!clientSocket.isConnected())
                clientSocket.connect(socketAddress);
            do {
                input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String message = input.readLine();
                System.out.println("Client: Message from Server " + message);
                clientObserver.onMessageReceive(message);
            } while (true);
        } catch (IOException e) {
            System.out.println("Server disconnected, while receiving message!");
            disconnect();
            retry();
        }
    }

    public void retry() {
        int tryNum = 0;
        double secondsToWait = 1;
        do {
            try {
                if(tryNum >= 4){
                    //throw new NumberOfRetriesExceededException();
                    System.out.println("Max number of tries reached.");
                    break;
                }

                secondsToWait = Math.min(60, Math.pow(2, tryNum));
                tryNum++;
                System.out.println("Retry (" + tryNum + ") connecting to server after sleep (" + secondsToWait + "s).");
                Thread.sleep((long)secondsToWait * 1000);
                clientSocket = new Socket();
                clientSocket.connect(socketAddress);
                System.out.println("Server connection established.");
                break;
            } catch (IOException e) {
                System.out.println("Connection retry failed.");
                if(tryNum == 4) {
                    e.printStackTrace();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }while(true);
        System.out.println("Finished reestablishing server connection.");
    }
    public void disconnect() {
        try {
            System.out.println("Try disconnecting from server.");
            //input.close();
            //output.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
