package kr.re.keti.workloadsimulator;


import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;


import kr.re.keti.DriverDataContainer;
import kr.re.keti.VehicleDataContainer;


public class VehicleManager {

    ////socket
    private static final int ServerPort = 9999;
    private String ServerIP;
    ////

    private boolean isdebug = true;
    private Context mContext;

    private WorkloadManager wm;
    private AlertnessManager am;
    //public CompDataLogger cdl;
    private VehicleDataContainer vdc;
    private DriverDataContainer ddc;

    boolean d_break;
    short steering;
    short turnsignal;
    short speed;
    short rpm;
    short transmission;
    int driver_state;

    private boolean isLogger;

    final private int LOGGER_SLEEP_TIME = 1000;
    int save_cnt;

    private File mfile;

    private boolean first_start;
    private InputStream mInputStream;
    //public File fVehicleData;

    public VehicleManager(Context context) {
//        fVehicleData = new File(mContext.getFilesDir(), "VehicleData.txt");//경로에 데이터를 저장. 학습용 데이터 저장
        wm = new WorkloadManager(context);
        am = new AlertnessManager(context);
        mContext = context;


//        try {
//            cdl = new CompDataLogger();
//        } catch (ParserConfigurationException e) {
//            e.printStackTrace();
//        }
        vdc = new VehicleDataContainer();
        ddc = new DriverDataContainer();
        isLogger = false;
        save_cnt = 120;
        first_start = false;
        //mfile = new File(mContext.getFilesDir(), "khlee3.xml");



        //Log.d("khlee", "serialinput Thread started");


        //serialInputTh.start();

        ServerIP = getLocalIpAddress();
        Log.d("khlee", "IP address : " + ServerIP);
        sendSocket.start();

    }


    int size;
    byte[] buffer = new byte[64];


    private Thread serialInputTh = new Thread(new Runnable() {
        @Override
        public void run() {
            while (true) {

                try {
                    size = mInputStream.read(buffer);
                    Log.d("khlee", "input list(" + size + ") :  " + buffer[0] + " " + buffer[1] + " " + buffer[2] + " " + buffer[3] + " " + buffer[4] + " " + buffer[5] + " " + buffer[6] + " " + buffer[7] + " " + buffer[8]);
                    if (buffer[0] == '[' && buffer[8] == ']') {
                        Bundle serial_bundle = new Bundle();
                        serial_bundle.putShort("speed", buffer[2]);
                        serial_bundle.putShort("steering", buffer[3]);
                        serial_bundle.putShort("turnsignal", buffer[4]);
                        serial_bundle.putInt("heart", (int) buffer[5]);
                        serial_bundle.putInt("driverstatus", buffer[6]);
                        //Log.d("khlee", "good input " + buffer[1] + " " + buffer[2] + " " + buffer[3] + " " + buffer[4] + " " + buffer[5]);
                        //Log.d("khlee", "Size : " + size);
                        setDriverData(serial_bundle);
                        setVehicleData(serial_bundle);
                        setDriverheartData(serial_bundle);
                        //serial_cnt++;

                    } else {
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        }
    });

    public void start() {
        wm.start();
        am.start();
        //LoggerThread.start();
        //isLogger = true;
        //Log.d("khlee", "LoggerThread start");
    }

    public void stop() {
        wm.stop();
        am.stop();
        isLogger = false;
    }


    public void saveVehicleData(Bundle bundle){

        //am용 wm용 두개를 저장.

        //동시에 테스트용 데이터 50개정도 저장.
    }

    public void setVehicleData(Bundle bundle) {
        wm.setVehicleData(bundle);
        am.setVehicleData(bundle);



        rpm = bundle.getShort("rpm");
        speed = bundle.getShort("speed");
        steering = bundle.getShort("steering");
        transmission = bundle.getShort("transmission");
        turnsignal = bundle.getShort("turnsignal");
        d_break = bundle.getBoolean("break");

        vdc.setData(d_break, transmission, turnsignal, steering, speed, rpm);
        if (first_start == false) {
            isLogger = true;
            first_start = true;
            //Log.d("khlee", "logger start");
        }
        //Log.d("khlee", "isLogger : " + isLogger);


        if (isdebug) {
            Log.d("VehicleDataManager", " ");
            Log.d("VehicleDataManager", "steering : " + steering);
            Log.d("VehicleDataManager", "speed : " + speed);
            Log.d("VehicleDataManager", "turnsignal : " + turnsignal);
            Log.d("VehicleDataManager", "steering : " + steering);
            Log.d("VehicleDataManager", "rpm : " + rpm);
        }
    }

    public void setDriverData(Bundle bundle) {
        wm.setDriverData(bundle);
        am.setDriverData(bundle);

        driver_state = bundle.getInt("driverstatus");
        ddc.setData(driver_state);

        if (isdebug) {

            Log.d("VehicleDataManager", "DriverState : " + bundle.getInt("driverstatus"));

        }
    }

    public void setDriverheartData(Bundle bundle) {
        wm.setHeartData(bundle);
        am.setHeartData(bundle);
        //Log.d("VehidleDataManager", "Heart : " + bundle.getInt("heart"));
    }


    public int getWorkload() {
        byte workload = wm.getWorkload();
        byte workload_packet[] = new byte[4];
        int ret_packet = 0;

        //workload = Math.round(workload*0.1);
        //workload_packet[3] = (byte)(workload * 10);
        workload_packet[3] = workload;
        workload_packet[1] = wm.getDsm();
        workload_packet[0] = wm.getSpSt();
        workload_packet[2] = 0;

        ret_packet = makeint(workload_packet);
        return ret_packet;
    }

    public int getAlertness() {
        byte alertness = am.getAlertness();
        byte alertness_packet[] = new byte[4];
        int ret_packet = 0;


        alertness_packet[3] = alertness;
        alertness_packet[1] = wm.getDsm();
        alertness_packet[0] = wm.getSpSt();
        alertness_packet[2] = 0;

        ret_packet = makeint(alertness_packet);
        return ret_packet;
    }

    public int makeint(byte data[]) {

        int result = 0;
        if (data.length != 4) {
            System.out.println("Worng data length");
            return 0;
        } else {

            int s1 = data[0] & 0xFF;
            int s2 = data[1] & 0xFF;
            int s3 = data[2] & 0xFF;
            int s4 = data[3] & 0xFF;

            return ((s1 << 24) + (s2 << 16) + (s3 << 8) + (s4 << 0));
        }
    }

    public byte[] makebyte(int data) {

        byte su[] = new byte[4];

        su[0] = (byte) (data >> 24);
        su[1] = (byte) (data >> 16);
        su[2] = (byte) (data >> 8);
        su[3] = (byte) (data);
        return su;
    }

//    private Thread LoggerThread = new Thread(new Runnable() {
////        @Override
////        public void run() {
////            while (true) {
////                while (isLogger) {
////                    cdl.appendData(vdc, ddc, (int) wm.getWorkload(), (int) am.getAlertness());
////                    Log.d("khlee", "Appended");
////                    try {
////                        Thread.sleep(LOGGER_SLEEP_TIME);
////                        Log.d("khlee", "LoggerThreadSLeep : " + isLogger);
////                        save_cnt--;
////                        if (save_cnt == 0) {
////                            isLogger = false;
////                            try {
////                                cdl.writeToFile(mfile.toString());
////                                Log.d("khlee", "WriteToFile success : " + mfile.toString());
////                            } catch (FileNotFoundException e) {
////                                e.printStackTrace();
////                                Log.d("khlee", "WriteToFile FileNotFoundException");
////                            } catch (TransformerException e) {
////                                e.printStackTrace();
////                                Log.d("khlee", "WriteToFile TransformerException");
////                            }
////                        }
////                    } catch (InterruptedException e) {
////                        e.printStackTrace();
////                    }
////                }
////            }
////        }
////    });

    Thread sendSocket = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                System.out.println("S: Connecting...");
                ServerSocket serverSocket = new ServerSocket(ServerPort);

                while (true) {
                    Socket client = serverSocket.accept();
                    System.out.println("S: Receiving...");
                    try {
                        while (true) {
                            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                            String str = in.readLine();

                            //System.out.println("S: Received: '" + str + "'");

                            String[] data = str.split(" ");

                            Bundle socket_bundle = new Bundle();
                            socket_bundle.putShort("speed", (short) Integer.parseInt(data[0]));
                            socket_bundle.putShort("steering", (short)Integer.parseInt(data[1]));
                            socket_bundle.putShort("turnsignal", (short)Integer.parseInt(data[2]));
                            socket_bundle.putInt("heart", Integer.parseInt(data[3]));
                            socket_bundle.putInt("driverstatus", Integer.parseInt(data[4]));

                            setDriverData(socket_bundle);
                            setVehicleData(socket_bundle);
                            setDriverheartData(socket_bundle);

                        }
                    } catch (Exception e) {
                        System.out.println("S: Error1");
                        e.printStackTrace();
                    } finally {
                        client.close();
                        System.out.println("S: Done.");
                    }
                }
            } catch (Exception e) {
                System.out.println("S: Error2");
                e.printStackTrace();
            }
        }
    });

    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }


}
