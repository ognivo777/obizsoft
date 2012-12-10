package ru.lanit.dibr.utils.core;

import com.jcraft.jsch.*;
import ru.lanit.dibr.utils.gui.configuration.Host;
import ru.lanit.dibr.utils.gui.configuration.LogFile;
import ru.lanit.dibr.utils.utils.MyUserInfo;
import ru.lanit.dibr.utils.utils.SshUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Vova
 * Date: 13.11.12
 * Time: 2:12
 */
public class SshSource implements LogSource {

    private boolean isClosed = false;
    private boolean paused = false;
    List<String> buffer = new ArrayList<String>();

    int readedLines = 0;
    //    StringBuffer buffer = new StringBuffer();
    private Host host;
    private LogFile logFile;
    BufferedReader reader = null;
    ChannelExec channel = null;
    Session session = null;

    public SshSource(Host host, LogFile logFile) {
        this.host = host;
        this.logFile = logFile;
    }

    public void startRead() throws Exception {
        checkClosed();
        JSch jsch = new JSch();
        session = jsch.getSession(host.getUser(), host.getHost(), host.getPort());
        if (host.getProxyHost() != null) {
            Proxy proxy = null;
            if (host.getProxyType().equals(Host.HTTP)) {
                proxy = new ProxyHTTP(host.getProxyHost(), host.getProxyPrort());
            } else if (host.getProxyType().equals(Host.SOCKS4)) {
                proxy = new ProxySOCKS4(host.getProxyHost(), host.getProxyPrort());
            } else if (host.getProxyType().equals(Host.SOCKS5)) {
                proxy = new ProxySOCKS4(host.getProxyHost(), host.getProxyPrort());
            } else {
                throw new Exception("Unknown proxy type! Please use one of following: '" + Host.HTTP + "'; '" + Host.SOCKS4 + "'; " + Host.SOCKS5 + "'; ");
            }
            //proxy.
//            Proxy proxy = new ProxySOCKS4(host.getHost(), host.getPort());
            session.setProxy(proxy);
        }
        session.setConfig("StrictHostKeyChecking", "no"); //��������� ����������� ����� �� ��������
        //������ ������
        session.setConfig("compression.s2c", "zlib@openssh.com,zlib,none");
        session.setConfig("compression.c2s", "zlib@openssh.com,zlib,none");
        session.setConfig("compression_level", "9");

        if (host.getPem() != null) {
            jsch.addIdentity(host.getPem());
        } else {
            UserInfo ui = new MyUserInfo(host.getPassword());
            session.setUserInfo(ui);
        }

        session.connect(30000);   // making a connection with timeout.
        channel = (ChannelExec) session.openChannel("exec");
        String linesCount = SshUtil.exec(host, "wc -l " + logFile.getPath() + " | awk \"{print $1}\"").getData().trim();
        System.out.println("Lines count in log file: " + linesCount);
        channel.setCommand("tail -5000f " + logFile.getPath());
        //channel.setCommand("tail -c +0 -f " + logFile.getPath()); //��� ����� ��������� ���� ����
        reader = new BufferedReader(new InputStreamReader(channel.getInputStream(), host.getDefaultEncoding()));
        channel.connect(3 * 1000);


        Thread readThread = new Thread(new Runnable() {
            public void run() {
                String nextLine;
                try {
                    while ((nextLine = reader.readLine()) != null && !isClosed) {

                        buffer.add(String.format("%6d: %s", (buffer.size()+1), nextLine));
                    }
                } catch (IOException e) {
                    try {
                        close();
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                    e.printStackTrace();
                }
            }
        });

        readThread.start();

    }

    private void checkClosed() {
        if (isClosed) {
            throw new RuntimeException("Reader is closed");
        }
    }

    public String readLine() throws IOException {
        try {
            while (paused) {
                System.out.println("I'm asleep..");
                Thread.sleep(200);
            }
            if (buffer.size() > readedLines) {
                return buffer.get(readedLines++);
            } else {
                Thread.sleep(200);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return SKIP_LINE;
    }

    public void reset() {
        readedLines = 0;
        //reader.reset();
    }

    public void reloadFull() throws Exception {
        if (channel != null) {
            channel.sendSignal("KILL");
            channel.setCommand("cat " + logFile.getPath());
        }
    }

    public void close() throws Exception {
        isClosed = true;
        if (reader != null)
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        if (channel != null && channel.isConnected()) {
            channel.disconnect();
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    public void setPaused(boolean paused) {
        System.out.println("set paused: " + paused);
        this.paused = paused;
    }

    public boolean isPaused() {
        return paused;
    }
}
