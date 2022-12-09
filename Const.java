package gitlet;

import java.io.File;

/** constants used for GITLET.*/
public class Const {
    /**
     * Current Working Directory.
     */
    public static final File CWD = new File(".");

    /**
     * Main gitlet directory.
     */
    public static final File GITLET_DIR = new File(CWD, ".gitlet");

    /**
     * directory storing both .commits and .blobmapping.
     */
    public static final File COMMIT_META_DIR =
            new File(GITLET_DIR, ".commitMeta");

    /**
     * directory storing all the commits serialized.
     */
    public static final File COMMIT_DIR = new File(COMMIT_META_DIR, ".commits");

    /**
     * directory storing all gitlet's metadata.
     */
    public static final File GITLET_META_DIR =
            new File(GITLET_DIR, ".gitletMeta");

    /**
     * directory storing all the blobs staged for addition.
     */
    public static final File ADD_BLOB_DIR = new File(GITLET_DIR, ".blobCart");

    /**
     * directory storing mappings of files of all commits.
     */
    public static final File BLOB_MAP_DIR =
            new File(COMMIT_META_DIR, ".blobMapping");

    /**
     * directory storing all the blobs that has been committed.
     */
    public static final File BLOB_DIR = new File(GITLET_DIR, ".blobs");

    /**
     * file storing the active branch.
     */
    public static final File ACTIVE_BRANCH_FILE =
            new File(GITLET_META_DIR, ".activeBranch");

    /**
     * file storing a serialized hashmap for files staged to be added.
     */
    public static final File ADD_STG_FILE =
            new File(GITLET_META_DIR, ".StagedAdd");

    /**
     * file storing a serialized hashmap for files staged to be removed.
     */
    public static final File RM_STG_FILE =
            new File(GITLET_META_DIR, ".StagedRm");

    /**
     * file storing a serialized hashmap for branches.
     */
    public static final File BRANCH_MAP_FILE =
            new File(GITLET_META_DIR, ".branchMAP");

    /**
     * file storing a serialized hashmap for remote repositories.
     * Mapping: repository name --> repository directory
     */
    public static final File REMOTE_REPO_FILE =
            new File(GITLET_META_DIR, ".remoteRepo");

    /**hour to minute.*/
    public static final int HOUR_TO_MILISEC = 3600000;


}
