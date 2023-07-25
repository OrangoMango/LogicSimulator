package com.orangomango.logicsim;

import javafx.geometry.Point2D;

import java.util.*;

import com.orangomango.logicsim.core.Gate;

public abstract class Util{
	public static final int GATE_DELAY = 1000;
	private static boolean CIRCUIT_POWER = true;

	public static boolean isPowerAvailable(){
		return CIRCUIT_POWER;
	}

	public static void toggleCircuitPower(List<Gate> gates){
		CIRCUIT_POWER = !CIRCUIT_POWER;
		if (!CIRCUIT_POWER){
			for (Gate gate : gates){
				for (Gate.Pin p : gate.getPins()){
					p.setSignal(false);
				}
			}
		}
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