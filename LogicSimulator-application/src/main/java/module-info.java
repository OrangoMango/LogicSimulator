// File managed by WebFX (DO NOT EDIT MANUALLY)

module LogicSimulator.application {

    // Direct dependencies modules
    requires java.base;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires webfx.extras.canvas.blob;
    requires webfx.extras.filepicker;
    requires webfx.platform.console;
    requires webfx.platform.file;
    requires webfx.platform.json;
    requires webfx.platform.resource;
    requires webfx.platform.scheduler;
    requires webfx.stack.ui.controls;

    // Exported packages
    exports com.orangomango.logicsim;
    exports com.orangomango.logicsim.core;
    exports com.orangomango.logicsim.ui;

    // Resources packages
    opens images;

    // Provided services
    provides javafx.application.Application with com.orangomango.logicsim.MainApplication;

}