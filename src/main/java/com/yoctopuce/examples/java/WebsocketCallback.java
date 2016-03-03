package com.yoctopuce.examples.java;

import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/callback")
public class WebsocketCallback
{
    @OnOpen
    public void onOpen(Session session)
    {
        System.out.println("WS Open " + session.getId());
        Logic logic = Logic.get();
        logic.AddHub(session);
    }

    @OnClose
    public void onClose(Session session)
    {
        System.out.println("WS Close " + session.getId());
        Logic logic = Logic.get();
        logic.RemoveHub(session);
    }

}
