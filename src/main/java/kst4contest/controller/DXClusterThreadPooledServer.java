package kst4contest.controller;

import kst4contest.model.ChatMember;
import kst4contest.model.ThreadStateMessage;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DXClusterThreadPooledServer implements Runnable {

    private static final Logger LOGGER =
            Logger.getLogger(DXClusterThreadPooledServer.class.getName());
    private static final String THREAD_NICKNAME = "DXCluster-Server";

    private final List<Socket> clientSockets =
            Collections.synchronizedList(new ArrayList<>());

    private final ChatController chatController;
    private final ThreadStatusCallback callBackToController;
    private final int serverPort;

    private final ExecutorService threadPool =
            Executors.newFixedThreadPool(10);

    private final ScheduledExecutorService keepAliveExecutor =
            Executors.newSingleThreadScheduledExecutor();

    private volatile boolean stopped;
    private ServerSocket serverSocket;

    public DXClusterThreadPooledServer(
            int port,
            ChatController chatController,
            ThreadStatusCallback callback
    ) {
        this.serverPort = port;
        this.chatController = chatController;
        this.callBackToController = callback;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("DXCluster-thread-pooled-server");

        try {
            serverSocket = new ServerSocket(serverPort);

            if (stopped) {
                return;
            }

            callBackToController.onThreadStatus(
                    THREAD_NICKNAME,
                    new ThreadStateMessage(
                            THREAD_NICKNAME,
                            true,
                            "Listening on TCP port " + serverPort,
                            false
                    )
            );

            keepAliveExecutor.scheduleAtFixedRate(
                    this::sendKeepAlive,
                    30,
                    30,
                    TimeUnit.SECONDS
            );

            while (!stopped) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    clientSockets.add(clientSocket);

                    threadPool.execute(
                            new DXClusterServerWorkerRunnable(
                                    clientSocket,
                                    clientSockets
                            )
                    );
                } catch (IOException exception) {
                    if (!stopped) {
                        LOGGER.log(
                                Level.SEVERE,
                                "Error accepting DX Cluster client connection",
                                exception
                        );
                    }
                }
            }
        } catch (IOException exception) {
            if (!stopped) {
                LOGGER.log(
                        Level.SEVERE,
                        "Cannot open DX Cluster TCP port " + serverPort,
                        exception
                );

                callBackToController.onThreadStatus(
                        THREAD_NICKNAME,
                        new ThreadStateMessage(
                                THREAD_NICKNAME,
                                false,
                                "Cannot open TCP port "
                                        + serverPort
                                        + ": "
                                        + exception.getMessage(),
                                true
                        )
                );
            }
        } finally {
            closeServerSocket();
            closeClientSockets();
            keepAliveExecutor.shutdownNow();
            threadPool.shutdownNow();
        }
    }

    public synchronized void stop() {
        stopped = true;
        closeServerSocket();
        closeClientSockets();
        keepAliveExecutor.shutdownNow();
        threadPool.shutdownNow();
    }

    public boolean hasConnectedClients() {
        synchronized (clientSockets) {
            removeClosedClients();
            return !clientSockets.isEmpty();
        }
    }

    /**
     * Sends one DX Cluster spot to all currently connected clients.
     *
     * @return true if the spot was delivered to at least one client
     */
    public boolean broadcastSingleDXClusterEntryToLoggers(
            ChatMember chatMember
    ) {
        final byte[] clusterPayload;
        final String clusterMessage;

        try {
            String frequency = Utils4KST.normalizeFrequencyString(
                    chatMember.getFrequency().getValue(),
                    chatController
                            .getChatPreferences()
                            .getNotify_optionalFrequencyPrefix()
            );

            clusterPayload = DXClusterSpotFormatter.formatPayload(
                    chatController
                            .getChatPreferences()
                            .getNotify_DXCSrv_SpottersCallSign()
                            .getValue(),
                    frequency,
                    chatMember.getCallSign(),
                    chatMember.getQra(),
                    new Utils4KST()
                            .time_generateCurrenthhmmZTimeStringForClusterMessage()
            );
            clusterMessage = new String(
                    clusterPayload,
                    StandardCharsets.US_ASCII
            );
        } catch (IllegalArgumentException exception) {
            LOGGER.log(
                    Level.WARNING,
                    "DX Cluster spot rejected: " + exception.getMessage()
            );
            return false;
        } catch (Exception exception) {
            LOGGER.log(
                    Level.SEVERE,
                    "Cannot build DX Cluster message",
                    exception
            );
            return false;
        }

        int deliveredClients = 0;

        synchronized (clientSockets) {
            Iterator<Socket> iterator = clientSockets.iterator();

            while (iterator.hasNext()) {
                Socket socket = iterator.next();

                if (socket == null || socket.isClosed()) {
                    iterator.remove();
                    continue;
                }

                try {
                    OutputStream output = socket.getOutputStream();
                    output.write(clusterPayload);
                    output.flush();
                    deliveredClients++;
                } catch (IOException exception) {
                    LOGGER.log(
                            Level.WARNING,
                            "DX Cluster client disconnected while sending a spot",
                            exception
                    );

                    closeSocket(socket);
                    iterator.remove();
                }
            }
        }

        if (deliveredClients > 0) {
            callBackToController.onThreadStatus(
                    THREAD_NICKNAME,
                    new ThreadStateMessage(
                            THREAD_NICKNAME,
                            true,
                            "Last spot sent to "
                                    + deliveredClients
                                    + " DX Cluster client(s):\n"
                                    + clusterMessage,
                            false
                    )
            );
        }

        return deliveredClients > 0;
    }

    private void sendKeepAlive() {
        synchronized (clientSockets) {
            Iterator<Socket> iterator = clientSockets.iterator();

            while (iterator.hasNext()) {
                Socket socket = iterator.next();

                if (socket == null || socket.isClosed()) {
                    iterator.remove();
                    continue;
                }

                try {
                    OutputStream output = socket.getOutputStream();
                    output.write(
                            "\r\n".getBytes(StandardCharsets.US_ASCII)
                    );
                    output.flush();
                } catch (IOException exception) {
                    closeSocket(socket);
                    iterator.remove();
                }
            }
        }
    }

    private void removeClosedClients() {
        clientSockets.removeIf(
                socket -> socket == null || socket.isClosed()
        );
    }

    private synchronized void closeServerSocket() {
        if (serverSocket == null || serverSocket.isClosed()) {
            return;
        }

        try {
            serverSocket.close();
        } catch (IOException exception) {
            LOGGER.log(
                    Level.WARNING,
                    "Error closing DX Cluster server socket",
                    exception
            );
        }
    }

    private void closeClientSockets() {
        synchronized (clientSockets) {
            for (Socket socket : clientSockets) {
                closeSocket(socket);
            }

            clientSockets.clear();
        }
    }

    private static void closeSocket(Socket socket) {
        if (socket == null || socket.isClosed()) {
            return;
        }

        try {
            socket.close();
        } catch (IOException ignored) {
            // The connection is already unusable.
        }
    }
}

class DXClusterServerWorkerRunnable implements Runnable {

    private static final Logger LOGGER =
            Logger.getLogger(DXClusterServerWorkerRunnable.class.getName());

    private final Socket clientSocket;
    private final List<Socket> clientSockets;

    DXClusterServerWorkerRunnable(
            Socket clientSocket,
            List<Socket> clientSockets
    ) {
        this.clientSocket = clientSocket;
        this.clientSockets = clientSockets;
    }

    @Override
    public void run() {
        try {
            OutputStream output = clientSocket.getOutputStream();
            output.write(
                    "login: ".getBytes(StandardCharsets.US_ASCII)
            );
            output.flush();

            System.out.println(
                    "[DXClusterServer] New client connected: "
                            + clientSocket.getInetAddress()
            );
        } catch (IOException exception) {
            LOGGER.log(
                    Level.WARNING,
                    "Cannot initialise DX Cluster client connection",
                    exception
            );

            synchronized (clientSockets) {
                clientSockets.remove(clientSocket);
            }

            try {
                clientSocket.close();
            } catch (IOException ignored) {
                // The connection is already unusable.
            }
        }
    }
}
