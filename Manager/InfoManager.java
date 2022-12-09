package gitlet.Manager;

import gitlet.Commit;
import gitlet.Utils;

import java.io.File;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import static gitlet.Const.*;

/**helper class that manages outputting information to the user during runtime.
 * as well as providing useful utility methods for quick extraction of info. */
public class InfoManager {
    /**prints out a series of logs starting from
     * the current commit's parent, back to the initial commit.
     */
    public static void log() {
        CommitManager.headCommit().traceLog();
    }

    /**returns a list of SHA-1 values of all commits ever made. */
    public static List<String> getAllCommitSHA() {
        return Utils.plainFilenamesIn(COMMIT_DIR);
    }

    /**return a list of names of all plain files in DIR. */
    public static List<String> getDirFileLst(File dir) {
        return Utils.plainFilenamesIn(dir);
    }

    /**prints out logs for all commits ever made.
     */
    public static void logAll() {
        List<String> commitSHAlst = getAllCommitSHA();
        for (String commitSHA : commitSHAlst) {
            CommitManager.getCommit(commitSHA).printLog();
        }
    }

    /**prints out SHA-1's for all commits with commit message identical
     * to MSG.
     */
    public static void find(String msg) {
        boolean foundMatch = false;
        List<String> commitSHAlst = getAllCommitSHA();
        for (String commitSHA : commitSHAlst) {
            Commit commit = CommitManager.getCommit(commitSHA);
            if (commit.getMsg().equals(msg)) {
                ptln(commit.getShaVal());
                foundMatch = true;
            }
        }
        if (!foundMatch) {
            Utils.error("Found no commit with that message.");
        }
    }

    /** Returns a timestamp corresponding to
     * current time suitable for Gitlet. */
    public static String gitletTime() {
        GregorianCalendar now = new GregorianCalendar();
        String timeZone =
                timeZoneFormatter(
                        now.get(Calendar.ZONE_OFFSET) / HOUR_TO_MILISEC);
        String date = "Date: "
                + weekdays[now.get(Calendar.DAY_OF_WEEK)]
                + " "
                + months[now.get(Calendar.MONTH)]
                + " "
                + twoDigitFormatter(now.get(Calendar.DAY_OF_MONTH))
                + " "
                + twoDigitFormatter(now.get(Calendar.HOUR_OF_DAY))
                + ":"
                + twoDigitFormatter(now.get(Calendar.MINUTE))
                + ":"
                + twoDigitFormatter(now.get(Calendar.SECOND))
                + " "
                + now.get(Calendar.YEAR)
                + " "
                + timeZone;
        return date;
    }

    /**return time zone formatted as the course demanded,
     * based on UNPROCESSEDTZ. */
    static String timeZoneFormatter(int unprocessedTZ) {
        boolean negativeTimeZone = unprocessedTZ < 0;
        String timeZoneStr = Integer.toString(Math.abs(unprocessedTZ));
        String timeZone1;
        if (timeZoneStr.length() == 2) {
            timeZone1 = timeZoneStr;
        } else {
            timeZone1 = "0" + timeZoneStr;
        }
        timeZone1 += "00";
        if (negativeTimeZone) {
            return "-" + timeZone1;
        } else {
            return "+" + timeZone1;
        }
    }

    /**return minute, hour, and second as the course demanded,
     * based on TIME.*/
    static String twoDigitFormatter(int time) {
        if (Integer.toString(time).length() == 1) {
            return "0" + time;
        }
        return Integer.toString(time);
    }

    /** array of months.*/
    private static String[] months =  {
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };
    /** array of weekdays.*/
    private static String[] weekdays = {
        "Poggers", "Sun", "Mon", "Tue", "Wed", "Thu",
        "Fri", "Sat"
    };

    /**Displays what branches currently exist,
     * and marks the current branch with a *.
     * Also displays what files have been staged for addition or removal. */
    public static void status() {
        ptln("=== Branches ===");
        branchStatus();
        ptln("");
        ptln("=== Staged Files ===");
        stagedFileStatus();
        ptln("");
        ptln("=== Removed Files ===");
        removedFileStatus();
        ptln("");
        ptln("=== Modifications Not Staged For Commit ===");
        unstagedModificationStatus();
        ptln("");
        ptln("=== Untracked Files ===");
        untrackedStatus();
        ptln("");
    }
    /**prints status of branch.*/
    static void branchStatus() {
        ptln("*" + BranchManager.activeBranch());
        String[] branchLst = lexiSort(BranchManager.getInactiveBranches());
        for (int i = 0; i < branchLst.length; i++) {
            ptln(branchLst[i]);
        }
    }
    /**prints status of files staged for addition.*/
    static void stagedFileStatus() {
        String[] addStageLst = lexiSort(StageManager.fileToAdd());
        for (int i = 0; i < addStageLst.length; i++) {
            ptln(addStageLst[i]);
        }
    }
    /**prints status of files staged for removal.*/
    static void removedFileStatus() {
        String[] rmStageLst = lexiSort(StageManager.fileToRemove());
        for (int i = 0; i < rmStageLst.length; i++) {
            ptln(rmStageLst[i]);
        }
    }
    /**prints status of files in CWD that are modified yet not staged.
     * a file is modified but not staged iff file is:
     * staged for addition but file in CWD is different.
     * staged for addition but file in CWD is deleted.
     * tracked in the current commit, changed in CWD, not staged for add.
     * tracked in the current commit, removed in CWD, not staged for removal. */
    static void unstagedModificationStatus() {
        LinkedHashMap<String, String> currMap =
                CommitManager.headCommit().getBlobMapping();
        List<String> cwdFiles = getDirFileLst(CWD);
        for (String fileName : StageManager.addStageMap().keySet()) {
            if (!cwdFiles.contains(fileName)) {
                pt(fileName);
                ptln(" (deleted)");
            } else {
                File file = new File(CWD, fileName);
                String blobHash = StageManager.addStageMap().get(fileName);
                String fileHash = Utils.sha1(file);
                if (!fileHash.equals(blobHash)) {
                    pt(fileName);
                    ptln(" (modified)");
                }
            }
        }
        for (String fileName : currMap.keySet()) {
            if (!cwdFiles.contains(fileName)) {
                if (!StageManager.rmStageMap().containsKey(fileName)) {
                    pt(fileName);
                    ptln(" (deleted)");
                }
            } else {
                File file = new File(CWD, fileName);
                if (!StageManager.addStageMap().containsKey(fileName)
                        && !Utils.sha1(file).equals(currMap.get(fileName))) {
                    pt(fileName);
                    ptln(" (modified)");
                }
            }
        }
    }
    /**prints status of files that are untracked.
     * the narrow definition:
     * a file is "untracked" iff it's untracked
     * by broad definition, and not staged for addition.*/
    static void untrackedStatus() {
        for (String fileName : untrackedInCWD()) {
            if (!StageManager.addStageMap().containsKey(fileName)) {
                ptln(fileName);
            }
        }
    }

    /**return true iff FILENAME file is tracked by the
     * head commit. return false otherwise.
     */
    static boolean fileTracked(String fileName) {
        LinkedHashMap currMap = CommitManager.headCommit().getBlobMapping();
        return currMap.containsKey(fileName);
    }

    /**return a list of untracked file names in CWD.*/
    static List<String> untrackedInCWD() {
        List<String> cwdFiles = getDirFileLst(CWD);
        List<String> untrackedFiles = new ArrayList<String>();
        for (String fileName : cwdFiles) {
            if (!fileTracked(fileName)) {
                untrackedFiles.add(fileName);
            }
        }
        return untrackedFiles;
    }

    /**sort INPUT in lexicographical order and return sorted INPUT.*/
    static String[] lexiSort(String[] input) {
        Arrays.sort(input);
        return input;
    }

    /**shorthand for System.out.println.
     * @param obj object to print.*/
    static void ptln(Object obj) {
        System.out.println(obj);
    }

    /**shorthand for System.out.print.
     * @param obj object to print.*/
    static void pt(Object obj) {
        System.out.print(obj);
    }

}
