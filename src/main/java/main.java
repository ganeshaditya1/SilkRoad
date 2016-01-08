
import com.mongodb.MongoClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.Buffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by aditya on 1/6/16.
 *
 * This is the crux of the program.
 */
public class main {

    private Map<String, String> options =  new HashMap<String, String>();
    private void sopn(Object o){
        System.out.println(o);
    }

    private void sop(Object o){
        System.out.print(o);
    }

    private void printHelpMenu(){
        sopn("Usage: main [OPTIONS]");
        sopn("Options:");
        sopn("--portno \t The port number where the server needs to run");
        sopn("--mongoip \t The ip address where MongoDB is running");
        sopn("--mongoportno \t The port number where the mongo is running");
        sopn("--root \t The location from where I need to serve the requested files from");
        sopn("--maxcachesize \t The maximum number of files that should be cached");
    }

    private boolean isNumber(String input){
        try{
            Integer.parseInt(input);
        }
        catch(NumberFormatException NFE){
            return false;
        }
        return true;
    }


    public main(String arguments[]) throws IOException{
        if(arguments.length != 0 && arguments[0].equals("--help")){
            printHelpMenu();
            return;
        }
        else{
            for(int i = 0; i < arguments.length; i+=2){
                if(i < arguments.length && i+1 < arguments.length ){
                    options.put(arguments[i].substring(2).trim(), arguments[i + 1]);
                }
                else{
                    sopn("Missing argument");
                    return;
                }
            }
        }
        if(options.get("portno") == null || !isNumber(options.get("portno"))){
            sopn("[Warning]Either portno was not specified or was not a number, so using port no 80");
            options.put("portno", "80");
        }

        if(options.get("root") == null){
            sopn("[Warning]root was not specified");
            options.put("root", "/home/aditya");
        }

        if(options.get("maxcachesize") == null || !isNumber(options.get("maxcachesize"))){
            sopn("[Warning]Either maxcachesize was not specified or was not a number. Using max cache size of 25");
            options.put("maxcachesize", "25");
        }

        if(options.get("mongoip") == null){
            sopn("[Warning] Ip address of mongo server was not specified so using localhost as server address");
            options.put("mongoip", "localhost");
        }

        if(options.get("mongoportno") == null || !isNumber(options.get("mongoportno"))){
            sopn("[Warning] Port no of mongo server was not specified so using 27017");
            options.put("mongoportno", "27017");
        }



        Bufferpool pool = new Bufferpool(Integer.parseInt(options.get("maxcachesize")), options.get("root"));
        MongoClient mongoConn = new MongoClient(options.get("mongoip"), Integer.parseInt(options.get("mongoportno")));

        ServerSocket s = new ServerSocket(Integer.parseInt(options.get("portno")));
        while(true){
            Socket client = s.accept();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(client.getInputStream()));

            String statusLine = in.readLine();
            Map<String, String> headerFields = new HashMap<String, String>();
            String line = "", content = "";
            byte state = 0;
            char alphabet;
            while(true){
                alphabet = (char)in.read();
                if(state == 0){
                    if(alphabet == '\r') {
                        state = 1;
                    }
                    else{
                        line += alphabet;
                    }
                }
                else if(state == 1){
                    if(alphabet == '\n'){
                        state = 2;
                        String arr[] = line.split(":");
                        headerFields.put(arr[0], arr[1]);
                        line = "";
                    }
                    else{
                        line += alphabet;
                    }
                }
                else if(state == 2){
                    if(alphabet == '\r'){
                        in.read();
                        break;
                    }
                    else{
                        line += alphabet;
                        state = 0;
                    }
                }
            }
            if(headerFields.get("Content-Length") != null) {
                int messageLength = Integer.parseInt(headerFields.get("Content-Length").trim());
                for (int i = 0; i < messageLength; i++) {
                    content += (char) in.read();
                }
            }
            String keywords[] = statusLine.split(" ");
            if(keywords[0].equals("GET")){
                (new FileFetcher(client, keywords[1], mongoConn, pool, headerFields)).run();
            }
            else if(keywords[0].equals("POST")){

            }
            in.close();
            client.close();
        }

    }

    public static void main(String arguments[]) throws IOException{
        new main(arguments);
    }
}
