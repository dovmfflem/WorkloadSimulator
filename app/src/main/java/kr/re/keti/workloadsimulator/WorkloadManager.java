package kr.re.keti.workloadsimulator;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;


public class WorkloadManager {

	private boolean is_debug = true;

	private double Workload;
	
	private double speed_weight;
	private double dsm_weight;
	
	private boolean gstart;
	private int steering_state;
	private int speed_state;

	private int steering;
	private int speed;

	private int down_index;
	
	private int down_cnt;
	private int down_value;
	
	private boolean eye;
	private boolean sleep;
	
	private double acceleration;
	private int before_speed;
	
	private boolean flag_overtake;
	private boolean flag_turn;
	private boolean flag_utern;
	private boolean flag_dsm;
		
	
	private int set_time = 250;
	private int checker_time = 3000;
	private int time_dsm = 3000;	
	private int down_time = 1000;
	private int acceleration_time = 1000;
	private int rapid_steering_time = 1000;

	
	private boolean flag_steering;

	int DriverStatus;
	int RapidSteeringStatus;
	int RapidSteering_cnt;

	int rapid_steering_value[];

	int DriverHeart;
	int heartFlag;
	byte heart_data[];
	int heart_cnt;
	final int NumberOfHeartData = 250;
	private boolean heartcheck;
	double heart_avg;
	private int heartrate_check_time = 1000;

	private Context mContext;
	///added for svm
	private File fWorkloadData;
	private final String filename = "WorkloadData.txt";
	private FileWriter fw;

	private LinkedList<Double> ll;
	private Thread th_trainData;

	private int cnt_cl1, cnt_cl2, cnt_cl3;
	private int cnt_limit = 100;
	private boolean trainning_flag = false;
	private boolean predict_flag = true;
	private int num_train_dim = 50;
	private int num_stored_data = 500;

	public WorkloadManager(Context context){
		gstart = false;
		
		steering = 0;
		speed = 0;
				
		flag_overtake = false;
		flag_turn = false;
		flag_utern = false;
		
		flag_steering = false;
		
		speed_weight = 1;
		dsm_weight = 1;
		
		before_speed = 0;
		speed_state = 0;
		down_index = 5;
		flag_dsm = false;
		DriverStatus = 0;
		DriverHeart = 0;

		RapidSteeringStatus = 0;
		RapidSteering_cnt = 0;
		rapid_steering_value = new int[3];
		heartFlag = 0;
		heart_data = new byte[NumberOfHeartData];
		heart_cnt = 0;
		heartcheck = false;
		heart_avg = 0;

		mContext = context;
		ll = new LinkedList<>();
		fWorkloadData = mContext.getFilesDir();

		cnt_cl1 = 0;
		cnt_cl2 = 0;
		cnt_cl3 = 0;
	}

	private svm_model model;
	private int svm_type;
	private int nr_class;

	private int[] labels;
	double[] prob_estimates;

	public void init_svm_predict(){

		try {
			model = svm.svm_load_model(fWorkloadData.toString() + "/result.model");
			svm_type = svm.svm_get_svm_type(model);
			nr_class = svm.svm_get_nr_class(model);

			labels=new int[nr_class];
			svm.svm_get_labels(model,labels);
			prob_estimates = new double[nr_class];

		} catch (IOException e) {
			e.printStackTrace();
		}

	}



	public int predict_driver(){
		svm_node[] x = new svm_node[num_train_dim];
		for(int j=0;j<num_train_dim;j++)
		{
			x[j] = new svm_node();
			x[j].index = j+1;
			x[j].value = ll.indexOf(j);
		}


		double v;
		v = svm.svm_predict_probability(model,x,prob_estimates);
		if(v > 0.5){
			return (int)v;
		}else{
			return -1;
		}

		//return (int)v;
	}

	private boolean is_trained_result = false;
	
	public void start(){
		gstart = true;
		setter.start();
		steering_checker.start();		
		workload_up.start();
		workload_down.start();
		Thrd_acceleration.start();
		Thrd_dsm.start();
		Thrd_rapid_steering.start();
        //Driver_Heart_checker.start();
		check_train_result();
		if(is_trained_result){
			init_svm_predict();
		}
		Thrd_unstable_status.start();
	}
	public void check_train_result(){
		File chfile = new File(fWorkloadData.toString() + "/result.model");
		if(chfile.exists()){
			is_trained_result = true;
		}else{
			is_trained_result = false;
		}
	}

	public void stop(){
		gstart = false;
	}
	
	public byte getWorkload(){

		byte byte_workload = 0;
		if(is_debug){
			byte_workload = (byte)((Math.round(Workload)));
		}else{
			byte_workload = (byte)((Math.round(Workload*0.1)) * 10);
		}


		if(!heartcheck){
			return byte_workload;
		}else {
			if(heartFlag == STATE_HEART_NORMAL){
				return byte_workload;
			}else if(heartFlag == STATE_HEART_HIGH){
				if(is_debug) Log.d("VehicleDataManager", "State_Heart_High Return " + Workload);
				return (byte)(byte_workload +10);
			}else{
				return byte_workload;
			}
		}
	}

	public byte getDsm(){
		byte dsm_data;
		dsm_data = (byte)DriverStatus;
		return dsm_data;
	}

	public byte getSpSt(){
		byte spst = 0;
		byte sum_steering = (byte)RapidSteeringStatus;
		byte sum_speed_state = (byte)speed_state;
		spst = (byte)(sum_speed_state | sum_steering);
		return spst;
	}


	public void setqueue(){


//		ll.add(speed / 25.0);
		ll.add((double)speed);
//		ll.addLast(steering / 450.0);

//		ll.addLast((double)steering);
//		ll.addLast((double)DriverStatus);

		if(ll.size() > num_train_dim){
			ll.removeFirst();
//			ll.removeFirst();
//			ll.removeFirst();


		}
	}

    public void setVehicleData(Bundle bundle) {
		speed = bundle.getShort("speed");
		steering = bundle.getShort("steering");
		setqueue();

    }

    public void setDriverData(Bundle bundle) {
//        eye = bundle.getBoolean("eye");
//        sleep = bundle.getBoolean("sleep");
		DriverStatus = bundle.getInt("driverstatus");
		setqueue();
    }

	public void setHeartData(Bundle bundle){
		DriverHeart = bundle.getInt("heart");
		if(!heartcheck){
			heart_data[heart_cnt] = (byte)DriverHeart;
			heart_avg += DriverHeart;
			heart_cnt++;
		}else{
			heart_avg -= heart_data[heart_cnt];
			heart_data[heart_cnt] = (byte)DriverHeart;
            heart_avg += DriverHeart;
			heart_cnt++;

		}

		if(heart_cnt >= NumberOfHeartData){
			heart_cnt = 0;
			heartcheck = true;
		}
	}
	
	private Thread setter = new Thread(new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			while(gstart) {
				if(Math.abs(steering) >= 450) {
					if(flag_steering == false) {
						flag_steering = true;
					}
				}
				if(!flag_dsm){
					if(DriverStatus == 3 || DriverStatus == 4) {    //sleep
						addWorkload(VALUE_WORKLOAD_SLEEP, "VALUE_WORKLOAD_SLEEP");
						flag_dsm = true;
					}
					if(DriverStatus == 2 || DriverStatus == 5) {    //eye
						addWorkload(VALUE_WORKLOAD_NONFRONTEYE, "VALUE_WORKLOAD_NONFRONTEYE");
						flag_dsm = true;
					}
				}
				try {
					Thread.sleep(set_time);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	});

	private Thread Driver_Heart_checker = new Thread(new Runnable() {
		@Override
		public void run() {
			int before_cnt = 0;
			while(gstart){
				if(heartcheck){
					if(heart_avg / NumberOfHeartData >= 83){
						heartFlag = STATE_HEART_HIGH;
						if(is_debug) Log.d("VehicleDataManager", "Heartrate : " + heart_avg / NumberOfHeartData + " Heart state : STATE_HEART_HIGH" );
					}else if(heart_avg / NumberOfHeartData <= 50){
						heartFlag = STATE_HEART_LOW;
						//Log.d("VehicleDataManager", "Heartrate : " + heart_avg / NumberOfHeartData + " Heart state : STATE_HEART_LOW");
					}else{
						heartFlag = STATE_HEART_NORMAL;
						//Log.d("VehicleDataManager", "Heartrate : " + heart_avg / NumberOfHeartData + " Heart state : STATE_HEART_NORMAL");
					}
				}
				try {
					Thread.sleep(heartrate_check_time);
					if(before_cnt == heart_cnt){
						heartcheck = false;
					}
					before_cnt = heart_cnt;

				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	});
	

	private Thread steering_checker = new Thread(new Runnable() {
		int timer = checker_time/set_time;
		@Override
		public void run() {
			while(gstart) {
				if(flag_steering) {
					if(Math.abs(steering) >= 1800 && Math.abs(steering) <= 5400) {
						flag_utern = true;
					}else if(Math.abs(steering) >= 900) {
						flag_turn = true;
					}else if(Math.abs(steering) >= 450) {
						flag_overtake = true;
					}else if(Math.abs(steering) < 450) {
						timer = 1;
					}
					try {
						Thread.sleep(set_time);
						timer--;
						if(timer == 0) {
							flag_steering = false;
							timer = checker_time/set_time;
							steering_state = setSteeringstate();
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}else{
					try {
						Thread.sleep(set_time);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	});
	
	private int setSteeringstate() {
		if(flag_utern) {
			flag_utern = false;
			flag_turn = false;
			flag_overtake = false;
			return STATE_UTURN;
		}else if(!flag_utern && flag_turn) {
			//System.out.println("STATE_TURN");
			flag_utern = false;
			flag_turn = false;
			flag_overtake = false;
			return STATE_TURN;
		}else if(!flag_utern && !flag_turn && flag_overtake) {
			//System.out.println("STATE_OVERTAKE");
			flag_utern = false;
			flag_turn = false;
			flag_overtake = false;
			return STATE_OVERTAKE;
		}else {
			//System.out.println("STATE_NORMAL");
			flag_utern = false;
			flag_turn = false;
			flag_overtake = false;
			return STATE_NORMAL;
		}
	}
	
	
	private Thread workload_up = new Thread(new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			while(gstart) {
				switch(steering_state) {
				case STATE_NORMAL:
					break;
				case STATE_OVERTAKE:
					addWorkload(VALUE_WORKLOAD_OVERTAKE, "VALUE_WORKLOAD_OVERTAKE");
					//System.out.println("\t\t\tSTATE_OVERTAKE");
					steering_state = STATE_NORMAL;
					break;
				case STATE_TURN:
					//System.out.println("\t\t\tSTATE_TURN");
					addWorkload(VALUE_WORKLOAD_TURN, "VALUE_WORKLOAD_TURN");
					steering_state = STATE_NORMAL;
					break;
				case STATE_UTURN:
					//System.out.println("\t\t\tSTATE_UTURN");
					addWorkload(VALUE_WORKLOAD_UTURN, "VALUE_WORKLOAD_UTURN");
					steering_state = STATE_NORMAL;
					break;
				default :
				}
				try {
					Thread.sleep(set_time);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	});

	private int unstable_time = 10000;
	private boolean unstable_flag = false;
	private int unstable_check_time = 1000;

	private Thread Thrd_unstable_status = new Thread(new Runnable() {
		@Override
		public void run() {
			while(gstart){
				if(unstable_flag){
					try {
						Thread.sleep(unstable_time);
						unstable_flag = false;
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}else{
					try {
						Thread.sleep(unstable_check_time);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	});

	
	private Thread workload_down = new Thread(new Runnable() {
		
		@Override
		public void run() {
			while(gstart) {
				if(Workload - down_value <= 0){
					Workload = 0;
				}else if(Workload > 0){
					Workload -= down_value;
				}
				try {
					Thread.sleep(down_time);
					down_cnt--;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	});
	
	
	private Thread Thrd_dsm = new Thread(new Runnable() {
		
		@Override
		public void run() {
			while(gstart){
				if(flag_dsm){
					try {
						Thread.sleep(time_dsm);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					flag_dsm = false;
				}else {
					try {
						Thread.sleep(set_time);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	});
	
	//가속도 관련 설정
	
	private Thread Thrd_acceleration = new Thread(new Runnable() {
		@Override
		public void run() {
			while(gstart){
				acceleration = speed - before_speed ;
				before_speed = speed;
				if(acceleration >= 11){
					speed_state = STATE_ACCELERATION;
					addWorkload(VALUE_WORKLOAD_ACCEL,"VALUE_WORKLOAD_ACCEL");
				}else if(acceleration <= -7.5){
					speed_state = STATE_DECELERATION;
					addWorkload(VALUE_WORKLOAD_DECEL,"VALUE_WORKLOAD_DECEL");
				}else{
					speed_state = STATE_NORMAL;
				}
				try {
					Thread.sleep(acceleration_time);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	});

	private Thread Thrd_rapid_steering = new Thread(new Runnable() {

		@Override
		public void run() {
			while(gstart){
				RapidSteering_cnt--;
				if(speed >= 15){
					rapid_steering_value[0] = rapid_steering_value[1];
					rapid_steering_value[1] = rapid_steering_value[2];
					rapid_steering_value[2] = steering;

					if(Math.abs(steering) > 900){
						if(rapid_steering_value[0] - rapid_steering_value[2] >= 700){
							//우
							RapidSteeringStatus = STATE_RAPIDSTEERING_RIGHT;
							RapidSteering_cnt = 3;
						}else if(rapid_steering_value[0] - rapid_steering_value[2] < -700){
							//좌
							RapidSteeringStatus = STATE_RAPIDSTEERING_LEFT;
							RapidSteering_cnt = 3;
						}else{
						}
					}
				}
				if(RapidSteering_cnt == 0){
					RapidSteeringStatus = STATE_NORMAL;
				}
				if(RapidSteering_cnt < -100){
					RapidSteering_cnt=-1;
				}
				try {
					Thread.sleep(rapid_steering_time);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	});

	private Thread Thrd_svm_training = new Thread(new Runnable() {
		@Override
		public void run() {

			svm_train t = new svm_train();
			String[] argv = {fWorkloadData.toString() + "/" + filename, fWorkloadData.toString() + "/result.model"};
			try {
				t.run(argv);
				if(is_debug) Log.d("khlee", "SVM Trainning finish");
				predict_flag = true;
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	});

	private void addWorkload(int work, String msg){

		int msg_index = 0;
		if(msg.equals("VALUE_WORKLOAD_SLEEP") || (msg.equals("VALUE_WORKLOAD_NONFRONTEYE"))){
			cnt_cl1++;
			msg_index = 1;
			if(cnt_cl1 <= cnt_limit){
				th_trainData = new Thread(new rtrainData(ll, fWorkloadData, 1, num_train_dim));
				th_trainData.start();
				if(is_debug) Log.d("khlee", "traindata_1");
			}else{
			}
		}else if(msg.equals("VALUE_WORKLOAD_OVERTAKE") || (msg.equals("VALUE_WORKLOAD_TURN")) || (msg.equals("VALUE_WORKLOAD_UTURN"))){
			cnt_cl2++;
			msg_index = 2;
			if(cnt_cl2 <= cnt_limit){
				th_trainData = new Thread(new rtrainData(ll, fWorkloadData, 2, num_train_dim));
				th_trainData.start();
				if(is_debug) Log.d("khlee", "traindata_2");
			}else{
			}
		}else if(msg.equals("VALUE_WORKLOAD_ACCEL") || (msg.equals("VALUE_WORKLOAD_DECEL"))){
			cnt_cl3++;
			msg_index = 3;
			if(cnt_cl3 <= cnt_limit){
				th_trainData = new Thread(new rtrainData(ll, fWorkloadData, 3, num_train_dim));
				th_trainData.start();
				if(is_debug) Log.d("khlee", "traindata_3");
			}else{
			}
		}else{
			msg_index = 0;
			if(is_debug) Log.d("khlee", "Unknown addWorkload case");
		}

		if(cnt_cl1 + cnt_cl2 + cnt_cl3 >= cnt_limit*3 && trainning_flag){

			Thrd_svm_training.start();
			if(is_debug) Log.d("khlee", "SVM Trainning start");
			trainning_flag = false;

		}

		int svm_result;
		if(is_trained_result){
			svm_result = predict_driver();
			if(svm_result == -1){
				unstable_flag = true;
			}else if(msg_index != svm_result){
				unstable_flag = true;
			}else{
				//같으면 아무것ㄷ 안함.
			}
			if(is_debug) Log.d("khlee", "predict result" + svm_result);
		}


		if(!heartcheck){
            if(Workload + work >= 100) {
                Workload = 100;
            }else{
				Workload +=work;
			}
		}else{
            if(Workload + work >= 90) {
                Workload = 90;
            }else{
				Workload += (work*0.9);
			}
		}

		if(unstable_flag){
			if(Workload + (work*0.5) >= 100){
				Workload = 100;
			}else{
				Workload += (work*0.2);
				if(is_debug) Log.d("khlee","unstable up");
			}
		}

		down_cnt = 5;
		down_value = (int)(Workload / 5);
		if(is_debug) Log.d("khlee", msg + " Workload up");
	}

	private static final int STATE_NORMAL = 0;
	private static final int STATE_OVERTAKE = 1;
	private static final int STATE_TURN = 2;
	private static final int STATE_UTURN = 3;
	private static final int STATE_ACCELERATION = 64;
	private static final int STATE_DECELERATION = 32;
	private static final int STATE_RAPIDSTEERING_RIGHT = 2;
	private static final int STATE_RAPIDSTEERING_LEFT = 4;
	private static final int VALUE_WORKLOAD_UTURN = 20;
	private static final int VALUE_WORKLOAD_TURN = 40;
	private static final int VALUE_WORKLOAD_OVERTAKE = 10;
	private static final int VALUE_WORKLOAD_SLEEP = 20;
	private static final int VALUE_WORKLOAD_NONFRONTEYE = 20;
	private static final int VALUE_WORKLOAD_ACCEL = 10;
	private static final int VALUE_WORKLOAD_DECEL = 10;

	private static final int STATE_HEART_NORMAL = 0;
	private static final int STATE_HEART_HIGH = 1;
	private static final int STATE_HEART_LOW = 2;
}

class rtrainData implements Runnable{
	public LinkedList mll;
	public File mfWorkloadData;
	public int mclasses;
	private final String filename = "WorkloadData.txt";
	public int mnum_train_dim;

	public rtrainData(LinkedList ll, File fWorkloadData, int classes, int num_train_dim) {
		mll = ll;
		mfWorkloadData = fWorkloadData;
		mclasses = classes;
		mnum_train_dim = num_train_dim;
	}
	@Override
	public void run() {
		int index = 1;
		StringBuilder sb = new StringBuilder();

		if(mll.size() < mnum_train_dim){
			Log.d("khlee", "trainData length is not dim");
		}else{
			//Log.d("khlee", "trainData length is over dim");
			sb.append(mclasses);
			sb.append(" ");
			for(int i = 0; i < mll.size(); i++){
				//sb.append(i+1);
				sb.append(":");
				sb.append(mll.get(i));
				sb.append(" ");
			}
			sb.append("\n");

			try {
				FileWriter fw = new FileWriter(new File(mfWorkloadData.toString()+"/" + filename),true);
				Log.d("khlee", "trainData write");

				fw.write(sb.toString());
				fw.close();
			} catch (IOException e) {
				Log.d("khlee", "trainData file open fail");
				e.printStackTrace();


			}
		}

	}
}