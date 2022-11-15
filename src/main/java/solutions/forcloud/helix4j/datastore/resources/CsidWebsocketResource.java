/*
 * The MIT License
 *
 * Copyright (C) 2022 4cloud Solutions.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package solutions.forcloud.helix4j.datastore.resources;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import java.io.IOException;
import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.EndpointConfig;
import javax.websocket.OnOpen;
import javax.websocket.OnMessage;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import javax.ws.rs.ForbiddenException;
import javax.websocket.server.PathParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import solutions.forcloud.helix4j.modules.connectionmanagement.ConnectionManager;
import solutions.forcloud.helix4j.modules.connectionmanagement.ConnectionTypesEnum;
import solutions.forcloud.helix4j.modules.sessionmanagement.ClientSession;
import solutions.forcloud.helix4j.modules.sessionmanagement.SessionManager;
import solutions.forcloud.helix4j.modules.usermanagement.User;
import solutions.forcloud.helix4j.modules.usermanagement.UserManager;
import solutions.forcloud.helix4j.websockets.BaseWebsocketEndpointConfig;
import solutions.forcloud.helix4j.utils.Utilities;


/**
 *
 * @author mpujic
 * 
 * The path is preceded by elements defined in the config.yml file:
 *   server.applicationContextPath
 */
@Metered
@ExceptionMetered
@Timed
@ServerEndpoint(value = "/api/csid/{csid}/websocket", configurator = CsidWebsocketEndpointConfig.class)
public class CsidWebsocketResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsidWebsocketResource.class);

    @OnOpen
    public void onOpen(final Session websocketSession,
                       final EndpointConfig endpointConfig,
                       final @PathParam("csid") String csid) throws IOException {
        // String csid = websocketSession.getPathParameters().get("csid");
        LOGGER.debug("onOpen(csid={}) called! ", csid);
        ClientSession session;
        if (BaseWebsocketEndpointConfig.AUTHENTICATE_IN_ONOPEN) {
            // Authenticate the websocket here
            session = SessionManager.authenticateAndGetSession(csid, 
                        Utilities.getAuthTokensFromWebsoketEndpointConfig(csid, endpointConfig));
        } else {
            // Authentication has already been done in the endpoint configurator
            session = SessionManager.getSession(csid);
        }
        if (session == null) {
            // This will cause onError() to be invoked
            throw new ForbiddenException(); // --------------->
        }
        
        String userId = session.getUserId();
        User user = UserManager.getUser(userId);
        
        LOGGER.debug("onOpen(csid={}) Resulting user: {}", csid, user);

        String response = "{\"status\": \"CONNECTED\"}";
        websocketSession.getAsyncRemote().sendText(response);

        // Process new websocket
        ConnectionManager.addLocalConnection(
                userId, csid, ConnectionTypesEnum.WEBSOCKET, websocketSession);
        
        LOGGER.debug("onOpen() Returned: '{}'", response);
    }

    @OnMessage
    public void onMessage(final Session websocketSession, 
                          final String message,
                          final @PathParam("csid") String csid) {
        LOGGER.debug("onMessage(csid={}) called! Message: {}", csid, message);

        ClientSession session = SessionManager.getSession(csid);
        if (session != null) {
            String userId = session.getUserId();
            User user = UserManager.getUser(userId);
        
            LOGGER.debug("onMessage(csid={}) Resulting user: {}", csid, user);

            // Process message 
            // Just echo the message back
            String response = message;
            websocketSession.getAsyncRemote().sendText(response);
            LOGGER.debug("onMessage(csid={}) Returned: {}", csid, response);
        }
    }
    
    @OnClose
    public void onClose(final Session websocketSession, 
                        final CloseReason cr,
                        final @PathParam("csid") String csid) {
        LOGGER.debug("onClose(csid={}) called! CloseReason={}: {}", csid, cr.getCloseCode(), cr.getReasonPhrase());
        
        ClientSession session = SessionManager.getSession(csid);
        if (session != null) {
            String userId = session.getUserId();
            User user = UserManager.getUser(userId);
            
            LOGGER.debug("onClose(csid={}) User: {}", csid, user);
            
            ConnectionManager.deleteLocalConnection(userId, csid, /* closeFlag */ false);
        }
    }
    
    @OnError
    public void onError(final Session websocketSession, 
                        final Throwable  exception,
                        final @PathParam("csid") String csid) {
        // String csid = websocketSession.getPathParameters().get("csid");
        LOGGER.debug("onError(csid={}) called! Exception {}", csid, exception.getMessage());
        
        ClientSession session = SessionManager.getSession(csid);
        if (session != null) {
            String userId = session.getUserId();
            User user = UserManager.getUser(userId);
            
            LOGGER.debug("onError(csid={}) User: {}", csid, user);            
            
            ConnectionManager.deleteLocalConnection(userId, csid, /* closeFlag */ false);
        }
        
        CloseReason cr = new CloseReason(CloseCodes.VIOLATED_POLICY, exception.getMessage());
        try {
            websocketSession.close(cr);
            LOGGER.debug("onError(csid={}) Closed the websocket!", csid);            
        } catch (IOException ex) {
            //
        }        
    }    
        
}
