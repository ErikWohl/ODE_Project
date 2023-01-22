package at.fhtw.bic.ode_project.Service;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import at.fhtw.bic.ode_project.Enums.CommandEnum;
import at.fhtw.bic.ode_project.Enums.TcpStateEnum;
import at.fhtw.bic.ode_project.Exceptions.NumberOfRetriesExceededException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
public class TcpService implements Runnable {
    private Logger logger = LogManager.getLogger(TcpService.class);
    private Socket clientSocket;
    private SocketAddress socketAddress;
    private BufferedReader input;
    private PrintWriter output;

    private List<ClientObserver> clientObserverList;
    private TcpStateEnum currentState = TcpStateEnum.DISCONNETED;
    public TcpService(String iPAddress, int port) {
        this.socketAddress = new InetSocketAddress(iPAddress, port);
        this.clientObserverList = new ArrayList<>();
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

    public void addClientObserver(ClientObserver clientObserver) {
        clientObserverList.add(clientObserver);
    }
    public boolean isDisconnected() {
        return currentState == TcpStateEnum.DISCONNETED;
    }

    public boolean isStarting() {
        return currentState == TcpStateEnum.STARTING;
    }

    public boolean isConnected() {
        return currentState == TcpStateEnum.CONNECTED;
    }

    private void sendReceivedMessageToObservers(String message) {
        for(var observer : clientObserverList) {
            observer.onMessageReceive(message);
        }
    }
    private void sendDebugMessageToObservers(String message) {
        for(var observer : clientObserverList) {
            observer.onDebugMessage(message);
        }
    }

    public void sendCommand(CommandEnum commandEnum) {
        sendCommand(commandEnum, "");
    }
    public void sendCommand(CommandEnum commandEnum, String message) {
        // Das Loglevel von drawing command ist trace!
        if(commandEnum.equals(CommandEnum.DRAWING)) {
            logger.trace("Trying to send command connection status: " + currentState);
        } else {
            logger.debug("Trying to send command connection status: " + currentState);
        }

        output.println(commandEnum.getCommand() + message);

        if(commandEnum.equals(CommandEnum.DRAWING)) {
            logger.trace("Command:  " + commandEnum.getCommand() + " with message: " + message + " sent. ");

        } else {
            logger.debug("Command:  " + commandEnum.getCommand() + " with message: " + message + " sent. ");

        }
    }

    @Override
    public void run() {
        logger.info("Starting client ...");
        logger.debug("Current connection status: " + currentState);

        ReadWriteLock lock = new ReentrantReadWriteLock();
        lock.writeLock().lock();
        try {
            if(currentState != TcpStateEnum.DISCONNETED) {
                logger.info("Client is in another state other than disconnected!");
                return;
            }
            currentState = TcpStateEnum.STARTING;
            logger.debug("Setting connection status to " + currentState);
        } finally {
            lock.writeLock().unlock();
        }


        try {
            do {
                listenFromServer();
            } while(true);
        } catch (NumberOfRetriesExceededException e) {
            logger.info("Max number of tries reached.");
            sendDebugMessageToObservers("Verbindung mit Server ist fehlgeschlagen!");
            lock.writeLock().lock();
            try {
                currentState = TcpStateEnum.DISCONNETED;
                logger.debug("Setting connection status to " + currentState);
            } finally {
                lock.writeLock().unlock();
            }

        }
    }

    public void listenFromServer() throws NumberOfRetriesExceededException{
        try {
            if(currentState == TcpStateEnum.STARTING) {
                logger.info("Client establishing connection ...");
                clientSocket = new Socket();
                clientSocket.connect(socketAddress);
                input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                output = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);

                logger.info("Client connected successfully!");

                ReadWriteLock lock = new ReentrantReadWriteLock();
                lock.writeLock().lock();
                try {
                    currentState = TcpStateEnum.CONNECTED;
                    logger.debug("Setting connection status to " + currentState);
                } finally {
                    lock.writeLock().unlock();
                }
            }

            do {
                String message = input.readLine();
                logger.trace("Received message: " + message);
                sendReceivedMessageToObservers(message);
            } while (true);
        } catch (IOException e) {
            sendDebugMessageToObservers("Server disconnected, while receiving message!");
            logger.error("Server disconnected, while receiving message!");
            ReadWriteLock lock = new ReentrantReadWriteLock();
            lock.writeLock().lock();
            try {
                currentState = TcpStateEnum.ERROR;
                logger.debug("Setting connection status to " + currentState);
            } finally {
                lock.writeLock().unlock();
            }
            disconnect();
            retry();
        }
    }

    public void retry() throws NumberOfRetriesExceededException{
        int tryNum = 0;
        double secondsToWait = 1;

        logger.info("Trying to reconnect to server ...");

        ReadWriteLock lock = new ReentrantReadWriteLock();
        lock.writeLock().lock();
        try {
            currentState = TcpStateEnum.RETRYING;
            logger.debug("Setting connection status to " + currentState);

        } finally {
            lock.writeLock().unlock();
        }
        do {
            try {
                secondsToWait = Math.min(60, Math.pow(2, tryNum));
                tryNum++;
                logger.info("Retry (" + tryNum + ") connecting to server after sleep (" + secondsToWait + "s).");
                logger.debug("Current connection status: " + currentState);

                Thread.sleep((long)secondsToWait * 1000);
                clientSocket = new Socket();
                clientSocket.connect(socketAddress);
                input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                output = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);
                logger.info("Server connection established.");
                break;
            } catch (IOException e) {
                logger.error("Connection retry failed.");
                if(tryNum >= 4){
                    throw new NumberOfRetriesExceededException();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }while(true);

        logger.info("Finished reestablishing server connection.");

        lock.writeLock().lock();
        try {
            currentState = TcpStateEnum.CONNECTED;
            logger.debug("Setting connection status to " + currentState);
        } finally {
            lock.writeLock().unlock();
        }
    }
    public void disconnect() {
        //Wird benutzt falls man kein rety haben will
/*
        ReadWriteLock lock = new ReentrantReadWriteLock();
        lock.writeLock().lock();
        try {
            currentState = TcpStateEnum.DISCONNETED;
        } finally {
            lock.writeLock().unlock();
        }
*/

        try {
            logger.info("Try disconnecting from server.");
            if(input != null) {
                input.close();
            }
            if(output != null) {
                output.close();
            }
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
