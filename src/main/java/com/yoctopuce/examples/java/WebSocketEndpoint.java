package com.yoctopuce.examples.java;

import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/callback")
public class WebSocketEndpoint
{
    @OnOpen
    public void onOpen(Session session)
    {
        System.out.println("WS Open " + session.getId());
        WorkerThread wt = WorkerThread.getInstance();
        wt.addSession(session);
    }

    @OnClose
    public void onClose(Session session)
    {
        System.out.println("WS Close " + session.getId());
        WorkerThread wt = WorkerThread.getInstance();
        wt.removeSession(session);
    }

}
