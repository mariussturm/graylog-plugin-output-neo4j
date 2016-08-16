package org.graylog.plugins.outputs.neo4j.transport;

import org.graylog2.plugin.Message;

/**
 * Created by dev on 16/08/16.
 */
public interface INeo4jTransport {
    /**
     * Sends the given message to the remote host. This <strong>blocks</strong> until there is sufficient capacity to
     * process the message. It is not guaranteed that the message has been sent once the method call returns because
     * a queue might be used to dispatch the message.
     *
     * @param message message to send to the remote host
     * @throws InterruptedException
     */
    public void send(Message message) throws InterruptedException;

    /**
     * Tries to send the given message to the remote host. It does <strong>not block</strong> if there is not enough
     * capacity to process the message. It is not guaranteed that the message has been sent once the method call
     * returns because a queue might be used to dispatch the message.
     *
     * @param message message to send to the remote host
     * @return true if the message could be dispatched, false otherwise
     */
    public boolean trySend(Message message);

    /**
     * Stops the transport. Should be used to gracefully shutdown the backend.
     */
    public void stop();
}
