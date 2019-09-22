package ServerStuff.Server;

import General.SecretManager;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class WebhookServerSession {
    private String auth = "";
    private Socket socket;
    private InputStream inputStream;

    public WebhookServerSession(Socket socket, String authKey) {
        try {
            this.auth = SecretManager.getString(authKey);
            this.socket = socket;
            this.inputStream = socket.getInputStream();
            new Thread(this::run).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void processData(String data) {}

    public void run() {
        try {
            int contentLength = -1;
            String authorization = "";

            boolean methodPost = false;

            BufferedInputStream bf = new BufferedInputStream(inputStream);
            BufferedReader r = new BufferedReader(
                    new InputStreamReader(bf, StandardCharsets.UTF_8));

            String line;
            do {
                line = r.readLine();

                if (line == null) break;

                if (!methodPost) {
                    if (line.startsWith("POST")) methodPost = true;
                    else {
                        System.out.println("Rejected: Not POST!");
                        break;
                    }
                }

                if (line.startsWith("content-length:")) {
                    contentLength = Integer.parseInt(line.split(": ")[1]);
                }

                if (line.startsWith("Authorization:")) {
                    authorization = line.split(": ")[1];
                }
            } while(line.length() != 0);

            if (methodPost) {
                if (authorization.equals(auth)) {
                    StringBuilder formularData = new StringBuilder();
                    do {
                        line = r.readLine();

                        if (line == null) break;
                        formularData.append(line).append("\n");

                        contentLength -= line.length();
                    } while(contentLength > 0);

                    processData(formularData.toString());
                } else {
                    System.out.println("Rejected: Wrong Authorization!");
                    System.out.println("Expected: " + auth);
                    System.out.println("Got: " + authorization);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
