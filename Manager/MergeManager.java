package gitlet.Manager;

import gitlet.Commit;
import gitlet.Const;
import gitlet.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * special task force dealing with merging.
 *
 * @author TY
 */
public class MergeManager {
    /**
     * a new merge manager.
     *
     * @param primaryBranch
     * primary branch for merging; a.k.a. active branch.
     * @param secondaryBranch
     * secondary branch, to merge into primary.
     *a.k.a. "given branch".
     */
    public MergeManager(String primaryBranch, String secondaryBranch)
            throws IOException {
        _primaryB = primaryBranch;
        _secondaryB = secondaryBranch;
        mergePreCheck();
        _pHead = BranchManager.getBranchHead(_primaryB);
        _sHead = BranchManager.getBranchHead(_secondaryB);
        _primaryPedigree = new LinkedHashMap<>();
        _secondaryPedigree = new LinkedHashMap<>();
        _fileToAdd = new LinkedHashMap<>();
        _fileToRemove = new LinkedHashMap<>();
        _mergedData = new LinkedHashMap<>();
        _splitPoint = getSplitPoint(_pHead, _sHead);
        _aMap = _splitPoint.getBlobMapping();

    }

    /**
     * merge secondary branch into primary branch.
     */
    public void merge() throws IOException {
        catagorizeFile(_pHead.getBlobMapping(), _sHead.getBlobMapping());
        mergeCWDCheck();
        proccessFile();
        CommitManager.mergeCommit(_pHead, _sHead, _secondaryB);
    }

    /**
     * catagotize files to be merged.
     * @param pMap blob mapping of primary branch.
     * @param sMap blob mapping of secondary branch.
     * logic:
     *             for files tracked by OG:
     * 1. iff file is changed in one branch but unchanged
     *            in the other, changed file goes to the next commit.
     * 2. iff file is removedd in one branch but unchanged
     *            in the other, file is removed in the next commit.
     * 3. iff file is modified in sMap and pMap
     *     differently, their conflicts will be merged.
     *
     *            for added files OG does not track:
     * 1. iff file is only present in one Map,
     *             file goes to the next commit.
     * 2. iff file is present in both Maps and are different,
     *             their conflicts will be merged.
     *
     */
    private void catagorizeFile(LinkedHashMap<String, String> pMap,
                                LinkedHashMap<String, String> sMap) {
        for (String fileName : _aMap.keySet()) {
            if (!modified(pMap, fileName)
                    && modified(sMap, fileName)) {
                if (sMap.containsKey(fileName)) {
                    _fileToAdd.put(fileName, sMap.get(fileName));
                } else {
                    _fileToRemove.put(fileName, "placeholder");
                }
            }
            if (modified(pMap, fileName) && modified(sMap, fileName)
                    && !(Utils.idMapping(pMap, sMap, fileName))
                    && (pMap.containsKey(fileName)
                    || sMap.containsKey(fileName))) {
                mergeConflict(fileName, pMap.get(fileName), sMap.get(fileName));
            }
            pMap.remove(fileName);
            sMap.remove(fileName);
        }
        for (String fileName : pMap.keySet()) {
            if (sMap.containsKey(fileName)) {
                if (!Utils.idMapping(pMap, sMap, fileName)) {
                    mergeConflict(fileName,
                            pMap.get(fileName), sMap.get(fileName));
                }
            }
        }
        for (String fileName : sMap.keySet()) {
            _fileToAdd.put(fileName, sMap.get(fileName));
        }
    }

    /**
     * process catagorized files.
     * put files from _filesToAdd into CWD and stage changes.
     * remove files in _filesToRemove and stage changes.
     * create/overwrite files using files from _mergedData.
     */
    private void proccessFile() throws IOException {
        for (String fileName : _fileToAdd.keySet()) {
            CheckoutManager.copyBlob(fileName, _sHead.getBlobMapping());
            StageManager.stageAdd(fileName);
        }
        for (String fileName : _fileToRemove.keySet()) {
            StageManager.stageRemove(fileName);
        }
        if (!_mergedData.isEmpty()) {
            System.out.println("Encountered a merge conflict.");
            for (Map.Entry<String, String> entry
                    : _mergedData.entrySet()) {
                String fileName = entry.getKey();
                String dataStr = entry.getValue();
                File file = new File(Const.CWD, fileName);
                file.createNewFile();
                Utils.writeContents(file, dataStr);
                StageManager.stageAdd(fileName);
            }
        }

    }

    /**
     * check iff the merging BRANCHNAME is valid. Errors iff:
     * 1. there are unstaged additions or removals.
     * 2. primary and secondary are the same branch.
     */
    private void mergePreCheck() {
        if (!StageManager.isStageEmpty()) {
            Utils.error("You have uncommitted changes.");
        }
        if (_primaryB.equals(_secondaryB)) {
            Utils.error("Cannot merge a branch with itself.");
        }
        if (!BranchManager.branchExists(_secondaryB)) {
            Utils.error("A branch with that name does not exist.");
        }
    }

    /**
     * check iff there are any files in CWD that are untrakced by
     * current commit and will be removed or modified by merge.
     */
    private void mergeCWDCheck() {
        List<String> uncheckedFiles = InfoManager.untrackedInCWD();
        for (String fileName : uncheckedFiles) {
            if (_mergedData.containsKey(fileName)
                    || _fileToRemove.containsKey(fileName)
                    || _fileToAdd.containsKey(fileName)) {
                Utils.error("There is an untracked file in the way;"
                        + " delete it, or add and commit it first.");
            }
        }
    }

    /**
     * return the split point of primary and secondary commit.
     * Special cases:
     * iff split point is the same commit as given branch,
     *     do nothing and conclude merge.
     * iff split point is the same commit as
     *
     * @param pCommit primary commit.
     * @param sCommit secondary commit.
     */
    private Commit getSplitPoint(Commit pCommit, Commit sCommit)
            throws IOException {
        CommitManager.traceAncestor(pCommit, 1, _primaryPedigree);
        CommitManager.traceAncestor(sCommit, 1, _secondaryPedigree);
        String lcaSHA = null;
        ArrayList<String> diffAncestors = new ArrayList<>();
        for (String commitSHA : _primaryPedigree.keySet()) {
            if (!_secondaryPedigree.containsKey(commitSHA)) {
                diffAncestors.add(commitSHA);
            }
        }
        for (String ancestor : diffAncestors) {
            _primaryPedigree.remove(ancestor);
        }
        int minDist = (int) Double.POSITIVE_INFINITY;
        for (Map.Entry<String, Integer> entry
                : _primaryPedigree.entrySet()) {
            if (entry.getValue() < minDist) {
                lcaSHA = entry.getKey();
                minDist = entry.getValue();
            }
        }
        Commit splitPoint = CommitManager.getCommit(lcaSHA);
        if (splitPoint.getShaVal().equals(_sHead.getShaVal())) {
            InfoManager.ptln("Given branch is an "
                    + "ancestor of the current branch.");
            System.exit(0);
        }
        if (splitPoint.getShaVal().equals(_pHead.getShaVal())) {
            CheckoutManager.checkoutBranch(_secondaryB);
            InfoManager.ptln("Current branch fast-forwarded.");
            System.exit(0);
        }
        return splitPoint;
    }




    /**
     * returns if the file blob in MAP which FILENAME maps to differs
     * from the common ancestral commit. If so, it means file blob in
     * map is modified. removing file from map also counts as modified.
     */
    private boolean modified(LinkedHashMap<String, String> map,
                             String fileName) {
        return !Utils.idMapping(_aMap, map, fileName);
    }

    /**merge conflicts from two blobs storing info of the same file.
     * @param fileName name of the file in conflict.
     * @param parentSHA SHA-1 value of blob in current commit.
     * @param secondarySHA SHA-1 value of blob in branch to be merged in.
     */
    private void mergeConflict(String fileName,
                               String parentSHA, String secondarySHA) {
        String content = "<<<<<<< HEAD\n";
        if (parentSHA != null) {
            content += Utils.readContentsAsString(
                    new File(Const.BLOB_DIR, parentSHA));
        }
        content += "=======\n";
        if (secondarySHA != null) {
            content += Utils.readContentsAsString(
                    new File(Const.BLOB_DIR, secondarySHA));
        }
        content += ">>>>>>>\n";
        _mergedData.put(fileName, content);
    }

    /**
     * all SHA-1 values of primary commit's ancestors, and
     * their distance(how many commit it takes to get to C1)
     * to C1.
     */
    private LinkedHashMap<String, Integer> _primaryPedigree;

    /**
     * all SHA-1 values of secondary commit's ancestors, and
     * their distance(how many commit it takes to get to C2)
     * to C2.
     */
    private LinkedHashMap<String, Integer> _secondaryPedigree;

    /**
     * string representation of primary branch.
     */
    private String _primaryB;

    /**
     * string representation of secondary branch.
     */
    private String _secondaryB;

    /**
     * head commit of primary branch.
     */
    private Commit _pHead;

    /**
     * head commit of secondary branch.
     */
    private Commit _sHead;

    /**
     * latest common ancestor A.K.A. split point of two branch heads.
     */
    private Commit _splitPoint;


    /**
     * file mapping from the commit at the split point of two branches.
     * maping: filename --> SHA value of blobs.
     */
    private LinkedHashMap<String, String> _aMap;

    /**
     * files from secondary branch that will be added to
     * CWD and staged.
     * mapping: fileName --> SHA value of blobs.
     */
    private LinkedHashMap<String, String> _fileToAdd;

    /**
     * all files to stage for removal.
     * mapping: fileName --> SHA value of blobs(does not matter in this case).
     */
    private LinkedHashMap<String, String> _fileToRemove;

    /**
     * store the mapping:
     * [File name] --> [merged file contents as String].
     */
    private LinkedHashMap<String, String> _mergedData;
}
