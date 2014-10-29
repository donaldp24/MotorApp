package com.Tony.Zakron.Motor;


public interface MotorControlListener {
	public void onPortOpened(boolean success);
	public void onMotorInited(Motor motor);
	public void onReceivedStatusPacket(Motor motor);
}
