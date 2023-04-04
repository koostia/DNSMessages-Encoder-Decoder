package ca.ubc.cs.cs317.dnslookup;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

public class DNSMessage {
    public static final int MAX_DNS_MESSAGE_LENGTH = 512;

    // The offset into the message where the header ends and the data begins.
    public final static int DataOffset = 12;

    // Opcode for a standard query
    public final static int QUERY = 0;

    /**
     * TODO:  You will add additional constants and fields
     */
    private final ByteBuffer buffer;
    private int ID;
    private boolean QR;
    private boolean AA;
    private boolean TC;
    private boolean RD;
    private boolean RA;

    private Map<String, Integer> nameMap = new HashMap<>();
    private Map<Integer, String> pointMap = new HashMap<>();

    /**
     * Initializes an empty DNSMessage with the given id.
     *
     * @param id The id of the message.
     */
    public DNSMessage(short id) {
        this.buffer = ByteBuffer.allocate(MAX_DNS_MESSAGE_LENGTH);
        // TODO: Complete this method

        // Initializing all the values for header then setting the position to 12 byte

        setID(id);
        setQR(false);
        setOpcode(0);
        setAA(false);
        setTC(false);
        setRD(false);
        setRA(false);
        setRcode(0);
        this.buffer.position(12);

    }

    /**
     * Initializes a DNSMessage with the first length bytes of the given byte array.
     *
     * @param recvd The byte array containing the received message
     * @param length The length of the data in the array
     */
    public DNSMessage(byte[] recvd, int length) {

//        System.out.println(recvd.length);
//        System.out.println(length);

        buffer = ByteBuffer.wrap(recvd, 0, length);
        // TODO: Complete this method

        // Since all the values are initialized, set the position to be after the header (12 byte)

        buffer.position(12);

    }

    /**
     * Getters and setters for the various fixed size and fixed location fields of a DNSMessage
     * TODO:  They are all to be completed
     */

    // Most of the getters and setters are inspired from the tutorial 3 solution.
    // Mostly working on bitshifting to either get or add the value at a specific bit position

    public int getID() {
        byte b = buffer.get(0);
        byte b1 = buffer.get(1);
        ID = (b << 8) & 0xff00 | (b1 & 0xff);
        return ID;
    }

    public void setID(int id) {
        buffer.putShort((short) id);
    }

    public boolean getQR() {
        byte b = buffer.get(2);
        return (b & 0x80) >> 7 == 1;
    }

    public void setQR(boolean qr) {
        byte b = buffer.get(2);
        if (qr) {
            b = (byte) (b | 0x80);
        } else {
            b = (byte) (b & 0x7F);
        }
        buffer.put(2, b);
    }

    public boolean getAA() {
        byte b = buffer.get(2);
        return (b & 0x04) >> 2 == 1;
    }

    public void setAA(boolean aa) {
        byte b = buffer.get(2);
        if (aa) {
            b = (byte) (b | 0x04);
        } else {
            b = (byte) (b & 0xFB);
        }
        buffer.put(2, b);
    }

    public int getOpcode() {
        byte b = buffer.get(2);
        return (b & 0x78) >> 3;
    }

    public void setOpcode(int opcode) {
        QR = getQR();
        AA = getAA();
        TC = getTC();
        RD = getRD();
        byte b = (byte) (opcode << 3);
        buffer.put(2, b);
        setQR(QR);
        setAA(AA);
        setTC(TC);
        setRD(RD);
    }

    public boolean getTC() {
        byte b = buffer.get(2);
        return (b & 0x02) >> 1 == 1;
    }

    public void setTC(boolean tc) {
        byte b = buffer.get(2);
        if (tc) {
            b = (byte) (b | 0x02);
        } else {
            b = (byte) (b & 0xFD);
        }
        buffer.put(2, b);
    }

    public boolean getRD() {
        byte b = buffer.get(2);
        return (b & 0x01) == 1;
    }

    public void setRD(boolean rd) {
        byte b = buffer.get(2);
        if (rd) {
            b = (byte) (b | 0x01);
        } else {
            b = (byte) (b & 0xFE);
        }
        buffer.put(2, b);
    }

    public boolean getRA() {
        byte b = buffer.get(3);
        return (b & 0x80) >> 7 == 1;
    }

    public void setRA(boolean ra) {
        byte b = buffer.get(3);
        if (ra) {
            b = (byte) (b | 0x80);
        } else {
            b = (byte) (b & 0x7F);
        }
        buffer.put(3, b);
    }

    public int getRcode() {
        byte b = buffer.get(3);
        return (byte) (b & 0x0f);
    }

    public void setRcode(int rcode) {
        byte b = (byte) (rcode);
        byte b1 = (byte) (buffer.get() & 0xf0);
        RA = getRA();
        buffer.put(3, (byte) (b | b1));
        setRA(RA);
    }

    public int getQDCount() {
        return buffer.getShort(4);
    }

    public void setQDCount(int count) {
        buffer.putShort(4, (short) count);
    }

    public int getANCount() {
        return buffer.getShort(6);
    }

    public int getNSCount() {
        return buffer.getShort(8);
    }

    public int getARCount() {
        return buffer.getShort(10);
    }

    public void setARCount(int count) {
        buffer.putShort(10, (short) count);
    }


    // Helper method to find the name given the pointer. *IGNORE THIS, DOESN'T WORK*
    // Instead just created another HashMap called pointMap <Integer, String>
    public static <K, V> K getKeyByValue(Map<K, V> map, V value) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;  // Value not found
    }

    /**
     * Return the name at the current position() of the buffer.
     *
     * The encoding of names in DNS messages is a bit tricky.
     * You should read section 4.1.4 of RFC 1035 very, very carefully.  Then you should draw a picture of
     * how some domain names might be encoded.  Once you have the data structure firmly in your mind, then
     * design the code to read names.
     *
     * @return The decoded name
     */
    public String getName() {
        // TODO: Complete this method

        String name;
        int currentPos = buffer.position();
        int lengthName  = buffer.get();
        // This would either inform us of the length of the upcoming name, if it is a pointer, or if the value is 0 signifying the end.
        String rest;
        StringBuilder sb = new StringBuilder();

        if ((lengthName & 0xc0) == 0xc0) {
            // If this is a pointer, find out if there is a name for that pointer on PointerMap
            int pointer = buffer.get() & 0xff;
            if (pointer == 0) {
                // Rare case when we get the value 0x100, which when read in byte form would give the decimal value of 0
                pointer = 256;
            }
            String pName = pointMap.get(pointer);
            return pName;

        } else {

            // If it is an address of size lengthName, create an array to get individual bytes, and convert each byte to its respected ascii value, and append into a StringBuilder
            byte[] addressArr = new byte[lengthName];
            for (int i = 0; i < lengthName; i++) {
                addressArr[i] = buffer.get();
            }
            for (byte b : addressArr) {
                char ascii = (char) b;
                sb.append(ascii);
            }

            String first = sb.toString();

            if (buffer.get() == 0) {
                // If the next byte is 0, that signifies the end of the name
                // Put the pointer to this name on the HashMap for future use
                pointMap.put(currentPos, first);
                return first;

            } else {
                // Reset the position after the initial if statement call
                buffer.position(buffer.position() - 1);
                // Check if there is a pointer or more to the name (Other length)
                rest = getName();
                name = first + "." + rest;
                // Put the pointer to this name on the HashMap for future use
                pointMap.put(currentPos, name);
                return name;
            }
        }

    }


    /**
     * The standard toString method that displays everything in a message.
     * @return The string representation of the message
     */
    public String toString() {
        // Remember the current position of the buffer so we can put it back
        // Since toString() can be called by the debugger, we want to be careful to not change
        // the position in the buffer.  We remember what it was and put it back when we are done.
        int end = buffer.position();
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("ID: ").append(getID()).append(' ');
            sb.append("QR: ").append(getQR() ? "Response" : "Query").append(' ');
            sb.append("OP: ").append(getOpcode()).append(' ');
            sb.append("AA: ").append(getAA()).append('\n');
            sb.append("TC: ").append(getTC()).append(' ');
            sb.append("RD: ").append(getRD()).append(' ');
            sb.append("RA: ").append(getRA()).append(' ');
            sb.append("RCODE: ").append(getRcode()).append(' ')
                    .append(dnsErrorMessage(getRcode())).append('\n');
            sb.append("QDCount: ").append(getQDCount()).append(' ');
            sb.append("ANCount: ").append(getANCount()).append(' ');
            sb.append("NSCount: ").append(getNSCount()).append(' ');
            sb.append("ARCount: ").append(getARCount()).append('\n');
            buffer.position(DataOffset);
            showQuestions(getQDCount(), sb);
            showRRs("Authoritative", getANCount(), sb);
            showRRs("Name servers", getNSCount(), sb);
            showRRs("Additional", getARCount(), sb);
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "toString failed on DNSMessage";
        }
        finally {
            buffer.position(end);
        }
    }

    /**
     * Add the text representation of all the questions (there are nq of them) to the StringBuilder sb.
     *
     * @param nq Number of questions
     * @param sb Collects the string representations
     */
    private void showQuestions(int nq, StringBuilder sb) {
        sb.append("Question [").append(nq).append("]\n");
        for (int i = 0; i < nq; i++) {
            DNSQuestion question = getQuestion();
            sb.append('[').append(i).append(']').append(' ').append(question).append('\n');
        }
    }

    /**
     * Add the text representation of all the resource records (there are nrrs of them) to the StringBuilder sb.
     *
     * @param kind Label used to kind of resource record (which section are we looking at)
     * @param nrrs Number of resource records
     * @param sb Collects the string representations
     */
    private void showRRs(String kind, int nrrs, StringBuilder sb) {
        sb.append(kind).append(" [").append(nrrs).append("]\n");
        for (int i = 0; i < nrrs; i++) {
            ResourceRecord rr = getRR();
            sb.append('[').append(i).append(']').append(' ').append(rr).append('\n');
        }
    }

    /**
     * Decode and return the question that appears next in the message.  The current position in the
     * buffer indicates where the question starts.
     *
     * @return The decoded question
     */
    public DNSQuestion getQuestion() {
        // TODO: Complete this method

        String name = getName();
        // Since two octets is equivalent to 2 bytes, we only care about the value of the last byte (Max value we need is 28)
        buffer.position(buffer.position() + 1);
        RecordType rt = RecordType.getByCode(buffer.get());
        buffer.position(buffer.position() + 1);
        RecordClass rc = RecordClass.getByCode(buffer.get());
        return new DNSQuestion(name, rt, rc);

    }

    /**
     * Decode and return the resource record that appears next in the message.  The current
     * position in the buffer indicates where the resource record starts.
     *
     * @return The decoded resource record
     */
    public ResourceRecord getRR() {
        // TODO: Complete this method

        // RR Format = Name -> Type -> Class -> TTL -> RDLength -> RData

        String name = getName();
        buffer.position(buffer.position());
        RecordType rt = RecordType.getByCode(buffer.getShort());
        buffer.position(buffer.position());
        RecordClass rc = RecordClass.getByCode(buffer.getShort());
        DNSQuestion question = new DNSQuestion(name, rt, rc);

        int TTL = buffer.getInt();
        short RDLength = buffer.getShort();

        if (rt.getCode() == 2 | rt.getCode() == 5) {
            // If the RecordType is NS or CNAME, RData should be a string
            String RData = getName();
            return new ResourceRecord(question, TTL, RData);

        } else if (rt.getCode() == 1 | rt.getCode() == 28) {
            // If the RecordType is A or AAAA, RData should be of type InetAddress
            byte[] addressArr = new byte[RDLength];
            for (int i = 0; i < RDLength; i++) {
                addressArr[i] = buffer.get();
            }

            try {
                // Converting byte array to its respective InetAddress object
                InetAddress address = InetAddress.getByAddress(addressArr);
                return new ResourceRecord(question, TTL, address);

            } catch (UnknownHostException e) {
                // Do nothing
            }

        } else if (rt.getCode() == 15) {
            // If the RecordType is MX, RData should be a string
            // MX has a 16 bit preference that we need to skip
            buffer.getShort();
            String address = getName();
            return new ResourceRecord(question, TTL, address);

        } else {

            return null;

        }

        return null;

    }

    /**
     * Helper function that returns a hex string representation of a byte array. May be used to represent the result of
     * records that are returned by a server but are not supported by the application (e.g., SOA records).
     *
     * @param data a byte array containing the record data.
     * @return A string containing the hex value of every byte in the data.
     */
    public static String byteArrayToHexString(byte[] data) {
        return IntStream.range(0, data.length).mapToObj(i -> String.format("%02x", data[i])).reduce("", String::concat);
    }
    /**
     * Helper function that returns a byte array from a hex string representation. May be used to represent the result of
     * records that are returned by a server but are not supported by the application (e.g., SOA records).
     *
     * @param hexString a string containing the hex value of every byte in the data.
     * @return data a byte array containing the record data.
     */
    public static byte[] hexStringToByteArray(String hexString) {
        byte[] bytes = new byte[hexString.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            String s = hexString.substring(i * 2, i * 2 + 2);
            bytes[i] = (byte)Integer.parseInt(s, 16);
        }
        return bytes;
    }

    /**
     * Add an encoded name to the message. It is added at the current position and uses compression
     * as much as possible.  Make sure you understand the compressed data format of DNS names.
     *
     * @param name The name to be added
     */
    public void addName(String name) {
        // TODO: Complete this method

        // Convert the given name into bytes
        byte[] bytes = name.getBytes();
        int lengthToPeriod = 0;
        boolean notFound = true;
        int totalAdded = 0;
        int index = 0;
        int pointer;

        if (nameMap.get(name) != null) {
            // If there is an offset associated with the name, create a pointer with the format of 0xc0xx and put it on the buffer
            pointer = nameMap.get(name) | 0xc000;
            buffer.putShort((short) pointer);
        } else {
            // If this is a new name to be added, put the name and offset on the nameMap HashMap
            nameMap.put(name, buffer.position());

            do {
                // Convert each byte to its respective hex form
                String hex = String.format("%02x", bytes[index] & 0xff);

                if (hex.equals("2e")) {
                    // If its hex form is equal to ".", put the number of length till period as the beginning label
                    buffer.put((byte) lengthToPeriod);
                    totalAdded++;
                    int added = totalAdded - 1;
                    for (int i = added; i < lengthToPeriod + added; i++) {
                        // Put the remaining bytes on the buffer
                        buffer.put(bytes[i]);
                        totalAdded++;
                    }
                    lengthToPeriod = 0;
                    index++;

                } else {
                    // If no period is found, keep increasing the index and lengthToPeriod
                    lengthToPeriod++;
                    index++;
                    if (index == bytes.length) {
                        // If we've reach the end, put the remaining bytes on the buffer and add a 0 at the end to signify the end of the name
                        buffer.put((byte) lengthToPeriod);
                        for (int i = totalAdded; i < index; i++) {
                            buffer.put(bytes[i]);
                        }
                        buffer.put((byte) 0);
                        notFound = false;
                    }
                }
            } while (notFound);
        }
    }

    /**
     * Add an encoded question to the message at the current position.
     * @param question The question to be added
     */
    public void addQuestion(DNSQuestion question) {
        // TODO: Complete this method

        addName(question.getHostName());
        addQType(question.getRecordType());
        addQClass(question.getRecordClass());
        setQDCount(getQDCount() + 1);

    }

    /**
     * Add an encoded resource record to the message at the current position.
     * The record is added to the additional records section.
     * @param rr The resource record to be added
     */
    public void addResourceRecord(ResourceRecord rr) {
        addResourceRecord(rr, "additional");
    }

    /**
     * Add an encoded resource record to the message at the current position.
     *
     * @param rr The resource record to be added
     * @param section Indicates the section to which the resource record is added.
     *                It is one of "answer", "nameserver", or "additional".
     */
    public void addResourceRecord(ResourceRecord rr, String section) {
        // TODO: Complete this method

        // Read on Piazza that we don't really have any usage for section besides incrementing the different counts (Answer, NS, Additional)
        // But the UI is always adding this to the additional section
        // RR Format = Name -> Type -> Class -> TTL -> RDLength -> RData

        String qName = rr.getHostName();
        RecordType qRt = rr.getRecordType();
        RecordClass qRc = rr.getRecordClass();

        addName(qName);
        addQType(qRt);
        addQClass(qRc);

        int type = qRt.getCode();
        long TTL = rr.getRemainingTTL();
        int RDLength;

        if (TTL != 0) {
            buffer.putInt((int) TTL);
        } else {
            buffer.putInt(0);
        }

        if ((type == 1) | (type == 28)) {
            // If the RecordType is A (Always Length = 4) or AAAA, RData should be of type InetAddress
            buffer.put((byte) 0);
            buffer.put((byte) 4);
            buffer.put(rr.getInetResult().getAddress());

        } else if (type == 2 | type == 5) {
            // If the RecordType is NS or CNAME, RData should be a string
            RDLength = rr.getTextResult().length();
            buffer.putShort((short) RDLength);
            addName(rr.getTextResult());

        } else if (type == 15) {
            // If the RecordType is MX, RData should be a string with a preference label
            buffer.putShort((short) 0);
            RDLength = rr.getTextResult().length();
            buffer.putShort((short) RDLength);
            addName(rr.getTextResult());
        }

        setARCount(getARCount() + 1);

    }

    /**
     * Add an encoded type to the message at the current position.
     * @param recordType The type to be added
     */
    private void addQType(RecordType recordType) {
        // TODO: Complete this method

        switch (recordType.getCode()) {

            case 1:
                buffer.putShort((short) 1);
                break;
            case 2:
                buffer.putShort((short) 2);
                break;
            case 5:
                buffer.putShort((short) 5);
                break;
            case 6:
                buffer.putShort((short) 6);
                break;
            case 15:
                buffer.putShort((short) 15);
                break;
            case 28:
                buffer.putShort((short) 28);
                break;
            default:
                buffer.putShort((short) 0);
                break;

        }

    }

    /**
     * Add an encoded class to the message at the current position.
     * @param recordClass The class to be added
     */
    private void addQClass(RecordClass recordClass) {
        // TODO: Complete this method

        switch (recordClass.getCode()) {
            case 1:
                buffer.putShort((short) 1);
                break;
            default:
                buffer.putShort((short) 0);
                break;
        }

    }

    /**
     * Return a byte array that contains all the data comprising this message.  The length of the
     * array will be exactly the same as the current position in the buffer.
     * @return A byte array containing this message's data
     */
    public byte[] getUsed() {
        // TODO: Complete this method

        if (buffer.hasArray()) {
            int currentPos = buffer.position();
            System.out.println("Current Position of Buffer in GetUsed is " + currentPos);
            byte[] arr = buffer.array();
            byte[] newArr = new byte[currentPos];
            System.arraycopy(arr, 0, newArr, 0, currentPos);
            return newArr;
        } else {
            return new byte[0];
        }

    }

    /**
     * Returns a string representation of a DNS error code.
     *
     * @param error The error code received from the server.
     * @return A string representation of the error code.
     */
    public static String dnsErrorMessage(int error) {
        final String[] errors = new String[]{
                "No error", // 0
                "Format error", // 1
                "Server failure", // 2
                "Name error (name does not exist)", // 3
                "Not implemented (parameters not supported)", // 4
                "Refused" // 5
        };
        if (error >= 0 && error < errors.length)
            return errors[error];
        return "Invalid error message";
    }
}