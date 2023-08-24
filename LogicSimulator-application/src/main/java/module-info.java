// File managed by WebFX (DO NOT EDIT MANUALLY)

module LogicSimulator.application {

    // Direct dependencies modules
    requires java.base;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires webfx.platform.file;
    requires webfx.platform.json;

    // Exported packages
    exports com.orangomango.logicsim;
    exports com.orangomango.logicsim.core;
    exports com.orangomango.logicsim.ui;

    // Provided services
    provides javafx.application.Application with com.orangomango.logicsim.MainApplication;

}