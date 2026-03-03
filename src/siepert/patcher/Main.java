package siepert.patcher;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            consoleMode();
        } else {
            if ("--generateHash".equalsIgnoreCase(args[0])) {
                if (args.length != 2) {
                    System.out.println("--generateHash <file>");
                    return;
                }
                String name = args[1];
                File target = new File(name);
                if (!target.isFile()) {
                    System.err.println("File not found");
                    return;
                }
                byte[] hash = new byte[256];
                byte[] packet = new byte[4096];
                byte pointer = 0;
                System.out.println("Target file has " + target.length() + " bytes");
                try (InputStream is = new FileInputStream(target)) {
                    int n;
                    while ((n = is.read(packet)) != -1) {
                        System.out.println("processing " + n + " bytes");
                        pointer = applyHash(pointer, hash, packet, n);
                    }
                } catch (Throwable e) {
                    e.printStackTrace(System.err);
                    return;
                }
                System.out.println("Produced hash:");
                System.out.println(Arrays.toString(hash));
                File hashed = new File(name + "_hash.bin");
                try {
                    if (hashed.exists()) hashed.delete();
                    if (!hashed.createNewFile()) {
                        throw new IOException("could not create hash file");
                    }
                    try (OutputStream os = new FileOutputStream(hashed)) {
                        os.write(hash);
                    }
                } catch (Throwable e) {
                    e.printStackTrace(System.err);
                    return;
                }
                System.out.println("Written hash to " + hashed.getName());
            }
            if ("--generateDiff".equalsIgnoreCase(args[0])) {
                if (args.length != 3) {
                    System.out.println("--generateDiff <original> <modified>");
                    return;
                }
                System.out.println("Warning: generating difference may take up large amounts of memory depending on the target files!");

                File original = new File(args[1]);
                File modified = new File(args[2]);
                if (!original.isFile()) {
                    System.err.println("Original file not found");
                    return;
                }
                if (!modified.isFile()) {
                    System.err.println("Modified file not found");
                    return;
                }
                if (original.length() > modified.length()) {
                    System.err.println("Currently does not support having the modified file being smaller than the original!");
                    return;
                }
                File result = new File(args[2] + ".patcher");
                if (result.exists()) result.delete();
                int log;
                try {
                    result.createNewFile();

                    try (
                            FileInputStream originalIn = new FileInputStream(original);
                            FileInputStream modifiedIn = new FileInputStream(modified);
                            FileOutputStream resultOut = new FileOutputStream(result);
                    ) {
                        List<Byte> bytesOriginal = new ArrayList<>(Math.toIntExact(original.length()));
                        List<Byte> bytesModified = new ArrayList<>(Math.toIntExact(modified.length()));
                        byte[] packet = new byte[4096];
                        int i, n;
                        while ((n = originalIn.read(packet)) != -1) {
                            for (i = 0; i < n; i++) {
                                bytesOriginal.add(packet[i]);
                            }
                        }
                        while ((n = modifiedIn.read(packet)) != -1) {
                            for (i = 0; i < n; i++) {
                                bytesModified.add(packet[i]);
                            }
                        }
                        for (i = 0; i < bytesOriginal.size(); i++) {
                            resultOut.write(bytesOriginal.get(i) ^ bytesModified.get(i));
                        }
                        for (i = bytesOriginal.size(); i < bytesModified.size(); i++) {
                            resultOut.write(bytesModified.get(i));
                        }
                        log = bytesModified.size();
                    }
                } catch (IOException e) {
                    e.printStackTrace(System.err);
                    return;
                }
                System.out.println("Written .patcher file (" + log + " bytes)");
            }
        }
    }

    private static byte applyHash(byte pointer, byte[] hash, byte[] data, int n) {
        int uPointer;
        for (int i = 0; i < n; i++) {
            uPointer = Byte.toUnsignedInt(pointer);
            hash[uPointer] ^= data[i];
            pointer = hash[uPointer];
        }
        return pointer;
    }
    private static byte[] generateHash(InputStream data) throws IOException {
        byte[] hash = new byte[256];
        byte[] packet = new byte[4096];
        byte pointer = 0;
        int n;
        while ((n = data.read(packet)) != -1) {
            pointer = applyHash(pointer, hash, packet, n);
        }
        return hash;
    }

    private static File target = null;
    private static ZipFile loadedZip = null;
    private static void consoleMode() {
        Scanner scanner = new Scanner(System.in);

        String cmd;
        do {
            System.out.print(">>>");
            cmd = scanner.nextLine();
            processCmd(cmd.trim());
        } while (!"exit".equalsIgnoreCase(cmd));

        if (loadedZip != null) {
            try {
                loadedZip.close();
            } catch (IOException e) {
                System.err.println("Failed to cleanup patcher file: ");
                e.printStackTrace(System.err);
            }
        }

        System.out.print("Press [ENTER] to continue...");
    }
    private static void processCmd(String cmd) {
        if ("exit".equalsIgnoreCase(cmd)) return;
        if (cmd.isEmpty()) return;
        String[] params = cmd.split(" ");
        System.out.println(Arrays.toString(params));
        if ("load".equalsIgnoreCase(params[0])) {
            if (params.length < 2) {
                System.err.println("usage:");
                System.err.println("load <zipfile>");
                return;
            }
            String path = params[1];
            File file = new File(path);
            if (!file.exists() || !file.isFile()) {
                System.err.println("File " + path + " not found!");
                return;
            }
            try {
                loadedZip = new ZipFile(file);
            } catch (Throwable e) {
                System.err.println("Failed to open patcher file:");
                e.printStackTrace(System.err);
            }
            System.out.println("Successfully opened patcher file");
            return;
        }
        if ("target".equalsIgnoreCase(params[0])) {
            if (params.length < 2) {
                System.err.println("usage:");
                System.err.println("target <file>");
                return;
            }
            String path = params[1];
            File file = new File(path);
            if (!file.exists() || !file.isFile()) {
                System.err.println("File " + path + " not found!");
                return;
            }
            target = file;
            System.out.println("Successfully set target file");
            return;
        }
        if ("hash".equalsIgnoreCase(params[0])) {
            if (checkLoadedAndTarget()) return;
            ZipEntry hash = loadedZip.getEntry("hash.bin");
            if (hash == null) {
                System.out.println("Provided file patcher has no hash, good luck!");
                return;
            }
            byte[] checkedHash = new byte[256];
            try (InputStream is = loadedZip.getInputStream(hash)) {
                if (is.read(checkedHash) != 256) {
                    throw new IOException("Could not read 256 bytes in one go");
                }
            } catch (IOException e) {
                e.printStackTrace(System.err);
                return;
            }
            byte[] comparingHash;
            try (InputStream is = new FileInputStream(target)) {
                comparingHash = generateHash(is);
            } catch (IOException e) {
                e.printStackTrace(System.err);
                return;
            }
            if (comparingHash.length < 256) {
                System.err.println("File hash returned " + comparingHash.length + " bytes, should be 256??");
                return;
            }

            System.out.println("Comparing hashes...");
            if (Arrays.equals(checkedHash, comparingHash)) {
                System.out.println("Hashes are equal!");
            } else {
                System.out.println("The target file hash does not match the bundles hash; are you targetting the correct  file?");
            }
            return;
        }
        if ("apply".equalsIgnoreCase(params[0])) {
            if (params.length != 2) {
                System.out.println("apply <destination>");
            }
            if (checkLoadedAndTarget()) return;
            ZipEntry patcher = loadedZip.getEntry("diff.patcher");
            if (patcher == null) {
                System.err.println("Patcher file missing!?");
                return;
            }
            File destination = new File(params[1]);
            if (destination.exists()) destination.delete();
            try {
                destination.createNewFile();
            } catch (Throwable e) {
                e.printStackTrace(System.err);
                return;
            }
            System.out.println("Warning: patching may take up large amounts of memory depending on the target files!");
            try (
                    InputStream patcherIn = loadedZip.getInputStream(patcher);
                    InputStream originalIn = new FileInputStream(target);
                    OutputStream modifiedOut = new FileOutputStream(destination)
            ) {
                List<Byte> bytesOriginal = new ArrayList<>(Math.toIntExact(target.length()));
                List<Byte> bytesPatcher = new ArrayList<>(Math.toIntExact(patcher.getSize()));
                byte[] packet = new byte[4096];
                int i, n;
                while ((n = originalIn.read(packet)) != -1) {
                    for (i = 0; i < n; i++) {
                        bytesOriginal.add(packet[i]);
                    }
                }
                while ((n = patcherIn.read(packet)) != -1) {
                    for (i = 0; i < n; i++) {
                        bytesPatcher.add(packet[i]);
                    }
                }
                for (i = 0; i < bytesOriginal.size(); i++) {
                    modifiedOut.write(bytesOriginal.get(i) ^ bytesPatcher.get(i));
                }
                for (i = bytesOriginal.size(); i < bytesPatcher.size(); i++) {
                    modifiedOut.write(bytesPatcher.get(i));
                }
            } catch (Throwable e) {
                e.printStackTrace(System.err);
            }
            System.out.println("Successfully applied patcher to file!");
        }
        if ("test".equalsIgnoreCase(params[0])) {
            ZipEntry entry = loadedZip.getEntry(params[1]);
            System.out.println(entry);
        }
    }

    private static boolean checkLoadedAndTarget() {
        if (loadedZip == null) {
            System.err.println("No patcher file loaded");
            return true;
        }
        if (target == null) {
            System.err.println("No target file selected");
            return true;
        }
        return false;
    }
}
