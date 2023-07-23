package com.orangomango.logicsim;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.GridPane;
import javafx.scene.input.MouseButton;
import javafx.scene.canvas.*;
import javafx.scene.paint.Color;
import javafx.animation.*;
import javafx.util.Duration;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Point2D;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ColorPicker;

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
	private Gate selectedGate;
	private List<UiButton> buttons = new ArrayList<>();
	private File selectedChipFile;
	
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
			fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("LogicSim chips", "*.lsimc"));
			File file = fc.showOpenDialog(stage);
			List<Gate> gates = new ArrayList<>();
			List<Wire> wires = new ArrayList<>();
			Gate.Pin.PIN_ID = 0;
			load(file, gc, gates, wires);
			this.gates = gates;
			this.wires = wires;
		});
		UiButton saveChipButton = new UiButton(gc, "SAVE CHIP", new Rectangle2D(300, 20, 150, 35), () -> {
			Alert alert = new Alert(Alert.AlertType.INFORMATION);
			alert.setHeaderText("Create chip");
			alert.setTitle("Create chip");
			GridPane gpane = new GridPane();
			gpane.setPadding(new Insets(5, 5, 5, 5));
			gpane.setHgap(5);
			gpane.setVgap(5);
			Label nameL = new Label("Name: ");
			TextField name = new TextField();
			ColorPicker colorPicker = new ColorPicker(Color.BLUE);
			gpane.add(nameL, 0, 0);
			gpane.add(name, 1, 0);
			gpane.add(colorPicker, 0, 1, 2, 1);
			alert.getDialogPane().setContent(gpane);
			alert.showAndWait();
			FileChooser fc = new FileChooser();
			fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("LogicSim chips", "*.lsimc"));
			File file = fc.showSaveDialog(stage);
			save(file, name.getText(), colorPicker.getValue());
		});
		UiButton clearButton = new UiButton(gc, "CLEAR", new Rectangle2D(475, 20, 100, 35), () -> {
			this.wires = new ArrayList<Wire>();
			this.gates = new ArrayList<Gate>();
			Gate.Pin.PIN_ID = 0;
		});
		this.buttons.add(saveButton);
		this.buttons.add(loadButton);
		this.buttons.add(saveChipButton);
		this.buttons.add(clearButton);
		
		this.sideArea = new SideArea(gc, new Rectangle2D(950, 250, 50, 75), new Rectangle2D(TOOLBAR_X, 0, 350, 800));
		this.sideArea.addButton("Switch", () -> this.selectedId = 0);
		this.sideArea.addButton("Wire", () -> this.selectedId = 1);
		this.sideArea.addButton("Light", () -> this.selectedId = 2);
		this.sideArea.addButton("NOT", () -> this.selectedId = 3);
		this.sideArea.addButton("AND", () -> this.selectedId = 4);
		this.sideArea.addButton("CHIP", () -> this.selectedId = 5);

		this.sideArea.startSection();
		for (File file : (new File(System.getProperty("user.dir"))).listFiles()){
			String nm = file.getName();
			if (nm.endsWith(".lsimc")){
				this.sideArea.addButton(nm.substring(0, nm.lastIndexOf(".")), () -> {
					this.selectedId = 6;
					this.selectedChipFile = file;
				});
			}
		}

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
							case 5:
								FileChooser fc = new FileChooser();
								fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("LogicSim chips", "*.lsimc"));
								File file = fc.showOpenDialog(stage);
								g = new Chip(gc, new Rectangle2D(e.getX(), e.getY(), 125, 0), file);
								break;
							case 6:
								g = new Chip(gc, new Rectangle2D(e.getX(), e.getY(), 125, 0), this.selectedChipFile);
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
			} else if (e.getButton() == MouseButton.SECONDARY){
				Gate found = null;
				for (Gate g : this.gates){
					if (g.contains(e.getX(), e.getY())){
						found = g;
						break;
					}
				}
				this.selectedGate = found;
				if (found == null) this.selectedId = -1;
			}
		});

		canvas.setOnMouseMoved(e -> {
			this.mouseMoved = new Point2D(e.getX(), e.getY());
		});

		canvas.setOnMouseDragged(e -> {
			if (e.getButton() == MouseButton.SECONDARY){
				if (this.selectedGate != null){
					this.selectedGate.setPos(e.getX(), e.getY());
				} else {
					// TODO
				}
			}
		});

		Timeline loop = new Timeline(new KeyFrame(Duration.millis(1000.0/FPS), e -> update(gc)));
		loop.setCycleCount(Animation.INDEFINITE);
		loop.play();
		
		stage.setResizable(false);
		stage.setScene(new Scene(pane, WIDTH, HEIGHT));
		stage.show();
	}

	private void save(File file){
		save(file, null, null);
	}

	private void save(File file, String chipName, Color color){
		if (!file.getName().endsWith(".lsim") || !file.getName().endsWith(".lsimc")){
			file = new File(file.getName()+".lsim"+(chipName == null ? "" : "c"));
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
			if (chipName != null && color != null){
				json.put("chipName", chipName);
				JSONObject cl = new JSONObject();
				cl.put("red", color.getRed());
				cl.put("green", color.getGreen());
				cl.put("blue", color.getBlue());
				json.put("color", cl);
			}
			writer.write(json.toString(4));
			writer.close();
		} catch (IOException ex){
			ex.printStackTrace();
		}
	}

	public static JSONObject load(File file, GraphicsContext gc, List<Gate> tempGates, List<Wire> tempWires){
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
				int lastPinId = Gate.Pin.PIN_ID; // Save pin id
				if (name.equals("AND")){
					gt = new AndGate(gc, rect);
				} else if (name.equals("LIGHT")){
					gt = new Light(gc, rect);
				} else if (name.equals("NOT")){
					gt = new NotGate(gc, rect);
				} else if (name.equals("SWITCH")){
					gt = new Switch(gc, rect);
				} else if (name.equals("CHIP")){
					JSONObject chipData = gate.getJSONObject("chipData");
					try {
						File temp = File.createTempFile("temp", ".lsim");
						temp.deleteOnExit();
						BufferedWriter writer = new BufferedWriter(new FileWriter(temp));
						writer.write(chipData.toString(4));
						writer.close();
						gt = new Chip(gc, rect, temp);
					} catch (IOException ex){
						ex.printStackTrace();
					}
				}
				Gate.Pin.PIN_ID = lastPinId; // Restore the last pin id
				gt.setPins(pins);
				tempGates.add(gt);
			}

			// Attach gates' pins
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

			return json;
		} catch (IOException ex){
			ex.printStackTrace();
			return null;
		}
	}

	private static Gate.Pin getPinById(List<Gate> gates, int id){
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
			g.update();
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
