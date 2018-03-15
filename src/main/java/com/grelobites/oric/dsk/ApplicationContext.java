package com.grelobites.oric.dsk;

import com.grelobites.oric.dsk.model.Archive;
import com.grelobites.oric.dsk.model.DiskGeometry;
import com.grelobites.oric.dsk.model.SimpleDiskGeometry;
import com.grelobites.oric.dsk.sedoric.SedoricArchive;
import com.grelobites.oric.dsk.sedoric.SedoricFileSystem;
import com.grelobites.oric.dsk.util.ArchiveUtil;
import com.grelobites.oric.dsk.util.LocaleUtil;
import com.grelobites.oric.dsk.util.OperationResult;
import com.grelobites.oric.dsk.view.util.DialogUtil;
import com.grelobites.oric.dsk.view.util.DirectoryAwareFileChooser;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.NumberBinding;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.grelobites.oric.dsk.Constants.DEFAULT_DISK_GEOMETRY;

public class ApplicationContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationContext.class);
    private Stage applicationStage;
    private final ObservableList<SedoricArchive> archiveList;
    private ReadOnlyObjectProperty<SedoricArchive> selectedArchive;
    private BooleanProperty archiveSelected;
    private StringProperty diskUsageDetail;
    private DoubleProperty diskUsage;
    private BooleanProperty generationAllowed;
    private IntegerProperty backgroundTaskCount;
    private DirectoryAwareFileChooser fileChooser;
    private SedoricFileSystem fileSystem;
    private ObjectProperty<DiskGeometry> diskGeometry;

    private final ExecutorService executorService = Executors.newFixedThreadPool(5, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("Task executor service");
        return t;
    });

    public ApplicationContext() {
        this.archiveList = FXCollections.observableArrayList(Archive::getObservables);
        this.archiveSelected = new SimpleBooleanProperty(false);
        this.diskUsage = new SimpleDoubleProperty(0);
        this.diskUsageDetail = new SimpleStringProperty();
        this.backgroundTaskCount = new SimpleIntegerProperty();
        this.diskGeometry = new SimpleObjectProperty<>(DEFAULT_DISK_GEOMETRY);
        this.fileSystem = new SedoricFileSystem(this);
        this.generationAllowed = new SimpleBooleanProperty(true);
        this.generationAllowed.bind(backgroundTaskCount.isEqualTo(0));
    }

    public boolean isGenerationAllowed() {
        return generationAllowed.get();
    }

    public BooleanProperty generationAllowedProperty() {
        return generationAllowed;
    }

    public void setGenerationAllowed(boolean generationAllowed) {
        this.generationAllowed.set(generationAllowed);
    }

    public ObservableList<SedoricArchive> getArchiveList() {
        return archiveList;
    }

    public boolean getArchiveSelected() {
        return archiveSelected.get();
    }

    public void setArchiveSelected(boolean archiveSelected) {
        this.archiveSelected.set(archiveSelected);
    }

    public SedoricFileSystem getFileSystem() {
        return fileSystem;
    }

    public BooleanProperty archiveSelectedProperty() {
        return archiveSelected;
    }

    public DirectoryAwareFileChooser getFileChooser() {
        if (this.fileChooser == null) {
            this.fileChooser = new DirectoryAwareFileChooser();
        }
        fileChooser.setInitialFileName(null);
        return fileChooser;
    }

    public void setDiskUsage(double diskUsage) {
        this.diskUsage.set(diskUsage);
    }

    public double getDiskUsage() {
        return diskUsage.get();
    }

    public DoubleProperty diskUsageProperty() {
        return diskUsage;
    }

    public void setDiskUsageDetail(String diskUsageDetail) {
        this.diskUsageDetail.set(diskUsageDetail);
    }

    public String getDiskUsageDetail() {
        return diskUsageDetail.get();
    }

    public StringProperty diskUsageDetailProperty() {
        return diskUsageDetail;
    }

    public IntegerProperty backgroundTaskCountProperty() {
        return backgroundTaskCount;
    }

    public Future<OperationResult> addBackgroundTask(Callable<OperationResult> task) {
        Platform.runLater(() -> backgroundTaskCount.set(backgroundTaskCount.get() + 1));
        return executorService.submit(new BackgroundTask(task, backgroundTaskCount));
    }

    public ReadOnlyObjectProperty<SedoricArchive> selectedArchiveProperty() {
        return selectedArchive;
    }

    public void setSelectedArchiveProperty(ReadOnlyObjectProperty<SedoricArchive> selectedInstallableProperty) {
        selectedArchive = selectedInstallableProperty;
        archiveSelected.bind(selectedArchive.isNotNull());
    }

    public Stage getApplicationStage() {
        return applicationStage;
    }

    public void setApplicationStage(Stage applicationStage) {
        this.applicationStage = applicationStage;
    }

    public void exportCurrentArchive() {
        SedoricArchive archive = selectedArchive.get();
        if (archive != null) {
            DirectoryAwareFileChooser chooser = getFileChooser();
            chooser.setTitle(LocaleUtil.i18n("exportCurrentArchive"));
            chooser.setInitialFileName(String.format("%s.%s", archive.getName().trim(), archive.getExtension().trim()));
            final File saveFile = chooser.showSaveDialog(applicationStage.getScene().getWindow());
            if (saveFile != null) {
                try {
                    ArchiveUtil.exportAsFile(archive, saveFile);
                } catch (IOException e) {
                    LOGGER.error("Exporting current archive", e);
                }
            }
        } else {
            DialogUtil.buildWarningAlert(LocaleUtil.i18n("exportCurrentArchiveErrorTitle"),
                    LocaleUtil.i18n("exportCurrentArchiveErrorHeader"),
                    LocaleUtil.i18n("exportCurrentArchiveNoArchiveSelected")).showAndWait();
        }
    }

    private static boolean confirmArchiveDeletion() {
        Optional<ButtonType> result = DialogUtil
                .buildAlert(LocaleUtil.i18n("archiveDeletionConfirmTitle"),
                        LocaleUtil.i18n("archiveDeletionConfirmHeader"),
                        LocaleUtil.i18n("archiveDeletionConfirmContent"))
                .showAndWait();
        return result.orElse(ButtonType.CLOSE) == ButtonType.OK;
    }

    public void mergeFromFile(File file) throws IOException {
        ArchiveUtil.addArchivesInFile(this, file);
    }

    public void openDsk(File dskFile) throws IOException {
        if (archiveList.size() == 0 || confirmArchiveDeletion()) {
            getArchiveList().clear();
            try (InputStream is = new FileInputStream(dskFile)) {
                fileSystem.openDsk(is);
            }
        }
    }

    public void saveDsk(OutputStream stream) throws IOException {
        getFileSystem().exportFileSystem(stream);
    }

    public DiskGeometry getDiskGeometry() {
        return diskGeometry.get();
    }

    public ObjectProperty<DiskGeometry> diskGeometryProperty() {
        return diskGeometry;
    }

    public void setDiskGeometry(DiskGeometry diskGeometry) {
        this.diskGeometry.set(diskGeometry);
    }

    class BackgroundTask implements Callable<OperationResult> {
        private IntegerProperty backgroundTaskCount;
        private Callable<OperationResult> task;

        public BackgroundTask(Callable<OperationResult> task, IntegerProperty backgroundTaskCount) {
            this.backgroundTaskCount = backgroundTaskCount;
            this.task = task;
        }

        @Override
        public OperationResult call() throws Exception {
            final OperationResult result = task.call();
            Platform.runLater(() -> {
                backgroundTaskCount.set(backgroundTaskCount.get() - 1);
                if (result.isError()) {
                    DialogUtil.buildErrorAlert(result.getContext(),
                            result.getMessage(),
                            result.getDetail())
                            .showAndWait();
                }
            });
            return result;
        }
    }
}


