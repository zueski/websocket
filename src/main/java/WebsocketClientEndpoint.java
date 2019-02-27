package com.amway.integration.mashery;

import java.net.URI;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.PongMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

@ClientEndpoint
public class WebsocketClientEndpoint 
{
	Session userSession = null;
	long lastPingSent = 0L;
	long lastPongReceived = 0L;
	
	public WebsocketClientEndpoint(URI endpointURI, Long asynctimeoutms, Long sessiontimeoutms)
	{
		try 
		{
			WebSocketContainer container = ContainerProvider.getWebSocketContainer();
			if(asynctimeoutms != null)
			{	container.setAsyncSendTimeout(asynctimeoutms); }
			if(sessiontimeoutms != null)
			{	container.setDefaultMaxSessionIdleTimeout(sessiontimeoutms); }
			container.connectToServer(this, endpointURI);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@OnOpen
	public void onOpen(Session userSession) 
	{
		this.userSession = userSession; 
		System.err.println("Connection opened: " + userSession);
	}
	
	@OnClose
	public void onClose(Session userSession, CloseReason reason) 
	{
		this.userSession = null; 
		System.err.println("Connection closed: " + reason);
		System.exit(1);
	}
	
	@OnMessage
    public void onPong(PongMessage pongMessage) 
	{
		lastPongReceived = System.currentTimeMillis();
		long latency = lastPongReceived - lastPingSent;
		System.err.println("Pong after " + latency + " ms " + new String(pongMessage.getApplicationData().array(), StandardCharsets.UTF_8));
	}
	
	public synchronized void sendPing() throws IOException
	{
		lastPingSent = System.currentTimeMillis();
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.putLong(0, lastPingSent);
		userSession.getAsyncRemote().sendPing(buffer);
		System.err.println("pinging " + lastPingSent);
	}
	
	public void sendPong() throws IOException
	{
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.putLong(0, System.currentTimeMillis());
		userSession.getAsyncRemote().sendPong(buffer);
		System.err.println("ponging " + buffer);
	}
	
	@OnMessage
	public void onMessage(byte[] message) 
	{
		try 
		{ 
			System.out.println(new String(message, "utf-8")); 
		} catch(java.io.UnsupportedEncodingException ie) {
			System.err.println(ie); 
		}
	}
}