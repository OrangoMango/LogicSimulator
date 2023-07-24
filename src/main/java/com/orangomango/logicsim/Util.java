package com.orangomango.logicsim;

import javafx.geometry.Point2D;

import java.util.*;

public class Util{
	/*public static void schedule(Runnable r, int delay){
		new Thread(() -> {
			try {
				Thread.sleep(delay);
				r.run();
			} catch (InterruptedException ex){
				ex.printStackTrace();
			}
		}).start();
	}*/

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