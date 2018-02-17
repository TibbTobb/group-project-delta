package uk.ac.cam.cl.group_project.delta.lego;

import lejos.hardware.BrickFinder;
import lejos.hardware.ev3.EV3;
import lejos.hardware.lcd.TextLCD;

class MainClass {
	public static void main(String[] args) {
		EV3 ev3 = (EV3) BrickFinder.getLocal();
		TextLCD lcd = ev3.getTextLCD();
		//Keys keys = ev3.getKeys();
		lcd.drawString("This is " + ev3.getName(), 0, 4);
	}
}
