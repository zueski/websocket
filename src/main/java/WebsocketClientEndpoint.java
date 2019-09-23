package com.amway.integration.mashery;

import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;

import com.google.gson.Gson;

import org.apache.http.client.utils.URIBuilder;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.PongMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClientEndpoint
public class WebsocketClientEndpoint 
{
	private static final Logger LOGGER = LoggerFactory.getLogger(WebsocketClientEndpoint.class);
	
	Session userSession = null;
	long lastPingSent = 0L;
	long lastPongReceived = 0L;
	boolean gotPing = true;
	
	URI endpointURI;
	Long asynctimeoutms;
	Long sessiontimeoutms;
	
	public WebsocketClientEndpoint(URI endpointURI, Long asynctimeoutms, Long sessiontimeoutms)
	{
		this.endpointURI = endpointURI;
		this.asynctimeoutms = asynctimeoutms;
		this.sessiontimeoutms = sessiontimeoutms;
		this.userSession = connect();
	}
	
	private Session connect()
	{
		try 
		{
			WebSocketContainer container = ContainerProvider.getWebSocketContainer();
			if(asynctimeoutms != null)
			{	container.setAsyncSendTimeout(asynctimeoutms); }
			if(sessiontimeoutms != null)
			{	container.setDefaultMaxSessionIdleTimeout(sessiontimeoutms); }
			return container.connectToServer(this, endpointURI);
		} catch (Exception e) {
			LOGGER.error("unable to connect", e);
			throw new RuntimeException(e);
		}
	}
	
	@OnOpen
	public void onOpen(Session userSession) 
	{
		this.userSession = userSession; 
		LOGGER.info("Connection opened: " + userSession);
	}
	
	@OnClose
	public void onClose(Session userSession, CloseReason reason) 
	{
		this.userSession = null; 
		LOGGER.error("Connection closed: " + reason);
		System.exit(1);
	}
	
	@OnMessage
    public void onPong(PongMessage pongMessage) 
	{
		lastPongReceived = System.currentTimeMillis();
		long latency = lastPongReceived - lastPingSent;
		gotPing = true;
		LOGGER.info("Pong after " + latency + " ms " + new String(pongMessage.getApplicationData().array(), StandardCharsets.UTF_8));
	}
	
	public synchronized void sendPing() throws IOException
	{
		if(!gotPing)
		{
			LOGGER.error("missed ping from " + lastPingSent + " will attempt reconnect");
			// detected disconnect
			this.userSession = connect();
			// skip this call
			return;
		}
		// clear status
		gotPing = false;
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
		LOGGER.info("ponging " + buffer);
	}
	
	@OnMessage
	public void onMessage(byte[] message) 
	{
		//try
		//{
		//	java.io.FileOutputStream fios = new java.io.FileOutputStream("mesgs/messge_" + System.currentTimeMillis());
		//	fios.write(message);
		//	fios.close();
		//} catch(Exception e) { 
		//	System.err.println(e);
		//}
		try 
		{
			Gson gson = new Gson();
			HashMap map = gson.fromJson(new String(message, "utf-8"), HashMap.class);
			if(map.containsKey("data"))
			{
				List<Map> data = (List<Map>) map.get("data");
				if(data != null)
				{
					for(Iterator<Map> i = data.iterator(); i.hasNext();)
					{
						Map datamap = i.next();
						santitizeMap(datamap);
						System.out.println(gson.toJson(datamap));
					}
				}
			}
		} catch(java.io.UnsupportedEncodingException ie) {
			LOGGER.error("unknown message type", ie);
		} catch(java.net.URISyntaxException use) {
			LOGGER.error("invalid uri", use);
		}
	}
	
	
	/* clean the follow
	
	/rest/oauth2/v1/token/XXXXXXXXXXXXXXXXXXXXXX?client_id=ygjjdhwd9kwtzd662es37amj
	/rest/oauth2/v1/token?client_id=4zkkxt88rdb8c25tx79qmaub&client_secret=XXXXXXXXXXXX&grant_type=client_credentials&scope=aboNum=6758593%20salesPlanAff=10%20partyId=8420129&user_context=8420129
	https://ahkbizcenter-ft.amwaynet.com.cn/CN/pc/Mygcc?ada=6737&token=XXXXXXXXXXXXX&type=pvbv
	
	*/
	
	private void santitizeMap(Map m)
		throws java.net.URISyntaxException
	{
		if(m == null)
		{	return; }
		String[] k = (String[]) m.keySet().toArray(new String[0]);
		for(int i = 0; i < k.length; i++)
		{
			if("uri".equalsIgnoreCase(k[i]))
			{
				String uristring = (String) m.remove(k[i]);
				URIBuilder uri;
				try
				{
					uri = new URIBuilder("http://" + m.get("request_host_name") + uristring);
				} catch(java.net.URISyntaxException urie) {
					uri = new URIBuilder("http://" + m.get("request_host_name") + uristring.replaceAll(" ", "%20"));
				}
				if(uri.getPath() != null && uri.getPath().startsWith("/rest/oauth2/v1/token/"))
				{	uri.setPath("/rest/oauth2/v1/token/xxxx"); }
				List<org.apache.http.NameValuePair> queryparams = uri.getQueryParams();
				if(queryparams != null)
				{
					for(int il = queryparams.size() - 1; il > -1; il--)
					{
						NameValuePair p = queryparams.get(il);
						if("client_secret".equalsIgnoreCase(p.getName()))
						{
							queryparams.remove(il);
							il--;
						} else if("token".equalsIgnoreCase(p.getName())) {
							queryparams.remove(il);
							il--;
						} else {
							m.put("query_" + p.getName(), p.getValue());
						}
					}
					uri.setParameters(queryparams);
					m.put("uri_query", queryparams);
				}
				m.put("uri", uri.toString());
				String path = uri.getPath();
				if(path != null)
				{
					m.put("uri_path", path);
					String[] pathparts = path.split("/");
					for(int ip = 0; ip < pathparts.length; ip++)
					{	m.put("uri_path_" + ip, pathparts[ip]); }
				}
			} else if("oauth_access_token".equalsIgnoreCase(k[i])) {
				String token = (String) m.remove(k[i]);
			}
		}
	}
}