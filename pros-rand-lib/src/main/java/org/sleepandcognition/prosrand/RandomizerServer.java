package org.sleepandcognition.prosrand;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

public class RandomizerServer {
    boolean listening;

    /* command-line arguments:
     *   -h print out summary of command-line arguments
     *   -v print version
     *   -b be extra verbose
     *   -q be quiet
     *   -c run interactively on the command line
     *   -n accept requests over the network using a TCP/IP socket
     *   -i run interactively with a graphical user interface
     *   -p NUM   use NUM for the port for internet communication, instead of the default port number 1996
     *   -r PATH  use the file at PATH as the configuration file for variables (defaults to ./variables.xml)
     *   -g PATH  use the file at PATH to read in the groups (defaults to ./groups.txt)
     *   -s PATH  use the file at PATH as the subjects database (defaults to ./subjects.txt)
     *   -x allow a subject's scores to be revised and group re-assigned until commit received for that subject
     *   -a alternate assignment of subjects to groups, rather than trying to do any matching
     *
     *   The -c and -n options may be used simultaneously. If neither -c, -n, nor -i specified, it is the equivalent of -c -n.
     */
    public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException {
        RandomizerServer app = new RandomizerServer();
        app.run(args);
    }

    protected void run(String[] args) throws SAXException, ParserConfigurationException, IOException {
        boolean commandLineMode = false;
        boolean networkMode = false;
        boolean guiMode = false;
        int portNumber = 1996;
        String variablesSpec = "variables.xml";
        String groupListFile = "groups.txt";
        String subjectFile = "subjects.txt";
        int verbosity = 0;
        boolean allowRevision = false;
        boolean balancing = true;

        try {
            int argNum = 0;
            while (argNum < args.length) {
                String currArg = args[argNum];
                if (currArg.length() == 2 && currArg.charAt(0) == '-') {
                    switch (currArg.charAt(1)) {
                        case 'h':
                            printUsageSummary();
                            break;
                        case 'v':
                            System.out.println(String.format(
                                    "Balancing Randomizer version %d", Randomizer.RandomizerCommVersion()));
                            break;
                        case 'b':
                            verbosity = 1;
                            break;
                        case 'q':
                            verbosity = -1;
                            break;
                        case 'c':
                            commandLineMode = true;
                            break;
                        case 'i':
                            guiMode = true;
                            break;
                        case 'n':
                            networkMode = true;
                            break;
                        case 'p':
                            portNumber = Integer.parseInt(args[argNum + 1]);
                            ++argNum;
                            break;
                        case 'r':
                            variablesSpec = args[argNum + 1];
                            ++argNum;
                            break;
                        case 'g':
                            groupListFile = args[argNum + 1];
                            ++argNum;
                            break;
                        case 's':
                            subjectFile = args[argNum + 1];
                            ++argNum;
                            break;
                        case 'x':
                            allowRevision = true;
                            break;
                        case 'a':
                            balancing = false;
                            break;
                        default:
                            printUsageSummary();
                            return;
                    } // END switch
                } // END if well-formed flag
                else {
                    printUsageSummary();
                    return;
                }
                ++argNum;
            } // END while
        } catch (Exception ex) {
            printUsageSummary();
            return;
        }
        if (verbosity >= 0) {
            System.out.println("source code version: $Name:  $");
        }

        if (!(commandLineMode || networkMode || guiMode)) {
            commandLineMode = true;
            networkMode = true;
        }

        if (guiMode) {
            // run dialog to let user specify files, etc.
        }

        StillGoingFlag listening = new StillGoingFlag();
        SubjectFileDatabase database = new SubjectFileDatabase(subjectFile);
        VariableSet variables = new VariableSet(variablesSpec);
        Randomizer randomizer;
        CommandInterface commander;
        if (balancing) {
            randomizer = new BalancingRandomizer(groupListFile, variables, database, listening, allowRevision);
        } else {
            randomizer = new AlternatingRandomizer(groupListFile, variables, database, listening, allowRevision);
        }
        randomizer.setVerbosity(verbosity);
        commander = new CommandInterface(randomizer);

        if (guiMode) {

        } else {
            ServerSocket serverSocket = null;
            if (networkMode) {
                if (verbosity >= 0) {
                    System.out.println("Just before creating listener socket...");
                }
                try {
                    serverSocket = new ServerSocket(portNumber);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println("Could not listen on port " + portNumber);
                    System.err.println(
                            "This could be because an existing instance of RandomizerServer is running on this machine.");
                    System.err.println(
                            "On Mac or Linux, use the ps command (probably with some flags) to hunt down previous instances");
                    System.err.println("of this process, and use kill -9 to kill them.");
                    System.err.println(
                            "On Windows, use ctl-alt=del to get a list of processes, and look for and stop javaw.");
                    System.err.println("Or, on any platform, just reboot!");
                    System.err.println("Then try again.");
                    System.out.println("ATTENTION: PROSPECTIVE RANDOMIZER NOT RUNNING!!!!!");
                    System.exit(-1);
                }
                if (verbosity >= 0) {
                    System.out.println("Created listener socket.");
                }
                ServerThread thread = new ServerThread(serverSocket, commander, listening, commandLineMode, verbosity);
                thread.start();
            }
            if (commandLineMode) {
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                while (listening.getFlagValue()) {
                    String input = br.readLine();
                    String reply = commander.parseCommand(input);
                    System.out.println(reply);
                }
                if (serverSocket != null) {
                    serverSocket.close();
                }
            }
        }
    }

    private static void printUsageSummary() {
        System.out.println("Usage:");
        System.exit(0);
    }

    private class ServerThread extends Thread {
        StillGoingFlag listening;
        CommandInterface commander;
        ServerSocket serverSocket;
        boolean isCommandLinePresent;
        int verbosity;

        public ServerThread(
                ServerSocket serverSocket,
                CommandInterface commander,
                StillGoingFlag flag,
                boolean isCommandLinePresent,
                int verbosity) {
            this.serverSocket = serverSocket;
            this.listening = flag;
            this.commander = commander;
            this.isCommandLinePresent = isCommandLinePresent;
            this.verbosity = verbosity;
        }

        public void run() {
            boolean socketWasClosed = false;
            while (listening.getFlagValue()) {
                Socket listeningSocket;
                try {
                    if (verbosity >= 0) {
                        System.out.println("Just before waiting for next accept...");
                    }
                    listeningSocket = serverSocket.accept();
                    new ListeningSocketThread(listeningSocket, commander, verbosity).start();
                } catch (SocketException e) {
                    socketWasClosed = true;
                } catch (IOException e) {
                    e.printStackTrace();
                } // this blocks until a client tries to contact
            } // END while
            if (verbosity >= 0) {
                System.out.println("Done with listening on socket.");
            }
            if (!socketWasClosed && isCommandLinePresent) { // if the request to close DIDN'T come from command line...
                System.out.println("Please enter QUIT on command line (here) to exit cleanly.");
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    // I don't know why this would happen. But, we are closing anyway, why should we care?
                    e.printStackTrace();
                }
            }
        }
    }

    private class ListeningSocketThread extends Thread {
        Socket socket;
        CommandInterface commander;
        int verbosity;

        public ListeningSocketThread(Socket socket, CommandInterface commander, int verbosity) {
            this.socket = socket;
            this.commander = commander;
            this.verbosity = verbosity;
        }

        public void run() {
            PrintWriter out = null;
            BufferedReader in = null;
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String inputLine = in.readLine();
                if (inputLine != null) {
                    inputLine = inputLine.trim();
                    if (verbosity >= 0) {
                        System.out.println("Server received: " + inputLine);
                    }
                    String reply = commander.parseCommand(inputLine);
                    out.println(reply);
                    if (verbosity >= 0) {
                        System.out.println("dealt with one message");
                        System.out.println();
                    }
                } else {
                    System.out.println("Null input from that client.");
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                out.flush();
                out.close();
            }
        }
    }
}
