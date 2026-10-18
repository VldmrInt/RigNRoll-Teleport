package teleport;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import players.Crew;
import players.actorveh;
import rnrcore.eng;
import rnrcore.vectorJ;
import rnrcore.teleport.ITeleported;
import rnrcore.teleport.MakeTeleport;
import rnrscenario.missions.MissionSystemInitializer;
import rnrscenario.missions.map.MissionsMap;
import rnrscenario.missions.map.Place;

public final class TeleportWindow {
    private final JFrame frame = new JFrame();
    private final DefaultListModel model = new DefaultListModel();
    private final JList list = new JList(model);
    private final JTextField search = new JTextField();
    private final JTextArea description = new JTextArea(6, 40);
    private final JButton refresh = new JButton();
    private final JButton move = new JButton();
    private final JLabel status = new JLabel();
    private final JLabel searchLabel = new JLabel();
    private final JComboBox language = new JComboBox(new String[] { "Русский", "English" });
    private ArrayList<Destination> destinations = new ArrayList<Destination>();
    private boolean busy;
    private volatile boolean english;

    public static void start(String args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { new TeleportWindow().frame.setVisible(true); }
        });
    }

    TeleportWindow() {
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) { frame.setState(JFrame.ICONIFIED); }
        });
        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.add(searchLabel, BorderLayout.WEST);
        JPanel searchRow = new JPanel(new BorderLayout(8, 8));
        searchRow.add(search, BorderLayout.CENTER);
        searchRow.add(language, BorderLayout.EAST);
        top.add(searchRow, BorderLayout.CENTER);
        top.add(refresh, BorderLayout.EAST);
        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.add(status, BorderLayout.CENTER);
        bottom.add(move, BorderLayout.SOUTH);
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.add(top, BorderLayout.NORTH);
        description.setEditable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setFont(list.getFont());
        description.getAccessibleContext().setAccessibleName(tr("desc_access"));
        JSplitPane content = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(list), new JScrollPane(description));
        content.setResizeWeight(0.7);
        panel.add(content, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        frame.setContentPane(panel);
        frame.setSize(850, 600);
        frame.setLocationByPlatform(true);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new DefaultListCellRenderer() {
            public java.awt.Component getListCellRendererComponent(JList owner, Object value,
                    int index, boolean selected, boolean focused) {
                super.getListCellRendererComponent(owner, value, index, selected, focused);
                setText(((Destination) value).toString(english));
                return this;
            }
        });
        updateLanguage();
        move.setEnabled(false);
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
        });
        list.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent e) {
                move.setEnabled(!busy && list.getSelectedValue() != null);
                Destination selected = (Destination) list.getSelectedValue();
                description.setText(selected == null ? tr("choose") : selected.description(english));
                description.setCaretPosition(0);
            }
        });
        refresh.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { work(null); }
        });
        language.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                english = language.getSelectedIndex() == 1;
                updateLanguage();
            }
        });
        move.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Destination d = (Destination) list.getSelectedValue();
                if (d != null) work(d);
            }
        });
    }

    private String tr(String key) { return Texts.get(english, key); }

    private void updateLanguage() {
        frame.setTitle(tr("title"));
        searchLabel.setText(tr("search"));
        refresh.setText(tr("refresh"));
        move.setText(tr("move"));
        search.getAccessibleContext().setAccessibleName(tr("search_access"));
        list.getAccessibleContext().setAccessibleName(tr("list_access"));
        description.getAccessibleContext().setAccessibleName(tr("desc_access"));
        if (destinations.isEmpty()) status.setText(tr("initial"));
        filter();
        Destination selected = (Destination) list.getSelectedValue();
        description.setText(selected == null ? tr("choose") : selected.description(english));
    }

    private void filter() {
        model.clear();
        for (Destination d : destinations) if (d.matches(search.getText().trim(), english)) model.addElement(d);
    }

    private void work(final Destination target) {
        if (busy) return;
        busy = true;
        move.setEnabled(false);
        refresh.setEnabled(false);
        status.setText(tr(target == null ? "loading" : "moving"));
        Thread worker = new Thread(new Runnable() {
            public void run() {
                String message;
                ArrayList<Destination> points = null;
                try {
                    if (target == null) {
                        points = readDestinations();
                        message = String.format(tr("count"), String.valueOf(points.size()));
                    } else {
                        teleport(target);
                        message = String.format(tr("done"), target.toString(english));
                    }
                } catch (Throwable failure) {
                    String problem = failure.getMessage();
                    message = String.format(tr("error"), failure instanceof IllegalStateException
                            ? tr(problem) : tr("internal_error"));
                    TeleportBootstrap.note("ERROR " + failure);
                }
                final String result = message;
                final ArrayList<Destination> loaded = points;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (loaded != null) { destinations = loaded; filter(); }
                        busy = false;
                        refresh.setEnabled(true);
                        move.setEnabled(list.getSelectedValue() != null);
                        status.setText(result);
                    }
                });
            }
        }, "Teleport-command");
        worker.setDaemon(true);
        worker.start();
    }

    static ArrayList<Destination> readDestinations() throws Exception {
        eng.lock();
        try {
            MissionsMap map = MissionSystemInitializer.getMissionsMap();
            if (map == null) throw new IllegalStateException("NO_MAP");
            // shortcut: this game exposes no iterator; update the field name for other scripts.jar versions.
            Field field = MissionsMap.class.getDeclaredField("places");
            field.setAccessible(true);
            Map places = (Map) field.get(map);
            ArrayList<Destination> result = new ArrayList<Destination>();
            for (Object value : places.values()) {
                try {
                    Destination point = new Destination((Place) value);
                    point.setDescription(rnrcore.loc.getMissionPointShortName(point.name),
                            rnrcore.loc.getMissionPointLongName(point.name));
                    result.add(point);
                }
                catch (IllegalArgumentException invalid) { TeleportBootstrap.note("INVALID_POINT " + invalid); }
            }
            Collections.sort(result);
            if (result.isEmpty()) throw new IllegalStateException("NO_POINTS");
            int described = 0;
            for (Destination point : result) if (point.hasGameDescription()) described++;
            TeleportBootstrap.note("POINTS " + result.size() + " described=" + described);
            return result;
        } finally { eng.unlock(); }
    }

    static void teleport(Destination selected) throws Exception {
        final CountDownLatch done = new CountDownLatch(1);
        actorveh car;
        Destination target;
        eng.lock();
        try {
            MissionsMap map = MissionSystemInitializer.getMissionsMap();
            Place current = map == null ? null : map.getPlace(selected.name);
            if (current == null) throw new IllegalStateException("STALE_POINT");
            target = new Destination(current);
            if (target.x != selected.x || target.y != selected.y || target.z != selected.z)
                throw new IllegalStateException("STALE_POINT");
            car = Crew.getIgrokCar();
            if (car == null) throw new IllegalStateException("NO_CAR");
            car.UpdateCar();
            if (car.getCar() == 0 || car.getCar() == -1) throw new IllegalStateException("CAR_UNAVAILABLE");
            vectorJ before = car.gPosition();
            TeleportBootstrap.note("REQUEST " + target.name + " from=" + before.x + "," + before.y + "," + before.z
                    + " to=" + target.x + "," + target.y + "," + target.z);
            car.sVeclocity(0.0);
            car.teleport(target.position());
            MakeTeleport.teleport(new ITeleported() {
                public void teleported() { done.countDown(); }
            });
        } finally { eng.unlock(); }
        if (!done.await(60, TimeUnit.SECONDS))
            throw new IllegalStateException("NO_CALLBACK");
        eng.lock();
        try {
            if (Crew.getIgrokCar() != car) throw new IllegalStateException("CAR_CHANGED");
            car.UpdateCar();
            vectorJ after = car.gPosition();
            double distance = Math.sqrt(Math.pow(after.x - target.x, 2) + Math.pow(after.y - target.y, 2));
            TeleportBootstrap.note("RESULT " + target.name + " actual=" + after.x + "," + after.y + "," + after.z + " distance=" + distance);
            if (Double.isNaN(distance) || distance > 150)
                throw new IllegalStateException("NOT_ARRIVED");
        } finally { eng.unlock(); }
    }
}
