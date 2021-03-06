
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * The Client that can be run both as a console or a GUI
 */
public class Client {

    // for I/O
    private ObjectInputStream sInput;		// to read from the socket
    private ObjectOutputStream sOutput;		// to write on the socket
    private Socket socket;

    // if I use a GUI or not
    private ClientGUI cg;

    // the server, the port and the username
    private String server, username, password;
    private int port;

    private String newUsername, newUserPassword; 
    private int newUserType;
    /*
     *  Constructor called by console mode
     *  server: the server address
     *  port: the port number
     *  username: the username
     */

    Client(String server, int port) {
        // which calls the common constructor with the GUI set to null
        this(server, port, null);
    }

    /*
     * Constructor call when used from a GUI
     * in console mode the ClienGUI parameter is null
     */
    Client(String server, int port, ClientGUI cg) {
        this.server = server;
        this.port = port;
        // save if we are in GUI mode or not
        this.cg = cg;
    }

    private boolean newUser() {
        boolean verified = false;
        boolean valid = false;
        Scanner scan = new Scanner(System.in);

        while (!valid) {
            // get a new username from admin
            System.out.println("Enter a new username (16 characters or less).");
            newUsername = scan.nextLine();

            // Validation
            if (newUsername.length() > 16 || newUsername.length() == 0) {
                System.out.println("Invalid new username, please try again.");
                return false;
            }

            System.out.println("Enter a new user's password (16 characters or less).");

            newUserPassword = scan.nextLine();
            //password is too long or empty
            if (newUserPassword.length() > 16 || newUserPassword.length() == 0) {
                System.out.println("Invalid password, please try again.");
                return false;
            }
            
            System.out.println("Enter a new user's type (0 = Scrum Master / 1 = Developer).");

            newUserType = scan.nextInt();
            if (newUserType != 0 && newUserType != 1) {
                System.out.println("Invalid Type, please try again.");
                return false;
            }
            
            
            // check with the database if this new username is already taken or not
            System.out.println("Verifying new username info...");
            //create an output/input stream
            try {
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
            } catch (IOException eIO) {
                display("Exception creating new output/input stream: " + eIO);
                return false;
            }

            //send new username to server
            try {
                sOutput.writeObject(newUsername);
                sOutput.writeObject(newUserPassword);
                sOutput.writeObject(newUserType);
            } catch (IOException eIO) {
                display("Exception during login: " + eIO);
                return false;
            }

            //get response from server
            try {
                System.out.println("Checking the new username is valid or not ...");
                try {
                    verified = ((Boolean) sInput.readObject()).booleanValue();
                } catch (ClassNotFoundException e) {
                    System.out.println(e + " ClassNotFoundException...");
                }
                System.out.println("Verified: " + verified);
            } catch (IOException eIO) {
                display("Exception reading login response: " + eIO);
                return false;
            }
            //if new username is valid
            if (verified == true) {
                System.out.println("The new user is created. \n"
                        + "username: " + newUsername + '\n');
                valid = true;
            } else {
                System.out.println("This username is NOT valid, please try again.");
            }
        }
        return false;
    }

    //this function is not finished!!!!11
    private boolean login() {
        boolean verified = false;
        // wait for messages from user
        Scanner scan = new Scanner(System.in);
        System.out.println("LOGIN: Enter your username and password.");
        System.out.println("After 5 incorrect login attempts your connection will be terminated.");
        //login to server, put this in it's own damn function
        for (int i = 0; i < 5; i++) {
            System.out.println("Please enter your username (16 characters or less).");

            username = scan.nextLine();
            //username is too long or empty
            if (username.length() > 16 || username.length() == 0) {
                System.out.println("Invalid username, please try again.");
                continue;
            }

            System.out.println("Please enter your password (16 characters or less).");

            password = scan.nextLine();
            //password is too long or empty
            if (password.length() > 16 || password.length() == 0) {
                System.out.println("Invalid password, please try again.");
                continue;
            }

            System.out.println("Verifying login info...");
            //create an output/input stream
            try {
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
            } catch (IOException eIO) {
                display("Exception creating new output/input stream: " + eIO);
                //this.disconnect();
                return false;
            }
            //send login information to server
            try {
                sOutput.writeObject(username);
                sOutput.writeObject(password);
            } catch (IOException eIO) {
                display("Exception during login: " + eIO);
                //this.disconnect();
                return false;
            }
            //get response from server
            try {
                System.out.println("Getting login response...");
                try {
                    verified = ((Boolean) sInput.readObject()).booleanValue();
                } catch (ClassNotFoundException e) {
                    System.out.println(e + " ClassNotFoundException...");
                }
                System.out.println("Verified: " + verified);
            } catch (IOException eIO) {
                display("Exception reading login response: " + eIO);
                //this.disconnect();
                return false;
            }
            //if login info correct
            if (verified == true) {
                System.out.println("Logged in!");
                return true;
            } else {
                System.out.println("Incorrect login information, please try again.");
                continue;
            }
        }
        System.out.println("You have used your 5 login attempts, program is now terminating.");
        //this.disconnect();
        return false;
    }

    /*
     * To start the dialog
     */
    public boolean start() {
        // try to connect to the server
        try {
            socket = new Socket(server, port);
        } // if it failed not much I can so
        catch (Exception ec) {
            display("Error connectiong to server:" + ec);
            return false;
        }

        String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
        display(msg);
        System.out.println("Test");
        // creates the Thread to listen from the server 
        new ListenFromServer().start();
        //try to login
        if (!login()) {
            return false;
        }
        // success we inform the caller that it worked
        return true;
    }

    /*
     * To send a message to the console or the GUI
     */
    private void display(String msg) {
        if (cg == null) {
            System.out.println(msg);      // println in console mode
        } else {
            cg.append(msg + "\n");		// append to the ClientGUI JTextArea (or whatever)
        }
    }

    /*
     * To send a message to the server
     */
    void sendMessage(ChatMessage msg) {
        try {
            sOutput.writeObject(msg);
        } catch (IOException e) {
            display("Exception writing to server: " + e);
        }
    }

    /*
     * When something goes wrong
     * Close the Input/Output streams and disconnect not much to do in the catch clause
     */
    private void disconnect() {
        try {
            if (sInput != null) {
                sInput.close();
            }
        } catch (Exception e) {
        } // not much else I can do
        try {
            if (sOutput != null) {
                sOutput.close();
            }
        } catch (Exception e) {
        } // not much else I can do
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (Exception e) {
        } // not much else I can do

        // inform the GUI
        if (cg != null) {
            cg.connectionFailed();
        }
    }

    private void messageLoop() {
        // wait for messages from user
        Scanner scan = new Scanner(System.in);
        // loop forever for message from the user
        while (true) {
            System.out.print("> ");
            // read message from user
            String msg = scan.nextLine();

            // logout if message is LOGOUT
            if (msg.equalsIgnoreCase("LOGOUT")) {
                this.sendMessage(new ChatMessage(ChatMessage.LOGOUT, ""));
                // break to do the disconnect
                break;
            } // message WhoIsIn
            else if (msg.equalsIgnoreCase("WHOISIN")) {
                this.sendMessage(new ChatMessage(ChatMessage.WHOISIN, ""));
            } // add user if message is USERADD
            else if (msg.equalsIgnoreCase("USERADD")) {
                this.sendMessage(new ChatMessage(ChatMessage.USERADD, ""));
                if (username.equalsIgnoreCase("ADMIN")) {
                    newUser();
                } else {
                    System.out.println("You are not allowed to add a user.\n "
                            + "Only \"admin\" can add a user.");
                }

            } else {
                // default to ordinary message
                this.sendMessage(new ChatMessage(ChatMessage.MESSAGE, msg));
            }
        }
    }

    /*
     * To start the Client in console mode use one of the following command
     * > java Client
     * > java Client 
     * > java Client portNumber
     * > java Client portNumber serverAddress
     * at the console prompt
     * If the portNumber is not specified 1500 is used
     * If the serverAddress is not specified "localHost" is used
     * > java Client 
     * is equivalent to
     * > java Client 1500 localhost 
     * are eqquivalent
     * 
     * In console mode, if an error occurs the program simply stops
     * when a GUI id used, the GUI is informed of the disconnection
     */
    public static void main(String[] args) {
        // default values
        int portNumber = 1500;
        String serverAddress = "localhost";

        // depending of the number of arguments provided we fall through
        switch (args.length) {
            // > javac Client portNumber serverAddr
            case 2:
                serverAddress = args[1];
            // > javac Client portNumber
            case 1:
                try {
                    portNumber = Integer.parseInt(args[0]);
                } catch (Exception e) {
                    System.out.println("Invalid port number.");
                    System.out.println("Usage is: > java Client [portNumber] [serverAddress]");
                    return;
                }
            // > java Client
            case 0:
                break;
            // invalid number of arguments
            default:
                System.out.println("Usage is: > java Client [portNumber] {serverAddress]");
                return;
        }
        // create the Client object
        Client client = new Client(serverAddress, portNumber);
        // test if we can start the connection to the Server
        // if it failed nothing we can do
        if (!client.start()) {
            return;
        }

        //call login function
        //client.login();
        //call message loop
        client.messageLoop();

        // done disconnect
        client.disconnect();
    }

    /*
     * a class that waits for the message from the server and append them to the JTextArea
     * if we have a GUI or simply System.out.println() it in console mode
     */
    class ListenFromServer extends Thread {

        public void run() {
            while (true) {
                try {
                    System.out.println("Test");

                    String msg = "";
                    try {
                        msg = (String) sInput.readObject();
                    } catch (NullPointerException e) {
                        display("Null Pointer exception: " + e);
                        break;
                    }

                    // if console mode print the message and add back the prompt
                    if (cg == null) {
                        System.out.println(msg);
                        System.out.print("> ");
                    } else {
                        cg.append(msg);
                    }
                } catch (IOException e) {
                    display("Server has close the connection: " + e);
                    if (cg != null) {
                        cg.connectionFailed();
                    }
                    break;
                } // can't happen with a String object but need the catch anyhow
                catch (ClassNotFoundException e2) {
                }
            }
        }
    }
}
