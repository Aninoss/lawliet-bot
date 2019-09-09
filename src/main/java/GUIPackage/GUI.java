package GUIPackage;

import CommandSupporters.CommandContainer;
import General.EmbedFactory;
import General.RunningCommands.RunningCommandManager;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.time.Instant;

import org.javacord.api.entity.server.Server;

public class GUI extends JFrame {
    private JPanel pnMain;
    private JTabbedPane tabbedPane1;
    private JTextField txConsoleInput;
    private JButton btConsoleSend;
    private JTextArea txConsole;
    private JTextArea txConsoleErrors;
    private JProgressBar prTraffic;
    private JProgressBar prMemory;
    private JLabel lbTraffic;
    private JLabel lbMemory;
    private JLabel lbServers;
    private JLabel lbTrackers;
    private JLabel lbVotes;
    private JLabel lbThreads;
    private JLabel lbCommands;
    private JTable tbLog;
    private JProgressBar prMaxMemory;
    private JLabel lbMaxMemory;
    private JButton btLogClear;
    private JButton btConsoleClear;
    private JButton btErrorClear;
    private JLabel lbRunning;
    private static GUI ourInstance = new GUI();

    private DiscordApi api;
    private PipedOutputStream consoleOut;
    private double maxMemory;

    public GUI() {
        maxMemory = 0;
    }

    public static GUI getInstance() {
        return ourInstance;
    }

    public void start() {
        setContentPane(pnMain);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);
        setTitle("Lawliet");
        setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);

        PrintStream printStreamConsole = new PrintStream(new CustomOutputStream(txConsole, 10000));
        PrintStream printStreamError = new PrintStream(new CustomOutputStream(txConsoleErrors, 50000));
        System.setOut(printStreamConsole);
        System.setErr(printStreamError);

        PipedInputStream pis = new PipedInputStream();
        try {
            consoleOut = new PipedOutputStream(pis);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.setIn(pis);

        onLogClear();

        btConsoleSend.addActionListener(e -> onConsoleSend());
        txConsoleInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                if (e.getKeyChar() == '\n') onConsoleSend();
            }
        });

        btConsoleClear.addActionListener(e -> onTextAreaClear(txConsole));
        btErrorClear.addActionListener(e -> onTextAreaClear(txConsoleErrors));
        btLogClear.addActionListener(e -> onLogClear());

        startStatsTracker();
    }

    private void onConsoleSend() {
        System.out.println(txConsoleInput.getText());
        byte[] bytes = (txConsoleInput.getText() + "\r\n").getBytes();
        try {
            consoleOut.write(bytes);
            consoleOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        txConsoleInput.setText("");
    }

    public void setTraffic(double gb) {
        prTraffic.setValue((int) (gb * 1000));
        lbTraffic.setText(String.format("%1$.2f", gb) + " / 40 GB");
    }

    public void startStatsTracker() {
        new Thread(() -> {
            while(true) {
                setStats();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void setStats() {
        //Memory
        double memoryTotal = Runtime.getRuntime().totalMemory() / (1024.0 * 1024.0);
        double memoryUsed = memoryTotal - (Runtime.getRuntime().freeMemory() / (1024.0 * 1024.0));

        prMemory.setMaximum((int) memoryTotal);
        prMemory.setValue((int) memoryUsed);
        lbMemory.setText(String.format("%1$.2f", memoryUsed) + " / " + String.format("%1$.2f", memoryTotal) + " MB");


        //Max Memory
        maxMemory = Math.max(maxMemory, memoryUsed);

        prMaxMemory.setMaximum((int) memoryTotal);
        prMaxMemory.setValue((int) maxMemory);
        lbMaxMemory.setText(String.format("%1$.2f", maxMemory) + " / " + String.format("%1$.2f", memoryTotal) + " MB");


        //Threads
        lbThreads.setText(String.valueOf(Thread.getAllStackTraces().keySet().size()));


        //Activities
        lbCommands.setText(String.valueOf(CommandContainer.getInstance().getActivitiesSize()));


        //Running Commands
        lbRunning.setText(String.valueOf(RunningCommandManager.getInstance().getRunningCommands().size()));
    }

    public void setBotStats(int servers, int trackers, int surveyVotes) {
        lbServers.setText(String.valueOf(servers));
        lbTrackers.setText(String.valueOf(trackers));
        lbVotes.setText(String.valueOf(surveyVotes));
    }

    public void addLog(Server server, User user, String content) {
        String dateString = Instant.now().toString().replace("T", " ").replace("Z", "");
        String serverString = server.getName() + " (" + server.getIdAsString() + ")";
        String userString = user.getName() + " (" + user.getIdAsString() + ")";

        DefaultTableModel model = (DefaultTableModel) tbLog.getModel();
        model.addRow(new Object[]{dateString, serverString, userString, content});

        if (model.getRowCount() > 300) model.removeRow(0);
    }

    public void onLogClear() {
        String[] col = {"Date", "ServerStuff/Server", "User", "Message"};
        String[][] data = new String[0][4];
        DefaultTableModel model = new DefaultTableModel(data,col);
        tbLog.setModel(model);
        tbLog.setAutoCreateRowSorter(true);
    }

    public void onTextAreaClear(JTextArea jTextArea) {
        jTextArea.setText("");
    }

    public void setApi(DiscordApi api) {
        this.api = api;
    }
}
