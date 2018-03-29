/**
* The BilkentMessageServer implements a simple text based database for the
* CS 421 Programming Assignment. 
* Bilkent University
* FALL 2017
*
* @author  Yarkın Deniz Cetin, Bulut Aygüneş
* @version 1.1
* @since   20-10-2017
*
* Available Commands:
* REGISTER <username>@<IP>:<PORT> e.g. REGISTER yarkin@192.168.1.1:32123
* Notes:
* You can modify the server, however do not forget that your projects will be evaluated
* based on this version. All projects must comply accordingly.
*/


import java.io.IOException;
import java.io.*;
import java.net.InetSocketAddress;
import java.util.Scanner;
import java.util.ArrayList;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;


public class BilkentMessageServer {

    static String userArrayList = "";
    static ArrayList<String> uNames = new ArrayList<String>(); // A dynamic array for usernames
    static ArrayList<String> uIPs = new ArrayList<String>();   // A dynamic array for IP addresses
    static ArrayList<String> uPorts = new ArrayList<String>(); // for Ports

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(80), 0); //Internet socket address
        server.createContext("/userlist.txt", new MyHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            
            //System.out.println(userArrayList);

            if("GET".equals(t.getRequestMethod()))
            {
                String response = userArrayList;
                t.sendResponseHeaders(200, response.length()); //Response is 200 OK
                OutputStream os = t.getResponseBody();
                os.write(userArrayList.getBytes());
                System.out.println(t.getRequestMethod());
                
                os.close();
            }
            else if("POST".equals(t.getRequestMethod()))
            {   
                int read = 0;
                Scanner s = new Scanner(t.getRequestBody()).useDelimiter("\\A"); //Read the contents of the post message
                String str = "";
                str = s.hasNext() ? s.next() : ""; //Read the outputstream

                if(CheckRegister(str)) //Check if a valid command is sent
                {
                    userArrayList = formUserArrayList(); //Form a string from the arrays
                    t.sendResponseHeaders(200, "POST".length());
                }
                else
                {
                    t.sendResponseHeaders(400, "POST".length()); //Malformed request
                }
                
                OutputStream os = t.getResponseBody();
                os.close();

            }


        }

        String formUserArrayList()
        {
            String uArrayList = "";
            
            for(int i = 0; i < uNames.size(); i++)
            {
                uArrayList += uNames.get(i)+ "@" + uIPs.get(i) + ":" + uPorts.get(i) + "\n";
            }
            return uArrayList;
        }

        boolean CheckRegister(String command)
        {
            String head = command.split(" ")[0];
            if(!head.equals("REGISTER"))
            {
                System.out.println("Invalid command" + head);
                return false;
            }

            String trailer = command.split(" ")[1];

            String[] user = trailer.split("@");
            
            if(user.length != 2)
            {
                System.out.println("Format error");
                return false;
            }

            String userName = user[0];
            if(uNames.contains(userName))
            {
               System.out.println("Username already exists");
               return false;  
            }

            String[] userAddress = user[1].split(":");

            if(userAddress.length != 2)
            {
                System.out.println("Address format error");
                return false;
            }

            String userIP = userAddress[0];

            if(!validIP(userIP))
            {
                System.out.println("Invalid or empty IP" + userIP);
                return false;
            }               

            String userPort = userAddress[1];

            if(userPort == "")
            {
                System.out.println("Empty port");
                return false;
            }
            
            if(uPorts.contains(userPort) && uIPs.contains(userIP))
            {
               System.out.println("IP-port pair already exists");
               return false;
            }                  
               
            uPorts.add(userPort);
            uNames.add(userName);
            uIPs.add(userIP);
            
            System.out.println("Username:  " +userName);
            System.out.println("User IP:   " +userIP);
            System.out.println("User Port: " +userPort);
            
            return true;
        }

        static boolean validIP (String ip) {
            try {
                if ( ip == null || ip.isEmpty() ) {
                    return false;
                }

                String[] parts = ip.split( "\\." );
                if ( parts.length != 4 ) {
                    return false;
                }

                for ( String s : parts ) {
                    int i = Integer.parseInt( s );
                    if ( (i < 0) || (i > 255) ) {
                        return false;
                    }
                }
                if ( ip.endsWith(".") ) {
                    return false;
                }

                return true;
                } catch (NumberFormatException nfe) {
                    return false;
                }
		}
    }

}