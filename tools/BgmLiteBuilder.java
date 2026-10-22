import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import touhou.BgmCatalog;

/** Offline arrangements for the original MLD channel layout; not run when packaging edited MIDIs. */
public final class BgmLiteBuilder {
    // Zero-based MIDI channels: melody, harmony, bass. Delayed/stacked copies are omitted.
    private static final int[][] LOOP = {
        null, { 0, 13, 12, 8 }, { 1, -1, 5, 8 },
        { 0, 1, 5, 8 }, { 0, -1, 2, 8 }, { 0, 5, 12, 8 },
        { 0, 4, 14, 8 }, { 3, 10, 14, 8 }, { 0, -1, 14, 7 },
        { 0, 3, 12, 8 }, { 0, 11, 13, 8 }, { 0, -1, 8, 7 },
        { 8, 11, 13, 7 }, { 1, 0, 4, 8 }, { 0, -1, 14, 8 },
        null, null, { 0, 1, 10, 8 }
    };
    private static final int[][] INTRO = {
        null, { -1, -1, 12, 8 }, { 0, -1, 11, 8 },
        { 3, -1, 5, 8 }, { 3, -1, 5, 8 }, { 5, -1, 10, 8 },
        { 12, -1, 14, 8 }, { 10, -1, 14, 8 }, { 0, -1, 14, 7 },
        { 0, -1, 12, 8 }, { 13, -1, -1, 8 }, null,
        { 10, -1, 13, 7 }, { 4, -1, 13, 8 }, { 0, -1, 4, 8 },
        null, null, { -1, -1, -1, 8 }
    };

    private static final class Note {
        long start;
        long end;
        int pitch;
        int velocity;
        Note(long start, int pitch, int velocity) {
            this.start = start;
            this.pitch = pitch;
            this.velocity = velocity;
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Usage: BgmLiteBuilder <res/bgm> <res/bgm-lite>");
        File source = new File(args[0]);
        File output = new File(args[1]);
        for (int song = 0; song < LOOP.length; song++) {
            if (!BgmCatalog.isBattleTrack(song)) continue;
            File single = new File(source, song + ".mid");
            if (single.isFile()) {
                arrange(single, new File(output, single.getName()), song, false);
            } else {
                for (String part : new String[] { "intro.mid", "loop.mid" }) {
                    arrange(new File(source, song + "/" + part), new File(output, song + "/" + part), song, part.equals("intro.mid"));
                }
            }
        }
    }

    private static boolean melody(int song, boolean intro, int channel, double beat) {
        int[] plan = intro ? INTRO[song] : LOOP[song];
        if (intro) return channel == plan[0] || channel == plan[1];
        // These arrangements hand the melody to another instrument between phrases.
        switch (song) {
            case 3: return channel == (beat < 32 ? 3 : beat < 96 ? 0 : 1);
            case 5: return channel == (beat >= 14 && beat < 144 ? 0 : 5);
            case 6: return channel == (beat < 62 ? 0 : beat < 128 ? 4 : 12);
            case 7: return channel == (beat < 160 ? 3 : 10);
            case 9: return channel == (beat < 209 ? 0 : 3);
            case 10: return channel == (beat < 31.5 ? 11 : 0);
            case 13: return channel == (beat >= 127 && beat < 256 ? 0 : 1);
            case 17: return channel == (beat < 64 ? 0 : 1);
            default: return channel == plan[0] || channel == plan[1];
        }
    }

    private static void arrange(File source, File output, int song, boolean intro) throws Exception {
        Sequence original = MidiSystem.getSequence(source);
        Sequence lite = new Sequence(original.getDivisionType(), original.getResolution());
        Track conductor = lite.createTrack();
        for (Track track : original.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                MidiMessage message = event.getMessage();
                if (message instanceof MetaMessage && ((MetaMessage) message).getType() != 0x2F) {
                    if (((MetaMessage) message).getType() != 3) conductor.add(copy(event));
                } else if (message.getStatus() == 0xF0 || message.getStatus() == 0xF7) {
                    conductor.add(copy(event));
                }
            }
        }
        finish(conductor, original.getTickLength());
        int[] plan = intro ? INTRO[song] : LOOP[song];
        int before = 0, after = 0;
        for (int channel = 0; channel < 16; channel++) {
            List<Note> notes = notes(original, channel);
            before += notes.size();
            List<Note> selected = new ArrayList<Note>();
            long[] drumAt = { -1000000, -1000000, -1000000, -1000000 };
            for (Note note : notes) {
                double beat = note.start / (double) original.getResolution();
                boolean lead = melody(song, intro, channel, beat);
                if (channel == 9) {
                    int family = drumFamily(note.pitch);
                    if (family < 0) continue;
                    long gap = original.getResolution() / (family == 2 ? 2 : 4);
                    if (family == 3 && note.start % (original.getResolution() * 4) > original.getResolution() / 16) continue;
                    if (note.start - drumAt[family] < gap) continue;
                    drumAt[family] = note.start;
                    note.end = Math.min(note.end, note.start + original.getResolution() / 4);
                } else if (!lead && channel != plan[2] && channel != plan[3]) {
                    continue;
                }
                selected.add(note);
            }
            if (selected.isEmpty()) continue;
            if (channel != 9) selected = limitVoices(selected, channel == plan[3] ? 1 : 2, channel == plan[3]);
            Track target = lite.createTrack();
            String role = channel == 9 ? "Drums" : channel == plan[3] ? "Bass" : channel == plan[2] ? "Harmony" : "Melody";
            MetaMessage title = new MetaMessage();
            byte[] name = (role + " (channel " + (channel + 1) + ")").getBytes("US-ASCII");
            title.setMessage(3, name, name.length);
            target.add(new MidiEvent(title, 0));
            for (Track track : original.getTracks()) {
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    if (!(event.getMessage() instanceof ShortMessage)) continue;
                    ShortMessage message = (ShortMessage) event.getMessage();
                    if (message.getChannel() != channel || message.getCommand() == 0x80 || message.getCommand() == 0x90) continue;
                    int cc = message.getData1();
                    if (message.getCommand() == 0xB0 && (cc == 10 || cc == 64 || cc == 91 || cc == 93)) continue;
                    target.add(copy(event));
                }
            }
            // A dry, centered mix also removes the source's dense stereo-pan automation.
            add(target, 0, 0xB0, channel, 10, 64);
            add(target, 0, 0xB0, channel, 64, 0);
            add(target, 0, 0xB0, channel, 91, 0);
            add(target, 0, 0xB0, channel, 93, 0);
            for (Note note : selected) {
                if (note.end <= note.start) continue;
                add(target, note.start, 0x90, channel, note.pitch, note.velocity);
                add(target, note.end, 0x80, channel, note.pitch, 0);
                after++;
            }
            finish(target, original.getTickLength());
        }
        output.getParentFile().mkdirs();
        MidiSystem.write(lite, 1, output);
        System.out.println(output + ": notes " + before + " -> " + after);
    }

    private static List<Note> notes(Sequence sequence, int channel) {
        List<Note> result = new ArrayList<Note>();
        List<long[]> pedals = new ArrayList<long[]>();
        long pedalStart = -1;
        List<ArrayDeque<Note>> held = new ArrayList<ArrayDeque<Note>>();
        for (int i = 0; i < 128; i++) held.add(new ArrayDeque<Note>());
        for (Track track : sequence.getTracks()) for (int i = 0; i < track.size(); i++) {
            MidiEvent event = track.get(i);
            if (!(event.getMessage() instanceof ShortMessage)) continue;
            ShortMessage message = (ShortMessage) event.getMessage();
            if (message.getChannel() != channel) continue;
            int command = message.getCommand(), pitch = message.getData1();
            if (command == 0x90 && message.getData2() > 0) {
                Note note = new Note(event.getTick(), pitch, message.getData2());
                note.end = sequence.getTickLength();
                held.get(pitch).add(note); result.add(note);
            } else if (command == 0x80 || command == 0x90) {
                if (!held.get(pitch).isEmpty()) held.get(pitch).remove().end = event.getTick();
            } else if (command == 0xB0 && pitch == 64) {
                if (message.getData2() >= 64 && pedalStart < 0) pedalStart = event.getTick();
                if (message.getData2() < 64 && pedalStart >= 0) {
                    pedals.add(new long[] { pedalStart, event.getTick() }); pedalStart = -1;
                }
            }
        }
        if (pedalStart >= 0) pedals.add(new long[] { pedalStart, sequence.getTickLength() });
        for (Note note : result) for (long[] pedal : pedals) {
            if (note.end >= pedal[0] && note.end < pedal[1]) { note.end = pedal[1]; break; }
        }
        Collections.sort(result, new Comparator<Note>() {
            public int compare(Note a, Note b) { return a.start == b.start ? b.pitch - a.pitch : Long.compare(a.start, b.start); }
        });
        return result;
    }

    private static List<Note> limitVoices(List<Note> source, int limit, boolean bass) {
        List<Note> result = new ArrayList<Note>(), active = new ArrayList<Note>();
        for (int i = 0; i < source.size();) {
            long tick = source.get(i).start;
            int end = i + 1;
            while (end < source.size() && source.get(end).start == tick) end++;
            for (int n = active.size() - 1; n >= 0; n--) if (active.get(n).end <= tick) active.remove(n);
            List<Note> chord = new ArrayList<Note>();
            for (int n = i; n < end && chord.size() < limit; n++) {
                Note note = source.get(bass ? end - 1 - (n - i) : n);
                // Identical note-ons are stacked unison copies, not another voice.
                if (chord.isEmpty() || chord.get(chord.size() - 1).pitch != note.pitch) chord.add(note);
            }
            for (int n = active.size() - 1; n >= 0; n--) {
                for (Note note : chord) {
                    if (active.get(n).pitch == note.pitch) {
                        active.remove(n).end = tick;
                        break;
                    }
                }
            }
            while (active.size() + chord.size() > limit) { active.remove(0).end = tick; }
            for (Note note : chord) {
                active.add(note); result.add(note);
            }
            i = end;
        }
        return result;
    }

    private static int drumFamily(int pitch) {
        if (pitch == 35 || pitch == 36) return 0;
        if (pitch == 38 || pitch == 40) return 1;
        if (pitch == 42 || pitch == 46) return 2;
        return pitch == 49 ? 3 : -1;
    }

    private static MidiEvent copy(MidiEvent event) { return new MidiEvent((MidiMessage) event.getMessage().clone(), event.getTick()); }
    private static void add(Track track, long tick, int command, int channel, int a, int b) throws Exception {
        ShortMessage message = new ShortMessage(); message.setMessage(command, channel, a, b); track.add(new MidiEvent(message, tick));
    }
    private static void finish(Track track, long tick) throws Exception {
        MetaMessage end = new MetaMessage(); end.setMessage(0x2F, new byte[0], 0); track.add(new MidiEvent(end, tick));
    }
}
