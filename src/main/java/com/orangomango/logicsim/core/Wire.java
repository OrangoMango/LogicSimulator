package com.orangomango.logicsim.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;

import java.util.*;

import org.json.JSONObject;
import org.json.JSONArray;

import com.orangomango.logicsim.Util;

public class Wire{
	private GraphicsContext gc;
	private Pin pin1, pin2;
	private List<WirePoint> points = new ArrayList<>();

	public static class WirePoint{
		private double x, y;

		public WirePoint(double x, double y){
			this.x = x;
			this.y = y;
		}

		public double getX(){
			return this.x;
		}

		public double getY(){
			return this.y;
		}

		public void setX(double x){
			this.x = x;
		}

		public void setY(double y){
			this.y = y;
		}

		public boolean contains(double x, double y){
			Rectangle2D rect = new Rectangle2D(this.x-5, this.y-5, 10, 10);
			return rect.contains(x, y);
		}

		public void render(GraphicsContext gc){
			gc.save();
			gc.setFill(Color.LIME);
			gc.setGlobalAlpha(0.6);
			gc.fillOval(this.x-5, this.y-5, 10, 10);
			gc.restore();
		}
	}

	public Wire(GraphicsContext gc, Pin p1, Pin p2, List<Point2D> list){
		this.gc = gc;
		this.pin1 = p1;
		this.pin2 = p2;
		for (Point2D p : list){
			this.points.add(new WirePoint(p.getX(), p.getY()));
		}
		this.pin1.attach(this.pin2);
		this.pin2.attach(this.pin1);
	}

	public Pin getPin1(){
		return this.pin1;
	}

	public Pin getPin2(){
		return this.pin2;
	}

	public List<WirePoint> getPoints(){
		return this.points;
	}

	public void destroy(){
		this.pin1.getAttachedPins().remove(this.pin2);
		this.pin2.getAttachedPins().remove(this.pin1);
	}

	public JSONObject getJSON(){
		JSONObject json = new JSONObject();
		json.put("pin1", this.pin1.getId());
		json.put("pin2", this.pin2.getId());
		JSONArray ps = new JSONArray();
		for (WirePoint p : this.points){
			JSONObject o = new JSONObject();
			o.put("x", p.getX());
			o.put("y", p.getY());
			ps.put(o);
		}
		json.put("points", ps);
		return json;
	}

	private static List<Point2D> convertPoints(List<WirePoint> points){
		List<Point2D> output = new ArrayList<>();
		for (WirePoint wp : points){
			output.add(new Point2D(wp.getX(), wp.getY()));
		}
		return output;
	}

	public void render(){
		render(this.gc);
	}

	public void render(GraphicsContext gc){
		gc.setStroke((this.pin1.isOn() && !this.pin1.isInput()) || (this.pin2.isOn() && !this.pin2.isInput()) ? Color.GREEN : Color.BLACK);
		gc.setLineWidth(3);

		List<Point2D[]> renderingPoints = Util.getPointsList(new Point2D(this.pin1.getX(), this.pin1.getY()), new Point2D(this.pin2.getX(), this.pin2.getY()), convertPoints(this.points));
		for (Point2D[] line : renderingPoints){
			gc.strokeLine(line[0].getX(), line[0].getY(), line[1].getX(), line[1].getY());
		}
	}
}