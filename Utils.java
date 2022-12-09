package gitlet;


import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Formatter;
import java.util.LinkedHashMap;
import java.util.List;

import static gitlet.Const.*;


/** Assorted utilities.
 *  @author P. N. Hilfinger
 */
public class Utils {

    /* SHA-1 HASH VALUES. */

    /** The length of a complete SHA-1 UID as a hexadecimal numeral. */
    static final int UID_LENGTH = 40;

    /** Returns the SHA-1 hash of the concatenation of VALS, which may
     *  be any mixture of byte arrays and Strings. */
    public static String sha1(Object... vals) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            for (Object val : vals) {
                if (val instanceof byte[]) {
                    md.update((byte[]) val);
                } else if (val instanceof String) {
                    md.update(((String) val).getBytes(StandardCharsets.UTF_8));
                } else {
                    throw new IllegalArgumentException("improper type to sha1");
                }
            }
            Formatter result = new Formatter();
            for (byte b : md.digest()) {
                result.format("%02x", b);
            }
            return result.toString();
        } catch (NoSuchAlgorithmException excp) {
            throw new IllegalArgumentException("System does not support SHA-1");
        }
    }

    /** Returns the SHA-1 hash of the concatenation of the strings in
     *  VALS. */
    static String sha1(List<Object> vals) {
        return sha1(vals.toArray(new Object[vals.size()]));
    }

    /** Returns the SHA-1 hash based on the contents inside the FILE.*/
    public static String sha1(File file) {
        return sha1(readContents(file));
    }

    /* FILE DELETION */

    /** Deletes FILE if it exists and is not a directory.  Returns true
     *  if FILE was deleted, and false otherwise.  Refuses to delete FILE
     *  and throws IllegalArgumentException unless the directory designated by
     *  FILE also contains a directory named .gitlet.
     *
     *  NOTE: can be usedful for removing files from Working directory*/
    public static boolean restrictedDelete(File file) {
        if (!(new File(file.getParentFile(), ".gitlet")).isDirectory()) {
            throw new IllegalArgumentException("not .gitlet working directory");
        }
        if (!file.isDirectory()) {
            return file.delete();
        } else {
            return false;
        }
    }

    /** Deletes the file named FILE if it exists and is not a directory.
     *  Returns true if FILE was deleted, and false otherwise.  Refuses
     *  to delete FILE and throws IllegalArgumentException unless the
     *  directory designated by FILE also contains a directory named .gitlet. */
    static boolean restrictedDelete(String file) {
        return restrictedDelete(new File(file));
    }

    /**simply deletes file named FILENAME is it exists in CWD. */
    public static void cwdDel(String fileName) {
        File toDel = new File(CWD, fileName);
        if (toDel.exists()) {
            toDel.delete();
        }
    }

    /**deletes all the files except directory in DIRECTORY. */
    public static void wipeDir(File directory) {
        if (!directory.isDirectory()) {
            System.out.println("Error: not a directory");
        }
        for (File file : directory.listFiles()) {
            if (!file.isDirectory()) {
                file.delete();
            }
        }
    }
    /* READING AND WRITING FILE CONTENTS */

    /** Return the entire contents of FILE as a byte array.  FILE must
     *  be a normal file.  Throws IllegalArgumentException
     *  in case of problems. */
    public static byte[] readContents(File file) {
        if (!file.isFile()) {
            throw new IllegalArgumentException("must be a normal file");
        }
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    /** Return the entire contents of FILE as a String.  FILE must
     *  be a normal file.  Throws IllegalArgumentException
     *  in case of problems. */
    public static String readContentsAsString(File file) {
        return new String(readContents(file), StandardCharsets.UTF_8);
    }

    /** Write the result of concatenating the bytes in CONTENTS to FILE,
     *  creating or overwriting it as needed.  Each object in CONTENTS may be
     *  either a String or a byte array.  Throws IllegalArgumentException
     *  in case of problems. */
    public static void writeContents(File file, Object... contents) {
        try {
            if (file.isDirectory()) {
                throw
                    new IllegalArgumentException("cannot overwrite directory");
            }
            BufferedOutputStream str =
                new BufferedOutputStream(Files.newOutputStream(file.toPath()));
            for (Object obj : contents) {
                if (obj instanceof byte[]) {
                    str.write((byte[]) obj);
                } else {
                    str.write(((String) obj).getBytes(StandardCharsets.UTF_8));
                }
            }
            str.close();
        } catch (IOException | ClassCastException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    /** Copy contents in ORG to DEST, if DEST does not exist,
     * create a new dest file.
     */
    public static void copyContents(File org, File dest) throws IOException {
        byte[] content = readContents(org);
        if (dest.exists()) {
            dest.createNewFile();
        }
        writeContents(dest, content);
    }

    /**copy all files from orgDir to DestDir, overwring files if
     * necessary.
     * @param orgDir directory to copy files from.
     * @param destDir directory files go to.
     */
    public static void copyAll(File orgDir, File destDir) throws IOException {
        List<String> fileNames = plainFilenamesIn(orgDir);
        for (String fileName : fileNames) {
            File orgFile = new File(orgDir, fileName);
            File destFile = new File(destDir, fileName);
            copyContents(orgFile, destFile);
        }
    }

    /** Return an object of type T read from FILE, casting it to EXPECTEDCLASS.
     *  Throws IllegalArgumentException in case of problems. */
    public static <T extends Serializable> T readObject(
            File file, Class<T> expectedClass) {
        try {
            ObjectInputStream in =
                new ObjectInputStream(new FileInputStream(file));
            T result = expectedClass.cast(in.readObject());
            in.close();
            return result;
        } catch (IOException | ClassCastException
                 | ClassNotFoundException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    /**return an object that's specifically a LinkedHashMap<String, String>,
     * by reading from FILE. Assuming a LinkedHashMap<String, String> is
     * stored in the file.
     */
    @SuppressWarnings("unchecked")
    public static LinkedHashMap<String, String> readLinkedHashMap(File file) {
        return (LinkedHashMap<String, String>)
                readObject(file, LinkedHashMap.class);
    }


    /** Write OBJ to FILE. */
    public static void writeObject(File file, Serializable obj) {
        writeContents(file, serialize(obj));
    }

    /* DIRECTORIES */

    /** Filter out all but plain files. */
    private static final FilenameFilter PLAIN_FILES =
        new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isFile();
            }
        };

    /** Returns a list of the names of all plain files in the directory DIR, in
     *  lexicographic order as Java Strings.  Returns null if DIR does
     *  not denote a directory. */
    public static List<String> plainFilenamesIn(File dir) {
        String[] files = dir.list(PLAIN_FILES);
        if (files == null) {
            return null;
        } else {
            Arrays.sort(files);
            return Arrays.asList(files);
        }
    }

    /** Returns a list of the names of all plain files in the directory DIR, in
     *  lexicographic order as Java Strings.  Returns null if DIR does
     *  not denote a directory. */
    static List<String> plainFilenamesIn(String dir) {
        return plainFilenamesIn(new File(dir));
    }

    /* OTHER FILE UTILITIES */

    /** Return the concatentation of FIRST and OTHERS into a File designator,
     *  analogous to the java.nio.file.Paths.get(String, String[])
     *  method. */
    static File join(String first, String... others) {
        return Paths.get(first, others).toFile();
    }

    /** Return the concatentation of FIRST and OTHERS into a File designator,
     *  analogous to the java.nio.file.Paths.get(String, String[])
     *  method. */
    static File join(File first, String... others) {
        return Paths.get(first.getPath(), others).toFile();
    }

    /**
     * return true iff the same key in both LinkedMap,
     * map to the same value.
     * return false iff the key does not exist in one map.
     * @param m1 first hash map.
     * @param m2 2nd hash map.
     * @param key key to be compared.
     */
    public static Boolean idMapping(LinkedHashMap m1,
                                    LinkedHashMap m2, String key) {
        if (m1.containsKey(key) && m2.containsKey(key)) {
            if (m1.get(key).equals(m2.get(key))) {
                return true;
            }
        }
        return false;
    }


    /* SERIALIZATION UTILITIES */

    /** Returns a byte array containing the serialized contents of OBJ. */
    static byte[] serialize(Serializable obj) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = new ObjectOutputStream(stream);
            objectStream.writeObject(obj);
            objectStream.close();
            return stream.toByteArray();
        } catch (IOException excp) {
            throw errorWithException("Internal error serializing commit.");
        }
    }


    /* MESSAGES AND ERROR REPORTING */
    /** Return a GitletException whose message
     * is composed from MSG and ARGS as
     *  for the String.format method. */
    public static GitletException errorWithException(
            String msg, Object... args) {
        return new GitletException(String.format(msg, args));
    }

    /** Print a message composed from MSG and ARGS as for the String.format
     *  method, followed by a newline. */
    static void message(String msg, Object... args) {
        System.out.printf(msg, args);
        System.out.println();
    }

    /**prints an error message composed from MSG and exist the program
     * with exit code 0.
     */
    public static void error(String msg) {
        System.out.println(msg);
        System.exit(0);
    }
}
