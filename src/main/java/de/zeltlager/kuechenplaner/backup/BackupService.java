package de.zeltlager.kuechenplaner.backup;

import java.nio.file.Path;

public interface BackupService {

    Path createBackup(Path targetFile);
}
