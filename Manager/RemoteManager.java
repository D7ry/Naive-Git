package gitlet.Manager;

import gitlet.Commit;
import gitlet.Const;
import gitlet.Utils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**class managing remote operations.
 */
public class RemoteManager {

    /**a new remote manager managing remote named REMOTENAME.
     */
    public RemoteManager(String remoteName) {
        _remoteName = remoteName;
        String remoteDir = _remoteRepoMap.get(remoteName);
        if (remoteDir == null) {
            Utils.error("A remote with that name does not exist.");
        }
        _remoteDir = new File(remoteDir);
        if (!_remoteDir.exists()) {
            Utils.error("Remote directory not found.");
        }
        _remoteMetaDir = new File(_remoteDir, ".gitletMeta");
        _remoteCommitMetaDir = new File(_remoteDir, ".commitMeta");
        _remoteCommitDir = new File(_remoteCommitMetaDir, ".commits");
        _remoteBlobMapDir = new File(_remoteCommitMetaDir, ".blobMapping");
        _remoteBlobDir = new File(_remoteDir, ".blobs");
        _remoteActiveBranchFile = new File(_remoteMetaDir, ".activeBranch");
        _remoteActiveBranch = Utils.readObject(
                _remoteActiveBranchFile, String.class);
        _remoteBranchMapFile = new File(_remoteMetaDir, ".branchMAP");
        _remoteBranchMap = Utils.readLinkedHashMap(_remoteBranchMapFile);
    }

    /**pushes file to BRANCH of THIS remote.*/
    public void pushToRepo(String branch) throws IOException {
        String remoteHead = _remoteBranchMap.get(branch);
        if (remoteHead == null) {
            _remoteBranchMap.put(branch,
                    BranchManager.getActiveBranchHeadSHA());
        } else {
            tracePath(CommitManager.headCommit(),
                    remoteHead, new ArrayList<>());
            if (_commitPath == null) {
                Utils.error("Please pull down remote changes before pushing.");
            } else {
                for (Commit commit : _commitPath) {
                    File blob = commit.getCommitBlob();
                    File remoteBlob =
                            new File(_remoteCommitDir, commit.getShaVal());
                    Utils.copyContents(blob, remoteBlob);
                }
                _remoteBranchMap.put(_remoteActiveBranch,
                        CommitManager.headCommit().getShaVal());
            }
        }
        saveRemote();
    }


    /**copies all commits and blobs from the given BRANCH in
     * the remote repo that are not in the current repo.
     * create a new branch [remote name]/[remote branch name]
     * if it does not exist already.
     * */
    public void fetchFromRepo(String branch) throws IOException {
        if (!_remoteBranchMap.containsKey(branch)) {
            Utils.error("That remote does not have that branch.");
        }
        getAllRepo();
        String newBranchName = _remoteName + "/" + branch;
        BranchManager.newBranch(newBranchName, _remoteBranchMap.get(branch));
    }

    /**simply downloads the entire remote repository to local.
     */
    private void getAllRepo() throws IOException {
        Utils.copyAll(_remoteCommitDir, Const.COMMIT_DIR);
        Utils.copyAll(_remoteBlobMapDir, Const.BLOB_MAP_DIR);
        Utils.copyAll(_remoteBlobDir, Const.BLOB_DIR);
    }

    /**pull files from BRANCH in the remote repository. */
    public void pullFromRepo(String branch) throws IOException {
        fetchFromRepo(branch);
        String newBranchName = _remoteName + "/" + branch;
        MergeManager merger = new MergeManager(BranchManager.activeBranch(),
                newBranchName);
        merger.merge();
    }

    /**list of commits that leads from local branch head to
     * remote branch head, including local branch head.
     */
    private ArrayList<Commit> _commitPath;

    /**recursively back trace a commit's head commits
     * util it reaches a commit with right SHA value.
     * when it reaches the right commit, path of commits is saved
     * to _commitPath.
     * @param head beginning commit of the trace.
     * @param targetSHA SHA value of the traget commit.
     * @param path list of commits leading to the beginning of the trace.
     * */
    private void tracePath(Commit head, String targetSHA,
                              ArrayList<Commit> path) {
        path.add(head);
        if (targetSHA.equals(head.getParentSHA())
                || targetSHA.equals(head.getMergeParentSHA())) {
            _commitPath = path;
        } else {
            if (head.hasParent()) {
                tracePath(head.getParentCommit(), targetSHA, path);
            }
            if (head.hasMergeParent()) {
                tracePath(head.getMergeParentCommit(), targetSHA, path);
            }
        }
    }

    /**save changes made to remote to metadata blobs. */
    private void saveRemote() {
        Utils.writeObject(_remoteActiveBranchFile, _remoteActiveBranch);
        Utils.writeObject(_remoteBranchMapFile, _remoteBranchMap);
    }


    /**stores all remote repositories.
     * maps repository name to string representation of repository's
     * location.*/
    private static LinkedHashMap<String, String> _remoteRepoMap;

    /**return mapping of remote repositories. */
    public static LinkedHashMap<String, String> repoMapping() {
        return _remoteRepoMap;
    }
    /**initialize remote repository mapping, creating an empty map. */
    public static void initRemoteMap() {
        _remoteRepoMap = new LinkedHashMap<String, String>();
    }

    /**read remoteRepoMap from its metadata blob.*/
    public static void readRemoteMap() {
        _remoteRepoMap = Utils.readLinkedHashMap(Const.REMOTE_REPO_FILE);
    }

    /**add remote to this gitlet's remote mapping for future use.
     * @param remote name of remote.
     * @param dir string representation of the directory of remote.
     */
    public static void addRemote(String remote, String dir) {
        if (_remoteRepoMap.containsKey(remote)) {
            Utils.error("A remote with that name already exists.");
        } else {
            _remoteRepoMap.put(remote, dir);
        }
    }
    /**remote remote from this gitlet's remote mapping.
     * @param remote remote name.
     */
    public static void rmRemote(String remote) {
        if (_remoteRepoMap.containsKey(remote)) {
            _remoteRepoMap.remove(remote);
        } else {
            Utils.error("A remote with that name does not exist.");
        }
    }

    /** remote equivalent of GITLET_DIR.*/
    private File _remoteDir;

    /** remote equivalent of BLOB_DIR.*/
    private File _remoteBlobDir;

    /** remote equivalent of GITLET_META_DIR.*/
    private File _remoteMetaDir;

    /**remote equivalent of ACTIVE_BRANCH_FILE. */
    private File _remoteActiveBranchFile;

    /**remote equivalent of BRANCH_MAP_FILE. */
    private File _remoteBranchMapFile;

    /**remote equivalent of COMMIT_META_DIR. */
    private File _remoteCommitMetaDir;

    /**remote equivalent of COMMIT_DIR. */
    private File _remoteCommitDir;

    /**remote equivalent of BLOB_MAP_DIR. */
    private File _remoteBlobMapDir;

    /**remote equivalent of BRANCH_MAP_FILE. */
    private LinkedHashMap<String, String> _remoteBranchMap;

    /**remote equivalent of ACTIVE_BRANCH_FILE. */
    private String _remoteActiveBranch;

    /**name of remote manager currently manages.*/
    private String _remoteName;
}
