package com.orangomango.logicsim;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;

import java.util.*;
import dev.webfx.platform.scheduler.Scheduler;

import com.orangomango.logicsim.core.*;

public abstract class Util{
	public static final int GATE_DELAY = 15;
	private static boolean CIRCUIT_POWER = true;
	public static boolean SHOW_PIN_ID = false;

	public static boolean isPowerOn(){
		return CIRCUIT_POWER;
	}

	public static void toggleCircuitPower(List<Gate> gates){
		CIRCUIT_POWER = !CIRCUIT_POWER;
		schedule(() -> updateGatesPower(gates, CIRCUIT_POWER), GATE_DELAY);
	}

	public static void updateGatesPower(List<Gate> gates, boolean value){
		for (Gate gate : gates){
			gate.setPower(value);
			if (gate instanceof Chip){
				updateGatesPower(((Chip)gate).getGates(), value);
			}
			if (gate instanceof DelayedGate){
				((DelayedGate)gate).setLastValue(!value); // Force the gate to auto-update
			}
		}
	}

	public static String wrapString(String text, int maxLength){
		String[] lines = text.split("\n");
		StringBuilder builder = new StringBuilder();
		for (String line : lines){
			int totalLength = 0;
			String[] words = line.split(" ");
			for (String word : words){
				builder.append(word);
				totalLength += word.length();
				if (totalLength > maxLength){
					totalLength = 0;
					builder.append("\n");
				} else {
					builder.append(" ");
				}
			}
			builder.append("\n");
		}
		return builder.toString();
	}

	public static Rectangle2D buildRect(Point2D point, double width, double height){
		double px = width > 0 ? point.getX() : point.getX()+width;
		double py = height > 0 ? point.getY() : point.getY()+height;
		return new Rectangle2D(px, py, Math.abs(width), Math.abs(height));
	}

	public static boolean isDarkColor(Color color){
		// Check StackOverflow
		if (color == null) return false;
		return color.getRed()*0.2126+color.getGreen()*0.7152+color.getBlue()*0.0722 < 127/255.0;
	}

	public static void schedule(Runnable r, int delay){
		Scheduler.scheduleDelay(delay, r);
	}

	public static List<Point2D[]> getPointsList(Point2D start, Point2D end, List<Point2D> points){
		List<Point2D[]> renderingPoints = new ArrayList<>();
		Point2D last = new Point2D(start.getX(), start.getY());
		for (int i = 0; i < points.size(); i++){
			renderingPoints.add(new Point2D[]{last, points.get(i)});
			last = points.get(i);
		}
		renderingPoints.add(new Point2D[]{last, new Point2D(end.getX(), end.getY())});
		return renderingPoints;
	}

	public static Wire getWire(List<Wire> wires, Pin p1, Pin p2){
		for (Wire w : wires){
			if ((w.getPin1() == p1 && w.getPin2() == p2) || (w.getPin1() == p2 && w.getPin2() == p1)){
				return w;
			}
		}
		return null;
	}
}