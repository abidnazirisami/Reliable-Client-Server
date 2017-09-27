/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reliableserver;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author kakas
 */
public class ReliableServer {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws SocketException, IOException, InterruptedException {
        try ( // TODO code application logic here
                DatagramSocket ds = new DatagramSocket(3402)) {
            Random random = new Random();
            //System.out.println("Enter the filename with directory: ");
            //Scanner sc = new Scanner("System.in");
            String fileName = "D:\\Study\\Email.txt";
            //String fileName = sc.nextLine();
            int cnt = 5;
            InetAddress client = null;
            int port = 0;
            while (cnt > 0) {
                cnt--;
                byte[] buf = new byte[100];
                DatagramPacket dp = new DatagramPacket(buf, 100);
                try {
                    ds.receive(dp);
                } catch (IOException ex) {
                    Logger.getLogger(ReliableServer.class.getName()).log(Level.SEVERE, null, ex);
                }
                byte[] receivedData = dp.getData();
                BufferedReader b = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(receivedData)));
                System.out.println("Received from " + dp.getAddress() + ": " + b.readLine());

                Thread.sleep(random.nextInt(10) * 10);
                client = dp.getAddress();
                port = dp.getPort();
                DatagramPacket response = new DatagramPacket(receivedData, receivedData.length, client, port);
                ds.send(response);
                System.out.println("Reply Sent");

            }
            FileReader f = new FileReader(fileName);
            BufferedReader b = new BufferedReader(f);
            String toSend = "";
            int seqNo = 0, windowSize = 10, curEnd = windowSize - 2, receivedSeq = windowSize;
            byte[] packet = new byte[8];
            int count = 0;
            boolean resend = false;
            byte[][] frames = new byte[windowSize][12];
            for (int k = 0; k < windowSize; k++) {
                byte[] temp = ByteBuffer.allocate(4).putInt(k).array();
                for (int l = 0; l < 4; l++) {
                    frames[k][l] = temp[l];
                }
            }
            byte[] buffer = new byte[1024 * 1024]; // 1 MB Buffer
            while ((toSend = b.readLine()) != null) {
                byte[] temp = toSend.getBytes();
                for (int ind = 0; ind < temp.length; ind++) {
                    buffer[count++] = temp[ind];
                }
                byte[] nl = "\r\n".getBytes();
                for (int ind = 0; ind < nl.length; ind++) {
                    buffer[count++] = nl[ind];
                }
            }
            for (int i = 0; i < count;) {
                if (seqNo == curEnd) {
                    if (seqNo == receivedSeq) {
                        curEnd = (curEnd + windowSize - 1) % windowSize;
                        resend = false;
                    } else {
                        seqNo = receivedSeq;
                        curEnd = (seqNo + windowSize - 2) % windowSize;
                        resend = true;
                    }
                }
                if (resend) {
                    System.out.println("SeqNo: " + seqNo + " Curend: " + curEnd);
                    DatagramPacket sp = new DatagramPacket(frames[seqNo], frames[seqNo].length, client, port);
                    ds.send(sp);
                    System.out.println("Resending packet " + seqNo);
                    seqNo = (seqNo + 1) % windowSize;
                    byte[] seq = new byte[4];
                    DatagramPacket rp = new DatagramPacket(new byte[4], 4);
                    try {
                        ds.receive(rp);
                    } catch (Exception e) {
                        System.out.println("Acknowledgement not received");
                    }
                    seq = rp.getData();
                    receivedSeq = ByteBuffer.wrap(seq).getInt();
                    Thread.sleep(random.nextInt(10) * 10);
                } else {
                    for (int k = 4; k < 12; k++) {
                        if (i < count) {
                            frames[seqNo][k] = buffer[i++];
                        } else {
                            frames[seqNo][k] = 0;
                        }
                    }

                    DatagramPacket sp = new DatagramPacket(frames[seqNo], frames[seqNo].length, client, port);
                    ds.send(sp);
                    System.out.println("Sending packet " + seqNo);

                    byte[] seq = new byte[4];
                    DatagramPacket rp = new DatagramPacket(new byte[4], 4);
                    try {
                        ds.receive(rp);
                    } catch (Exception e) {
                        System.out.println("Acknowledgement not received");
                    }
                    seq = rp.getData();
                    receivedSeq = ByteBuffer.wrap(seq).getInt();

                    seqNo = (seqNo + 1) % windowSize;
                    Thread.sleep(random.nextInt(10) * 10);
                }
            }
        }
    }

}
