package kr.re.keti.workloadsimulator;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.TimerTask;

import kr.re.keti.ConfigTask;
import kr.re.keti.DriverDataContainer;
import kr.re.keti.VehicleDataContainer;
import kr.re.keti.ScheduleAgent;


public class MainActivity extends AppCompatActivity {

    private Handler mHandler;

    private final int SEEKBAR_MAX = 9000;
    private final int SEEKBAR_ZERO = SEEKBAR_MAX/2;

    private TextView tvSpeed;
    private TextView tvRpm;
    private TextView tvHeartrate;
    private TextView tvSteering;
    private TextView tvTurnsignal;
    private TextView tvBreak;

    private TextView tvDriverStatus;
//    private TextView tvSleep;
//    private TextView tvFace;

    private TextView tvWorkload;
    private TextView tvAlertness;
    private SeekBar seekbar;

    private Button btnSpeedUp;
    private Button btnSpeedDown;
    private Button btnRpmUp;
    private Button btnRpmDown;
    private Button btnHeartrateUp;
    private Button btnHeartrateDown;
    private Button btnTurnNone;
    private Button btnTurnLeft;
    private Button btnTurnRight;
    private Button btnTurnBoth;

    private Button btnBreak;

    private Button btnDsmDisable;
    private Button btnDsmEye;
    private Button btnDsmFace;
    private Button btnDsm1stsleep;
    private Button btnDsm2ndsleep;

    private Button btnNormal;
    private Button btnAbnormal;


    private Button btnConnect;

    private EditText etip;
    private CheckBox cb10;


    ////socket
    private String html = "";


    private Socket socket;

    private BufferedReader networkReader;
    private BufferedWriter networkWriter;

    private Editable ServerIP; // IP
    private int port = 9999; // PORT번호


    ////socket

    private Bundle bundle;
    private int plus = 2;

    private short speed = 0;
    private short rpm = 0;
    private short steering = 0;
    private short turn_signal = 0;
    private boolean eye = false;
    private boolean sleep = false;
    private boolean car_break = false;
    private int RapidAcc = 0; // 0 idle, 64 acc, 32 dec
    private int RapidSteer = 0; //0 idle, 4 left, 2 right
    private int heart_rate = 0;
    private int driverstatus = 0;
    private short modelnum = 0;

    private byte Workload_packet[];
    private byte Alertness_packet[];

    private int workload;
    private int alertness;

    private VehicleManager vm;
    private boolean is_thread = false;

    private ImageView iv;

    private double before_angle = 0;
    private double now_angle = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);


        tvSpeed = findViewById(R.id.tvSpeed);
        tvRpm = findViewById(R.id.tvRPM);
        tvHeartrate = findViewById(R.id.tvHeartrate);
        tvSteering = findViewById(R.id.tvSteering);
        tvTurnsignal = findViewById(R.id.tvTurnSignal);
        tvBreak = findViewById(R.id.tvBreak);

        tvDriverStatus = findViewById(R.id.tvDriverStatus);
//        tvSleep = findViewById(R.id.tvSleep);
//        tvFace = findViewById(R.id.tvFace);

        btnSpeedUp = findViewById(R.id.btnSpeedUp);
        btnSpeedUp.setBackgroundColor(Color.LTGRAY);
        btnSpeedUp.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    btnSpeedUp.setBackgroundColor(Color.RED);
                    onSpeedUp();
                }else if(event.getAction() == MotionEvent.ACTION_UP){
                    btnSpeedUp.setBackgroundColor(Color.LTGRAY);
                }
                return false;
            }
        });

        btnSpeedDown = findViewById(R.id.btnSpeedDown);
        btnSpeedDown.setBackgroundColor(Color.LTGRAY);
        btnSpeedDown.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    btnSpeedDown.setBackgroundColor(Color.RED);
                    onSpeedDown();
                }else if(event.getAction() == MotionEvent.ACTION_UP){
                    btnSpeedDown.setBackgroundColor(Color.LTGRAY);
                }
                return false;
            }
        });

        btnRpmUp = findViewById(R.id.btnRpmUp);
        btnRpmUp.setBackgroundColor(Color.LTGRAY);
        btnRpmUp.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    btnRpmUp.setBackgroundColor(Color.RED);
                    onRpmUp();
                }else if(event.getAction() == MotionEvent.ACTION_UP){
                    btnRpmUp.setBackgroundColor(Color.LTGRAY);
                }
                return false;
            }
        });

        btnRpmDown = findViewById(R.id.btnRpmDown);
        btnRpmDown.setBackgroundColor(Color.LTGRAY);
        btnRpmDown.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    btnRpmDown.setBackgroundColor(Color.RED);
                    onRpmDown();
                }else if(event.getAction() == MotionEvent.ACTION_UP){
                    btnRpmDown.setBackgroundColor(Color.LTGRAY);
                }
                return false;
            }
        });

        btnHeartrateUp = findViewById(R.id.btnHeartUp);
        btnHeartrateUp.setBackgroundColor(Color.LTGRAY);
        btnHeartrateUp.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    btnHeartrateUp.setBackgroundColor(Color.RED);
                    onHeartUp();
                }else if(event.getAction() == MotionEvent.ACTION_UP){
                    btnHeartrateUp.setBackgroundColor(Color.LTGRAY);
                }
                return false;
            }
        });


        btnHeartrateDown = findViewById(R.id.btnHeartDown);
        btnHeartrateDown.setBackgroundColor(Color.LTGRAY);
        btnHeartrateDown.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    btnHeartrateDown.setBackgroundColor(Color.RED);
                    onHeartDown();
                }else if(event.getAction() == MotionEvent.ACTION_UP){
                    btnHeartrateDown.setBackgroundColor(Color.LTGRAY);
                }
                return false;
            }
        });


        btnTurnNone = findViewById(R.id.btnTurnNone);
        btnTurnNone.setBackgroundColor(Color.LTGRAY);
        btnTurnNone.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    btnTurnNone.setBackgroundColor(Color.RED);
                    onTurnNone();
                }else if(event.getAction() == MotionEvent.ACTION_UP){
                    btnTurnNone.setBackgroundColor(Color.LTGRAY);
                }
                return false;
            }
        });

        btnTurnLeft = findViewById(R.id.btnTurnLeft);
        btnTurnLeft.setBackgroundColor(Color.LTGRAY);
        btnTurnLeft.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    btnTurnLeft.setBackgroundColor(Color.RED);
                    onTurnLeft();
                }else if(event.getAction() == MotionEvent.ACTION_UP){
                    btnTurnLeft.setBackgroundColor(Color.LTGRAY);
                }
                return false;
            }
        });

        btnTurnRight = findViewById(R.id.btnTurnRight);
        btnTurnRight.setBackgroundColor(Color.LTGRAY);
        btnTurnRight.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    btnTurnRight.setBackgroundColor(Color.RED);
                    onTurnRight();
                }else if(event.getAction() == MotionEvent.ACTION_UP){
                    btnTurnRight.setBackgroundColor(Color.LTGRAY);
                }
                return false;
            }
        });

        btnTurnBoth = findViewById(R.id.btnTurnBoth);
        btnTurnBoth.setBackgroundColor(Color.LTGRAY);
        btnTurnBoth.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    btnTurnBoth.setBackgroundColor(Color.RED);
                    onTurnBoth();
                }else if(event.getAction() == MotionEvent.ACTION_UP){
                    btnTurnBoth.setBackgroundColor(Color.LTGRAY);
                }
                return false;
            }
        });

        btnBreak = findViewById(R.id.btnBreak);
        btnBreak.setBackgroundColor(Color.LTGRAY);
        btnBreak.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    btnBreak.setBackgroundColor(Color.RED);
                    car_break = true;
                }else if(event.getAction() == MotionEvent.ACTION_UP){
                    btnBreak.setBackgroundColor(Color.LTGRAY);
                    car_break = false;
                }
                return false;
            }
        });



        btnDsmDisable = findViewById(R.id.btnDsmDisable);
        btnDsmDisable.setBackgroundColor(Color.LTGRAY);
        btnDsmDisable.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    btnDsmDisable.setBackgroundColor(Color.RED);
                    onDsmDisable();
                }else if(event.getAction() == MotionEvent.ACTION_UP){
                    btnDsmDisable.setBackgroundColor(Color.LTGRAY);
                }
                return false;
            }
        });

        btnDsmEye = findViewById(R.id.btnDsmEyeF);
        btnDsmEye.setBackgroundColor(Color.LTGRAY);
        btnDsmEye.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    btnDsmEye.setBackgroundColor(Color.RED);
                    onDsmEyeF();
                }else if(event.getAction() == MotionEvent.ACTION_UP){
                    btnDsmEye.setBackgroundColor(Color.LTGRAY);
                }
                return false;
            }
        });

        btnDsmFace = findViewById(R.id.btnDsmFace);
        btnDsmFace.setBackgroundColor(Color.LTGRAY);
        btnDsmFace.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    btnDsmFace.setBackgroundColor(Color.RED);
                    onDsmFace();
                }else if(event.getAction() == MotionEvent.ACTION_UP){
                    btnDsmFace.setBackgroundColor(Color.LTGRAY);
                }
                return false;
            }
        });

        btnDsm1stsleep = findViewById(R.id.btnDsm1stsleep);
        btnDsm1stsleep.setBackgroundColor(Color.LTGRAY);
        btnDsm1stsleep.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    btnDsm1stsleep.setBackgroundColor(Color.RED);
                    onDsm1stSleep();
                }else if(event.getAction() == MotionEvent.ACTION_UP){
                    btnDsm1stsleep.setBackgroundColor(Color.LTGRAY);
                }
                return false;
            }
        });

        btnDsm2ndsleep = findViewById(R.id.btnDsm2ndsleep);
        btnDsm2ndsleep.setBackgroundColor(Color.LTGRAY);
        btnDsm2ndsleep.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    btnDsm2ndsleep.setBackgroundColor(Color.RED);
                    onDsm2ndSleep();
                }else if(event.getAction() == MotionEvent.ACTION_UP){
                    btnDsm2ndsleep.setBackgroundColor(Color.LTGRAY);
                }
                return false;
            }
        });

        btnConnect = findViewById(R.id.btnConnect);
        btnConnect.setBackgroundColor(Color.LTGRAY);
        btnConnect.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    btnConnect.setBackgroundColor(Color.RED);
                }else if(event.getAction() == MotionEvent.ACTION_UP){
                    btnConnect.setBackgroundColor(Color.LTGRAY);
                }
                return false;
            }
        });


        btnNormal = findViewById(R.id.btnnormal);
        btnNormal.setBackgroundColor(Color.LTGRAY);
        btnNormal.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    btnNormal.setBackgroundColor(Color.RED);
                    btnAbnormal.setBackgroundColor(Color.LTGRAY);
                    onModelNormal();
                }
                return false;
            }
        });

        btnAbnormal = findViewById(R.id.btnabnormal);
        btnAbnormal.setBackgroundColor(Color.LTGRAY);
        btnAbnormal.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    btnAbnormal.setBackgroundColor(Color.RED);
                    btnNormal.setBackgroundColor(Color.LTGRAY);
                    onModelAbnormal();
                }
                return false;
            }
        });





        /////////////////



        cb10 = findViewById(R.id.cb10);
        etip = findViewById(R.id.editText);
        seekbar = findViewById(R.id.seekBar2);

        iv = findViewById(R.id.imageView);
        iv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                animate(iv, before_angle, 0);
                steering = 0;
                tvSteering.setText("Steering : " + steering);
                seekbar.setProgress(SEEKBAR_ZERO);
                return false;
            }
        });




        ServerIP = etip.getText();
        heart_rate = 65;



        bundle = new Bundle();
        bundle.putShort("speed", speed);
        bundle.putShort("steering", steering);
        bundle.putShort("turnsignal", turn_signal);
        bundle.putInt("driverstatus", driverstatus);



        tvWorkload = findViewById(R.id.tvWorkload);
        tvWorkload.setVisibility(View.INVISIBLE);
        tvAlertness = findViewById(R.id.tvAlertness);
        tvAlertness.setVisibility(View.INVISIBLE);

        seekbar.setMax(SEEKBAR_MAX);
        seekbar.setProgress(SEEKBAR_ZERO);
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                before_angle = now_angle;
                steering = (short)(seekBar.getProgress() - SEEKBAR_ZERO);
                now_angle = steering*0.1;
                tvSteering.setText("Steering : " + String.format("%.1f", steering*0.1));
                animate(iv, before_angle, now_angle);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                steering = (short)(seekBar.getProgress() - SEEKBAR_ZERO);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                steering = (short)(seekBar.getProgress() - SEEKBAR_ZERO);
            }
        });


//        btnSpeedDown = findViewById(R.id.btnSpeedDown);
//        btnRpmUp = findViewById(R.id.btnRpmUp);
//        btnRpmDown = findViewById(R.id.btnRpmDown);
//        btnHeartrateUp = findViewById(R.id.btnHeartUp);
//        btnHeartrateDown = findViewById(R.id.btnHeartDown);
//        btnTurnNone = findViewById(R.id.btnTurnNone);
//        btnTurnLeft = findViewById(R.id.btnTurnLeft);
//        btnTurnRight = findViewById(R.id.btnTurnRight);
//        btnTurnBoth = findViewById(R.id.btnTurnBoth);
//
//
//        btnDsmDisable = findViewById(R.id.btnDsmDisable);
//        btnDsmEye = findViewById(R.id.btnDsmEyeF);
//        btnDsmFace = findViewById(R.id.btnDsmFace);
//        btnDsm1stsleep = findViewById(R.id.btmDsm1stsleep);
//        btnDsm2ndsleep = findViewById(R.id.btnDsm2ndsleep);


//        btnConnect = findViewById(R.id.btnConnect);

        vm = new VehicleManager(getApplicationContext());
        //vm.start();

        threadBreak.start();
        connectThread.start();
        checkUpdate.start();
        th.start();

    }

    private void animate(View view, double fromDegrees, double toDegrees) {
        final RotateAnimation rotate = new RotateAnimation((float) fromDegrees, (float) toDegrees,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(0);
        rotate.setFillAfter(true);
        view.startAnimation(rotate);
    }



    final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {   // Message id 가 0 이면
                workload = vm.getWorkload();
                alertness = vm.getAlertness();

                Workload_packet = vm.makebyte(workload);
                Alertness_packet = vm.makebyte(alertness);


                tvWorkload.setText("Workload : " + Workload_packet[3]); // 메인스레드의 UI 내용 변경
                tvAlertness.setText("Alertness : " + Alertness_packet[3]);
                    /*
                        	System.out.println("급가속 : " + (s >> 6));	//급가속
    	    System.out.println("급감속 : " + (s >> 5));	//급감속

    	System.out.println("급조향(좌) : " + ((s&15) >> 2));	//급감속
    	System.out.println("급조향(우) : " + ((s&15) >> 1));	//급감속
                     */
                //Log.d("dddd", "packet : " + Workload_packet[0]);

//                if((Workload_packet[0] >> 6) == 1){
//                    tvRapidAcc.setText("RapidAcc : 급가속");
//                }else if((Workload_packet[0] >> 5) == 1){
//                    tvRapidAcc.setText("RapidAcc : 급감속");
//                }else{
//                    tvRapidAcc.setText("RapidAcc : None");
//                }
//                if(((Workload_packet[0]&15) >> 2) == 1){
//                    tvRapidSteer.setText("RapidSteer : 급조향 좌");
//                }else if(((Workload_packet[0]&15) >> 1) == 1){
//                    tvRapidSteer.setText("RapidSteer : 급조향 우");
//                }else{
//                    tvRapidSteer.setText("RapidSteer : None");
//                }

            }
        }
    };

    final StringBuffer sb = new StringBuffer();

    Thread th = new Thread(new Runnable() {
        @Override
        public void run() {
            while(true){
                if(is_thread){
                    break;
                }else{
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            while (true) {
                handler.sendEmptyMessage(0);
                bundle.putShort("speed", speed);
                bundle.putShort("steering", steering);
                bundle.putShort("turnsignal", turn_signal);
                bundle.putInt("heart", heart_rate);
                bundle.putInt("driverstatus", driverstatus);
                bundle.putBoolean("break", car_break);
                bundle.putShort("model", modelnum);
                sb.setLength(0);
                sb.append(speed);
                sb.append(" ");
                sb.append(steering);
                sb.append(" ");
                sb.append(turn_signal);
                sb.append(" ");
                sb.append(heart_rate);
                sb.append(" ");
                sb.append(driverstatus);
                sb.append(" ");
                sb.append(car_break);
                sb.append(" ");
                sb.append(modelnum);

                vm.setDriverData(bundle);
                vm.setVehicleData(bundle);
                vm.setDriverheartData(bundle);


                PrintWriter out = new PrintWriter(networkWriter, true);
                //String return_msg = "Socket";
                //System.out.println("Send message : " + sb.toString());
                out.println(sb.toString());


                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    });
    public int breakdown_scale = 1;
    private Thread threadBreak = new Thread(new Runnable() {
        @Override
        public void run() {

            while(true){
                if(car_break == true){
                    if(speed >= 1){
                        speed = (short) (speed - (breakdown_scale));
                        outhandler.sendEmptyMessage(0);
                    }else{
                        speed = 0;
                    }
                }else{

                }
                try {
                    if(car_break){
                        breakdown_scale+=1;
                    }else{
                        breakdown_scale = 1;
                    }
                    Thread.sleep(70);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    });
    final Handler outhandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(speed <=0) speed = 0;
            tvSpeed.setText("Speed : " + speed);
        }
    };

    private Thread checkUpdate = new Thread() {
        public void run() {
            while(true){
                if(is_connect){
                    break;
                }else{
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                String line;
                Log.w("ChattingStart", "Start Thread");
                while (true) {
                    Log.w("Chatting is running", "chatting is running");
                    line = networkReader.readLine();
                    html = line;
                    mHandler.post(showUpdate);
                }
            } catch (Exception e) {

            }
        }
    };

    private Runnable showUpdate = new Runnable() {

        public void run() {
            Toast.makeText(getApplicationContext(), "Coming word: " + html, Toast.LENGTH_SHORT).show();
        }

    };

    public void setSocket(String ip, int port) throws IOException {

        try {
            socket = new Socket(ip, port);
            networkWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            networkReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.out.println(e);
            e.printStackTrace();
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            if(socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Thread connectThread = new Thread(new Runnable() {
        @Override
        public void run() {
            while(true){
                if(is_connect){
                    break;
                }else{
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                setSocket(ServerIP.toString(), port);
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                System.out.println("Connect success");
                is_thread = true;
            }
        }
    });

    private boolean is_connect = false;
    public void onConnect(View view) {
        ServerIP = etip.getText();
        is_connect = true;
    }


    public void onSpeedUp() {
        speed += plus;
        if(speed >= 250){
            speed = 250;
            Toast.makeText(getApplicationContext(),"Speed Limit 250km",Toast.LENGTH_SHORT).show();
        }
        tvSpeed.setText("Speed : " + speed);
    }

    public void onSpeedDown() {
        speed -= plus;
        if(speed <= 0){
            speed = 0;
        }
        tvSpeed.setText("Speed : " + speed);
        Log.d("khlee", "scHandler send message");
    }

    public void onRpmUp() {
        rpm += (plus*100);
        if(rpm >= 8000){
            rpm = 8000;
            Toast.makeText(getApplicationContext(),"Rpm Limit 8000rpm",Toast.LENGTH_SHORT).show();
        }
        tvRpm.setText("Rpm : " + rpm);
    }

    public void onRpmDown() {
        rpm -= (plus*100);
        if(rpm <= 0){
            rpm = 0;
        }
        tvRpm.setText("Rpm : " + rpm);
    }

    public void onHeartUp() {
        heart_rate += plus;
        tvHeartrate.setText("Heart rate : " + heart_rate);
    }

    public void onHeartDown() {
        heart_rate -= plus;
        tvHeartrate.setText("Heart rate : " + heart_rate);
    }

    public void onTurnNone() {
        turn_signal = VehicleDataContainer.TurnSignal.Invalid;
        tvTurnsignal.setText("Turn signal : None");
    }

    public void onTurnLeft() {
        turn_signal = VehicleDataContainer.TurnSignal.Left;
        tvTurnsignal.setText("Turn signal : Left");
    }

    public void onTurnRight() {
        turn_signal = VehicleDataContainer.TurnSignal.Right;
        tvTurnsignal.setText("Turn signal : Right");
    }

    public void onTurnBoth() {
        turn_signal = VehicleDataContainer.TurnSignal.Both;
        tvTurnsignal.setText("Turn signal : Both");
    }

    public void onDsmDisable() {
        driverstatus = DriverDataContainer.DSM_DISABLE;
        tvDriverStatus.setText("Clear");
    }

    public void onDsmEyeF() {
        driverstatus = DriverDataContainer.DSM_KEEP_EYES_FORWARD;
        tvDriverStatus.setText("Keep Eyes Forward");
    }

    public void onDsmFace() {
        driverstatus = DriverDataContainer.DSM_CANNOT_RECOGNIZE_FACE;
        tvDriverStatus.setText("Can't Recognize Face");
    }

    public void onDsm1stSleep() {
        driverstatus = DriverDataContainer.DSM_1ST_DROWSINESS;
        tvDriverStatus.setText("1ST_DROWSINESS");
    }

    public void onDsm2ndSleep() {
        driverstatus = DriverDataContainer.DSM_2ND_DROWSINESS;
        tvDriverStatus.setText("2ND_DROWSINESS");
    }

    public void onBreak(View view) {
//        if(car_break == false){
//            car_break = true;
//            tvBreak.setText("Break : true");
//        }else{
//            car_break = false;
//            tvBreak.setText("Break : false");
//        }

    }

    public void onPlus10(View view) {
        if(cb10.isChecked()){
            plus = 10;
        }else{
            plus = 1;
        }
    }

    public void onModelNormal() {
        modelnum = 1;
    }

    public void onModelAbnormal() {
        modelnum = 2;

    }

    private Runnable run = new Runnable() {
        @Override
        public void run() {
            Log.d("khlee", "thread start");
            String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/sc.conf";
            ScheduleAgent agent = new ScheduleAgent(path);
            agent.insertJob("Speedup", new ConfigTask() {
                @Override
                public void run() {
                    Bundle bnd = new Bundle();
                    bnd.putString("task", "Speedup");
                    Message msg = new Message();
                    msg.setData(bnd);
                    uiUpdate.sendMessage(msg);
                }
            });
            agent.insertJob("Speeddown", new ConfigTask() {
                @Override
                public void run() {
                    Bundle bnd = new Bundle();
                    bnd.putString("task", "Speeddown");
                    Message msg = new Message();
                    msg.setData(bnd);
                    uiUpdate.sendMessage(msg);
                }
            });
            agent.insertJob("Rpmup", new ConfigTask() {
                @Override
                public void run() {
                    Bundle bnd = new Bundle();
                    bnd.putString("task", "Rpmup");
                    Message msg = new Message();
                    msg.setData(bnd);
                    uiUpdate.sendMessage(msg);
                }
            });
            agent.insertJob("Rpmdown", new ConfigTask() {
                @Override
                public void run() {
                    Bundle bnd = new Bundle();
                    bnd.putString("task", "Rpmdown");
                    Message msg = new Message();
                    msg.setData(bnd);
                    uiUpdate.sendMessage(msg);
                }
            });
            agent.insertJob("Heartup", new ConfigTask() {
                @Override
                public void run() {
                    Bundle bnd = new Bundle();
                    bnd.putString("task", "Heartup");
                    Message msg = new Message();
                    msg.setData(bnd);
                    uiUpdate.sendMessage(msg);
                }
            });
            agent.insertJob("Heartdown", new ConfigTask() {
                @Override
                public void run() {
                    Bundle bnd = new Bundle();
                    bnd.putString("task", "Heartdown");
                    Message msg = new Message();
                    msg.setData(bnd);
                    uiUpdate.sendMessage(msg);
                }
            });
            agent.insertJob("Turnnone", new ConfigTask() {
                @Override
                public void run() {
                    Bundle bnd = new Bundle();
                    bnd.putString("task", "Turnnone");
                    Message msg = new Message();
                    msg.setData(bnd);
                    uiUpdate.sendMessage(msg);
                }
            });
            agent.insertJob("Turnleft", new ConfigTask() {
                @Override
                public void run() {
                    Bundle bnd = new Bundle();
                    bnd.putString("task", "Turnleft");
                    Message msg = new Message();
                    msg.setData(bnd);
                    uiUpdate.sendMessage(msg);
                }
            });
            agent.insertJob("Turnright", new ConfigTask() {
                @Override
                public void run() {
                    Bundle bnd = new Bundle();
                    bnd.putString("task", "Turnright");
                    Message msg = new Message();
                    msg.setData(bnd);
                    uiUpdate.sendMessage(msg);
                }
            });
            agent.insertJob("Turnboth", new ConfigTask() {
                @Override
                public void run() {
                    Bundle bnd = new Bundle();
                    bnd.putString("task", "Turnboth");
                    Message msg = new Message();
                    msg.setData(bnd);
                    uiUpdate.sendMessage(msg);
                }
            });
            agent.insertJob("Break", new ConfigTask() {
                @Override
                public void run() {
                    Bundle bnd = new Bundle();
                    bnd.putString("task", "Break");
                    Message msg = new Message();
                    msg.setData(bnd);
                    uiUpdate.sendMessage(msg);
                }
            });
            agent.insertJob("Dsmdisable", new ConfigTask() {
                @Override
                public void run() {
                    Bundle bnd = new Bundle();
                    bnd.putString("task", "Dsmdisable");
                    Message msg = new Message();
                    msg.setData(bnd);
                    uiUpdate.sendMessage(msg);
                }
            });
            agent.insertJob("Dsmeye", new ConfigTask() {
                @Override
                public void run() {
                    Bundle bnd = new Bundle();
                    bnd.putString("task", "Dsmeye");
                    Message msg = new Message();
                    msg.setData(bnd);
                    uiUpdate.sendMessage(msg);
                }
            });
            agent.insertJob("Dsmface", new ConfigTask() {
                @Override
                public void run() {
                    Bundle bnd = new Bundle();
                    bnd.putString("task", "Dsmface");
                    Message msg = new Message();
                    msg.setData(bnd);
                    uiUpdate.sendMessage(msg);
                }
            });
            agent.insertJob("Dsm1stsleep", new ConfigTask() {
                @Override
                public void run() {
                    Bundle bnd = new Bundle();
                    bnd.putString("task", "Dsm1stsleep");
                    Message msg = new Message();
                    msg.setData(bnd);
                    uiUpdate.sendMessage(msg);
                }
            });
            agent.insertJob("Dsm2ndsleep", new ConfigTask() {
                @Override
                public void run() {
                    Bundle bnd = new Bundle();
                    bnd.putString("task", "Dsm2ndsleep");
                    Message msg = new Message();
                    msg.setData(bnd);
                    uiUpdate.sendMessage(msg);
                }
            });


            try {
                agent.loadConfigFile();
                Thread.sleep(10000);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    private Thread th_simul = new Thread(run);

    public void onSimul(View view) {
        th_simul.start();
    }

    final Handler uiUpdate = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            Bundle bnd = new Bundle();
            bnd = msg.getData();
            if(bnd.getString("task") == "Speedup"){
                onSpeedUp();
            }
            if(bnd.getString("task") == "Speeddown"){
                onSpeedDown();
            }
            if(bnd.getString("task") == "Rpmup"){
                onRpmUp();
            }
            if(bnd.getString("task") == "Rpmdown"){
                onRpmDown();
            }
            if(bnd.getString("task") == "Heartup"){
                onHeartUp();
            }
            if(bnd.getString("task") == "Heartdown"){
                onHeartDown();
            }
            if(bnd.getString("task") == "Turnnone"){
                onTurnNone();
            }
            if(bnd.getString("task") == "Turnleft"){
                onTurnLeft();
            }
            if(bnd.getString("task") == "Turnright"){
                onTurnRight();
            }
            if(bnd.getString("task") == "Turnboth"){
                onTurnBoth();
            }
            if(bnd.getString("task") == "Breakdown"){
                //onBreak();
                car_break = true;
            }
            if(bnd.getString("task") == "Breakup"){
                //onBreak();
                car_break = false;
            }
            if(bnd.getString("task") == "Dsmdisable"){
                onDsmDisable();
            }
            if(bnd.getString("task") == "Dsmeye"){
                onDsmEyeF();
            }
            if(bnd.getString("task") == "Dsmface"){
                onDsmFace();
            }
            if(bnd.getString("task") == "Dsm1stsleep"){
                onDsm1stSleep();
            }
            if(bnd.getString("task") == "Dsm2ndsleep"){
                onDsm2ndSleep();
            }

        }
    };
}
