package com.grelobites.oric.dsk.util;

import com.grelobites.oric.dsk.Constants;
import com.grelobites.oric.dsk.ApplicationContext;
import com.grelobites.oric.dsk.model.Archive;
import com.grelobites.oric.dsk.model.Disk;
import com.grelobites.oric.dsk.oricdos.OricDosDirectory;
import com.grelobites.oric.dsk.sedoric.SedoricArchive;
import com.grelobites.oric.dsk.sedoric.SedoricDirectory;
import com.grelobites.oric.dsk.sedoric.SedoricHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class ArchiveUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveUtil.class);

    public static Pair<String, String> getBestName(String name) {
        return getBestNameWithSuffix(name, null);
    }

    private static String cutToLength(String value, int length) {
        return value.length() < length ? value : value.substring(0, length);
    }

    public static Pair<String, String> getBestNameWithSuffix(String name, String suffix) {
        int lastSeparator = name.lastIndexOf(Constants.FILE_EXTENSION_SEPARATOR);

        String candidateName = lastSeparator > -1 ? name.substring(0, lastSeparator) : name;
        String candidateExtension = lastSeparator > -1 ? name.substring(lastSeparator + 1) : "";

        candidateName = cutToLength(candidateName,
                suffix != null ? Constants.SEDORIC_FILENAME_MAXLENGTH - suffix.length() :
                        Constants.SEDORIC_FILENAME_MAXLENGTH).toUpperCase();
        candidateExtension = cutToLength(candidateExtension, Constants.SEDORIC_FILEEXTENSION_MAXLENGTH)
                .toUpperCase();
        if (suffix != null) {
            candidateName += suffix;
        }
        return new Pair(candidateName, candidateExtension);
    }

    public static boolean isNameInUse(Pair<String, String> name, ApplicationContext context) {
        return context.getArchiveList().filtered(a ->
                a.getName().equals(name.left()) &&
                a.getExtension().equals(name.right())).size() > 0;
    }

    public static Pair<String, String> calculateArchiveName(String name, ApplicationContext context) {
        Pair<String, String> candidate = getBestName(name);
        int index = 0;
        while (isNameInUse(candidate, context)) {
            LOGGER.info("Name " + candidate + " already in use");
            candidate = getBestNameWithSuffix(name, String.format("%02d", index++));
        }
        return candidate;
    }

    public static FileType guessFileType(File file) {
        if (DskUtil.isOricPlainDskFile(file)) {
            return FileType.PLAINDSK;
        } else if (DskUtil.isOricMfmDskFile(file)) {
            return FileType.MFMDSK;
        } else if ("TAP".equalsIgnoreCase(Util.getFileExtension(file.getName()).orElse(""))) {
            return FileType.TAP;
        } else {
            return FileType.ARCHIVE;
        }
    }

    public static byte[] readFromFileWithOffset(File file, int offset) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.skip(offset);
            return Util.fromInputStream(fis);
        }
    }

    public static SedoricArchive createArchiveFromFile(File file, ApplicationContext context)
            throws IOException {
        Pair<String, String> name = calculateArchiveName(file.getName(), context);
        Optional<SedoricHeader> headerOpt = SedoricHeader.fromFile(file);
        byte[] data;
        if (headerOpt.isPresent()) {
            data = readFromFileWithOffset(file, SedoricHeader.HEADER_SIZE);
        } else {
            data = Files.readAllBytes(file.toPath());
        }

        SedoricArchive archive = new SedoricArchive(name.left(), name.right(), data);
        if (headerOpt.isPresent()) {
            SedoricHeader header = headerOpt.get();
            archive.setLoadAddress(header.getLoadAddress());
            archive.setExecAddress(header.getExecAddress());
            archive.setBlockAttribute(header.getBlockAttribute());
            archive.setExecutableAttribute(header.getExecutableAttribute());
        }
        return archive;
    }

    public static SedoricArchive updateArchiveName(SedoricArchive archive, ApplicationContext context) {
        String name = toSedoricValidName(archive.getName(), Constants.SEDORIC_FILENAME_MAXLENGTH);
        if (archive.getExtension().length() > 0) {
            name += Constants.FILE_EXTENSION_SEPARATOR +
                    toSedoricValidName(archive.getExtension(),
                            Constants.SEDORIC_FILEEXTENSION_MAXLENGTH);
        }
        Pair<String, String> bestName = calculateArchiveName(name, context);
        archive.setName(bestName.left());
        archive.setExtension(bestName.right());
        return archive;
    }

    public static void addArchivesFromDsk(File file, ApplicationContext context) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            Disk disk = DskUtil.diskFromDskStream(fis);
            LOGGER.debug("Guessing disk format");
            switch (DskUtil.getDiskFormat(disk)) {
                case UNKNOWN:
                case SEDORIC:
                    SedoricDirectory.fromDisk(disk).forEach(d -> {
                        context.getArchiveList().add(updateArchiveName(d.getArchive(disk), context));
                    });
                    break;
                case ORICDOS:
                    OricDosDirectory.fromDisk(disk).forEach(d -> {
                        d.getArchive(disk).map(t ->
                                context.getArchiveList().add(updateArchiveName(t, context)))
                                .orElse(false);
                    });
                    break;
            }
        }
    }

    public static void addArchivesFromTap(File tapFile, ApplicationContext context)
            throws IOException {
        try (FileInputStream fis = new FileInputStream(tapFile)) {
            TapReader reader = new TapReader(fis);
            while (reader.hasNext()) {
                context.getArchiveList().add(updateArchiveName(reader.getNext(), context));
            }
        }
    }

    public static void addArchivesInFile(ApplicationContext context, File file) throws IOException {
        LOGGER.debug("getArchivesInFile " + file);
        switch (guessFileType(file)) {
            case ARCHIVE:
                SedoricArchive archive = createArchiveFromFile(file, context);
                context.getArchiveList().add(archive);
                break;
            case PLAINDSK:
            case MFMDSK:
                addArchivesFromDsk(file, context);
                break;
            case TAP:
                addArchivesFromTap(file, context);
                break;
            default:
                throw new IllegalArgumentException("Not implemented yet");
        }
    }

    public static void exportAsFile(SedoricArchive sourceArchive, File outputFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            if (SedoricHeader.needsHeader(sourceArchive)) {
                fos.write(SedoricHeader.forArchive(sourceArchive).asByteArray());
            }
            fos.write(sourceArchive.getData(), 0, sourceArchive.getSize());
        }
    }

    public static void exportAsBinaryFile(SedoricArchive sourceArchive, File outputFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(sourceArchive.getData(), 0, sourceArchive.getSize());
        }
    }

    public static void exportAsTapFile(File outputFile, SedoricArchive ...sources) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            TapWriter tapWriter = new TapWriter(fos);
            for (SedoricArchive source : sources) {
                tapWriter.write(source);
            }
        }
    }

    public static File toTemporaryFile(SedoricArchive sourceArchive) throws IOException {
        File file = new File(new File(System.getProperty("java.io.tmpdir")),
                String.format("%s.%s", sourceArchive.getName().trim(), sourceArchive.getExtension().trim()));
        exportAsFile(sourceArchive, file);
        return file;
    }

    public static String toSedoricValidName(String newValue, int maxLength) {
        StringBuilder result = new StringBuilder();
        for (Character c : newValue.toCharArray()) {
            Character u = Character.toUpperCase(c);
            if (Character.isLetterOrDigit(u)) {
                result.append(u);
            }
            if (result.length() == maxLength) {
                break;
            }
        }
        return result.toString().toUpperCase().trim();
    }
}
