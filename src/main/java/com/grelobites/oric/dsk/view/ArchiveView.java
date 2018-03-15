package com.grelobites.oric.dsk.view;

import com.grelobites.oric.dsk.Constants;
import com.grelobites.oric.dsk.ApplicationContext;
import com.grelobites.oric.dsk.sedoric.SedoricArchive;
import com.grelobites.oric.dsk.util.ArchiveFlags;
import com.grelobites.oric.dsk.util.ArchiveUtil;
import com.grelobites.oric.dsk.util.Util;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.UnaryOperator;

public class ArchiveView {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveView.class);
    private ApplicationContext applicationContext;
    private TextField name;
    private TextField extension;
    private Label size;
    private CheckBox protectedAttribute;
    private CheckBox executableAttribute;
    private CheckBox blockAttribute;
    private TextField loadAddress;
    private TextField execAddress;

    private SedoricArchive currentArchive;

    private ChangeListener<Boolean> protectedAttributeChangeListener = (observable, oldValue, newValue) ->
            updateAttribute(ArchiveFlags.PROTECTED, newValue);
    private ChangeListener<Boolean> executableAttributeChangeListener = (observable, oldValue, newValue) ->
            updateAttribute(ArchiveFlags.EXECUTABLE, newValue);
    private ChangeListener<Boolean> blockAttributeChangeListener = (observable, oldValue, newValue) ->
            updateAttribute(ArchiveFlags.BLOCK, newValue);

    private TextFormatter<String> getRestrictedLengthTextFormatter(final int maxLength) {
        return new TextFormatter<>((UnaryOperator<TextFormatter.Change>)  c -> {
            if (c.isContentChange()) {
                LOGGER.debug("Change was " + c);
                String filteredName = ArchiveUtil.toSedoricValidName(c.getControlNewText(),
                        maxLength);
                int oldLength = c.getControlText().length();
                int newLength = c.getControlNewText().length();
                int correctedLength = filteredName.length();
                c.setText(filteredName);
                c.setRange(0, oldLength);
                c.setCaretPosition(Math.max(0, c.getCaretPosition() - (newLength - correctedLength)));

                c.setAnchor(Math.max(0, c.getAnchor() - (newLength - correctedLength)));
                LOGGER.debug("Change updated to " + c);
            }
            return c;
        });
    }

    private boolean isNameAlreadyInUse(long id, String name, String extension, int userArea) {
        return applicationContext.getArchiveList().filtered(a ->
                a.getId() != id &&
                a.getName().equals(name) &&
                a.getExtension().equals(extension)).size() > 0;
    }

    private ChangeListener<String> nameChangeListener = (observable, oldValue, newValue) -> {
        if (currentArchive != null) {
            if (isNameAlreadyInUse(currentArchive.getId(), newValue, currentArchive.getExtension(),
                        0)) {
                name.getStyleClass().add(Constants.TEXT_ERROR_STYLE);
            } else {
                name.getStyleClass().removeAll(Constants.TEXT_ERROR_STYLE);
                currentArchive.setName(newValue);
            }
        }
    };

    private ChangeListener<String> loadAddressChangeListener = (observable, oldValue, newValue) -> {
        if (currentArchive != null) {
            Optional<Integer> addressValue = Util.decodeAddress(newValue);
            if (!addressValue.isPresent()) {
                loadAddress.getStyleClass().add(Constants.TEXT_ERROR_STYLE);
            } else {
                loadAddress.getStyleClass().removeAll(Constants.TEXT_ERROR_STYLE);
                currentArchive.setLoadAddress(addressValue.get());
            }
        }
    };

    private ChangeListener<String> execAddressChangeListener = (observable, oldValue, newValue) -> {
        if (currentArchive != null) {
            Optional<Integer> addressValue = Util.decodeAddress(newValue);
            if (!addressValue.isPresent()) {
                execAddress.getStyleClass().add(Constants.TEXT_ERROR_STYLE);
            } else {
                execAddress.getStyleClass().removeAll(Constants.TEXT_ERROR_STYLE);
                currentArchive.setExecAddress(addressValue.get());
            }
        }
    };

    private ChangeListener<String> extensionChangeListener = (observable, oldValue, newValue) -> {
        if (currentArchive != null) {
            if (isNameAlreadyInUse(currentArchive.getId(), currentArchive.getName(),
                    newValue, 0)) {
                extension.getStyleClass().add(Constants.TEXT_ERROR_STYLE);
            } else {
                extension.getStyleClass().removeAll(Constants.TEXT_ERROR_STYLE);
                currentArchive.setExtension(newValue);
            }
        }
    };

    public ArchiveView(ApplicationContext applicationContext,
                       TextField name, TextField extension, Label size,
                       CheckBox protectedAttribute,
                       CheckBox executableAttribute, CheckBox blockAttribute,
                       TextField execAddress, TextField loadAddress) {
        this.applicationContext = applicationContext;
        this.name = name;
        this.extension = extension;
        this.size = size;
        this.protectedAttribute = protectedAttribute;
        this.executableAttribute = executableAttribute;
        this.blockAttribute = blockAttribute;
        this.execAddress = execAddress;
        this.loadAddress = loadAddress;

        this.name.textProperty().addListener(nameChangeListener);
        this.extension.textProperty().addListener(extensionChangeListener);
        this.protectedAttribute.selectedProperty().addListener(protectedAttributeChangeListener);
        this.executableAttribute.selectedProperty().addListener(executableAttributeChangeListener);
        this.blockAttribute.selectedProperty().addListener(blockAttributeChangeListener);
        this.execAddress.textProperty().addListener(execAddressChangeListener);
        this.loadAddress.textProperty().addListener(loadAddressChangeListener);

        this.name.setTextFormatter(getRestrictedLengthTextFormatter(Constants.SEDORIC_FILENAME_MAXLENGTH));
        this.extension.setTextFormatter(getRestrictedLengthTextFormatter(Constants.SEDORIC_FILEEXTENSION_MAXLENGTH));
    }

    private void updateAttribute(ArchiveFlags attribute, boolean value) {
        if (currentArchive != null) {
            switch (attribute) {
                case BLOCK:
                    currentArchive.setBlockAttribute(value);
                    break;
                case EXECUTABLE:
                    currentArchive.setExecutableAttribute(value);
                    break;
                case PROTECTED:
                    currentArchive.setProtectedAttribute(value);
                    break;
            }
            LOGGER.debug("Updated flags " + attribute + " for archive " + currentArchive
                    + " to value " + value);
        }
    }

    public void bindToArchive(SedoricArchive archive) {
        unbindFromCurrentArchive();
        if (archive != null) {
            LOGGER.debug("Setting name to " + archive.getName());
            this.name.setText(archive.getName());
            this.extension.setText(archive.getExtension());
            size.setText(String.format("%d", archive.getSize()));
            currentArchive = archive;
            this.loadAddress.setText(String.format("0x%04x", archive.getLoadAddress()));
            this.execAddress.setText(String.format("0x%04x", archive.getExecAddress()));
            this.blockAttribute.setSelected(archive.isBlockAttribute());
            this.executableAttribute.setSelected(archive.isExecutableAttribute());
            this.protectedAttribute.setSelected(archive.isProtectedAttribute());
        }
    }

    private void resetValues() {
        name.setText(Constants.EMPTY_STRING);
        name.getStyleClass().removeAll(Constants.TEXT_ERROR_STYLE);
        extension.setText(Constants.EMPTY_STRING);
        extension.getStyleClass().removeAll(Constants.TEXT_ERROR_STYLE);
        size.setText(Constants.NO_VALUE);
        protectedAttribute.setSelected(false);
        executableAttribute.setSelected(false);
        blockAttribute.setSelected(false);
        loadAddress.setText(Constants.EMPTY_STRING);
        loadAddress.getStyleClass().removeAll(Constants.TEXT_ERROR_STYLE);
        execAddress.setText(Constants.EMPTY_STRING);
        execAddress.getStyleClass().removeAll(Constants.TEXT_ERROR_STYLE);
    }

    private void unbindFromCurrentArchive() {
        currentArchive = null;
        resetValues();
    }
}