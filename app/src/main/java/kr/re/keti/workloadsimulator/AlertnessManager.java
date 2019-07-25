package kr.re.keti.workloadsimulator;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

import kr.re.keti.VehicleDataContainer.TurnSignal;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;

public class AlertnessManager {
	public boolean is_debug = false;

	private double Alertness;
	
	private int speed;
	private boolean isbreak;
	private int steering;
	private int rpm;
	private boolean d_break;

	
	private boolean flag_steering;
	private boolean flag_dsm;
	
	private double acceleration;
	private double speed_weight;
	private int speed_state;
	private int before_speed;

	private int down_cnt;
	private int down_value;

	private short turn_signal;
	
	private boolean eye;
	private boolean sleep;

	private boolean gstart;
	
	private int set_time = 250;
	private int time_steering = 3000;
	private int time_dsm = 3000;
	private int down_time = 1000;
	private int acceleration_time = 1000;
	private int rapid_steering_time = 1000;
	
	private double dsm_weight;

	double now_alert;
	double previous_alert;
	int now_state;
	int previous_state;
	int Alertness_state;
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
	public File fAlertnessData;
	private final String filename = "AlertnessData.txt";
	private FileWriter fw;

	public LinkedList<Double> ll;
	public LinkedList<Double> dl;
	private Thread th_trainData;

	private int cnt_cl1, cnt_cl2, cnt_cl3;
	private int cnt_limit = 0;
	public boolean trainning_flag = true;
	private boolean predict_flag = true;
	public int num_train_dim = 50;
	private int num_stored_data = 500;
	public int data_counter = 0;


	public svm_model model;
	private int svm_type;
	private int nr_class;

	private int[] labels;
	double[] prob_estimates;
	private String model_filename = "/Aresult.model";
	public boolean data_queue_flag = true;
	public boolean dl_queue_flag = false;
	private boolean is_trained_result = false;

	public AlertnessManager(Context context){
		dsm_weight = 1;
		flag_steering = false;
		flag_dsm = false;
		down_cnt = 5;
		down_value = 0;

		now_alert = 0;
		previous_alert = 0;

		Alertness_state = 0;
		DriverStatus = 0;
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
		fAlertnessData = mContext.getFilesDir();

		cnt_cl1 = 0;
		cnt_cl2 = 0;
		cnt_cl3 = 0;

	}
	//svm add
	public void init_svm_predict(){

		try {
			model = svm.svm_load_model(fAlertnessData.toString() + model_filename);
			svm_type = svm.svm_get_svm_type(model);
			nr_class = svm.svm_get_nr_class(model);

			labels=new int[nr_class];
			svm.svm_get_labels(model,labels);
			prob_estimates = new double[nr_class];

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	public void setqueue() {
		data_counter++;
		//1그대로


		//2변화량

		ll.add(speed / 25.0);
		//ll.add((double)speed);
		ll.add(steering / 450.0);
		ll.add(rpm / 800.0);
		if (d_break) {
			ll.add(0.9);
		} else {
			ll.add(0.1);
		}
	}

	private int unstable_time = 10000;
	public boolean unstable_flag = false;  //이상하면 true, 정상이면 false
	private int unstable_check_time = 1000;
	public int msg_index = 0;


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

	private Thread Thrd_svm_training = new Thread(new Runnable() {
		@Override
		public void run() {
			is_trained_result = true;
			svm_train t = new svm_train();
			String[] argv = {"-b","1", fAlertnessData.toString() + "/" + filename, fAlertnessData.toString() + model_filename};
			try {
				t.run(argv);
				if(is_debug) Log.d("khlee", "SVM Trainning finish");
				predict_flag = true;
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	});




	public void setVehicleData(Bundle bundle) {
		steering = bundle.getShort("steering");
		speed = bundle.getShort("speed");
		turn_signal = bundle.getShort("turnsignal");
		d_break = bundle.getBoolean("break");
		if(data_queue_flag) setqueue();

	}

	public void setDriverData(Bundle bundle) {
		DriverStatus = bundle.getInt("driverstatus");
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


	public byte getAlertness(){

		byte byte_alertness = 0;
		//        alertness = Math.round(alertness*0.1);
//        alertness_packet[3] = (byte)(alertness * 10);

		if(is_debug){
			byte_alertness = (byte)((Math.round(Alertness)));
		}else{
			byte_alertness = (byte)((Math.round(Alertness*0.1)) * 10);
		}

		if(!heartcheck){
			return byte_alertness;
		}else {
			if(heartFlag == STATE_HEART_NORMAL){
				return byte_alertness;
			}else if(heartFlag == STATE_HEART_HIGH){
				return (byte)(byte_alertness + 10);
			}else{
				return byte_alertness;
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

	
	public void start(){
		gstart = true;
		setter.start();
		Thrd_steering.start();
		Thrd_acceleration.start();
		alertness_down.start();
		Thrd_dsm.start();
		Thrd_rapid_steering.start();
		//Driver_Heart_checker.start();
		check_train_result();
		if(is_trained_result){
			init_svm_predict();
			//predict_data.start();
		}
		Thrd_unstable_status.start();
	}
	public void check_train_result(){
		File chfile = new File(fAlertnessData.toString() + model_filename);
		if(chfile.exists()){
			is_trained_result = true;
		}else{
			is_trained_result = false;
		}
	}

	public void stop(){
		gstart = false;
	}
	
	private Thread setter = new Thread(new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			while(gstart){
				if(!flag_dsm){
					if(DriverStatus == 3 || DriverStatus == 4) {    //sleep
						addAlertness(VALUE_ALERTNESS_SLEEP, "VALUE_ALERTNESS_SLEEP");
						flag_dsm = true;
					}
					if(DriverStatus == 2 || DriverStatus == 5) {    //eye
						addAlertness(VALUE_ALERTNESS_NONFRONTEYE, "VALUE_ALERTNESS_NONFRONTEYE");
						flag_dsm = true;
					}
				}

				if (!flag_steering) {
					if (speed <= 10) {
						if (Math.abs(steering) > 150) {
							if (turn_signal == TurnSignal.Left || turn_signal == TurnSignal.Right) {
								addAlertness(VALUE_ALERTNESS_INTENTOP, "VALUE_ALERTNESS_INTENTOP");
								//System.out.println("state 1");
								flag_steering = true;
							} else {
								addAlertness(VALUE_ALERTNESS_IGNOREOP, "VALUE_ALERTNESS_IGNOREOP");
								//System.out.println("state 2");
								flag_steering = true;
							}
						}
					}else if (speed <= 30){
						if (Math.abs(steering) > 90) {
							if (turn_signal == TurnSignal.Left || turn_signal == TurnSignal.Right) {
								addAlertness(VALUE_ALERTNESS_INTENTOP, "VALUE_ALERTNESS_INTENTOP");
								//System.out.println("state 1");
								flag_steering = true;
							} else {
								addAlertness(VALUE_ALERTNESS_IGNOREOP, "VALUE_ALERTNESS_IGNOREOP");
								//System.out.println("state 2");
								flag_steering = true;
							}
						}
					}else if(speed <= 60){
						if (Math.abs(steering) > 50) {
							if (turn_signal == TurnSignal.Left || turn_signal == TurnSignal.Right) {
								addAlertness(VALUE_ALERTNESS_INTENTOP, "VALUE_ALERTNESS_INTENTOP");
								//System.out.println("state 1");
								flag_steering = true;
							} else {
								addAlertness(VALUE_ALERTNESS_IGNOREOP, "VALUE_ALERTNESS_IGNOREOP");
								//System.out.println("state 2");
								flag_steering = true;
							}
						}
					}else{
						if (Math.abs(steering) > 30) {
							if (turn_signal == TurnSignal.Left || turn_signal == TurnSignal.Right) {
								addAlertness(VALUE_ALERTNESS_INTENTOP, "VALUE_ALERTNESS_INTENTOP");
								//System.out.println("state 1");
								flag_steering = true;
							} else {
								addAlertness(VALUE_ALERTNESS_IGNOREOP, "VALUE_ALERTNESS_IGNOREOP");
								//System.out.println("state 2");
								flag_steering = true;
							}
						}
					}


//					if(Math.abs(steering) > 30){
//						if(turn_signal == TurnSignal.Left || turn_signal == TurnSignal.Right){
//							addAlertness(VALUE_ALERTNESS_INTENTOP, "VALUE_ALERTNESS_INTENTOP");
//							//System.out.println("state 1");
//							flag_steering = true;
//						}else{
//							addAlertness(VALUE_ALERTNESS_IGNOREOP, "VALUE_ALERTNESS_IGNOREOP");
//							//System.out.println("state 2");
//							flag_steering = true;
//						}
//					}else if(Math.abs(steering) > 10){
//						if(turn_signal == TurnSignal.Left || turn_signal == TurnSignal.Right){
//							if(speed >= 60){
//								addAlertness(VALUE_ALERTNESS_INTENTOP, "VALUE_ALERTNESS_INTENTOP");
//								//System.out.println("state 3");
//								flag_steering = true;
//							}
//						}else{
//							if(speed >= 60){
//								addAlertness(VALUE_ALERTNESS_IGNOREOP, "VALUE_ALERTNESS_IGNOREOP");
//								//System.out.println("state 4");
//								flag_steering = true;
//							}
//						}
//					}
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
						//Log.d("VehicleDataManager", "Heartrate : " + heart_avg / NumberOfHeartData + " Heart state : STATE_HEART_HIGH" );
					}else if(heart_avg / NumberOfHeartData <= 50){
						heartFlag = STATE_HEART_LOW;
						Log.d("VehicleDataManagerA", "Heartrate : " + heart_avg / NumberOfHeartData + " Heart state : STATE_HEART_LOW");
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
	
	private Thread Thrd_steering = new Thread(new Runnable() {
		
		@Override
		public void run() {
			while(gstart){
				if(flag_steering){
					try {
						Thread.sleep(time_steering);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					flag_steering = false;
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

	private Thread Thrd_acceleration = new Thread(new Runnable() {
		
		@Override
		public void run() {
			while(gstart){
				acceleration = speed - before_speed;
				before_speed = speed;
				if(acceleration >= 11){
					speed_state = STATE_ACCELERATION;
				}else if(acceleration <= -7.5){
					speed_state = STATE_DECELERATION;
					if(turn_signal != TurnSignal.Both){
						addAlertness(VALUE_ALERTNESS_DECEL, "VALUE_ALERTNESS_DECEL");
					}
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
	
	private Thread alertness_down = new Thread(new Runnable() {
		
		@Override
		public void run() {
			while(gstart) {
				if(Alertness - down_value <= 0){
					Alertness = 0;
				}else if(Alertness > 0){
					Alertness -= down_value;
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
	
	
	private void addAlertness(int alert, String msg){

		msg_index = 0;
		if(msg.equals("VALUE_ALERTNESS_SLEEP") || (msg.equals("VALUE_ALERTNESS_NONFRONTEYE"))){
			msg_index = 1;
			if(cnt_cl1 <= cnt_limit){
				cnt_cl1++;
				th_trainData = new Thread(new rtrainData_am(this, 1));
				th_trainData.start();
				if(is_debug) Log.d("khlee", "traindata_1");
			}else{
			}
		}else if(msg.equals("VALUE_ALERTNESS_INTENTOP") || (msg.equals("VALUE_ALERTNESS_IGNOREOP"))){
			msg_index = 2;
			if(cnt_cl2 <= cnt_limit){
				cnt_cl2++;
				th_trainData = new Thread(new rtrainData_am(this, 2));
				th_trainData.start();
				if(is_debug) Log.d("khlee", "traindata_2");
			}else{
			}
		}else if(msg.equals("VALUE_ALERTNESS_DECEL")){
			msg_index = 3;
			if(cnt_cl3 <= cnt_limit){
				cnt_cl3++;
				th_trainData = new Thread(new rtrainData_am(this, 3));
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
			th_trainData = new Thread(new rtrainData_am(this, -1));
			th_trainData.start();
		}


		if(!heartcheck){
            if(Alertness + alert >= 100) {
                Alertness = 100;
            }else{
                Alertness +=alert;
            }
        }else{
            if(Alertness + alert >= 90) {
                Alertness = 90;
            }else{
                Alertness += (alert*0.9);
            }
        }
        down_cnt = 5;
        down_value = (int)(Alertness / 5);
        Log.d("khlee", msg + " Alertness up");
	}


	private static final int STATE_NORMAL = 0;
	private static final int STATE_ACCELERATION = 64;
	private static final int STATE_DECELERATION = 32;
	private static final int STATE_RAPIDSTEERING_RIGHT = 2;
	private static final int STATE_RAPIDSTEERING_LEFT = 4;
	private static final int VALUE_ALERTNESS_INTENTOP = 10;
	private static final int VALUE_ALERTNESS_IGNOREOP = 20;
	private static final int VALUE_ALERTNESS_DECEL = 20;
	private static final int VALUE_ALERTNESS_SLEEP = 60;
	private static final int VALUE_ALERTNESS_NONFRONTEYE = 60;

	private static final int STATE_HEART_NORMAL = 0;
	private static final int STATE_HEART_HIGH = 1;
	private static final int STATE_HEART_LOW = 2;
}

class rtrainData_am implements Runnable{
	public LinkedList mll;
	public File mfAlertnessData;
	public int mclasses;
	private final String filename = "AlertnessData.txt";
	public int mnum_train_dim;
	public int data_counter;
	public AlertnessManager mam;

	public rtrainData_am(AlertnessManager am, int classes) {
		//mll = wm.ll;
		mfAlertnessData = am.fAlertnessData;
		mclasses = classes;
		mnum_train_dim = am.num_train_dim;
		data_counter = am.data_counter;
		mam = am;
	}
	@Override
	public void run() {

		int index = 1;
		StringBuilder sb = new StringBuilder();

		while(true){


			if(mam.data_counter - data_counter == mam.num_train_dim){
				mam.data_queue_flag = false;
				sb.append(mclasses);
				sb.append(" ");
				mll = mam.ll;
				for(int i = 0; i < mll.size(); i++) {
					sb.append(i+1);
					sb.append(":");
					sb.append(mll.get(i));
					mam.dl.set(i,(double)mll.get(i));
					sb.append(" ");
				}
				sb.append("\n");
				if(mam.trainning_flag){
					try {
						FileWriter fw = new FileWriter(new File(mfAlertnessData.toString()+"/" + filename),true);
						Log.d("khlee", "trainData write");

						fw.write(sb.toString());
						fw.close();
					} catch (IOException e) {
						Log.d("khlee", "trainData file open fail");
						e.printStackTrace();
					}
				}

				mam.data_queue_flag = true;
				mam.dl_queue_flag = true;
				if(mclasses == -1){
					Thread predict = new Thread(new predict_data_am(mam));
					predict.start();
				}
				break;
			}else if(mam.data_counter - data_counter > mam.num_train_dim){
				Log.d("khlee", "trainData length is over dim : " + mam.data_counter +" "+ data_counter);
				break;
			}else{

			}
		}
	}
}

class predict_data_am implements Runnable{

	AlertnessManager mam;
	public predict_data_am(AlertnessManager am){
		mam = am;
	}

	@Override
	public void run() {
		svm_node[] x = new svm_node[mam.num_train_dim];
		StringBuilder sb = new StringBuilder();
		int now_state = mam.msg_index;
		for(int j=0;j<mam.num_train_dim;j++)
		{
			x[j] = new svm_node();
			x[j].index = j+1;
			x[j].value = mam.dl.get(j);
			sb.append(x[j].value);
			sb.append(" ");

		}
		//Log.d("khlee", "trained data : " + sb.toString());
		mam.dl_queue_flag=false;
		double v;
		double[] prob_estimates = new double[4];
		v = svm.svm_predict_probability(mam.model,x,prob_estimates);
		Log.d("khlee", "predict result : " + v + "now state : " + now_state);
		//Log.d("khlee", "predict result : " + prob_estimates[0] + " " + prob_estimates[1] + " " + prob_estimates[2] + " " + prob_estimates[3]);
		if(now_state != (int)v){
			mam.unstable_flag = true;
		}else{
		}
	}
}