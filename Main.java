package gitlet;


import gitlet.Manager.BranchManager;
import gitlet.Manager.CheckoutManager;
import gitlet.Manager.CommitManager;
import gitlet.Manager.InfoManager;
import gitlet.Manager.MergeManager;
import gitlet.Manager.RemoteManager;
import gitlet.Manager.StageManager;

import java.io.IOException;

import static gitlet.Const.*;

/** Driver class for gitlet, the teeny tiny version-control system.
 *  main class handles input detection and persistence.
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws IOException {
        if (args.length == 0) {
            Utils.error("Please enter a command.");
        }
        _input = args;
        if (_input[0].equals("init")) {
            initGitlet();
        } else {
            readGitlet();
            processInput();
        }
        saveGitlet();
    }

    /**process the input ARGS and operate accordingly. */
    static void processInput() throws IOException {
        switch (_input[0]) {
        case "add": add(_input[1]); break;
        case "commit": commit(); break;
        case "rm": remove(_input[1]); break;
        case "log": log(); break;
        case "global-log": logAll(); break;
        case "find": find(_input[1]); break;
        case "status": status(); break;
        case "checkout": checkout(_input); break;
        case "branch": branchInit(_input[1]); break;
        case "rm-branch": branchDel(_input[1]); break;
        case "reset": reset(_input[1]); break;
        case "merge": merge(_input[1]); break;
        case "add-remote": addRemote(_input[1], _input[2]); break;
        case "rm-remote": rmRemote(_input[1]); break;
        case "push": pushToRemote(_input[1], _input[2]); break;
        case "fetch": fetchFromRemote(_input[1], _input[2]); break;
        case "pull": pullFromRemote(_input[1], _input[2]); break;
        default: noMatchingInput(); break;
        }
    }

    /** get the gitlet for the current directory stored as a serialized
     * file inside .gitlet subdirectory.*/
    public static void readGitlet() {
        if (!GITLET_DIR.exists()) {
            Utils.error("Not in an initialized Gitlet directory.");
        }
        StageManager.getAddStageMapFromFile();
        StageManager.getRmStageMapFromFile();
        BranchManager.getBranchMapFromFile();
        BranchManager.getActiveBranchFromFile();
        RemoteManager.readRemoteMap();
    }

    /**initialize a Gitlet in the CWR through following steps:
     * 1. creates all necessary directories.
     * 2. creates all necessary metadatas.
     * 3. create an initial commit and an initial branch.
     * */
    public static void initGitlet() throws IOException {
        checkOperandNum(0);
        if (GITLET_DIR.exists()) {
            Utils.error("A Gitlet version-control system "
                    + "already exists in the current directory.");
        }
        GITLET_DIR.mkdir();
        COMMIT_META_DIR.mkdir();
        COMMIT_DIR.mkdir();
        GITLET_META_DIR.mkdir();
        BLOB_MAP_DIR.mkdir();
        BLOB_DIR.mkdir();
        ACTIVE_BRANCH_FILE.createNewFile();
        BRANCH_MAP_FILE.createNewFile();
        REMOTE_REPO_FILE.createNewFile();
        StageManager.initStage();
        Commit initCommit = new Commit();
        BranchManager.initBranchManager(initCommit);
        RemoteManager.initRemoteMap();
    }

    /**add a file to the staging area to be committed.
     * @param fileName name of the file to be added. */
    public static void add(String fileName) throws IOException {
        checkOperandNum(1);
        StageManager.stageAdd(fileName);
    }

    /**make a gitlet commit.
     * */
    public static void commit() throws IOException {
        checkOperandNum(1);
        CommitManager.commit(_input[1]);
    }

    /**remove the file from git.
     * @param fileName name of the file to be removed.
     */
    public static void remove(String fileName) {
        checkOperandNum(1);
        StageManager.stageRemove(fileName);
    }

    /** display log of each commit back to the initial commit
     * iff there's a merge, trace the 1st parent commit.
     */
    public static void log() {
        checkOperandNum(0);
        InfoManager.log();
    }

    /** display info about all commits regardless of ordering.*/
    public static void logAll() {
        checkOperandNum(0);
        InfoManager.logAll();
    }

    /** print the ID of commit(s) with matching commit message.
     * @param msg commit message.
     * */
    public static void find(String msg) {
        checkOperandNum(1);
        InfoManager.find(msg);
    }

    /** display the status.
     * */
    public static void status() {
        checkOperandNum(0);
        InfoManager.status();
    }

    /** general checkout command.
     * errors are handled by subroutines separately.
     * @param args arguments to be passed in.*/
    public static void checkout(String[] args) throws IOException {
        switch (args.length) {
        case 2 : CheckoutManager.checkoutBranch(args[1]); break;
        case 3 :
            if (args[1].equals("--")) {
                CheckoutManager.checkoutFileInHead(args[2]); break;
            } else {
                wrongOperandInput(); break;
            }
        case 4 :
            if (args[2].equals("--")) {
                CheckoutManager.checkoutFileInCommit(args[1], args[3]); break;
            } else {
                wrongOperandInput(); break;
            }
        default : wrongOperandInput();
        }
    }

    /**creates a new branch pointing at the current head node.
     * @param branchName name of the branch to be added.
     * */
    public static void branchInit(String branchName) {
        checkOperandNum(1);
        BranchManager.newBranch(branchName);
    }

    /**delete the branch.
     * @param branchName name of the branch to be deleted.
     */
    public static void branchDel(String branchName) {
        checkOperandNum(1);
        BranchManager.delBranch(branchName);
    }

    /**a reset command.
     * @param shortCommitSHA SHA-1 value of commit to checkout.
     */
    public static void reset(String shortCommitSHA)
            throws IOException {
        checkOperandNum(1);
        CheckoutManager.reset(shortCommitSHA);
    }

    /**merge given branch with current branch.
     * @param branchName name of the branch to be merged.
     * */
    public static void merge(String branchName) throws IOException {
        checkOperandNum(1);
        MergeManager merger =
                new MergeManager(BranchManager.activeBranch(), branchName);
        merger.merge();
    }

    /**add DIRECTORY as one of this Gitlet's remote repo.
     * @param remoteName name of the remote repo.
     * @param remoteDirStr string representation of remote directory.
     *                     assuming it contains .gitlet.
     */
    public static void addRemote(String remoteName, String remoteDirStr) {
        checkOperandNum(2);
        RemoteManager.addRemote(remoteName, remoteDirStr);
    }

    /**remove REMOTENAME as one of this Gitlet's remote repo.
     * errors if remote is not one of Gitlet's remote repo.
     */
    public static void rmRemote(String remoteName) {
        checkOperandNum(1);
        RemoteManager.rmRemote(remoteName);
    }

    /**Attempts to append the current branch's commits
     * to the end of the given branch at the given remote.
     * @param remoteStr name of the remote repo.
     * @param branchStr string representation of the remote branch to append on.
     */
    public static void pushToRemote(String remoteStr, String branchStr)
            throws IOException {
        checkOperandNum(2);
        RemoteManager manager = new RemoteManager(remoteStr);
        manager.pushToRepo(branchStr);
    }

    /**Brings down commits from the remote Gitlet repository
     * into the local Gitlet repository.
     * @param remoteStr name of remote repo.
     * @param branchStr string representation of the remote branch to fetch.
     */
    public static void fetchFromRemote(String remoteStr, String branchStr)
            throws IOException {
        checkOperandNum(2);
        RemoteManager manager = new RemoteManager(remoteStr);
        manager.fetchFromRepo(branchStr);
    }

    /**Fetch branch from remote and merges fetch into current branch.
     * @param remoteStr name of remote repo.
     * @param branchStr string representation of the remote branch to pull.
     */
    public static void pullFromRemote(String remoteStr, String branchStr)
            throws IOException {
        checkOperandNum(2);
        RemoteManager manager = new RemoteManager(remoteStr);
        manager.pullFromRepo(branchStr);
    }


    /**save the current changes.*/
    public static void saveGitlet() {
        StageManager.saveAddStageMapToFile(ADD_STG_FILE);
        StageManager.saveRmStageMapToFile(RM_STG_FILE);
        Utils.writeObject(BRANCH_MAP_FILE, BranchManager.branchMap());
        Utils.writeObject(ACTIVE_BRANCH_FILE, BranchManager.activeBranch());
        Utils.writeObject(REMOTE_REPO_FILE, RemoteManager.repoMapping());
    }


    /**when no matching command exists.*/
    public static void noMatchingInput() {
        Utils.error("No command with that name exists.");
    }

    /**checks if the number of operand of input ARGS is LENGTH
     * iff not, call wrongOperandInput.
     * @param operandNum number of operands there are supposed to
     * be from INPUT.*/
    public static void checkOperandNum(int operandNum) {
        if (_input.length - 1 != operandNum) {
            wrongOperandInput();
        }
    }

    /**when the num or format of operand is wrong for
     * the corresponding input. */
    public static void wrongOperandInput() {
        Utils.error("Incorrect operands.");
    }

    /**input from user.*/
    private static String[] _input;

}
