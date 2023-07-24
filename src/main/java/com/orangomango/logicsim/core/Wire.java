package com.orangomango.logicsim.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;

import java.util.List;

import org.json.JSONObject;
import org.json.JSONArray;

import com.orangomango.logicsim.Util;

public class Wire{
	private GraphicsContext gc;
	private Gate.Pin pin1, pin2;
	private List<Point2D> points;

	public Wire(GraphicsContext gc, Gate.Pin p1, Gate.Pin p2, List<Point2D> list){
		this.gc = gc;
		this.pin1 = p1;
		this.pin2 = p2;
		this.points = list;
		this.pin1.attach(this.pin2);
		this.pin2.attach(this.pin1);
	}

	public Gate.Pin getPin1(){
		return this.pin1;
	}

	public Gate.Pin getPin2(){
		return this.pin2;
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
		for (Point2D p : this.points){
			JSONObject o = new JSONObject();
			o.put("x", p.getX());
			o.put("y", p.getY());
			ps.put(o);
		}
		json.put("points", ps);
		return json;
	}

	public void render(){
		render(this.gc);
	}

	public void render(GraphicsContext gc){
		gc.setStroke((this.pin1.isOn() && !this.pin1.isInput()) || (this.pin2.isOn() && !this.pin2.isInput()) ? Color.GREEN : Color.BLACK);
		gc.setLineWidth(3);

		List<Point2D[]> renderingPoints = Util.getPointsList(new Point2D(this.pin1.getX(), this.pin1.getY()), new Point2D(this.pin2.getX(), this.pin2.getY()), this.points);
		for (Point2D[] line : renderingPoints){
			gc.strokeLine(line[0].getX(), line[0].getY(), line[1].getX(), line[1].getY());
		}
	}
}