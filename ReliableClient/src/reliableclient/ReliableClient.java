/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reliableclient;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

/**
 *
 * @author kakas
 */
public class ReliableClient {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws SocketException, UnknownHostException, IOException {
        try ( // TODO code application logic here
                DatagramSocket ds = new DatagramSocket()) {
            //System.out.println("Enter the number of pings: ");
            InetAddress ip = InetAddress.getByName("127.0.0.1");
            Scanner sc = new Scanner(System.in);
            int n = 5, seqNo = 0;
            long[] del = new long[n];
            long sum = 0, avg;
            for (int i = 0; i < n; i++) {
                String send = "PING ";
                seqNo++;
                send += seqNo;
                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Date date = new Date();
                send += " " + dateFormat.format(date) + "\r\n";
                //InetAddress ip = InetAddress.getByName("127.0.0.1");
                DatagramPacket dp = new DatagramPacket(send.getBytes(), send.length(), ip, 3402);
                ds.send(dp);

                long startTime = System.currentTimeMillis();

                byte[] buf = new byte[100];
                ds.setSoTimeout(1000);
                DatagramPacket response = new DatagramPacket(buf, 100);
                try {
                    ds.receive(response);
                } catch (Exception e) {
                    System.out.println("Request timed out");
                    continue;
                }
                byte[] receivedData = response.getData();
                BufferedReader b = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(receivedData)));
                String str = b.readLine();

                long endTime = System.currentTimeMillis();
                del[i] = endTime - startTime;
                System.out.println("PING received from " + dp.getAddress() + ": seq#=" + i + " time=" + del[i] + " ms ");
                sum += del[i];
            }
            avg = (long) (sum / (n * 1.0));
            System.out.println("Average time: " + avg);
            FileOutputStream f = new FileOutputStream(new File("out.txt"));
            byte[] buff = new byte[100];
            byte[] data = new byte[100];
            int curSeq = 0;
            int windowSize = 10;
            boolean isDone = false, isPrint = true;
            while (true) {
                DatagramPacket rData = new DatagramPacket(buff, 100);
                ds.setSoTimeout((int) (4 * avg));
                try {
                    ds.receive(rData);
                } catch (Exception e) {
                    if (isDone) {
                        break;
                    }
                    System.out.println("Reached the end");
                    byte[] toSend = ByteBuffer.allocate(4).putInt((curSeq)).array();
                    DatagramPacket dd = new DatagramPacket(toSend, 4, ip, 3402);
                    ds.send(dd);
                    isDone = true;
                    continue;
                }
                isDone = false;
                data = rData.getData();
                byte[] seq = new byte[4];
                System.arraycopy(data, 0, seq, 0, 4);
                byte[] toWrite = new byte[8];
                for (int i = 0; i < 8; i++) {
                    toWrite[i] = data[i + 4];
                }
                if (isPrint) {
                    f.write(toWrite);
                }
                int seqN = ByteBuffer.wrap(seq).getInt();
                if (curSeq == seqN) {
                    curSeq = (curSeq + 1) % windowSize;
                    isPrint = true;
                } else {
                    isPrint = false;
                }
                System.out.println("Expecting: " + curSeq);
                byte[] toSend = ByteBuffer.allocate(4).putInt(curSeq).array();
                DatagramPacket dd = new DatagramPacket(toSend, 4, ip, 3402);
                ds.send(dd);
            }
            f.close();
        }
    }

}
