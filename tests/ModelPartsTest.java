package com.voice.studio;
import java.io.*;
import java.util.*;
public class ModelPartsTest {
    static int assertions, opened, closed, live, peak;
    static void check(boolean ok, String message) { assertions++; if (!ok) throw new AssertionError(message); }
    static ModelParts.Source part(String name, String data) {
        return new ModelParts.Source() {
            public String name() { return name; }
            public InputStream open() throws Exception {
                opened++; live++; peak = Math.max(peak, live);
                return new ByteArrayInputStream(data.getBytes("UTF-8")) {
                    boolean ended;
                    public void close() throws IOException { if (!ended) { ended = true; closed++; live--; } super.close(); }
                };
            }
        };
    }
    static String content(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] bytes = new byte[3]; int n;
        while ((n = in.read(bytes)) != -1) out.write(bytes,0,n);
        return out.toString("UTF-8");
    }
    static void rejects(ModelParts.Source... sources) throws Exception {
        int before = opened; boolean failed = false;
        try (InputStream in = ModelParts.open(Arrays.asList(sources))) { content(in); }
        catch (IOException e) { failed = true; }
        check(failed, "Bad volume selection accepted"); check(opened == before, "Validation opened files too early");
    }
    public static void main(String[] args) throws Exception {
        try (InputStream in = ModelParts.open(Arrays.asList(part("model.part02 (1).bin","B"), part("model (1).part01.bin","A")))) {
            check(opened == 0, "Should open lazily"); check(content(in).equals("AB"), "Downloaded suffix and shuffled selection");
        }
        check(live == 0 && opened == closed && peak == 1, "Descriptor leak");
        List<ModelParts.Source> ten = new ArrayList<>();
        for (int n = 10; n >= 1; n--) ten.add(part("volume.part" + n, Integer.toString(n)+","));
        try (InputStream in = ModelParts.open(ten)) { check(content(in).equals("1,2,3,4,5,6,7,8,9,10,"), "Numeric order"); }
        rejects(part("model.part01", "A"));
        rejects(part("model.part01", "A"), part("model.part03", "C"));
        rejects(part("model.part01", "A"), part("model.part01 (1)", "A"));
        rejects(part("clone.part01", "A"), part("omni.part02", "B"));
        rejects(part("model.zip", "A"), part("model.part02", "B"));
        rejects(part("model.partx", "A"));
        rejects();
        try (InputStream in = ModelParts.open(Collections.singletonList(part("model (2).zip", "ZIP")))) { check(content(in).equals("ZIP"), "Full ZIP"); }
        InputStream early = ModelParts.open(Arrays.asList(part("model.part01","A"),part("model.part02","B")));
        int before = opened; check(early.read() == 'A', "First volume"); early.close();
        check(opened == before+1 && live == 0, "Early close must not open unused volumes");
        boolean failed = false; try { early.read(); } catch (IOException e) { failed = true; }
        check(failed, "Read after close");
        try (InputStream in = ModelParts.open(Arrays.asList(part("model.part01",""),part("model.part02","OK")))) { check(content(in).equals("OK"), "Empty volume"); }
        check(live == 0 && opened == closed && peak == 1, "Final descriptor leak");
        System.out.println(assertions + " assertions passed: volume ordering, download names, missing/mixed/duplicate volumes, lazy reads and closing.");
    }
}
