package com.orangomango.logicsim;

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;

import java.util.*;

import com.orangomango.logicsim.core.Gate;
import com.orangomango.logicsim.core.Chip;
import com.orangomango.logicsim.core.DelayedGate;

public abstract class Util{
	public static final int GATE_DELAY = 15;
	private static boolean CIRCUIT_POWER = true;

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

	public static boolean isDarkColor(Color color){
		// Check StackOverflow
		return color.getRed()*0.2126+color.getGreen()*0.7152+color.getBlue()*0.0722 < 127/255.0;
	}

	public static void schedule(Runnable r, int delay){
		new Thread(() -> {
			try {
				Thread.sleep(delay);
				r.run();
			} catch (InterruptedException ex){
				ex.printStackTrace();
			}
		}).start();
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
}