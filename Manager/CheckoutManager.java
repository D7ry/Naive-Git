package gitlet.Manager;

import gitlet.Commit;
import gitlet.Utils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;

import static gitlet.Const.*;
/**manager class mainly dealing with checkout.*/
public class CheckoutManager {

    /**check out file from head commit.
     * @param fileName name of the file to be checked out.*/
    public static void checkoutFileInHead(String fileName) throws IOException {
        checkoutFileInCommit(BranchManager.getActiveBranchHeadSHA(), fileName);
    }

    /**check out file from specific commit.
     * @param commitId SHA-1 value of the commit to be checked out from
     * @param fileName name of the file to be checked out.
     * */
    public static void checkoutFileInCommit(String commitId, String fileName)
            throws IOException {
        if (InfoManager.untrackedInCWD().contains(fileName)) {
            Utils.error("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
        }
        Commit commit = CommitManager.fetchCommit(commitId);
        checkout(commit, fileName);
    }


    /**check out the entire branch.
     * @param branch string representation of the branch to be checked out.*/
    public static void checkoutBranch(String branch) throws IOException {
        if (branch.equals(BranchManager.activeBranch())) {
            Utils.error("No need to checkout the current branch.");
        }
        if (!BranchManager.branchMap().containsKey(branch)) {
            Utils.error("No such branch exists.");
        }
        String branchHeadSHA = BranchManager.branchMap().get(branch);
        Commit branchHead = CommitManager.getCommit(branchHeadSHA);
        checkCWDTracking(branchHead);
        checkoutCommit(branchHead);
        BranchManager.setActiveBranch(branch);
        StageManager.clearStage();
    }



    /**check out file named FILENAME from COMMIT, assuming commit exists. */
    public static void checkout(Commit commit, String fileName)
            throws IOException {
        if (!copyBlob(fileName, commit.getBlobMapping())) {
            Utils.error("File does not exist in that commit.");
        }
    }

    /** what's relaly going on in an checkout.
     * grab file blob corresponding to FILENAME from BLOBMAPPING, a mapping
     * of filename -> committed file blob, and overwrite the file with
     * same filename or create file in CWD.
     * returns if the operation is successful. operation fails only if
     * file cannot be found from blobMapping.
     */
    public static Boolean copyBlob(String fileName,
                                 LinkedHashMap<String, String> blobMapping)
            throws IOException {
        if (!blobMapping.containsKey(fileName)) {
            return false;
        }
        File file = new File(CWD, fileName);
        File blob = new File(BLOB_DIR, blobMapping.get(fileName));
        Utils.copyContents(blob, file);
        return true;
    }

    /**check out all files from commit with SHA value of COMMITSHA;
     * tracked files that are not present in that commit are removed.
     * also moves current branch's head to the commit node, and
     * clears the staging area.
     */
    public static void reset(String commitSHA) throws IOException {
        Commit commit = CommitManager.fetchCommit(commitSHA);
        checkCWDTracking(commit);
        checkoutCommit(commit);
        BranchManager.setBranchHead(commit);
        StageManager.clearStage();
    }

    /**checks if there is a file in CWD that is untracked in the
     * current commit but tracked by commit to be checked out.
     * If so, prints the error
     * log and exits the program.
     * @param commit the commit to be checked out.
     */
    static void checkCWDTracking(Commit commit) {
        LinkedHashMap<String, String> blobMapping = commit.getBlobMapping();
        for (String fileName : InfoManager.untrackedInCWD()) {
            if (blobMapping.containsKey(fileName)) {
                Utils.error("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
            }
        }
    }

    /**checks out all files in COMMIT, putting them in CWD,
     * overwring & creating new files as it needs to.
     * Also delete files that are tracked
     * in current commit but not in new commit.
     * @param commit the commit to be checked out.
     */
    static void checkoutCommit(Commit commit)
            throws IOException {
        LinkedHashMap<String, String> newMapping =
                commit.getBlobMapping();
        LinkedHashMap<String, String> currMapping =
                CommitManager.headCommit().getBlobMapping();
        for (String fileName : newMapping.keySet()) {
            copyBlob(fileName, newMapping);
            currMapping.remove(fileName);
        }
        for (String fileName : currMapping.keySet()) {
            Utils.cwdDel(fileName);
        }
    }


}
