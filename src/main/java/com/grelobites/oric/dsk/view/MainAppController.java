package com.grelobites.oric.dsk.view;

import com.grelobites.oric.dsk.Constants;
import com.grelobites.oric.dsk.ApplicationContext;
import com.grelobites.oric.dsk.model.Archive;
import com.grelobites.oric.dsk.model.ArchiveOperationException;
import com.grelobites.oric.dsk.model.ComplexDiskGeometry;
import com.grelobites.oric.dsk.model.DiskGeometry;
import com.grelobites.oric.dsk.sedoric.SedoricArchive;
import com.grelobites.oric.dsk.sedoric.SedoricFileSystem;
import com.grelobites.oric.dsk.util.ArchiveUtil;
import com.grelobites.oric.dsk.util.LocaleUtil;
import com.grelobites.oric.dsk.util.Util;
import com.grelobites.oric.dsk.view.util.DialogUtil;
import com.grelobites.oric.dsk.view.util.DirectoryAwareFileChooser;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class MainAppController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainAppController.class);
    private static final DataFormat SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");

    private ApplicationContext applicationContext;

    @FXML
    private Pane archiveInformationPane;

    @FXML
    private TableView<SedoricArchive> archiveTable;


    @FXML
    private TableColumn<Archive, String> archiveNameColumn;

    @FXML
    private TableColumn<Archive, Number> archiveSizeColumn;

    @FXML
    private Button createDskButton;

    @FXML
    private Button addArchiveButton;

    @FXML
    private Button removeSelectedArchiveButton;

    @FXML
    private Button purgeArchivesButton;

    @FXML
    private ProgressIndicator operationInProgressIndicator;

    @FXML
    private TextField archiveName;

    @FXML
    private TextField archiveExtension;

    @FXML
    private Label archiveSize;

    @FXML
    private CheckBox archiveProtectedAttribute;

    @FXML
    private CheckBox archiveExecutableAttribute;

    @FXML
    private CheckBox archiveBlockAttribute;

    @FXML
    private CheckBox bootableDisk;

    @FXML
    private TextField archiveLoadAddress;

    @FXML
    private TextField archiveExecAddress;

    @FXML
    private ProgressBar diskUsage;

    @FXML
    private Spinner<Integer> trackCount;

    @FXML
    private Spinner<Integer> sideCount;

    @FXML
    private Spinner<Integer> sectorCount;

    @FXML
    private TextField diskName;

    @FXML
    private TextField diskInitString;

    private ArchiveView archiveView;

    public MainAppController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    private SedoricFileSystem getFileSystem() {
        return applicationContext.getFileSystem();
    }

    private void addArchivesFromFiles(List<File> files) {
        try {
            for (File file : files) {
                ArchiveUtil.addArchivesInFile(applicationContext, file);
            }
        } catch (ArchiveOperationException aoe) {
            LOGGER.error("Adding archives", aoe);
            DialogUtil.buildErrorAlert(
                    LocaleUtil.i18n("archiveAddError"),
                    LocaleUtil.i18n("archiveAddErrorHeader"),
                    LocaleUtil.i18n(aoe.getMessageKey()))
                    .showAndWait();

        } catch (Exception e) {
            LOGGER.error("Adding archives", e);
            DialogUtil.buildErrorAlert(
                    LocaleUtil.i18n("archiveAddError"),
                    LocaleUtil.i18n("archiveAddErrorHeader"),
                    LocaleUtil.i18n("archiveAddGenericError"))
                    .showAndWait();
        }
    }

    @FXML
    private void initialize() throws IOException {
        applicationContext.setSelectedArchiveProperty(archiveTable.getSelectionModel().selectedItemProperty());

        purgeArchivesButton.disableProperty()
                .bind(Bindings.size(applicationContext.getArchiveList())
                        .isEqualTo(0));

        archiveTable.setItems(applicationContext.getArchiveList());
        archiveTable.setPlaceholder(new Label(LocaleUtil.i18n("dropArchivesMessage")));

        operationInProgressIndicator.visibleProperty().bind(
                applicationContext.backgroundTaskCountProperty().greaterThan(0));

        archiveView = new ArchiveView(applicationContext, archiveName, archiveExtension, archiveSize,
                archiveProtectedAttribute, archiveExecutableAttribute, archiveBlockAttribute,
                archiveLoadAddress, archiveExecAddress);

        onArchiveSelection(null, null);

        archiveTable.setRowFactory(rf -> {
            TableRow<SedoricArchive> row = new TableRow<>();
            row.setOnDragDetected(event -> {
                if (!row.isEmpty()) {
                    Integer index = row.getIndex();
                    LOGGER.debug("Dragging content of row " + index);
                    Dragboard db = row.startDragAndDrop(TransferMode.ANY);
                    db.setDragView(row.snapshot(null, null));
                    ClipboardContent cc = new ClipboardContent();
                    try {
                        cc.putFiles(Collections.singletonList(ArchiveUtil.toTemporaryFile(row.getItem())));
                        db.setContent(cc);
                        event.consume();
                    } catch (Exception e) {
                        LOGGER.error("In drag operation", e);
                    }
                }
            });

            row.setOnDragDone(event -> {
                event.consume();
            });

            row.setOnMouseClicked(e -> {
                if (row.isEmpty()) {
                    archiveTable.getSelectionModel().clearSelection();
                }
            });
            return row;
        });

        archiveNameColumn.setCellValueFactory(
                cellData -> cellData.getValue().nameProperty()
                        .concat(Util.spacePadding(
                                Constants.SEDORIC_FILENAME_MAXLENGTH -
                                        cellData.getValue().nameProperty().get().length()))
                        .concat(Constants.FILE_EXTENSION_SEPARATOR)
                        .concat(cellData.getValue().extensionProperty()));

        archiveSizeColumn.setCellValueFactory(
                cellData -> cellData.getValue().sizeProperty());

        archiveTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> onArchiveSelection(oldValue, newValue));


        archiveTable.setOnDragOver(event -> {
            if (event.getGestureSource() == null &&
                    event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        archiveTable.setOnDragEntered(event -> {
            if (event.getGestureSource() == null &&
                    event.getDragboard().hasFiles()) {
                archiveTable.getStyleClass().add(Constants.RED_BACKGROUND_STYLE);
            }
            event.consume();
        });

        archiveTable.setOnDragExited(event -> {
            archiveTable.getStyleClass().remove(Constants.RED_BACKGROUND_STYLE);
            event.consume();
        });

        archiveTable.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            LOGGER.debug("onDragDropped. Transfer modes are " + db.getTransferModes());
            boolean success = false;
            if (db.hasFiles()) {
                addArchivesFromFiles(db.getFiles());
                success = true;
            }
            /* let the source know whether the files were successfully
             * transferred and used */
            event.setDropCompleted(success);
            event.consume();
        });

        createDskButton.setOnAction(c -> {
            DirectoryAwareFileChooser chooser = applicationContext.getFileChooser();
            chooser.setTitle(LocaleUtil.i18n("saveDsk"));
            chooser.setInitialFileName("oric_" + Constants.currentVersion() + ".dsk");
            final File saveFile = chooser.showSaveDialog(createDskButton.getScene().getWindow());
            if (saveFile != null) {
                try (FileOutputStream fos = new FileOutputStream(saveFile)) {
                    applicationContext.saveDsk(fos);
                } catch (IOException e) {
                    LOGGER.error("Creating Dsk", e);
                }
            }
        });

        addArchiveButton.setOnAction(c -> {
            DirectoryAwareFileChooser chooser = applicationContext.getFileChooser();
            chooser.setTitle(LocaleUtil.i18n("addArchiveDialog"));
            final List<File> sourceFiles = chooser.showOpenMultipleDialog(addArchiveButton.getScene().getWindow());
            if (sourceFiles != null) {
                try {
                    addArchivesFromFiles(sourceFiles);
                } catch (Exception e) {
                    LOGGER.error("Opening files " + sourceFiles, e);
                }
            }
        });

        removeSelectedArchiveButton.setOnAction(c -> {
            Optional<SedoricArchive> selectedInstallable = Optional.of(archiveTable.getSelectionModel().getSelectedItem());
            selectedInstallable.ifPresent(index -> getFileSystem()
                    .removeArchive(selectedInstallable.get()));
        });

        purgeArchivesButton.setOnAction(c -> {
            Optional<ButtonType> result = DialogUtil
                    .buildAlert(LocaleUtil.i18n("archiveDeletionConfirmTitle"),
                            LocaleUtil.i18n("archiveDeletionConfirmHeader"),
                            LocaleUtil.i18n("archiveDeletionConfirmContent"))
                    .showAndWait();

            if (result.orElse(ButtonType.CANCEL) == ButtonType.OK) {
                applicationContext.clear();
            }
        });

        diskUsage.progressProperty().bind(applicationContext.diskUsageProperty());
        Tooltip diskUsageDetail = new Tooltip();
        diskUsage.setTooltip(diskUsageDetail);
        diskUsageDetail.textProperty().bind(applicationContext.diskUsageDetailProperty());
        diskUsage.progressProperty().addListener(
                (observable, oldValue, newValue) -> {
                    LOGGER.debug("Changing bar style on disk usage change to " + newValue.doubleValue());
                    diskUsage.getStyleClass().removeAll(Constants.BLUE_BAR_STYLE, Constants.RED_BAR_STYLE);
                    diskUsage.getStyleClass().add(
                            (newValue.doubleValue() > 1.0 ?
                                    Constants.RED_BAR_STYLE : Constants.BLUE_BAR_STYLE));

                });

        trackCount.setValueFactory(new SpinnerValueFactory
                .IntegerSpinnerValueFactory(1, 82, applicationContext.getDiskGeometry()
                .getTrackCount()));
        sideCount.setValueFactory(new SpinnerValueFactory
                .IntegerSpinnerValueFactory(1, 2, applicationContext.getDiskGeometry()
                .getSideCount()));
        sectorCount.setValueFactory(new SpinnerValueFactory
                .IntegerSpinnerValueFactory(15, 19, applicationContext.getDiskGeometry()
                .getSectorCount()));

        bindToContextDiskGeometry();


        applicationContext.diskGeometryProperty().addListener((observable, oldValue, newValue) -> {
            oldValue.sideCountProperty().unbind();
            oldValue.trackCountProperty().unbind();
            bindToContextDiskGeometry();
        });

        applicationContext.getFileSystem().nameProperty()
                .bindBidirectional(diskName.textProperty());
        applicationContext.getFileSystem().initStringProperty()
                .bindBidirectional(diskInitString.textProperty());
        applicationContext.getFileSystem().bootableProperty()
                .bindBidirectional(bootableDisk.selectedProperty());

        createDskButton.disableProperty().bind(applicationContext.generationAllowedProperty().not());
    }

    private void bindToContextDiskGeometry() {
        DiskGeometry geometry = applicationContext.getDiskGeometry();
        LOGGER.debug("bindToContextDiskGeometry with geometry " + geometry);
        trackCount.getValueFactory().setValue(geometry.getTrackCount());
        geometry.trackCountProperty().bind(trackCount.valueProperty());
        sideCount.getValueFactory().setValue(geometry.getSideCount());
        geometry.sideCountProperty().bind(sideCount.valueProperty());
        if (geometry.hasSectorCount()) {
            sectorCount.getValueFactory().setValue(geometry.getSectorCount());
            geometry.sectorCountProperty().bind(sectorCount.valueProperty());
        }
        //If the Disk Geometry is complex we cannot change the number of sides/tracks
        //without compromising the stability of the format
        boolean disable = applicationContext.getDiskGeometry() instanceof ComplexDiskGeometry;
        trackCount.setDisable(disable);
        sideCount.setDisable(disable);
        sectorCount.setDisable(disable);
    }

    private void onArchiveSelection(SedoricArchive oldArchive, SedoricArchive newArchive) {
        LOGGER.debug("onArchiveSelection oldArchive=" + oldArchive + ", newArchive=" + newArchive);
        archiveView.bindToArchive(newArchive);
        archiveInformationPane.setDisable(newArchive == null);
        if (newArchive == null) {
            removeSelectedArchiveButton.setDisable(true);
        } else {
            removeSelectedArchiveButton.setDisable(false);
        }
    }

}
