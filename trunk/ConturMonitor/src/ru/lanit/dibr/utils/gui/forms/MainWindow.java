package ru.lanit.dibr.utils.gui.forms;

import ru.lanit.dibr.utils.CmdLineConfiguration;
import ru.lanit.dibr.utils.Configuration;
import ru.lanit.dibr.utils.core.SshSource;
import ru.lanit.dibr.utils.core.TestSource;
import ru.lanit.dibr.utils.gui.FunctionPanel;
import ru.lanit.dibr.utils.gui.LogFrame;
import ru.lanit.dibr.utils.gui.LogPanel;
import ru.lanit.dibr.utils.gui.MenuButton;
import ru.lanit.dibr.utils.gui.configuration.Host;
import ru.lanit.dibr.utils.gui.configuration.LogFile;
import ru.lanit.dibr.utils.utils.FileDrop;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Vladimir
 * Date: 18.04.14
 * Time: 13:31
 */

public class MainWindow {
    private JFrame window;
    private JPanel rootPanel;
    private JTabbedPane tabbedPane1;
    private JPanel logList;
//    private Configuration configuration;

    public static int logsCnt = 0;
    public MainWindow(Configuration cfg) {
//        configuration = cfg;
        window = new JFrame();
        window.setTitle("Log monitor 3.9");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        window.add(rootPanel);
        window.setSize(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getSize());

        tabbedPane1.remove(0);

        logList.setLayout(new BoxLayout(logList, BoxLayout.Y_AXIS));
        for (Map.Entry<Host, Map<String, LogFile>> entry : cfg.getServers().entrySet()) {
            JPanel hostPane = new JPanel();
            hostPane.setLayout(new BoxLayout(hostPane, BoxLayout.Y_AXIS));
            Label hostLabel = new Label(entry.getKey().getDescription(), Label.CENTER);
            hostLabel.setFont(new Font("Courier", Font.BOLD, CmdLineConfiguration.fontSize+4));
            hostPane.add(hostLabel);
            JPanel buttons = new JPanel();
            GridBagLayout mgr = new GridBagLayout();
            buttons.setLayout(mgr);
            hostPane.add(buttons);
            for (Map.Entry<String, LogFile> logEntry : entry.getValue().entrySet()) {
                addButton(buttons,  logEntry.getValue(), entry.getKey());
                logsCnt++;
            }
            logList.add(hostPane);
        }
        window.setVisible(true);
    }

    private void addButton(JPanel buttons, final LogFile logFile, final Host host) {
        final JButton b = new JButton(logFile.getName());
        System.out.println(b.getFont());
        b.setFont(new Font("Courier", 0, CmdLineConfiguration.fontSize+2));
        b.setBorder(new LineBorder(Color.GRAY));
        final MenuButton menuButton = logFile.isLocal()? null : new MenuButton(host, logFile.getPath(), logFile.getName(), this, logFile.getBlockPattern());
        b.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                System.out.println(e.paramString());
                LogPanel lp = null;
                if(logFile.isLocal()) {
                    //TODO: реализовать нормальный Source для локальных файлов, используюя org.apache.commons.io.input.Tailer
                    //lp = new LogFrame(b, menuButton, logFile.getName(), new TestSource(logFile.getPath()), logFile.getBlockPattern());
                } else {
                    lp = new LogPanel(new SshSource(host, logFile), logFile.getBlockPattern());
                    createTab(lp, host.getDescription()+ " : " + logFile.getName());
                    new FileDrop(System.out, lp.getViewport().getView(), new FileDrop.Listener() {
                        @Override
                        public void filesDropped(File[] files) {
                            for (int i = 0; i < files.length; i++) {
                                createTab(new LogPanel(new TestSource(files[i].getAbsolutePath(), 0), logFile.getBlockPattern()), "[" + files[i].getName() + "]");
                            }
                        }
                    });
                }
                lp.getViewport().getView().requestFocusInWindow();
            }

        });

        GridBagConstraints gbc =  new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridx = 0;

        buttons.add(b, gbc);

        gbc.gridx++;
        if(menuButton!=null) {
            buttons.add(menuButton, gbc);
        }
    }

    public Component createTab(final LogPanel lp, String name) {
        JPanel contentPanel  = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.add(lp);
        contentPanel.add(new FunctionPanel(lp));

        new Thread() {
            @Override
            public void run() {
                try {
                    lp.connect();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        }.start();
        final Component newTab = tabbedPane1.add(name, contentPanel);
        tabbedPane1.setSelectedComponent(newTab);

        JPanel pnl = new JPanel();
        JLabel label = new JLabel(name + " ");
        label.setFont(new Font("Courier New", 0, CmdLineConfiguration.fontSize));
        pnl.add(label);
        ((FlowLayout)pnl.getLayout()).setVgap(0);
        ((FlowLayout)pnl.getLayout()).setHgap(0);
        JButton goAwayButton = new JButton("X");
        goAwayButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tabbedPane1.remove(newTab);
                lp.close();
            }
        });
        goAwayButton.setMargin(new Insets(0,0,0,0));
        int closeBtnSize = (int) (CmdLineConfiguration.fontSize * 1.2);
        goAwayButton.setPreferredSize(new Dimension(closeBtnSize, closeBtnSize));
        goAwayButton.setFont(new Font("Courier New", 0, CmdLineConfiguration.fontSize));
        pnl.setOpaque(false);
        pnl.add(goAwayButton);
        tabbedPane1.setTabComponentAt(tabbedPane1.getSelectedIndex(), pnl);

        return newTab;
    }

}
