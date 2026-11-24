package data_access;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import use_case.export_schedule.ScheduleFileWriter;
import use_case.export_schedule.ScheduleWriteException;

/**
 * Writes the exported calendar to a file on disk.
 *
 * The text goes to a temporary file in the same folder first and is then moved
 * over the target, so a write that fails halfway (disk full, file locked)
 * never leaves a half-written calendar or wipes out an older good one.
 * Every failure becomes a ScheduleWriteException with a readable message.
 */
public class FileScheduleWriter implements ScheduleFileWriter {

    @Override
    public void write(String targetPath, String content) {
        final Path target;
        try {
            target = Path.of(targetPath).toAbsolutePath();
        }
        catch (InvalidPathException ex) {
            throw new ScheduleWriteException("\"" + targetPath + "\" isn't a valid file name.", ex);
        }
        // Checked up front: moving onto an empty folder with REPLACE_EXISTING
        // would quietly delete the folder.
        if (Files.isDirectory(target)) {
            throw new ScheduleWriteException(target + " is a folder, not a file.", null);
        }

        final Path folder = target.getParent();
        Path temp = null;
        try {
            temp = Files.createTempFile(folder, ".team-schedule-", ".tmp");
            Files.writeString(temp, content, StandardCharsets.UTF_8);
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            }
            catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (AccessDeniedException ex) {
            throw new ScheduleWriteException("Can't write to " + target
                    + ". You may not have permission, or another program has it open.", ex);
        }
        catch (NoSuchFileException ex) {
            throw new ScheduleWriteException("The folder " + folder + " doesn't exist.", ex);
        }
        catch (IOException ex) {
            throw new ScheduleWriteException("Could not write " + target + ": " + ex.getMessage(), ex);
        }
        finally {
            deleteQuietly(temp);
        }
    }

    /** Removes the temporary file if the move never happened. */
    private static void deleteQuietly(Path temp) {
        if (temp == null) {
            return;
        }
        try {
            Files.deleteIfExists(temp);
        }
        catch (IOException ignored) {
            // Nothing more to do; the export error has already been reported.
        }
    }
}
