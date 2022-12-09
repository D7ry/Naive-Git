package gitlet.Manager;

import gitlet.Commit;
import gitlet.Utils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

import static gitlet.Const.COMMIT_DIR;

/**helper class the manages commits.*/
public class CommitManager {


    /**return the current head commit.*/
    public static Commit headCommit() {
        String commitSHA = BranchManager.getActiveBranchHeadSHA();
        return getCommit(commitSHA);
    }

    /**make a new commit with commit message MSG,
     * updating active branch's head to point to
     * the new commit.*/
    public static void commit(String msg) throws IOException {
        if (msg.length() == 0) {
            Utils.error("Please enter a commit message.");
        }
        if (StageManager.isStageEmpty()) {
            Utils.error("No changes added to the commit.");
        }
        Commit newCommit = new Commit(msg, headCommit());
        BranchManager.setBranchHead(newCommit.getShaVal());
    }

    /**make a merge commit, merging HEAD commit with MERGEHEAD commit,
     * from MERGEBRANCH.
     */
    public static void mergeCommit(Commit head, Commit mergeHead,
                                   String mergeBranch) throws IOException {
        String mergeMsg = "Merged " + mergeBranch
                + " into " + BranchManager.activeBranch() + ".";
        Commit newCommit = new Commit(mergeMsg,
                head, mergeHead);
        BranchManager.setBranchHead(newCommit.getShaVal());
    }

    /**return commit with CommitSHA matching abbreviated
     * SHA-1 value SHORTSHA. Errors if no such commit exists. */
    public static Commit fetchCommit(String shortSHA) {
        List<String> commitSHAs = InfoManager.getDirFileLst(COMMIT_DIR);
        for (String commitSHA : commitSHAs) {
            String commitHead = commitSHA.substring(0, shortSHA.length());
            if (commitHead.equals(shortSHA)) {
                return getCommit(commitSHA);
            }
        }
        Utils.error("No commit with that id exists.");
        return null;
    }

    /**return cmomit from local commit blob directory, from blob
     * named COMMITSHA.
     */
    public static Commit getCommit(String commitSHA) {
        return getCommit(commitSHA, COMMIT_DIR);
    }

    /**return a commit by reading from a commit blob.
     * @param commitDir directory storing the commit.
     * @param commitSHA name of the blob storing a serialized commit.*/
    public static Commit getCommit(String commitSHA, File commitDir) {
        File commitBlob = new File(commitDir, commitSHA);
        return getCommit(commitBlob);
    }

    /**return commit read from blob COMMITBLOB.*/
    public static Commit getCommit(File commitBlob) {
        return Utils.readObject(commitBlob, Commit.class);
    }

    /**
     * recursively add all ancestors of a commit and their distance
     * to it into a pedigree, a linked hash map that stores ancestor's
     * SHA-1 reference as well as their distance to the commit.
     *
     * @param commit    commit to trace.
     * @param cumulDist cumulative distance from tracer to original commit.
     * @param pedigree linked hash map to store ancestors.
     */
    public static void traceAncestor(Commit commit, int cumulDist,
                               LinkedHashMap<String, Integer> pedigree) {
        pedigree.put(commit.getShaVal(), cumulDist);
        if (commit.hasParent()) {
            traceAncestor(commit.getParentCommit(),
                    cumulDist + 1, pedigree);
        }
        if (commit.hasMergeParent()) {
            traceAncestor(commit.getMergeParentCommit(),
                    cumulDist + 1, pedigree);
        }
    }

}
