package kst4contest.controller;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.function.LongPredicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import kst4contest.model.ChatMessage;

/**
 * Reads exactly one immutable ON4KST connection session.
 *
 * <p>EOF is a connection-loss event, not an empty chat message. Every line is
 * associated with the session id captured by this reader, so a delayed exception
 * from an obsolete socket cannot affect a newer reconnect.</p>
 */
public class ReadThread extends Thread {
    private static final Logger LOGGER = Logger.getLogger(ReadThread.class.getName());

    private final long sessionId;
    private final Socket socket;
    private final LinkedBlockingQueue<ChatMessage> receiveQueue;
    private final LongPredicate sessionIsActive;
    private final Consumer<String> inboundActivity;
    private final Consumer<Throwable> connectionFailure;
    private final BufferedReader reader;

    /**
     * Compatibility constructor for the pre-session controller path.
     *
     * @deprecated new connections should be created by
     *             {@link On4KstConnectionManager}
     */
    @Deprecated
    public ReadThread(Socket socket, ChatController client) throws IOException {
        this(0L, socket, client.getMessageRXBus(), ignored -> true,
                ignored -> { }, ignored -> { });
    }

    /**
     * Creates the reader for one connection generation.
     *
     * @param sessionId immutable id of the owning socket session
     * @param socket connected ON4KST socket
     * @param receiveQueue private receive queue belonging to this session
     * @param sessionIsActive guard against callbacks from an obsolete session
     * @param inboundActivity callback used for liveness and protocol progress
     * @param connectionFailure callback for EOF, I/O and unexpected runtime errors
     * @throws IOException if the socket input stream cannot be opened
     */
    public ReadThread(
            long sessionId,
            Socket socket,
            LinkedBlockingQueue<ChatMessage> receiveQueue,
            LongPredicate sessionIsActive,
            Consumer<String> inboundActivity,
            Consumer<Throwable> connectionFailure
    ) throws IOException {
        this.sessionId = sessionId;
        this.socket = socket;
        this.receiveQueue = receiveQueue;
        this.sessionIsActive = sessionIsActive;
        this.inboundActivity = inboundActivity;
        this.connectionFailure = connectionFailure;
        this.reader = new BufferedReader(new InputStreamReader(
                socket.getInputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public void run() {
        Thread.currentThread().setName("ReadFromOn4Kst-" + sessionId);
        LOGGER.log(Level.FINE,
                "ON4KST reader started for session {0}", sessionId);

        try {
            while (!isInterrupted() && sessionIsActive.test(sessionId)) {
                String response = reader.readLine();
                if (response == null) {
                    throw new EOFException("ON4KST closed the TCP connection");
                }

                inboundActivity.accept(response);
                if (!sessionIsActive.test(sessionId)) {
                    break;
                }

                ChatMessage message = new ChatMessage();
                message.setMessageText(response);
                receiveQueue.put(message);
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (IOException exception) {
            if (sessionIsActive.test(sessionId)) {
                LOGGER.log(Level.FINE,
                        "ON4KST read failed for session " + sessionId,
                        exception);
                connectionFailure.accept(exception);
            }
        } catch (RuntimeException exception) {
            if (sessionIsActive.test(sessionId)) {
                LOGGER.log(Level.SEVERE, "Unexpected ON4KST reader failure", exception);
                connectionFailure.accept(exception);
            }
        } finally {
            LOGGER.log(Level.FINE,
                    "ON4KST reader stopped for session {0}", sessionId);
        }
    }

    /**
     * Interrupts the read loop and closes the session socket.
     *
     * @return always {@code true} after a successful close
     * @throws IOException if closing the reader or socket fails
     */
    public boolean terminateConnection() throws IOException {
        interrupt();
        reader.close();
        socket.close();
        return true;
    }
}