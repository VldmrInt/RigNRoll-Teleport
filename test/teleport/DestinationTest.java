package teleport;

import rnrcore.vectorJ;
import rnrscenario.missions.map.Place;

public class DestinationTest {
    public static void main(String[] args) throws Exception {
        Place p = new Place();
        vectorJ coordinates = new vectorJ(123.5, -200, 8);
        p.init(Place.WAREHOUSE_POINT, "San_Francisco", coordinates);
        Destination d = new Destination(p);
        coordinates.x = 999;
        check(d.x == 123.5 && d.position().y == -200, "snapshot must not follow mutable game coordinates");
        check(d.matches("FRANCISCO", false) && d.matches("склад", false) && !d.matches("zzzz", false), "Russian filter");
        d.setDescription("Сан-Франциско", "Склад на северной окраине города");
        check(d.toString().contains("Сан-Франциско") && d.toString().contains("San_Francisco"), "localized title and stable ID");
        check(d.matches("СЕВЕРНОЙ", false) && d.matches("сан-франциско", false), "Russian search description and title");
        check(d.matches("warehouse", true) && !d.matches("northern", true), "English search description and type");
        check(d.description(false).contains("123.5") && d.hasGameDescription(), "description with coordinates");
        check(d.description(true).contains("Warehouse: loading and unloading jobs."), "English warehouse description");
        d.setDescription("San_Francisco", null);
        check(!d.hasGameDescription() && d.description(false).contains("Описание в игре отсутствует"), "Russian missing translation fallback");
        check(d.description(false).contains("погрузка") && d.description(true).contains("loading and unloading"), "bilingual fallback explains object type");
        check(d.toString(true).contains("Warehouse"), "English type name");
        check(Texts.get(true, "move").equals("Teleport") && Texts.get(false, "move").equals("Переместиться"), "interface strings");
        for (double invalid : new double[] { Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY }) {
            p.init(0, "bad", new vectorJ(0, invalid, 0));
            try { new Destination(p); throw new AssertionError("accepted non-finite position"); }
            catch (IllegalArgumentException expected) { }
        }
        p.init(0, " ", new vectorJ());
        try { new Destination(p); throw new AssertionError("accepted empty name"); }
        catch (IllegalArgumentException expected) { }
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                new TeleportWindow();
                java.awt.Frame[] frames = java.awt.Frame.getFrames();
                check(frames.length > 0, "GUI created");
                for (java.awt.Frame frame : frames) frame.dispose();
            }
        });
        System.out.println("PASS: coordinate validation, snapshot, localized descriptions, fallback, search, Java 5 GUI construction");
        System.exit(0);
    }
    private static void check(boolean ok, String label) { if (!ok) throw new AssertionError(label); }
}
