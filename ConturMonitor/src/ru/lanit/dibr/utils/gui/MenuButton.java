package ru.lanit.dibr.utils.gui;

import ru.lanit.dibr.utils.core.AbstractHost;
import ru.lanit.dibr.utils.core.SimpleLocalFileSource;
import ru.lanit.dibr.utils.gui.configuration.SshHost;
import ru.lanit.dibr.utils.gui.forms.MainWindow;
import ru.lanit.dibr.utils.utils.ScpUtils;
import ru.lanit.dibr.utils.utils.SshUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

import com.jcraft.jsch.JSchException;

/**
 * User: Vova
 * Date: 07.09.2010
 * Time: 23:47:43
 */
public class MenuButton extends JButton {
    public MenuButton(final AbstractHost host, final String file, final String fileDescription, final MainWindow mainWindow, final String blockPattern) {
        setText("...");
        setPreferredSize(new Dimension(15,15));

        final JPopupMenu opts = new JPopupMenu();
        opts.setInvoker(MenuButton.this);

        JMenuItem menuItem;
        // Меню ======== СОХРАНИТЬ ============
        //TODO ПЕРЕПИСАТЬ host на AbstractHost!!! Логику скачивания полного файла унести в хост или сорс!
        if(host instanceof SshHost) {
            menuItem = new JMenuItem("Сохранить весь файл");
            menuItem.addActionListener(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    saveFile(e, (SshHost) host, file, fileDescription, false);

                }
            });
            opts.add(menuItem);
            menuItem = new JMenuItem("Сохранить весь файл и открыть в табе");
            menuItem.addActionListener(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    String fileName = saveFile(e, (SshHost) host, file, fileDescription, true);
                    mainWindow.createTab(new LogPanel(new SimpleLocalFileSource(fileName, 0), blockPattern), host.getDescription() + ": [saved] " + file);
                }
            });
            opts.add(menuItem);
        }

        //  Показ меню
        addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
                if(opts.isVisible()) {
                    opts.setVisible(false);
                    //opts.updateUI();
                } else {
                    opts.show(MenuButton.this, 10, 10);
                }
            }
        });
    }

    private String saveFile(ActionEvent e, SshHost host, String file, String fileDescription, boolean forcePlain) {
        System.out.println(e);
        String savedFileName = null;
        try {
            // `basename /smartpaas-app/was/node1/WAShome/profiles/AppSrv01/logs/server1/stopServer.log`.` date +%Y%m%d%H%M%S -r /smartpaas-app/was/node1/WAShome/profiles/AppSrv01/logs/server1/stopServer.log`.zip
            String tmpDir = SshUtil.exec(host, "if [ -z \"$TMPDIR\" ]; then echo \"/tmp\"; else echo $TMPDIR; fi;", null).getData().trim();
            if(forcePlain) {
                savedFileName = savePlain(host, file, fileDescription);
            } else if(SshUtil.exec(host, "zip --help", null).getStatusCode()==0) {
                //Use ZIP
                String zipFileName = SshUtil.exec(host, "echo " + tmpDir + "/`basename "+file+"`.`date +%Y%m%d%H%M%S -r " + file +"`.zip ", null).getData().trim();
                System.out.println("zipFileName: " + zipFileName);
                SshUtil.ExecResult execResult = SshUtil.exec(host, "zip -j -9 " + zipFileName + " " + file, null);
                if(!(execResult.getData().contains("error") || execResult.getStatusCode()!=0)) {
                    savedFileName = ScpUtils.getFile(host, zipFileName, host.getDescription() + "_" + fileDescription, null);
                    SshUtil.exec(host, "rm " + zipFileName, null);
                    JOptionPane.showMessageDialog(this, "Файл \n'" + file + "'\nс хоста\n'" + host.getHost() + "'\nзаархивирован и сохранён в катлоге программы как\n'" + savedFileName + "'.");
                } else {
                    savedFileName = savePlain(host, file, fileDescription);
                }

            } else if(SshUtil.exec(host, "gzip --help", null).getStatusCode()==0) {
                //gzip
                String gzipFileName = SshUtil.exec(host, "echo " + tmpDir + "/`basename "+file+"`.`date +%Y%m%d%H%M%S -r " + file +"`.gz ", null).getData().trim();
                System.out.println("gzipFileName: " + gzipFileName);
                SshUtil.ExecResult execResult = SshUtil.exec(host, "gzip -c9 " + file + " > " + gzipFileName, null);
                if(!(execResult.getData().contains("error") || execResult.getStatusCode()!=0)) {
                    savedFileName = ScpUtils.getFile(host, gzipFileName, host.getDescription()+"_"+fileDescription, null);
                    SshUtil.exec(host, "rm " + gzipFileName, null);
                    JOptionPane.showMessageDialog(this, "Файл \n'"+file+"'\nс хоста\n'"+host.getHost()+"'\nзаархивирован и сохранён в катлоге программы как\n'"+savedFileName+"'.");
                } else {
                    savedFileName = savePlain(host, file, fileDescription);
                }
            }  else {
                //Plain
                savedFileName = savePlain(host, file, fileDescription);
            }
        } catch (JSchException e1) {
            e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e1) {
            e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return savedFileName;
    }

    private String savePlain(SshHost host, String file, String fileDescription) throws JSchException, IOException {
        String savedFileName;
        savedFileName = ScpUtils.getFile(host, file, host.getDescription() + "_" + fileDescription, null);
        JOptionPane.showMessageDialog(this, "Файл \n'" + file + "'\nс хоста\n'" + host.getHost() + "'\nсохранён в катлоге программы как\n'" + savedFileName + "'.");
        return savedFileName;
    }
}

