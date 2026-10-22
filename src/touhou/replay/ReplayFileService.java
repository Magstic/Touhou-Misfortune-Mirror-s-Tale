package touhou.replay;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;

/** JSR-75 access stays separate from screens on devices without FileConnection. */
public final class ReplayFileService {
    private ReplayFileService() {
    }

    public static String[] listEntries(String path, boolean includeReplayFiles) throws IOException {
        Vector names = new Vector();
        if (path.length() == 0) {
            collectEntries(FileSystemRegistry.listRoots(), names, false);
        } else {
            FileConnection directory = (FileConnection) Connector.open("file:///" + path, Connector.READ);
            try {
                collectEntries(directory.list(), names, includeReplayFiles);
            } finally {
                directory.close();
            }
        }
        String[] result = new String[names.size()];
        names.copyInto(result);
        return result;
    }

    private static void collectEntries(Enumeration entries, Vector names, boolean includeReplayFiles) {
        while (entries.hasMoreElements()) {
            String name = (String) entries.nextElement();
            if (!name.endsWith("/") && !(includeReplayFiles && name.toLowerCase().endsWith(".rep"))) continue;
            int i = 0;
            while (i < names.size() && compareNames(name, (String) names.elementAt(i)) > 0) i++;
            names.insertElementAt(name, i);
        }
    }

    private static int compareNames(String a, String b) {
        boolean aDirectory = a.endsWith("/");
        if (aDirectory != b.endsWith("/")) return aDirectory ? -1 : 1;
        return a.compareTo(b);
    }

    public static ReplaySlotData readReplay(String path) throws IOException {
        FileConnection file = (FileConnection) Connector.open("file:///" + path, Connector.READ);
        try {
            long size = file.fileSize();
            if (size <= 0 || size > ReplayShareCodec.MAX_PACKET_SIZE) return null;
            byte[] packet = new byte[(int) size];
            DataInputStream stream = file.openDataInputStream();
            try {
                stream.readFully(packet);
            } finally {
                stream.close();
            }
            return ReplayShareCodec.decode(packet);
        } finally {
            file.close();
        }
    }

    public static String exportSlot(int slot, String directory) throws IOException {
        byte[] packet = ReplayShareService.exportSlotPacket(slot);
        if (packet == null) throw new IOException("Replay unavailable");

        String base = "THMMT_" + (slot + 1);
        for (int copy = 0; ; copy++) {
            String name = base + (copy == 0 ? "" : "_" + copy) + ".rep";
            FileConnection file = (FileConnection) Connector.open("file:///" + directory + name, Connector.READ_WRITE);
            boolean created = false;
            boolean complete = false;
            try {
                if (file.exists()) continue;
                file.create();
                created = true;
                OutputStream stream = file.openOutputStream();
                try {
                    stream.write(packet);
                    stream.flush();
                } finally {
                    stream.close();
                }
                complete = true;
                return directory + name;
            } finally {
                try {
                    if (created && !complete) file.delete();
                } finally {
                    file.close();
                }
            }
        }
    }
}
