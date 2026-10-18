package teleport;

import rnrcore.vectorJ;
import rnrscenario.missions.map.Place;

final class Destination implements Comparable<Destination> {
    final String name;
    final int type;
    final double x, y, z;
    private String title = "";
    private String details = "";

    Destination(Place place) {
        name = place.getName();
        type = place.getTip();
        vectorJ p = place.getCoords();
        if (name == null || name.trim().length() == 0 || p == null
                || !finite(p.x) || !finite(p.y) || !finite(p.z))
            throw new IllegalArgumentException("Некорректная точка карты");
        x = p.x; y = p.y; z = p.z;
    }

    private static boolean finite(double n) {
        return !Double.isNaN(n) && !Double.isInfinite(n);
    }

    vectorJ position() { return new vectorJ(x, y, z); }
    void setDescription(String shortName, String longName) {
        title = clean(shortName);
        details = clean(longName);
    }

    private String clean(String text) {
        if (text == null) return "";
        text = text.trim();
        return text.equals(name) ? "" : text;
    }

    boolean hasGameDescription() { return details.length() > 0; }

    String description(boolean en) {
        String purpose = Texts.get(en, type == Place.BAR_POINT ? "bar"
                : type == Place.OFFICE_POINT ? "office" : type == Place.MOTEL_POINT ? "motel"
                : type == Place.WAREHOUSE_POINT ? "warehouse" : type == Place.REPAIR_POINT ? "repair"
                : type == Place.GAS_POINT ? "gas" : type == Place.SCENARIO_POINT ? "scenario"
                : type == Place.MISSION_POINT ? "mission" : "unknown");
        String gameTitle = en && hasCyrillic(title) ? "" : title;
        String gameDetails = en && hasCyrillic(details) ? Texts.get(true, "game_text") : details;
        if (gameDetails.length() == 0) gameDetails = Texts.get(en, "no_description");
        return (gameTitle.length() > 0 ? gameTitle + "\n" : "") + gameDetails
                + "\n" + purpose + "\n" + Texts.get(en, "code") + " " + name
                + "\n" + Texts.get(en, "coordinates") + " X=" + round(x) + ", Y=" + round(y) + ", Z=" + round(z);
    }

    private static boolean hasCyrillic(String text) {
        for (int i = 0; i < text.length(); ++i)
            if (text.charAt(i) >= '\u0400' && text.charAt(i) <= '\u04ff') return true;
        return false;
    }

    private static double round(double value) { return Math.round(value * 100.0) / 100.0; }

    boolean matches(String query, boolean en) {
        return (toString(en) + " " + description(en)).toLowerCase(java.util.Locale.ENGLISH)
                .contains(query.toLowerCase(java.util.Locale.ENGLISH));
    }
    public int compareTo(Destination other) { return name.compareToIgnoreCase(other.name); }

    public String toString() { return toString(false); }

    String toString(boolean en) {
        String kind = Texts.get(en, type == Place.BAR_POINT ? "bar_type"
                : type == Place.OFFICE_POINT ? "office_type" : type == Place.MOTEL_POINT ? "motel_type"
                : type == Place.WAREHOUSE_POINT ? "warehouse_type" : type == Place.REPAIR_POINT ? "repair_type"
                : type == Place.GAS_POINT ? "gas_type" : type == Place.SCENARIO_POINT ? "scenario_type"
                : type == Place.MISSION_POINT ? "mission_type" : "point_type");
        String shownTitle = en && hasCyrillic(title) ? "" : title;
        return kind + " — " + (shownTitle.length() > 0 ? shownTitle + " [" + name + "]" : name);
    }
}
