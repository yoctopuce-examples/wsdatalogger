package com.yoctopuce.examples.java;

import javax.websocket.Session;

public class Logic
{
    private static Logic __logic = null;
    private final Worker _worker;
    private Thread _thread;


    private Logic()
    {
        _worker = new Worker();
        _thread = new Thread(_worker);
        _thread.start();
        //fixme: test EJB stuff
    }

    public static synchronized Logic get()
    {
        if (__logic == null) {
            __logic = new Logic();
        }
        return __logic;
    }


    public void AddHub(Session session)
    {
        _worker.addSession(session);
    }

    public void RemoveHub(Session session)
    {
        _worker.removeSession(session);
    }
}
