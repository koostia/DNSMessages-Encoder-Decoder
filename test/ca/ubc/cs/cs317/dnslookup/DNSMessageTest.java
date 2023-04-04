package ca.ubc.cs.cs317.dnslookup;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;

public class DNSMessageTest {
    @Test
    public void testConstructor() {
        DNSMessage message = new DNSMessage((short)23);
        assertFalse(message.getQR());
        assertFalse(message.getRD());
        assertEquals(0, message.getQDCount());
        assertEquals(0, message.getANCount());
        assertEquals(0, message.getNSCount());
        assertEquals(0, message.getARCount());
        assertEquals(23, message.getID());
    }

    @Test
    public void testOPcode() {
        DNSMessage message = new DNSMessage((short)23);
        assertEquals(23, message.getID());
        message.setQR(true);
        message.setOpcode(15);
        assertEquals(15, message.getOpcode());
    }

    @Test
    public void testFirstByte() {
        DNSMessage message = new DNSMessage((short)23);
        assertEquals(23, message.getID());
        message.setQR(true);
        message.setAA(false);
        message.setTC(true);
        message.setRD(true);
        message.setOpcode(15);
        assertTrue(message.getQR());
        assertFalse(message.getAA());
        assertTrue(message.getTC());
        assertTrue(message.getRD());
        assertEquals(15, message.getOpcode());
    }

    @Test
    public void testHeader() {
        DNSMessage message = new DNSMessage((short)23);
        assertEquals(23, message.getID());
        message.setQR(true);
        message.setAA(false);
        message.setTC(true);
        message.setRD(true);
        message.setOpcode(5);
        assertTrue(message.getQR());
        assertFalse(message.getAA());
        assertTrue(message.getTC());
        assertTrue(message.getRD());
        assertEquals(5, message.getOpcode());

    }

    @Test
    public void testBasicFieldAccess() {
        DNSMessage message = new DNSMessage((short)23);
        message.setOpcode(DNSMessage.QUERY);
        message.setQR(true);
        message.setRD(true);
        message.setQDCount(1);
        assertTrue(message.getQR());
        assertTrue(message.getRD());
        assertEquals(1, message.getQDCount());
    }

    @Test
    public void testRcode() {

        DNSMessage message = new DNSMessage((short)23);
        assertEquals(23, message.getID());
        message.setRA(true);
        message.setRcode(7);
        assertEquals(7, message.getRcode());
        assertTrue(message.getRA());

    }

    @Test
    public void testCounts() {

        DNSMessage message = new DNSMessage((short)23);
        assertEquals(23, message.getID());
        message.setARCount(10);
        assertEquals(10, message.getARCount());
        assertEquals(0, message.getANCount());

    }

    @Test
    public void testAddQuestion() {
        DNSMessage request = new DNSMessage((short)23);
        DNSQuestion question = new DNSQuestion("www.dropbox.com", RecordType.A, RecordClass.IN);
        request.addQuestion(question);
        byte[] content = request.getUsed();

        DNSMessage reply = new DNSMessage(content, content.length);
        assertEquals(request.getID(), reply.getID());
        assertEquals(request.getQDCount(), reply.getQDCount());
        assertEquals(request.getANCount(), reply.getANCount());
        assertEquals(request.getNSCount(), reply.getNSCount());
        assertEquals(request.getARCount(), reply.getARCount());
        DNSQuestion replyQuestion = reply.getQuestion();
        assertEquals(question, replyQuestion);
    }
    @Test
    public void testAddResourceRecord() {
        DNSMessage request = new DNSMessage((short)23);
        DNSQuestion question = new DNSQuestion("norm.cs.ubc.ca", RecordType.CNAME, RecordClass.IN);
        ResourceRecord rr = new ResourceRecord(question, RecordType.CNAME.getCode(), "ns1.cs.ubc.ca");
        request.addResourceRecord(rr);
        byte[] content = request.getUsed();
        System.out.println("Length of bytebuffer after adding RR: " + content.length);

        DNSMessage reply = new DNSMessage(content, content.length);
        System.out.println("Content length is: " + content.length);
        assertEquals(request.getID(), reply.getID());
        assertEquals(request.getQDCount(), reply.getQDCount());
        assertEquals(request.getANCount(), reply.getANCount());
        assertEquals(request.getNSCount(), reply.getNSCount());
        assertEquals(request.getARCount(), reply.getARCount());
        ResourceRecord replyRR = reply.getRR();
        assertEquals(rr, replyRR);
    }

}
