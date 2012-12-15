package org.lantern;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.xmpp.JID;
import com.google.appengine.api.xmpp.Message;
import com.google.appengine.api.xmpp.XMPPService;
import com.google.appengine.api.xmpp.XMPPServiceFactory;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.mrbean.MrBeanModule;

@SuppressWarnings("serial")
public class XmppReceiverServlet extends HttpServlet {
    
    private final transient Logger log = Logger.getLogger(getClass().getName());

    @Override
    public void doPost(final HttpServletRequest req, 
        final HttpServletResponse res) throws IOException {
        final XMPPService xmpp = XMPPServiceFactory.getXMPPService();
        final Message msg = xmpp.parseMessage(req);
        
        final JID fromJid = msg.getFromJid();
        final String body = msg.getBody();
        log.info("Received "+fromJid+" body:\n"+body);

        final ObjectMapper mapper = new ObjectMapper();
        try {
            final Map<String, Object> m = mapper.readValue(
            		body, Map.class);
            final String subject = (String)m.get("subject");
            if (subject == null) {
                log.warning("Got JSON chat with no subject.");
                return;
            }
            if (!subject.equals("invited-server-up")) {
                log.warning("Unrecognized subject: " + subject);
                return;
            }
            final String inviterEmail = (String)m.get("user");
            if (inviterEmail == null) {
                log.severe("invited-server-up with no inviter e-mail.");
                return;
            }
            final String address = (String)m.get("address");
            if (address == null) {
                log.severe(inviterEmail + " sent invited-server-up with no address.");
                return;
            }
            InvitedServerLauncher.onInvitedServerUp(inviterEmail, address);
        } catch (final JsonParseException e) {
            log.warning("Error parsing chat: "+e.getMessage());
            return;
        } catch (final JsonMappingException e) {
            log.warning("Error parsing chat: "+e.getMessage());
            return;
        } catch (final IOException e) {
            log.warning("Error parsing chat: "+e.getMessage());
            return;
        }

    }
}
