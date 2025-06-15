package com.mint.boilerws.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mint.boilerws.scheduler.Schedule;
import com.mint.boilerws.scheduler.ScheduleItem;
import com.mint.boilerws.scheduler.ScheduleItem.DayType;
import com.mint.boilerws.temp.TemperatureSetting;

public class FileUtil {

    private static final Logger LOG = Logger.getLogger(FileUtil.class);

    private static final String D = ",";
    private static final String NL = "\n";

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    
    public static boolean isRunningInJar() {
        String protocol = FileUtil.class.getResource("").getProtocol();
        return (Objects.equals(protocol, "jar"));
    }

    public static void copyFromJar(
            final Object refObject, 
            final String source, 
            final Path target, 
            final Set<String> executableFilenames,
            final boolean overwrite) throws URISyntaxException, IOException {
        /**
         * if you get a "Provider "rsrc" not found" exception here, make sure you export your Java with the option 
         * "Extract required JAR libraries into generated JAR"
         */
        URI resource = refObject.getClass().getResource("").toURI();
        FileSystem fileSystem = FileSystems.newFileSystem(
                resource,
                Collections.<String, String>emptyMap()
        );
        final Path jarPath = fileSystem.getPath(source);
        Files.walkFileTree(jarPath, new SimpleFileVisitor<Path>() {
            private Path currentTarget;
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                currentTarget = target.resolve(jarPath.relativize(dir).toString());
                Files.createDirectories(currentTarget);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                final Path destination = target.resolve(jarPath.relativize(file).toString());
                final boolean exists = destination.toFile().exists();
                if (exists && !overwrite) {
                    LOG.info("File exists, not replacing: " + destination.toFile().getAbsolutePath());
                } else {
                    LOG.info("Copying to: " + destination.toFile().getAbsolutePath());
                    Files.copy(file, destination, StandardCopyOption.REPLACE_EXISTING);
                    if (executableFilenames.contains(file.getFileName().toString())) {
                        setExecutable(destination);
                    } else {
                        //LOG.info("Non executable: " + file.getFileName().toString());
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    public static void setExecutable(final Path path) throws IOException {
        final Set<PosixFilePermission> ownerWritable = PosixFilePermissions.fromString("rwxr-xr-x");
        final Path result = Files.setPosixFilePermissions(path, ownerWritable);
        LOG.info("Made executable: " + result.toFile().getAbsolutePath());
    }
    
    public static void toFile(final TemperatureSetting tempSetting, final File outFile) {
        try {
            JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValue(outFile, tempSetting);
        } catch (IOException e) {
            LOG.error("Error writing temperature file: " + outFile.getAbsolutePath(), e);
        }
    }
    
    public static TemperatureSetting temperatureSettingfromFile(final File inFile) {
        try {
            final TemperatureSetting r = JSON_MAPPER.readValue(inFile, TemperatureSetting.class);
            return r;
        } catch (IOException e) {
            LOG.error("Error reading schedule file: " + inFile.getAbsolutePath(), e);
        }
        return null;
    }
    
    public static void toFile(final Schedule schedule, final File outFile) {
        try {
            JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValue(outFile, schedule);
        } catch (IOException e) {
            LOG.error("Error writing schedule file: " + outFile.getAbsolutePath(), e);
        }
    }

    public static Schedule fromFile(final File inFile) {
        try {
            final Schedule r = JSON_MAPPER.readValue(inFile, Schedule.class);
            return r;
        } catch (IOException e) {
            LOG.error("Error reading schedule file: " + inFile.getAbsolutePath(), e);
        }
        return null;
    }
    
    public static void toFileSimple(final Schedule schedule, final File outFile) {
        try (final BufferedWriter bw = new BufferedWriter(new FileWriter(outFile))) {
            for (final ScheduleItem item : schedule.getSchedule(DayType.WEEKDAY)) {
                bw.write(toString(item));
            }
            for (final ScheduleItem item : schedule.getSchedule(DayType.WEEKEND)) {
                bw.write(toString(item));
            }
            bw.close();
        } catch (IOException e) {
            LOG.error("Error writing to " + outFile, e);
        }
    }

    public static Schedule fromFileSimple(final File inFile) {
        final Map<DayType, SortedSet<ScheduleItem>> schedule = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(inFile))){
            String line;
            while((line=br.readLine())!= null) {
                if (line.startsWith("#")) {
                    continue;
                }
                final String[] values = line.split(D);
                final DayType dayType = DayType.valueOf(values[0]);
                final int hour = Integer.parseInt(values[1]);
                final int minutes = Integer.parseInt(values[2]);
                final boolean isOn = Boolean.parseBoolean(values[3]);
                final ScheduleItem item = new ScheduleItem(dayType, hour, minutes, isOn);
                final SortedSet<ScheduleItem> items = schedule.computeIfAbsent(dayType, (k) -> {
                    return new TreeSet<>();
                });
                items.add(item);
            }
            final Schedule r = new Schedule(schedule);
            return r;
        } catch (IOException e) {
            LOG.error("Error reading from: " + inFile.getAbsolutePath(), e);
        }
        return null;
    }
    
    private static String toString(ScheduleItem item) {
        final StringBuilder sb = new StringBuilder();
        sb.append(item.getDayType())
                .append(D).append(item.getHour()).append(D).append(item.getMinute())
                .append(D).append(item.isOn()).append(NL);
        return sb.toString();
    }
    
    public static Path getFilePath(final String seekPath) {
        String checkPath = seekPath;
        while (true) {
            Path resultPath = Paths.get(checkPath);
            if (Files.exists(resultPath, LinkOption.NOFOLLOW_LINKS)) {
                return resultPath;
            } else {
                LOG.info("Path not exists: " + checkPath);
            }
            final int ix = checkPath.indexOf("/");
            if (ix < 0) {
                break;
            }
            checkPath = checkPath.substring(ix + 1);
        }
        LOG.error("Can't find any of path: " + seekPath);
        return Paths.get(seekPath);
    }

}
