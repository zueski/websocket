package com.amway.integration.mashery;

import java.net.URI;
import java.net.URISyntaxException;
import java.io.IOException;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.ApplicationRunner;

import org.springframework.beans.factory.annotation.Value;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SpringBootApplication
public class WebsocketLogsListenerApp implements ApplicationRunner
{
	private static final Logger LOGGER = LoggerFactory.getLogger(WebsocketLogsListenerApp.class);
	
	@Value("${websocket.url}")
	private String url;
	
	@Value("${websocket.pingtimems}")
	private Long pingtimems = 60000L;
	
	@Value("${websocket.asynctimeoutms}")
	private Long asynctimeoutms = null;
	
	@Value("${websocket.sessiontimeoutms}")
	private Long sessiontimeoutms = null;
	
	@Override
	public void run(ApplicationArguments args) throws Exception 
	{
		// abort if config not set
		if(url == null || url.length() < 3)
		{
			LOGGER.error("Missing config property mashery.url, exiting!");
			System.exit(1);
		}
		// open websocket
		final WebsocketClientEndpoint clientEndPointclientEndPoint = new WebsocketClientEndpoint(new URI(url), asynctimeoutms, sessiontimeoutms);
		
		// always keep running
		while(true)
		{
			try { Thread.sleep(pingtimems); } catch(InterruptedException ie) { }
			try 
			{
				clientEndPointclientEndPoint.sendPing(); 
			} catch(IOException ioe) {
				LOGGER.error("Unable to ping", ioe);
			}
		}
	}
	
	public static void main(String[] args) 
	{
		SpringApplication.run(WebsocketLogsListenerApp.class, args);
	}
}