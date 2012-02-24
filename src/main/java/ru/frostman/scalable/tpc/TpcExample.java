package ru.frostman.scalable.tpc;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author slukjanov aka Frostman
 */
public class TpcExample {

    public static void main(String[] args) {

    }

    public void dispatch(int port) {
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.err.println("Can't open port " + port);
            e.printStackTrace();
            System.exit(-1);
            return;
        }

        while (true) {
            Socket socket = null;
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                System.err.println("Can't accept new connection");
                e.printStackTrace();
            }

            if (socket == null) {
                continue;
            }

            new Worker(socket).start();
        }
    }

    public final class Worker implements Runnable {
        private final Thread thread = new Thread(this);
        private final Socket socket;

        public Worker(Socket socket) {
            this.socket = socket;
        }

        public void start() {
            thread.start();
        }

        public void run() {
            DataInputStream in = null;
            DataOutputStream out = null;
            try {
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());

                out.writeInt(in.readInt() + 1);

            } catch (IOException e) {
                System.err.println("IO problems");
                e.printStackTrace();
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                    if (out != null) {
                        out.close();
                    }
                    socket.close();
                } catch (Exception e) {
                    // no operations
                }
            }
        }
    }

}
