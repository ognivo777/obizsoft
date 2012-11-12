package ru.lanit.dibr.utils.gui;

import ru.lanit.dibr.utils.core.SshSource;
import ru.lanit.dibr.utils.gui.configuration.Host;
import ru.lanit.dibr.utils.gui.configuration.LogFile;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: VTaran
 * Date: 16.08.2010
 * Time: 17:57:48
 */
public class LogFrame  extends JFrame {

	private Thread t;
	private LogPanel panel;

	public LogFrame(final JButton b, final JComponent c, final Host host, final LogFile logFile) {
		setTitle(host.getDescription()+ " : " + logFile.getName());

		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        final LogPanel lp = new LogPanel(new SshSource(host, logFile), logFile.getBlockPattern());
		panel = lp;
		add(lp);
		t = new Thread() {
			@Override
			public void run() {
				try {
					lp.connect();
				} catch (Exception e) {
                    e.printStackTrace();
                    System.out.println(e);
					JOptionPane.showMessageDialog(LogFrame.this, "Can't open log '" + logFile.getPath() + " on '"+host.getHost() + "'!\n" + e.getMessage());
                    LogFrame.this.setVisible(false);
                    b.setBorder(new LineBorder(Color.RED));
                    b.setEnabled(false);
                    c.setEnabled(false);
				}
			}

			@Override
			public void interrupt() {
				lp.stop();
			}
		};
		t.start();

	}

	public void stop() {
		t.interrupt();
		setVisible(false);
	}

	public void setAutoScroll(boolean autoScroll) {
		panel.setAutoScroll(autoScroll);
	}
}
