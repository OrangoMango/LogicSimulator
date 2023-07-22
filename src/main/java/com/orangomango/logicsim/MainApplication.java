package com.orangomango.logicsim;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.input.MouseButton;
import javafx.scene.canvas.*;
import javafx.scene.paint.Color;
import javafx.animation.*;
import javafx.util.Duration;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Point2D;

import java.util.*;
import java.io.*;
import org.json.JSONObject;
import org.json.JSONArray;

import com.orangomango.logicsim.ui.*;
import com.orangomango.logicsim.core.*;

public class MainApplication extends Application{
	private static final double WIDTH = 1000;
	private static final double HEIGHT = 800;
	private static final int FPS = 40;
	private static final double TOOLBAR_X = 650;
	private static final double TOOLBAR_Y = 75;

	private SideArea sideArea;
	private int selectedId = -1;
	private Point2D mouseMoved = new Point2D(0, 0);
	private List<Gate> gates = new ArrayList<>();
	private List<Wire> wires = new ArrayList<>();
	private Gate.Pin connG;
	private List<UiButton> buttons = new ArrayList<>();
	
	@Override
	public void start(Stage stage){
		stage.setTitle("LogicSim");
		StackPane pane = new StackPane();
		Canvas canvas = new Canvas(WIDTH, HEIGHT);
		GraphicsContext gc = canvas.getGraphicsContext2D();
		pane.getChildren().add(canvas);

		UiButton saveButton = new UiButton(gc, "SAVE", new Rectangle2D(50, 20, 100, 35), () -> {
			FileChooser fc = new FileChooser();
			fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("LogicSim files", "*.lsim"));
			File file = fc.showSaveDialog(stage);
			save(file);
		});
		UiButton loadButton = new UiButton(gc, "LOAD", new Rectangle2D(175, 20, 100, 35), () -> {
			FileChooser fc = new FileChooser();
			fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("LogicSim files", "*.lsim"));
			File file = fc.showOpenDialog(stage);
			load(file, gc);
		});
		this.buttons.add(saveButton);
		this.buttons.add(loadButton);
		
		this.sideArea = new SideArea(gc, new Rectangle2D(950, 250, 50, 75), new Rectangle2D(TOOLBAR_X, 0, 350, 800));
		this.sideArea.addButton("Switch", () -> this.selectedId = 0);
		this.sideArea.addButton("Wire", () -> this.selectedId = 1);
		this.sideArea.addButton("Light", () -> this.selectedId = 2);
		this.sideArea.addButton("NOT", () -> this.selectedId = 3);
		this.sideArea.addButton("AND", () -> this.selectedId = 4);

		canvas.setOnMousePressed(e -> {
			if (e.getButton() == MouseButton.PRIMARY){
				if (e.getY() < TOOLBAR_Y && e.getX() < TOOLBAR_X){
					for (UiButton ub : this.buttons){
						ub.onClick(e.getX(), e.getY());
					}
				} else {
					if (this.selectedId >= 0){
						Gate g = null;
						switch (this.selectedId){
							case 0:
								g = new Switch(gc, new Rectangle2D(e.getX(), e.getY(), 50, 50));
								break;
							case 1:
								Gate.Pin found = null;
								for (Gate gt : this.gates){
									Gate.Pin pin = gt.getPin(e.getX(), e.getY());
									if (pin != null){
										found = pin;
										break;
									}
								}
								if (found != null){
									if (this.connG == null){
										this.connG = found;
									} else {
										this.wires.add(new Wire(gc, this.connG, found));
										this.connG = null;
										this.selectedId = -1;
									}
								} else {
									this.selectedId = -1;
								}
								break;
							case 2:
								g = new Light(gc, new Rectangle2D(e.getX(), e.getY(), 50, 50));
								break;
							case 3:
								g = new NotGate(gc, new Rectangle2D(e.getX(), e.getY(), 50, 50));
								break;
							case 4:
								g = new AndGate(gc, new Rectangle2D(e.getX(), e.getY(), 50, 50));
								break;
						}
						if (g != null){
							this.gates.add(g);
							this.selectedId = -1;
						}
					} else {
						this.sideArea.onClick(e.getX(), e.getY());
						for (Gate g : this.gates){
							Gate.Pin pin = g.getPin(e.getX(), e.getY());
							if (pin == null){
								g.onClick(e.getX(), e.getY());
							} else {
								this.selectedId = 1;
								this.connG = pin;
							}
						}
					}
				}
			}
		});

		canvas.setOnMouseMoved(e -> {
			this.mouseMoved = new Point2D(e.getX(), e.getY());
		});

		Timeline loop = new Timeline(new KeyFrame(Duration.millis(1000.0/FPS), e -> update(gc)));
		loop.setCycleCount(Animation.INDEFINITE);
		loop.play();
		
		stage.setScene(new Scene(pane, WIDTH, HEIGHT));
		stage.show();
	}

	private void save(File file){
		if (!file.getName().endsWith(".lsim")){
			file = new File(file.getName()+".lsim");
		}
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			JSONObject json = new JSONObject();
			JSONArray data = new JSONArray();
			for (Gate gate : this.gates){
				data.put(gate.getJSON());
			}
			json.put("gates", data);
			data = new JSONArray();
			for (Wire wire : this.wires){
				data.put(wire.getJSON());
			}
			json.put("wires", data);
			writer.write(json.toString(4));
			writer.close();
		} catch (IOException ex){
			ex.printStackTrace();
		}
	}

	private void load(File file, GraphicsContext gc){
		List<Gate> tempGates = new ArrayList<>();
		List<Wire> tempWires = new ArrayList<>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			StringBuilder builder = new StringBuilder();
			reader.lines().forEach(builder::append);
			reader.close();
			JSONObject json = new JSONObject(builder.toString());

			// Load gates
			for (Object o : json.getJSONArray("gates")){
				JSONObject gate = (JSONObject)o;
				String name = gate.getString("name");
				Color color = Color.color(gate.getJSONObject("color").getDouble("red"), gate.getJSONObject("color").getDouble("green"), gate.getJSONObject("color").getDouble("blue"));
				Rectangle2D rect = new Rectangle2D(gate.getJSONObject("rect").getDouble("x"), gate.getJSONObject("rect").getDouble("y"), gate.getJSONObject("rect").getDouble("w"), gate.getJSONObject("rect").getDouble("h"));
				List<Gate.Pin> pins = new ArrayList<>();
				for (Object o2 : gate.getJSONArray("pins")){
					JSONObject pin = (JSONObject)o2;
					Gate.Pin p = new Gate.Pin(pin);
					pins.add(p);
				}
				Gate gt = null;
				if (name.equals("AND")){
					gt = new AndGate(gc, rect);
				} else if (name.equals("GATE")){
					gt = new Gate(gc, rect, color);
				} else if (name.equals("LIGHT")){
					gt = new Light(gc, rect);
				} else if (name.equals("NOT")){
					gt = new NotGate(gc, rect);
				} else if (name.equals("SWITCH")){
					gt = new Switch(gc, rect);
				}
				gt.setPins(pins);
				tempGates.add(gt);
			}

			// Load gates' pins
			for (Object o : json.getJSONArray("gates")){
				JSONObject gate = (JSONObject)o;
				for (Object o2 : gate.getJSONArray("pins")){
					JSONObject pin = (JSONObject)o2;
					int pinId = pin.getInt("id");
					Gate.Pin currentPin = getPinById(tempGates, pinId);
					for (Object o3 : pin.getJSONArray("attached")){
						Integer apinId = (Integer)o3;
						Gate.Pin apin = getPinById(tempGates, apinId);
						currentPin.attach(apin);
					}
				}
			}

			// Load wires
			for (Object o : json.getJSONArray("wires")){
				JSONObject wire = (JSONObject)o;
				Gate.Pin p1 = getPinById(tempGates, wire.getInt("pin1"));
				Gate.Pin p2 = getPinById(tempGates, wire.getInt("pin2"));
				tempWires.add(new Wire(gc, p1, p2));
			}

			this.gates = tempGates;
			this.wires = tempWires;
		} catch (IOException ex){
			ex.printStackTrace();
		}
	}

	private Gate.Pin getPinById(List<Gate> gates, int id){
		for (Gate g : gates){
			for (Gate.Pin p : g.getPins()){
				if (p.getId() == id){
					return p;
				}
			}
		}
		return null;
	}
	
	private void update(GraphicsContext gc){
		gc.clearRect(0, 0, WIDTH, HEIGHT);
		gc.setFill(Color.web("#EDEDED"));
		gc.fillRect(0, 0, WIDTH, HEIGHT);

		for (Gate g : this.gates){
			g.render();
		}
		for (Wire w : this.wires){
			w.render();
		}

		// UI
		for (UiButton ub : this.buttons){
			ub.render();
		}
		this.sideArea.render();

		if (this.selectedId >= 0 && this.selectedId != 1){
			gc.save();
			gc.setFill(Color.YELLOW);
			gc.setGlobalAlpha(0.6);
			gc.fillRect(mouseMoved.getX(), mouseMoved.getY(), 50, 50);
			gc.restore();
		}
	}
	
	public static void main(String[] args){
		launch(args);
	}
}
