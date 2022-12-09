package gitlet.Manager;

import gitlet.Commit;
import gitlet.Utils;

import java.io.Serializable;
import java.util.LinkedHashMap;

import static gitlet.Const.ACTIVE_BRANCH_FILE;
import static gitlet.Const.BRANCH_MAP_FILE;

/**the class storing active branch in its metadata, as well
 * as in charge of manipulating branches.
 */
public class BranchManager implements Serializable {


    /** initialize a branchManager with activeBranch
     * "master" pointing to COMMIT.
     */
    public static void initBranchManager(Commit commit) {
        _branchMap = new LinkedHashMap<String, String>();
        _branchMap.put("master", commit.getShaVal());
        _activeBranch = "master";
    }

    /**read branchMapping from FILE that stores the serialized
     * mapping. */
    public static void getBranchMapFromFile() {
        _branchMap = Utils.readLinkedHashMap(BRANCH_MAP_FILE);
    }

    /**read active branch from FILE that stores serialied cctive branch.*/
    public static void getActiveBranchFromFile() {
        _activeBranch = Utils.readObject(ACTIVE_BRANCH_FILE, String.class);
    }


    /**mapping of branches.*/
    private static LinkedHashMap<String, String> _branchMap;

    /**return mapping of branches. */
    public static LinkedHashMap<String, String> branchMap() {
        return _branchMap;
    }

    /**change active branch to be BRANCH.*/
    public static void setActiveBranch(String branch) {
        _activeBranch = branch;
    }

    /**string representation of the active branch.*/
    private static String _activeBranch;
    /**return string representation of active branch.*/
    public static String activeBranch() {
        return _activeBranch;
    }

    /**return active branch head, a SHA-1 reference to the current commit.*/
    public static String getActiveBranchHeadSHA() {
        return getBranchHeadSHA(_activeBranch);
    }
    /**return SHA-1 reference to the head of the branch BRANCH.*/
    public static String getBranchHeadSHA(String branch) {
        return _branchMap.get(branch);
    }
    /**return head commit of branch BRANCH.*/
    public static Commit getBranchHead(String branch) {
        return CommitManager.getCommit(getBranchHeadSHA(branch));
    }


    /**return all branches except for active branch as an arrayList of String.
     * order does not matter.*/
    public static String[] getInactiveBranches() {
        String[] inActiveBranches = new String[_branchMap.size() - 1];
        int i = 0;
        for (String branch : _branchMap.keySet()) {
            if (!branch.equals(_activeBranch)) {
                inActiveBranches[i] = branch;
                i++;
            }
        }
        return inActiveBranches;
    }


    /**change active branch head to COMMITHASH, a SHA-1 reference to
     * a specific commit.
     */
    public static void setBranchHead(String commitHash) {
        _branchMap.put(_activeBranch, commitHash);
    }

    /**change active branch head to point to NEWHEADCOMMIT.
     */
    public static void setBranchHead(Commit newHeadCommit) {
        setBranchHead(newHeadCommit.getShaVal());
    }

    /**adds a new branch BRANCHNAME to branch mapping,
     * pointing to the current commit; does not activate
     * the branch.
     * Errors if branch with same name already exists.
     */
    public static void newBranch(String branchName) {
        if (branchExists(branchName)) {
            Utils.error("A branch with that name already exists.");
        }
        _branchMap.put(branchName, getActiveBranchHeadSHA());
    }

    /**adds a new branch BRANCHNAME to branch mapping,
     * pointing to commit with HEADSHA; does not activate
     * branch.
     */
    public static void newBranch(String branchName, String headSHA) {
        _branchMap.put(branchName, headSHA);
    }

    /**removes BRANCHNAME from branch mapping.
     * Errors if no branch with such name exist.
     */
    public static void delBranch(String branchName) {
        if (!branchExists(branchName)) {
            Utils.error("A branch with that name does not exist.");
        }
        if (branchName.equals(_activeBranch)) {
            Utils.error("Cannot remove the current branch.");
        }
        _branchMap.remove(branchName);
    }

    /**checks if the branch with name BRANCHNAME exists.
     * @return true if it exists and false otherwise.*/
    public static boolean branchExists(String branchName) {
        return _branchMap.containsKey(branchName);
    }

}
