// File managed by WebFX (DO NOT EDIT MANUALLY)

module LogicSimulator.application.openjfx {

    // Direct dependencies modules
    requires LogicSimulator.application;
    requires webfx.extras.canvas.blob.openjfx;
    requires webfx.extras.filepicker.openjfx;
    requires webfx.extras.webtext.peers.openjfx;
    requires webfx.kit.openjfx;
    requires webfx.platform.boot.java;
    requires webfx.platform.console.java;
    requires webfx.platform.file.java;
    requires webfx.platform.json.java;
    requires webfx.platform.os.java;
    requires webfx.platform.resource.java;
    requires webfx.platform.scheduler.java;
    requires webfx.platform.shutdown.java;
    requires webfx.stack.ui.fxraiser.json;

    // Meta Resource package
    opens dev.webfx.platform.meta.exe;

}