package gitlet;

import gitlet.Manager.CommitManager;
import gitlet.Manager.InfoManager;
import gitlet.Manager.StageManager;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import static gitlet.Const.*;

/**
 * representing a single commit, contains info of this commit.
 *
 */
public class Commit implements Serializable {
    /**
     * a new gitlet commit with:
     * -commit message MSG.
     * -reference to parent commit PARENT.
     * -reference to all files in the commit BLOBS,
     * stored in another directory with identical name.
     */
    public Commit(String msg, Commit parent) throws IOException {
        initCommit(msg, parent);
    }

    /** a new merge commit.
     * @param mergeMsg merge message of the commit.
     * @param parent parent of this commit.
     * @param mergeParent merge parnet of this commit.
     * @throws IOException
     */

    public Commit(String mergeMsg, Commit parent, Commit mergeParent)
            throws IOException {
        _mergeParentSHA = mergeParent.getShaVal();
        initCommit(mergeMsg, parent);
    }


    /**
     * initialize a standard commit.
     * @param msg    the commit message.
     * @param parent parent commit of THIS commit.
     **/
    private void initCommit(String msg, Commit parent) throws IOException {
        _parentSHA = parent.getShaVal();
        _msg = msg;
        _time = InfoManager.gitletTime();
        LinkedHashMap<String, String> blobMapping = processStage();
        _shaVal = Utils.sha1(
                Utils.serialize(blobMapping),
                Utils.serialize(_msg),
                Utils.serialize(_time),
                Utils.serialize(_parentSHA));
        saveCommitBlob();
        saveMappingBlob(blobMapping);
    }

    /**
     * an initial commit.
     **/
    public Commit() throws IOException {
        _msg = "initial commit";
        _time = "Date: Thu Jan 1 00:00:00 1970 -0000";
        _shaVal = Utils.sha1(Utils.serialize(this));
        LinkedHashMap<String, String> initialMapping =
                new LinkedHashMap<String, String>();
        saveMappingBlob(initialMapping);
        saveCommitBlob();
    }

    /**
     * process the mapping of [Filename -> file blob] from parent, based on
     * addStage, removeStage, and blob mapping from previous commit.
     * return the processed Linked Hashmap of the current commit.
     */
    private LinkedHashMap<String, String> processStage() throws IOException {
        LinkedHashMap<String, String> map = getParentCommit().getBlobMapping();
        for (String key : StageManager.rmStageMap().keySet()) {
            map.remove(key);
        }
        for (Map.Entry<String, String> entry
                : StageManager.addStageMap().entrySet()) {
            String fileName = entry.getKey();
            String blobSHA = entry.getValue();
            map.put(fileName, blobSHA);
            File stagedBlob = new File(ADD_BLOB_DIR, blobSHA);
            File newBlob = new File(BLOB_DIR, blobSHA);
            Utils.copyContents(stagedBlob, newBlob);
        }
        StageManager.clearStage();
        return map;
    }

    /**
     * return the mapping of (File--> blob) corresponding to THIS commit.
     */
    public LinkedHashMap<String, String> getBlobMapping() {
        File blob = this.getMappingBlob();
        return Utils.readLinkedHashMap(blob);
    }

    /**
     * get the SHA value of FILENAME file in this commit.
     * return null if this commit does not track FILENAME.
     */
    public String getFileSHA(String fileName) {
        return getBlobMapping().get(fileName);
    }

    /**
     * return the blob corresponding to FILENAME file
     * in this commit. return null if this commit does not
     * track FILENAME.
     */
    public File getFileBlob(String fileName) {
        String fileSHA = getFileSHA(fileName);
        if (fileSHA == null) {
            return null;
        }
        return new File(BLOB_DIR, fileSHA);
    }


    /**
     * return the blob representing THIS commit.
     */
    public File getCommitBlob() {
        return new File(COMMIT_DIR, _shaVal);
    }

    /**
     * return the blob representing mapping of THIS commit.
     */
    public File getMappingBlob() {
        return new File(BLOB_MAP_DIR, _shaVal);
    }

    /**
     * save the blob representing THIS commit.
     * only called when a new commit is instantiated,
     * since commits are immutable.
     */
    private void saveCommitBlob() throws IOException {
        File commitBlob = new File(COMMIT_DIR, _shaVal);
        commitBlob.createNewFile();
        Utils.writeObject(commitBlob, this);
    }

    /**
     * save the mapping of filename -> file blob, an obj
     * as MAPPINGOBJ into a blob file with name identical
     * to the commit's _shaVal.
     */
    private void saveMappingBlob(LinkedHashMap<String, String> mappingObj)
            throws IOException {
        File mappingBlob = new File(BLOB_MAP_DIR, _shaVal);
        mappingBlob.createNewFile();
        Utils.writeObject(mappingBlob, mappingObj);
    }



    /** simply prints my log.
     */
    public void printLog() {
        System.out.println("===");
        System.out.println("commit " + _shaVal);
        if (_mergeParentSHA != null) {
            System.out.println("Merge: "
                    + _parentSHA.substring(0, 7) + " "
                    + _mergeParentSHA.substring(0, 7));
        }
        System.out.println(_time);
        System.out.println(_msg);
        System.out.println();
    }

    /**
     * print my log, and trace the previous
     * commit's log if previous commit exists.
     */
    public void traceLog() {
        printLog();
        if (_parentSHA != null) {
            getParentCommit().traceLog();
        }
    }

    /**
     * return parent commit of THIS commit.
     */
    public Commit getParentCommit() {
        return CommitManager.getCommit(_parentSHA);
    }

    /**
     * return parent commit SHA of THIS commit.
     */
    public String getParentSHA() {
        return _parentSHA;
    }

    /**
     * return merge parent commit of THIS commit.
     */
    public Commit getMergeParentCommit() {
        return CommitManager.getCommit(_mergeParentSHA);
    }

    /**
     * return merge parent commit SHA of THIS commit.
     */
    public String getMergeParentSHA() {
        return _mergeParentSHA;
    }

    /**return iff THIS commit has parent. */
    public boolean hasParent() {
        return _parentSHA != null;
    }

    /**return iff THIS commit has a merge parent. */
    public boolean hasMergeParent() {
        return _mergeParentSHA != null;
    }


    /**
     * return my commit message.
     */
    public String getMsg() {
        return _msg;
    }


    /**
     * commit message.
     */
    private String _msg;

    /**
     * timestamp.
     */
    private String _time;

    /**
     * parent commit's SHA-1 VAL.
     */
    private String _parentSHA;

    /**
     * reference to 2nd parent commit if there's a merge.
     */
    private String _mergeParentSHA;

    /**
     * SHA-1 value refering to THIS commit.
     * also the name of the blob storing this Commit
     * also the name of the blob storing this commit's mapping
     * to its files.
     */
    private String _shaVal;

    /**
     * return the SHAVAL of THIS commit, which
     * is also its name and its blob mapping's name.
     */
    public String getShaVal() {
        return _shaVal;
    }



}
