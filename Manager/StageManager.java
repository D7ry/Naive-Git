package gitlet.Manager;

import gitlet.Utils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;

import static gitlet.Const.*;
/**helper class to process staging and unstaging of files. */
public class StageManager {

    /**adds FILENAME file to the addition stage.
     * copy it to .gitlet/.stagedAdd as a blob named after FILENAME's SHA-1,
     * and register it to ADDSTSGMAP
     */
    public static void stageAdd(String fileName) throws IOException {
        File orgFile = new File(CWD, fileName);
        if (!orgFile.exists()) {
            Utils.error("File does not exist.");
        }
        String blobHash = Utils.sha1(orgFile);
        File blobToStage = new File(ADD_BLOB_DIR, blobHash);
        unstageFromRm(fileName);
        LinkedHashMap currMap = CommitManager.headCommit().getBlobMapping();
        if (currMap.containsKey(fileName)
                && currMap.get(fileName).equals(blobHash)) {
            unstageFromAdd(fileName);
            return;
        }
        Utils.copyContents(orgFile, blobToStage);
        _addStageMap.put(fileName, blobHash);
    }
    /** stage FILENAME file to be removed.
     * 1. unstage file iff the file is staged for add.
     * 2. iff the file is tracked, stage for removal
     * and remove file from CWD.
     */
    public static void stageRemove(String fileName) {
        boolean opSuccess = false;
        if (_addStageMap.containsKey(fileName)) {
            unstageFromAdd(fileName);
            opSuccess = true;
        }
        if (CommitManager.headCommit().getBlobMapping().containsKey(fileName)) {
            Utils.cwdDel(fileName);
            _removeStageMap.put(fileName, "placeholder");
            opSuccess = true;
        }
        if (!opSuccess) {
            Utils.error("No reason to remove the file.");
        }
    }


    /**simply unstage FILENAME from add stage area
     * does two things:
     * 1. remove the ORGFILENAME from ADDSTGMAP.
     * 2. delete the BLOBNAME from ADD_BLOB_DIR.
     * */
    public static void unstageFromAdd(String orgFileName) {
        String blobName = _addStageMap.get(orgFileName);
        File blobToUnstage = null;
        if (blobName != null) {
            blobToUnstage = new File(ADD_BLOB_DIR, blobName);
        }
        if (_addStageMap.containsKey(orgFileName)) {
            _addStageMap.remove(orgFileName);
        }
        if (blobToUnstage != null && blobToUnstage.exists()) {
            blobToUnstage.delete();
        }
    }

    /**simply unstage FILENAME from removal stage area.
     */
    public static void unstageFromRm(String fileName) {
        if (_removeStageMap.containsKey(fileName)) {
            _removeStageMap.remove(fileName);
        }
    }

    /**wipes ADDSTGMAP and RMSTGMAP, as well as
     * the directory storing blobs that contain
     * data of staged files.
     */
    public static void clearStage() {
        _addStageMap.clear();
        Utils.wipeDir(ADD_BLOB_DIR);
        _removeStageMap.clear();
    }

    /**return a list of file names that are staged
     * for addition.
     */
    public static String[] fileToAdd() {
        String[] stagedFile = new String[_addStageMap.size()];
        int i = 0;
        for (String fileName : _addStageMap.keySet()) {
            stagedFile[i] = fileName;
            i++;
        }
        return stagedFile;
    }

    /**return a list of file names that are staged
     * for removal.
     */
    public static String[] fileToRemove() {
        String[] stagedFile = new String[_removeStageMap.size()];
        int i = 0;
        for (String fileName : _removeStageMap.keySet()) {
            stagedFile[i] = fileName;
            i++;
        }
        return stagedFile;
    }

    /**initiate staging area.*/
    public static void initStage() throws IOException {
        ADD_BLOB_DIR.mkdir();
        ADD_STG_FILE.createNewFile();
        RM_STG_FILE.createNewFile();
        _addStageMap = new LinkedHashMap<String, String>();
        _removeStageMap = new LinkedHashMap<String, String>();
    }

    /**returns whether the staging area is empty. */
    public static boolean isStageEmpty() {
        return _addStageMap.isEmpty() && _removeStageMap.isEmpty();
    }

    /**map of files staged for addition.*/
    private static LinkedHashMap<String, String> _addStageMap;
    /**return mapping of files staged for addition. */
    public static LinkedHashMap<String, String> addStageMap() {
        return _addStageMap;
    }
    /**read from serialized file storing addstagemap,
     * and store it in StageManager. */
    public static void getAddStageMapFromFile() {
        _addStageMap = Utils.readLinkedHashMap(ADD_STG_FILE);
    }
    /**write serialized addstagemap into a blob, recording its changes.
     * @param addStgFile the blob saving addstageMap.*/
    public static void saveAddStageMapToFile(File addStgFile) {
        Utils.writeObject(addStgFile, _addStageMap);
    }

    /**map of files staged for removal.*/
    private static LinkedHashMap<String, String> _removeStageMap;

    /**return mapping of files staged for removal. */
    public static LinkedHashMap<String, String> rmStageMap() {
        return _removeStageMap;
    }
    /**read from serialized file storing removestagemap,
     * and store it in StageManager. */
    public static void getRmStageMapFromFile() {
        _removeStageMap = Utils.readLinkedHashMap(RM_STG_FILE);
    }
    /**write removestagemap into rmstageFile, saving it for next use.
     * @param rmStgFile the blob saving removeStageMap*/
    public static void saveRmStageMapToFile(File rmStgFile) {
        Utils.writeObject(rmStgFile, _removeStageMap);
    }
}
