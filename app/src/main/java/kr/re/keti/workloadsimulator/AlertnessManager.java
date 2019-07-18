package kr.re.keti.workloadsimulator;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import java.io.File;

import kr.re.keti.VehicleDataContainer.TurnSignal;

public class AlertnessManager {
	
	private double Alertness;
	
	private int speed;
	private boolean isbreak;
	private int steering;
	
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

	}


	public void setVehicleData(Bundle bundle) {
		steering = bundle.getShort("steering");
		speed = bundle.getShort("speed");
		turn_signal = bundle.getShort("turnsignal");
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

		byte byte_alertness;
		//        alertness = Math.round(alertness*0.1);
//        alertness_packet[3] = (byte)(alertness * 10);

		byte_alertness = (byte)((Math.round(Alertness*0.1)) * 10);
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
