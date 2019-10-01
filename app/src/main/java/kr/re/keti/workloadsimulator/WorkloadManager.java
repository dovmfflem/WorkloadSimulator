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
    private boolean is_tablet = true;
	private double Workload;
	
	private double speed_weight;
	private double dsm_weight;
	
	private boolean gstart;
	private int steering_state;
	private int speed_state;

	private int steering;
	private int speed;
	private int rpm;
	private boolean d_break;

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
	final int NumberOfHeartData = 10;
	private boolean heartcheck;
	double heart_avg;
	private int heartrate_check_time = 3000;

	private Context mContext;
	///added for svm
	public File fWorkloadData;
	private final String filename = "WorkloadData.txt";
	private FileWriter fw;

	public LinkedList<Double> ll;
	public LinkedList<Double> dl;
	private Thread th_trainData;

	private int cnt_cl1, cnt_cl2, cnt_cl3;
	private int cnt_limit = 1;
	public boolean trainning_flag = true;
	private boolean predict_flag = true;
	public int num_train_dim = 100;
	private int num_stored_data = 500;
	public int data_counter = 0;


	public svm_model model;
	private int svm_type;
	private int nr_class;

	private int[] labels;
	double[] prob_estimates;
	private String model_filename = "/Wresult.model";
	public boolean data_queue_flag = true;
	public boolean dl_queue_flag = false;


	public WorkloadManager(Context context){
		gstart = false;
		
		steering = 0;
		speed = 0;
		rpm = 0;
		d_break = false;
				
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
		dl = new LinkedList<>();
		for(int i = 0; i < num_train_dim; i++){
			dl.add(0.0);
		}
		fWorkloadData = mContext.getFilesDir();

		cnt_cl1 = 0;
		cnt_cl2 = 0;
		cnt_cl3 = 0;
	}


	public void init_svm_predict(){

		try {
			model = svm.svm_load_model(fWorkloadData.toString() + model_filename);
			svm_type = svm.svm_get_svm_type(model);
			nr_class = svm.svm_get_nr_class(model);

			labels=new int[nr_class];
			svm.svm_get_labels(model,labels);
			prob_estimates = new double[nr_class];

		} catch (IOException e) {
			e.printStackTrace();
		}

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
        Driver_Heart_checker.start();
		check_train_result();
		check_traindata_size();
		if(is_trained_result){
			init_svm_predict();
			//predict_data.start();
		}
		Thrd_unstable_status.start();
	}
	public void check_train_result(){
		File chfile = new File(fWorkloadData.toString() + model_filename);
		if(chfile.exists()){
			is_trained_result = true;
		}else{
			is_trained_result = false;
		}
	}

	private int limite_file_size = 100000;  //100KB
	public void check_traindata_size(){
		File datafile = new File(fWorkloadData.toString() + filename);
		if(datafile.exists()){
			long file_size = datafile.length();
			if(is_debug) Log.d("khlee","datafile size : " + file_size);
			if(file_size >= limite_file_size){
				if(datafile.delete()){
					if(is_debug) Log.d("khlee","datafile deleted");
				}else{
					if(is_debug) Log.d("khlee","datafile continue");
				}

			}
		}
	}

	public void stop(){
		gstart = false;
	}
	
	public byte getWorkload(){

		byte byte_workload = 0;
		byte_workload = (byte)((Math.round(Workload)));
		return byte_workload;
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

    public byte getHeart(){
        byte heart_data;
        heart_data = (byte)DriverHeart;
        return heart_data;
    }

	public void setqueue(){
		data_counter++;

		if(ll.size() < num_train_dim){
			ll.add(speed / 25.0);
			ll.add(steering / 450.0);
			ll.add(rpm / 800.0);
			if(d_break){
				ll.add(0.9);
			}else{
				ll.add(0.1);
			}
			ll.addLast((double)DriverStatus);
		}else if(ll.size() >= num_train_dim){
			ll.add(speed / 25.0);
			ll.removeFirst();
			ll.add(steering / 450.0);
			ll.removeFirst();
			ll.add(rpm / 800.0);
			ll.removeFirst();
			if(d_break){
				ll.add(0.9);
				ll.removeFirst();
			}else{
				ll.add(0.1);
				ll.removeFirst();
			}
			ll.addLast((double)DriverStatus);
			ll.removeFirst();
		}
	}

    public void setVehicleData(Bundle bundle) {
		speed = bundle.getShort("speed");
		steering = bundle.getShort("steering");
		rpm = bundle.getShort("rpm");
		d_break = bundle.getBoolean("break");
		if(data_queue_flag) setqueue();

    }

    public void setDriverData(Bundle bundle) {
		DriverStatus = bundle.getInt("driverstatus");
		//setqueue();
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
					if(heart_avg / NumberOfHeartData >= 100){
						heartFlag = STATE_HEART_HIGH;	//10점
                        addWorkload(VALUE_WORKLOAD_HEART_HIGH, "VALUE_WORKLOAD_HEART_HIGH");
                        if(is_debug) Log.d("VehicleDataManager", "Heartrate : " + heart_avg / NumberOfHeartData + " Heart state : STATE_HEART_HIGH" );
					}else if(heart_avg / NumberOfHeartData <= 100 && heart_avg / NumberOfHeartData < 80){
						heartFlag = STATE_HEART_LITTLE_HIGH;	//5점
                        addWorkload(VALUE_WORKLOAD_HEART_LITTLE_HIGH, "VALUE_WORKLOAD_HEART_LITTLE_HIGH");
                        if(is_debug) Log.d("VehicleDataManager", "Heartrate : " + heart_avg / NumberOfHeartData + " Heart state : STATE_HEART_LITTLE_HIGH");
					}else if(heart_avg / NumberOfHeartData < 65){
						//Log.d("VehicleDataManager", "Heartrate : " + heart_avg / NumberOfHeartData + " Heart state : STATE_HEART_NORMAL");
					}else{
						heartFlag = STATE_HEART_NORMAL;
					}
				}
				try {
					Thread.sleep(heartrate_check_time);
					if(is_debug) Log.d("khlee", "heartrate_check_time");
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
	public boolean unstable_flag = false;  //이상하면 true, 정상이면 false
	private int unstable_check_time = 1000;

	private Thread Thrd_unstable_status = new Thread(new Runnable() {
		@Override
		public void run() {
			while(gstart){
				if(unstable_flag){
					try {
						Log.d("khlee", "unstable time");
						Thread.sleep(unstable_time);
						unstable_flag = false;
						Log.d("khlee", "unstable tiem done");
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
			is_trained_result = true;
			svm_train t = new svm_train();
			String[] argv = {"-b","1", fWorkloadData.toString() + "/" + filename, fWorkloadData.toString() + model_filename};
			if(is_debug) Log.d("khlee", "SVM Training argv : " + argv[0] + " " + argv[1] + " " + argv[2] + " " + argv[3]);
			try {
				t.run(argv);
				if(is_debug) Log.d("khlee", "SVM Trainning finish");
				predict_flag = true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	});

	private Thread th_trainData2;
	public int msg_index = 0;
	private void addWorkload(int work, String msg){

		msg_index = 0;
		if(msg.equals("VALUE_WORKLOAD_SLEEP") || (msg.equals("VALUE_WORKLOAD_NONFRONTEYE"))){
			msg_index = 1;
			if(cnt_cl1 <= cnt_limit){
				cnt_cl1++;
				th_trainData = new Thread(new rtrainData(this, 1));
				th_trainData.start();
				if(is_debug) Log.d("khlee", "traindata_1");
			}else{
			}
		}else if(msg.equals("VALUE_WORKLOAD_OVERTAKE") || (msg.equals("VALUE_WORKLOAD_TURN")) || (msg.equals("VALUE_WORKLOAD_UTURN"))){
			msg_index = 2;
			if(cnt_cl2 <= cnt_limit){
				cnt_cl2++;
				th_trainData = new Thread(new rtrainData(this, 2));
				th_trainData.start();
				if(is_debug) Log.d("khlee", "traindata_2");
			}else{
			}
		}else if(msg.equals("VALUE_WORKLOAD_ACCEL") || (msg.equals("VALUE_WORKLOAD_DECEL"))){
			msg_index = 3;
			if(cnt_cl3 <= cnt_limit){
				cnt_cl3++;
				th_trainData = new Thread(new rtrainData(this, 3));
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
		if(is_trained_result && !trainning_flag){
			th_trainData = new Thread(new rtrainData(this, -1));
			th_trainData.start();
		}

//heart 알고리즘 변경으로 인한 삭제
//		if(!heartcheck){
//            if(Workload + work >= 100) {
//                Workload = 100;
//            }else{
//				Workload +=work;
//			}
//		}else{
//            if(Workload + work >= 90) {
//                Workload = 90;
//            }else{
//				Workload += (work*0.9);
//			}
//		}

		if(unstable_flag){
			if(Workload + (work*0.5) >= 100){
				Workload = 100;
			}else{
				Workload += (work*0.2);
				if(is_debug) Log.d("khlee","unstable up");
			}
		}else{
            if(Workload + work >= 100) {
                Workload = 100;
            }else{
				Workload +=work;
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
    private static final int VALUE_WORKLOAD_HEART_LITTLE_HIGH = 5;
    private static final int VALUE_WORKLOAD_HEART_HIGH = 10;

	private static final int STATE_HEART_NORMAL = 0;
	private static final int STATE_HEART_HIGH = 1;
	private static final int STATE_HEART_LITTLE_HIGH = 2;
	private static final int STATE_HEART_LOW = 3;

}

class rtrainData implements Runnable{
	public LinkedList mll;
	public File mfWorkloadData;
	public int mclasses;
	private final String filename = "WorkloadData.txt";
	public int mnum_train_dim;
	public int data_counter;
	public WorkloadManager mwm;

	public rtrainData(WorkloadManager wm, int classes) {
		//mll = wm.ll;
		mfWorkloadData = wm.fWorkloadData;
		mclasses = classes;
		mnum_train_dim = wm.num_train_dim;
		data_counter = wm.data_counter;
		mwm = wm;
	}
	@Override
	public void run() {

		int index = 1;
		StringBuilder sb = new StringBuilder();

		while(true){


			if(mwm.data_counter - data_counter == mwm.num_train_dim){
				mwm.data_queue_flag = false;
				sb.append(mclasses);
				sb.append(" ");
				mll = mwm.ll;
				for(int i = 0; i < mll.size(); i++) {
					sb.append(i+1);
					sb.append(":");
					sb.append(mll.get(i));
					mwm.dl.set(i,(double)mll.get(i));
					sb.append(" ");
				}
				sb.append("\n");
				if(mwm.trainning_flag){
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

				mwm.data_queue_flag = true;
				mwm.dl_queue_flag = true;
				if(mclasses == -1){
					Thread predict = new Thread(new predict_data(mwm));
					predict.start();
				}
			break;
			}else if(mwm.data_counter - data_counter > mwm.num_train_dim){
				Log.d("khlee", "trainData length is over dim : " + mwm.data_counter +" "+ data_counter);
				break;
			}else{

			}
		}
	}
}

class predict_data implements Runnable{

	WorkloadManager mwm;
	public predict_data(WorkloadManager wm){
		mwm = wm;
	}

	@Override
	public void run() {
				svm_node[] x = new svm_node[mwm.num_train_dim];
				StringBuilder sb = new StringBuilder();
				int now_state = mwm.msg_index;
				for(int j=0;j<mwm.num_train_dim;j++)
				{
					x[j] = new svm_node();
					x[j].index = j+1;
					x[j].value = mwm.dl.get(j);
					sb.append(x[j].value);
					sb.append(" ");

				}
				//Log.d("khlee", "trained data : " + sb.toString());
				mwm.dl_queue_flag=false;
				double v;
				double[] prob_estimates = new double[4];
				v = svm.svm_predict_probability(mwm.model,x,prob_estimates);
				Log.d("khlee", "predict result : " + v + "now state : " + now_state);
				//Log.d("khlee", "predict result : " + prob_estimates[0] + " " + prob_estimates[1] + " " + prob_estimates[2] + " " + prob_estimates[3]);
				if(now_state != (int)v){
					mwm.unstable_flag = true;
				}else{
				}
	}
}
