package com.grelobites.oric.dsk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

import com.grelobites.oric.dsk.util.LocaleUtil;
import com.grelobites.oric.dsk.view.MainAppController;
import com.grelobites.oric.dsk.view.util.DialogUtil;
import com.grelobites.oric.dsk.view.util.DirectoryAwareFileChooser;
import de.codecentric.centerdevice.MenuToolkit;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainApp extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainApp.class);
    private static final String APP_NAME = "ORIC DSK Manager";

	private Stage primaryStage;
    private Stage aboutStage;
    private AnchorPane aboutPane;
    private MenuToolkit menuToolkit;
    private ApplicationContext applicationContext;

    private void populateMenuBar(MenuBar menuBar, Scene scene, ApplicationContext applicationContext) {
        Menu fileMenu = new Menu(LocaleUtil.i18n("fileMenuTitle"));

        fileMenu.getItems().addAll(
                newDskMenuItem(scene, applicationContext),
                openDskMenuItem(scene, applicationContext),
                mergeDskMenuItem(scene, applicationContext),
                saveDskMenuItem(scene, applicationContext),
                exportCurrentArchiveMenuItem(applicationContext),
                exportCurrentArchiveAsBinaryMenuItem(applicationContext),
                exportCurrentArchiveAsTapMenuItem(applicationContext));

        if (menuToolkit == null) {
            fileMenu.getItems().add(new SeparatorMenuItem());
            fileMenu.getItems().add(quitMenuItem());
        }

        menuBar.getMenus().add(fileMenu);

        if (menuToolkit == null) {
            Menu helpMenu = new Menu(LocaleUtil.i18n("helpMenuTitle"));
            helpMenu.getItems().add(aboutMenuItem());
            menuBar.getMenus().add(helpMenu);
        }
    }

    private MenuItem newDskMenuItem(Scene scene, ApplicationContext context) {
        MenuItem newDsk = new MenuItem((LocaleUtil.i18n("newDskMenuEntry")));
        newDsk.setAccelerator(
                KeyCombination.keyCombination("SHORTCUT+N")
        );

        newDsk.setOnAction(f -> {
            if (applicationContext.getArchiveList().size() > 0) {
                Optional<ButtonType> result = DialogUtil
                        .buildAlert(LocaleUtil.i18n("archiveDeletionConfirmTitle"),
                                LocaleUtil.i18n("archiveDeletionConfirmHeader"),
                                LocaleUtil.i18n("archiveDeletionConfirmContent"))
                        .showAndWait();

                if (result.orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    applicationContext.clear();
                }
            } else {
                applicationContext.clear();
            }
        });
        return newDsk;
    }

    private MenuItem exportCurrentArchiveMenuItem(ApplicationContext applicationContext) {
        MenuItem exportArchive = new MenuItem(LocaleUtil.i18n("exportArchiveMenuEntry"));
        exportArchive.setAccelerator(
                KeyCombination.keyCombination("SHORTCUT+E")
        );
        exportArchive.disableProperty().bind(applicationContext
                .archiveSelectedProperty().not());

        exportArchive.setOnAction(f -> {
            try {
                applicationContext.exportCurrentArchive();
            } catch (Exception e) {
                LOGGER.error("Exporting current installable", e);
                DialogUtil.buildErrorAlert(
                        LocaleUtil.i18n("archiveOperationError"),
                        LocaleUtil.i18n("archiveOperationErrorHeader"),
                        LocaleUtil.i18n("archiveOperationGenericError"))
                        .showAndWait();
            }
        });
        return exportArchive;
    }

    private MenuItem exportCurrentArchiveAsBinaryMenuItem(ApplicationContext applicationContext) {
        MenuItem exportArchive = new MenuItem(LocaleUtil.i18n("exportArchiveAsBinaryMenuEntry"));
        exportArchive.setAccelerator(
                KeyCombination.keyCombination("SHORTCUT+B")
        );
        exportArchive.disableProperty().bind(applicationContext
                .archiveSelectedProperty().not());

        exportArchive.setOnAction(f -> {
            try {
                applicationContext.exportCurrentArchiveAsBinary();
            } catch (Exception e) {
                LOGGER.error("Exporting current installable", e);
                DialogUtil.buildErrorAlert(
                        LocaleUtil.i18n("archiveOperationError"),
                        LocaleUtil.i18n("archiveOperationErrorHeader"),
                        LocaleUtil.i18n("archiveOperationGenericError"))
                        .showAndWait();
            }
        });
        return exportArchive;
    }

    private MenuItem exportCurrentArchiveAsTapMenuItem(ApplicationContext applicationContext) {
        MenuItem exportArchive = new MenuItem(LocaleUtil.i18n("exportArchiveAsTapMenuEntry"));
        exportArchive.setAccelerator(
                KeyCombination.keyCombination("SHORTCUT+T")
        );
        exportArchive.disableProperty().bind(applicationContext
                .archiveSelectedProperty().not());

        exportArchive.setOnAction(f -> {
            try {
                applicationContext.exportCurrentArchiveAsTap();
            } catch (Exception e) {
                LOGGER.error("Exporting current installable", e);
                DialogUtil.buildErrorAlert(
                        LocaleUtil.i18n("archiveOperationError"),
                        LocaleUtil.i18n("archiveOperationErrorHeader"),
                        LocaleUtil.i18n("archiveOperationGenericError"))
                        .showAndWait();
            }
        });
        return exportArchive;
    }


    private MenuItem openDskMenuItem(Scene scene, ApplicationContext applicationContext) {
        MenuItem openDsk = new MenuItem(LocaleUtil.i18n("openDskMenuEntry"));
        openDsk.setAccelerator(
                KeyCombination.keyCombination("SHORTCUT+O")
        );
        openDsk.disableProperty().bind(applicationContext
                .backgroundTaskCountProperty().greaterThan(0));
        openDsk.setOnAction(f -> {
            DirectoryAwareFileChooser chooser = applicationContext.getFileChooser();
            chooser.setTitle(LocaleUtil.i18n("openDskChooser"));
            final File fsFile = chooser.showOpenDialog(scene.getWindow());
            try {
                if (fsFile != null) {
                    applicationContext.openDsk(fsFile);
                }
            } catch (Exception e) {
                LOGGER.error("Reading Dsk from file " +  fsFile, e);
                DialogUtil.buildErrorAlert(
                        LocaleUtil.i18n("archiveOperationError"),
                        LocaleUtil.i18n("archiveOperationErrorHeader"),
                        LocaleUtil.i18n("archiveOperationGenericError"))
                        .showAndWait();
            }
        });
        return openDsk;
    }

    private MenuItem mergeDskMenuItem(Scene scene, ApplicationContext applicationContext) {
        MenuItem mergeDsk = new MenuItem(LocaleUtil.i18n("mergeFileMenuEntry"));
        mergeDsk.setAccelerator(
                KeyCombination.keyCombination("SHORTCUT+M")
        );
        mergeDsk.disableProperty().bind(applicationContext
                .backgroundTaskCountProperty().greaterThan(0));
        mergeDsk.setOnAction(f -> {
            DirectoryAwareFileChooser chooser = applicationContext.getFileChooser();
            chooser.setTitle(LocaleUtil.i18n("mergeFileChooser"));
            final File fsFile = chooser.showOpenDialog(scene.getWindow());
            try {
                if (fsFile != null) {
                    applicationContext.mergeFromFile(fsFile);
                }
            } catch (Exception e) {
                LOGGER.error("Merging from file " +  fsFile, e);
                DialogUtil.buildErrorAlert(
                        LocaleUtil.i18n("archiveOperationError"),
                        LocaleUtil.i18n("archiveOperationErrorHeader"),
                        LocaleUtil.i18n("archiveOperationGenericError"))
                        .showAndWait();
            }
        });
        return mergeDsk;
    }

    private MenuItem saveDskMenuItem(Scene scene, ApplicationContext applicationContext) {
        MenuItem saveDsk = new MenuItem(LocaleUtil.i18n("saveDskMenuEntry"));
        saveDsk.setAccelerator(
                KeyCombination.keyCombination("SHORTCUT+S")
        );
        saveDsk.disableProperty().bind(applicationContext
                .generationAllowedProperty().not());
        saveDsk.setOnAction(f -> {
            DirectoryAwareFileChooser chooser = applicationContext.getFileChooser();
            chooser.setTitle(LocaleUtil.i18n("saveDskFileChooser"));
            final File fsFile = chooser.showSaveDialog(scene.getWindow());
            try {
                if (fsFile != null) {
                    try (FileOutputStream fos = new FileOutputStream(fsFile)) {
                        applicationContext.saveDsk(fos);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Saving Dsk to " +  fsFile, e);
                DialogUtil.buildErrorAlert(
                        LocaleUtil.i18n("archiveOperationError"),
                        LocaleUtil.i18n("archiveOperationErrorHeader"),
                        LocaleUtil.i18n("archiveOperationGenericError"))
                        .showAndWait();
            }
        });
        return saveDsk;
    }

    public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		this.primaryStage = primaryStage;
		this.primaryStage.getIcons()
			.add(new Image(MainApp.class.getResourceAsStream("/app-icon.png")));

		initRootLayout();

	}

    private AnchorPane getAboutPane() throws IOException {
        if (aboutPane == null) {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/about.fxml"));
            loader.setResources(LocaleUtil.getBundle());
            aboutPane = loader.load();
        }
        return aboutPane;
    }

    private Stage getAboutStage() throws IOException {
        if (aboutStage == null) {
            aboutStage = new Stage();
            Scene aboutScene = new Scene(getAboutPane());
            aboutScene.getStylesheets().add(Constants.getThemeResourceUrl());
            aboutStage.setScene(aboutScene);
            aboutStage.setTitle(LocaleUtil.i18n("aboutTitle"));
            aboutStage.initModality(Modality.APPLICATION_MODAL);
            aboutStage.initOwner(primaryStage.getOwner());
            aboutStage.setResizable(false);
        }
        return aboutStage;
    }

    private void showAboutStage() {
        try {
            getAboutStage().show();
        } catch (Exception e) {
            LOGGER.error("Trying to show about stage", e);
        }
    }

    private MenuItem aboutMenuItem() {
        MenuItem aboutMenuItem = new MenuItem(LocaleUtil.i18n("aboutMenuEntry"));
        aboutMenuItem.setOnAction(event -> showAboutStage());
        return aboutMenuItem;
    }

    public static MenuItem quitMenuItem() {
        MenuItem menuItem = new MenuItem(LocaleUtil.i18n("quitMenuEntry"));
        menuItem.setAccelerator(
                KeyCombination.keyCombination("SHORTCUT+Q"));

        menuItem.setOnAction(e -> Platform.exit());
        return menuItem;
    }

    public Menu createApplicationMenu(String appName) throws IOException {
        if (menuToolkit != null) {
            return new Menu(appName, null,
                    menuToolkit.createAboutMenuItem(appName, getAboutStage()),
                    new SeparatorMenuItem(),
                    new SeparatorMenuItem(),
                    menuToolkit.createHideMenuItem(appName),
                    menuToolkit.createHideOthersMenuItem(),
                    menuToolkit.createUnhideAllMenuItem(),
                    new SeparatorMenuItem(),
                    menuToolkit.createQuitMenuItem(appName));
        } else {
            return null;
        }
    }

    private MenuBar initMenuBar() throws IOException {
        MenuBar menuBar = new MenuBar();
        if (menuToolkit != null) {
            Menu applicationMenu = createApplicationMenu(APP_NAME);
            menuBar.getMenus().add(applicationMenu);
        }
        return menuBar;
    }

    private Pane getApplicationPane() throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(MainApp.class.getResource("view/mainapp.fxml"));
        loader.setResources(LocaleUtil.getBundle());

        loader.setController(new MainAppController(applicationContext));
        return loader.load();
    }

	private void initRootLayout() {
		try {
		    applicationContext = new ApplicationContext();
		    primaryStage.setTitle(APP_NAME);
            primaryStage.setOnCloseRequest(e -> Platform.exit());
            BorderPane mainPane = new BorderPane();
            Scene scene = new Scene(mainPane);
            menuToolkit = MenuToolkit.toolkit(Locale.getDefault());
            MenuBar menuBar = initMenuBar();
            if (menuToolkit == null) {
                mainPane.setTop(menuBar);
            } else {
                menuBar.setUseSystemMenuBar(true);
                menuToolkit.setGlobalMenuBar(menuBar);
            }
            populateMenuBar(menuBar, scene, applicationContext);
            mainPane.setCenter(getApplicationPane());
            mainPane.getStylesheets().add(Constants.getThemeResourceUrl());

            primaryStage.setScene(scene);
            applicationContext.setApplicationStage(primaryStage);

            if (menuToolkit != null) {
                menuToolkit.setMenuBar(primaryStage, menuBar);
            }
            primaryStage.setResizable(false);
            primaryStage.show();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
}
