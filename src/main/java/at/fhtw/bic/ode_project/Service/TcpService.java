package at.fhtw.bic.ode_project.Service;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import at.fhtw.bic.ode_project.Controller.CommandEnum;
import at.fhtw.bic.ode_project.Exceptions.NumberOfRetriesExceededException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
public class TcpService implements Runnable {
    private Logger logger = LogManager.getLogger(TcpService.class);
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
        logger.debug("Trying to send message connection status: " + started);
        output.println(message);
        logger.debug("Message:  " + message + "sent. ");

    }

    public void sendCommand(String message, CommandEnum commandEnum) {
        logger.debug("Trying to send command connection status: " + started);
        output.println(commandEnum.getCommand() + message);
        logger.debug("Command:  " + message + "sent. ");
    }

    @Override
    public void run() {
        ReadWriteLock lock = new ReentrantReadWriteLock();
        lock.writeLock().lock();
        try {
            if(started) {
                logger.info("Client is already started!");
                return;
            }
            started = true;
        } finally {
            lock.writeLock().unlock();
        }


        try {
            do {
                listenFromServer();
            } while(true);
        } catch (NumberOfRetriesExceededException e) {
            logger.info("Max number of tries reached.");
            logger.info("Setting client connection started to false.");
            clientObserver.onDebugMessage("Verbindung mit Server ist fehlgeschlagen!");
            lock.writeLock().lock();
            try {
                started = false;
            } finally {
                lock.writeLock().unlock();
            }

        }
    }

    public void listenFromServer() throws NumberOfRetriesExceededException{
        try {
            if(!clientSocket.isConnected()) {
                clientSocket.connect(socketAddress);
                input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                output = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);
            }

            do {
                String message = input.readLine();
                logger.info("Received message: " + message);
                clientObserver.onMessageReceive(message);
            } while (true);
        } catch (IOException e) {
            logger.info("Server disconnected, while receiving message!");
            disconnect();
            retry();
        }
    }

    public void retry() throws NumberOfRetriesExceededException{
        int tryNum = 0;
        double secondsToWait = 1;
        do {
            try {
                secondsToWait = Math.min(60, Math.pow(2, tryNum));
                tryNum++;
                logger.info("Retry (" + tryNum + ") connecting to server after sleep (" + secondsToWait + "s).");
                Thread.sleep((long)secondsToWait * 1000);
                clientSocket = new Socket();
                clientSocket.connect(socketAddress);
                input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                output = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);
                logger.info("Server connection established.");
                break;
            } catch (IOException e) {
                logger.info("Connection retry failed.");
                if(tryNum >= 4){
                    throw new NumberOfRetriesExceededException();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }while(true);

        ReadWriteLock lock = new ReentrantReadWriteLock();
        lock.writeLock().lock();
        try {
            started = true;
        } finally {
            lock.writeLock().unlock();
        }

        logger.info("Finished reestablishing server connection.");
    }
    public void disconnect() {
        ReadWriteLock lock = new ReentrantReadWriteLock();
        lock.writeLock().lock();
        try {
            started = false;
        } finally {
            lock.writeLock().unlock();
        }

        try {
            logger.info("Try disconnecting from server.");
            input.close();
            output.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
