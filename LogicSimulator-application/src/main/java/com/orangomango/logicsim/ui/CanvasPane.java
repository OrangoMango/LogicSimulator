package com.orangomango.logicsim.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Pane;

/**
 * @author Bruno Salmon
 */
public class CanvasPane extends Pane{
    public static interface CanvasRefresher{
        public void refreshCanvas(double canvasWidth, double canvasHeight);
    }

    protected final Canvas canvas;
    private final CanvasRefresher canvasRefresher;

    public CanvasPane(Canvas canvas, CanvasRefresher canvasRefresher){
        super(canvas);
        this.canvas = canvas;
        this.canvasRefresher = canvasRefresher;
    }
    
    @Override
    protected void layoutChildren() {
        resizeStandardCanvas();
    }

    private void resizeStandardCanvas() {
        double newCanvasWidth = getWidth();
        double newCanvasHeight = getHeight();
        boolean canvasWidthChanged = newCanvasWidth != this.canvas.getWidth();
        boolean canvasHeightChanged = newCanvasHeight != this.canvas.getHeight();
        if ((canvasWidthChanged || canvasHeightChanged) && newCanvasWidth > 0 && newCanvasHeight > 0){
            this.canvas.setWidth(newCanvasWidth);
            this.canvas.setHeight(newCanvasHeight);
            callCanvasRefresher(newCanvasWidth, newCanvasHeight);
        }
    }

    protected void callCanvasRefresher(double canvasWidth, double canvasHeight){
        this.canvasRefresher.refreshCanvas(canvasWidth, canvasHeight);
    }
}