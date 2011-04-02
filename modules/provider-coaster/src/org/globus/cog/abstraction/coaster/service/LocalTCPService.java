//----------------------------------------------------------------------
//This code is developed as part of the Java CoG Kit project
//The terms of the license can be found at http://www.cogkit.org/license
//This message may not be removed or altered.
//----------------------------------------------------------------------

/*
 * Created on Feb 15, 2008
 */
package org.globus.cog.abstraction.coaster.service;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

import org.apache.log4j.Logger;
import org.globus.cog.karajan.workflow.service.GSSService;
import org.globus.cog.karajan.workflow.service.RequestManager;
import org.globus.cog.karajan.workflow.service.channels.ChannelContext;
import org.globus.cog.karajan.workflow.service.channels.ChannelException;
import org.globus.cog.karajan.workflow.service.channels.ChannelManager;
import org.globus.cog.karajan.workflow.service.channels.KarajanChannel;

public class LocalTCPService extends GSSService implements Registering {
    public static final Logger logger = Logger.getLogger(LocalTCPService.class);

    public static final int TCP_BUFSZ = 32768;

    private RegistrationManager registrationManager;
    private final TCPBufferManager buffMan;

    // private int idseq;

    // private static final NumberFormat IDF = new DecimalFormat("000000");

    public LocalTCPService(RequestManager rm) throws IOException {
        super(false, 0);
        setRequestManager(rm);
        buffMan = new TCPBufferManager();
    }

    public LocalTCPService(RequestManager rm, int port) throws IOException {
        super(false, port);
        setRequestManager(rm);
        buffMan = new TCPBufferManager();
    }

    public String registrationReceived(String blockid,
                                       @SuppressWarnings("hiding") String url,
                                       KarajanChannel channel)
    throws ChannelException {
        if (logger.isInfoEnabled()) {
            logger.info("Received registration: blockid = " +
                        blockid + ", url = " + url);
        }
        ChannelContext cc = channel.getChannelContext();
        cc.getChannelID().setLocalID(blockid);
        String wid = registrationManager.nextId(blockid);
        cc.getChannelID().setRemoteID(wid);
        ChannelManager.getManager().registerChannel(cc.getChannelID(), channel);
        registrationManager.registrationReceived(blockid, wid, url,
                                                 cc);
        return wid;
    }

    public void unregister(String id) {
        throw new UnsupportedOperationException();
    }

    public RegistrationManager getRegistrationManager() {
        return registrationManager;
    }

    public void setRegistrationManager(RegistrationManager workerManager) {
        this.registrationManager = workerManager;
    }

    @Override
    protected void handleConnection(Socket socket) {
        try {
            buffMan.addSocket(socket);
            socket.setTcpNoDelay(true);
        }
        catch (SocketException e) {
            e.printStackTrace();
        }
        super.handleConnection(buffMan.wrap(socket));
    }
}