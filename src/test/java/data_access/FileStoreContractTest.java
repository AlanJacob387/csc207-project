package data_access;

import java.nio.file.Path;

import org.junit.jupiter.api.io.TempDir;

/**
 * Runs the store contract against the local JSON file, a fresh file for every
 * test so no test sees another's accounts.
 */
class FileStoreContractTest extends AppDataStoreContractTest {

    @TempDir
    Path folder;

    private int next;

    @Override
    protected AppDataStore newStore() {
        next++;
        return new FileAppDataAccessObject(folder.resolve("app-data-" + next + ".json"));
    }
}
