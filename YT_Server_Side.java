/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package yt_server_side;

import GUI.Tile;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.IncorrectnessListener;
import com.gargoylesoftware.htmlunit.InteractivePage;
import com.gargoylesoftware.htmlunit.ScriptException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HTMLParserListener;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlImage;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.JavaScriptErrorListener;
import java.awt.Image;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import org.apache.commons.logging.LogFactory;
import org.w3c.css.sac.CSSException;
import org.w3c.css.sac.CSSParseException;
import org.w3c.css.sac.ErrorHandler;
import yt_client_side.Client_Profile;

/**
 *
 * @author Your Name
 * <Tristan Janicki, Teacher: Anthony Ganuelas, Last Edited: , Date Created, Program Description>
 */
public class YT_Server_Side implements Runnable {

    ServerSocket serverSocket;
    Socket clientSocket;
    DataInputStream di;
    FileOutputStream fo;
    ObjectOutputStream ou;
    static String serverLocal = "C:\\Users\\tt700\\Desktop\\YT_Server\\";
    int port = 3306;

    @Override
    public void run() {
        listen(port);
    }

    /**
     * listens on the specified port.
     *
     * @param port - port to listen on.
     */
    void listen(int port) {
        System.out.println("Attempting to listen on " + port);
        try {

            if (serverSocket == null) {
                serverSocket = new ServerSocket(port);
            } else if (!serverSocket.isBound()) {
                serverSocket = new ServerSocket(port);
            }
            System.out.println("Server Socket initalized on " + port);

            while (true) { // loop it so that even after a connection has been made and data sent it listents again

                System.out.println("Server is Accepting Connections.");

                clientSocket = serverSocket.accept();

                clientSocket.setSoLinger(true, 1500);
                clientSocket.setKeepAlive(true);

                System.out.println("Accept stopped blocking");

                System.out.println("Attempting to read objects from " + clientSocket.getInetAddress() + " @ " + System.currentTimeMillis());

                try (ObjectInputStream oi = new ObjectInputStream(clientSocket.getInputStream());) {
                    Object obj = oi.readObject();

                    System.out.println("Obj to String \n\t" + obj.toString());
                    if (obj instanceof Client_Profile) {

                        new Thread(new Serializer((Client_Profile) obj)).start();

                    } else if (((String) obj).contains("Request CP")) {
                        String cid = ((String) obj).substring(9);
                        if (respond(findCP(cid), clientSocket.getOutputStream())) {
                            System.out.println("Succesfully responded with CP: " + cid + " @ " + System.currentTimeMillis());
                        } else {
                            System.out.println("Failed to respond with CP: " + cid);
                        }
                    } else if (((String) obj).contains("Testing")) {
                        String defaultResponse = "Responding to test message from " + clientSocket.getRemoteSocketAddress();
                        respond(defaultResponse, clientSocket.getOutputStream());
                    }
                } catch (IOException | ClassNotFoundException ex) {
                    ex.printStackTrace(System.out);
                }

            }
            // always return to listening... dont let the server die.
        } catch (java.net.BindException be) {
            System.out.println(port + " already in use");
            listen(0);
        } catch (SocketException se) {
            se.printStackTrace(System.out);
            listen(port);
        } catch (IOException ex) {
            ex.printStackTrace(System.out);
            listen(port);
        }
    }

    public Client_Profile getStoredProfile(String cid) {
        Client_Profile cp = null;
        try {
            FileInputStream fi;

            if (cid.equals("Random")) {
                File[] files = new File(serverLocal).listFiles();
                ArrayList<File> fl = new ArrayList(asList(files));
                Collections.shuffle(fl);
                fi = new FileInputStream(fl.get(0));
            } else {
                fi = new FileInputStream(serverLocal + cid + ".ser");
            }
            cp = (Client_Profile) new ObjectInputStream(fi).readObject();

            System.out.println("Successfully read cp " + cp.getCid());

            fi.close();
        } catch (FileNotFoundException ex) {
            System.out.println("Client_Profile " + cid + " not found in YT_Server_CP.");
            return new Client_Profile();
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(YT_Server_Side.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ((cp != null) ? cp : new Client_Profile());
    }

    public void send_profile_to_client(Client_Profile cp) {
        try {
            ou = new ObjectOutputStream(clientSocket.getOutputStream());

            System.out.println("Writing cp (cid: " + cp.getCid() + ")");

            ou.writeObject(cp);

            ou.flush();

            System.out.println("Sent CP with CID: " + cp.getCid());
        } catch (IOException ex) {
            Logger.getLogger(YT_Server_Side.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NullPointerException n) {
            System.out.println("CP null generating default");
            send_profile_to_client(getStoredProfile("Random"));
        }
    }

    /**
     * Default test path -> C:\\Users\\tt700\\Desktop\\RMI Server
     *
     * @param cp - client profile to be serialized
     * @return
     */
    public boolean serialize_CP(Client_Profile cp) {

        String suffix = "\\" + cp.getCid() + ".ser";
        try {
            fo = new FileOutputStream(serverLocal + suffix); // save location
            ou = new ObjectOutputStream(fo);

            ou.writeObject(cp);

            ou.flush();
            fo.flush();

        } catch (IOException ex) {
            ex.printStackTrace(System.out);
            return false;
        } finally {
            try {
                if (fo != null) {
                    fo.close();
                }
                if (ou != null) {
                    ou.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace(System.out);
            }
        }
        return true;
    }

    boolean respond(Object response, OutputStream out) {
        System.out.println("Attempting to respond with: " + response.toString() + " ...");
        try {
            ou = new ObjectOutputStream(out);
            ou.writeObject(response);
            ou.flush();

            ou.close();
            out.close();
        } catch (IOException ex) {
            System.out.println("Response failed.");
            ex.printStackTrace(System.out);
            return false;
        }
        System.out.println("Response sent succesfuly.");
        return true;
    }

    public Client_Profile findCP(String cid) {
        Client_Profile cp = null;
        try {
            try (FileInputStream fileIn = new FileInputStream(serverLocal + cid + ".ser");
                    ObjectInputStream in = new ObjectInputStream(fileIn)) {
                cp = (Client_Profile) in.readObject();
                System.out.println("Found CP " + cp.getCid());
            }
        } catch (ClassNotFoundException c) {
            System.out.println("CP class not found");
            c.printStackTrace(System.out);
        } catch (FileNotFoundException fnf) {
            System.out.println("Could not find CP with cid: " + cid);
            System.out.println("Returning default CP.");
            return cp = findCP("default");
        } catch (IOException i) {
            i.printStackTrace(System.out);
        }
        return cp;
    }

    void close() {
        try {
            clientSocket.close();
            serverSocket.close();
            ou.close();
            fo.close();
        } catch (IOException io) {
            io.printStackTrace(System.out);
        }
    }

    public ArrayList<Tile> getRecommendedPlaylistTiles(String url) {
        System.out.println("Attempting to generate recomended tiles...");
        ArrayList<Tile> tiles = new ArrayList();

        try {
            WebClient wc = new WebClient();

            //<editor-fold defaultstate="collapsed" desc="Turn off HtmlUnit logging">
            LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");

            java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);
            java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);

            wc.getOptions().setCssEnabled(false);

            wc.setIncorrectnessListener(new IncorrectnessListener() {
                @Override
                public void notify(String string, Object o) {
                }
            });
            wc.setCssErrorHandler(new ErrorHandler() {
                @Override
                public void warning(CSSParseException csspe) throws CSSException {
                }

                @Override
                public void error(CSSParseException csspe) throws CSSException {
                }

                @Override
                public void fatalError(CSSParseException csspe) throws CSSException {
                }
            });
            wc.setJavaScriptErrorListener(new JavaScriptErrorListener() {

                @Override
                public void scriptException(InteractivePage ip, ScriptException se) {
                }

                @Override
                public void timeoutError(InteractivePage ip, long l, long l1) {
                }

                @Override
                public void malformedScriptURL(InteractivePage ip, String string, MalformedURLException murle) {
                }

                @Override
                public void loadScriptError(InteractivePage ip, URL url, Exception excptn) {
                }
            });
            wc.setHTMLParserListener(new HTMLParserListener() {
                @Override
                public void error(String string, URL url, String string1, int i, int i1, String string2) {
                }

                @Override
                public void warning(String string, URL url, String string1, int i, int i1, String string2) {
                }
            });

            wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
            wc.getOptions().setThrowExceptionOnScriptError(false);
            //</editor-fold>

            HtmlPage yt = wc.getPage(url);

            //span[@class='yt-thumb-clip']//img
            yt.getByXPath("//tr[@class='pl-video yt-uix-tile ']").forEach(tr -> {

                System.out.println("TR");

                String songName = ((HtmlElement) tr).getAttribute("data-title");
                String songUrl = "https://www.youtube.com/watch?v=" + ((HtmlElement) tr).getAttribute("data-video-id");
                HtmlImage hi = ((HtmlElement) tr).getFirstByXPath(".//td[3]//img");
                String imgUrl = hi.getAttribute("data-thumb");

                tiles.add(new Tile(urlToJpg(imgUrl), songName, songUrl));

            });

        } catch (IOException io) {
            io.printStackTrace(System.out);
        }

        System.out.println("Finished getting recomeded playlists.");
        return tiles;
    }

    /**
     * This method goes to a specified webpage, recieves the content and
     * attempts to parse it to an ImageIcon. This will only work if the only
     * content on the page is the img. This is why the url must be derived from
     * the 'src' attribute of an <img> element.
     *
     * @param url The direct url to an image, often gotten from the 'src'
     * attribute in an <img> element.
     * @return an ImageIcon from the give url.
     */
    private ImageIcon urlToJpg(String url) {
        ImageIcon bi;
        System.out.println("Attempting to convert " + url + " to jpg...");
        try {
            System.out.println("Reading WebResponse...");
            WebClient wc = new WebClient();
            WebResponse wr = wc.getPage(url).getWebResponse();
            System.out.println("\tWebResponse read " + " @ " + System.currentTimeMillis() + "...");
            byte[] webResponseData = new byte[2000000]; // buffer for images up to 2mb in size

            wr.getContentAsStream().read(webResponseData);
            System.out.println("\t\tWebResponseData filled with data " + " @ " + System.currentTimeMillis() + "...");

            bi = new ImageIcon(new ImageIcon(webResponseData).getImage().getScaledInstance(120, 80, Image.SCALE_DEFAULT));
            System.out.println("\t\t\tbi intialized...");
        } catch (IOException | FailingHttpStatusCodeException ex) {
            System.out.println("IO Exception has been thrown when retrieving an image from " + url + ".");
            System.out.println("\tAttempting to retrieve default background image...");
            bi = new ImageIcon(getClass().getResource("/graphics/TGJ_Logo_Medium.png"));
            System.out.println("Default background image initialized.");
        }
        System.out.println("Returning bi.");
        return (bi);
    }
}
