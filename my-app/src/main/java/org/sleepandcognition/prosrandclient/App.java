package org.sleepandcognition.prosrandclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Obviously this is not a full-fledged application of any kind. It does contain the bare minimum amount
 * of code required to communicate with the prospective randomizer server over a bare socket connection.
 * Assuming you're leveling one numeric feature, and you name it "score", you could probably drop this
 * class into your project as-is to use as the interface-- the required methods are public.
 * Rather lacking in niceties such as error handling and defensive programming, though.
 */
public class App {
    public static void main(String[] args) {
        System.out.println("Hello World!");
        System.out.println("My main function doesn't do much. But I do contain demo PR client code!");
        System.out.println("Ciao!");
    }

    class RandomizerConnection {
        Socket sock;
        PrintWriter out;
        BufferedReader in;

        RandomizerConnection() throws IOException {
            sock = getRandomizerSocket();
            out = new PrintWriter(sock.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        }

        String getRandomizerIP() {
            return "128.0.0.1";
        }

        Socket getRandomizerSocket() throws IOException {
            Socket sock = new Socket();
            sock.connect(new InetSocketAddress(getRandomizerIP(), 6000));
            return sock;
        }

        void close() throws IOException {
            sock.close();
        }
    }

    public String getSubjectID() {
        return "S01";
    }

    public void submitScore(int score) throws IOException {
        RandomizerConnection rc = new RandomizerConnection();
        rc.out.println(String.format("PUT %s score=%d", getSubjectID(), score));
        String reply = rc.in.readLine().trim();
        if (!reply.equals("OK")) {
            System.err.println(reply);
            throw new IOException("Unexpected reply from randomization server!");
        }
        rc.close();
    }

    public String getStudyGroup() throws IOException {
        RandomizerConnection rc = new RandomizerConnection();
        rc.out.println(String.format("GET %s", getSubjectID()));
        String reply = rc.in.readLine().trim();
        rc.close();
        return reply;
    }
}
