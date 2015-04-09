package ru.lanit.dibr.utils.core;

import com.jcraft.jsch.*;
import ru.lanit.dibr.utils.gui.configuration.SshHost;
import ru.lanit.dibr.utils.gui.configuration.LogFile;
import ru.lanit.dibr.utils.utils.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * User: Vova
 * Date: 13.11.12
 * Time: 2:12
 */
public class SshSource implements LogSource {

    private boolean isClosed = false;
    private boolean paused = false;
    private boolean writeLineNumbers = false;
    private Thread readThread;

    List<String> buffer;

    int readedLines;
    private SshHost host;
    private LogFile logFile;
    BufferedReader reader = null;
    ChannelExec channel = null;
    Session session = null;

    public SshSource(SshHost host, LogFile logFile) {
        this.host = host;
        this.logFile = logFile;
    }

    public void startRead() throws Exception {
        //checkClosed();
        readedLines = 0;
        paused = false;
        buffer = new ArrayList<String>();
        session = host.connect(debugOutput);
        isClosed = false;
        channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand("tail -1000f " + logFile.getPath());
        //channel.setCommand("tail -c +0 -f " + logFile.getPath()); //Так можно загрузить весь файл
        reader = new BufferedReader(new InputStreamReader(channel.getInputStream(), host.getDefaultEncoding()));
        Utils.writeToDebugQueue(debugOutput, "Starting tailing '" + logFile.getName() + "' for host '" + host.getDescription() + "'..");
        channel.connect(30000);
        Utils.writeToDebugQueue(debugOutput, "Tailing '" + logFile.getName() + "' for host '" + host.getDescription() + "' are started.");

        readThread = new Thread(new Runnable() {
            public void run() {
                String nextLine;
                try {
                    while ((nextLine = reader.readLine()) != null && !isClosed) {
//                        buffer.add(String.format("%6d: %s", (buffer.size()+1), nextLine));
                        buffer.add(nextLine);
                    }
                } catch (IOException e) {
                    try {
                        close();
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                    e.printStackTrace();
                }
                System.out.println("Stopped SSH read thread.");
            }
        });

        new Thread(new Runnable() {
            public void run() {
                while(channel.isConnected() && !isClosed && (host.getTunnel()==null || host.getTunnel().isConnectionAlive())) {
                    try {
                        if(host.getTunnel()!=null) {
                            if(!host.getTunnel().isConnectionAlive()) {
                                System.out.println("Tunnel are disconnected!");
                                close();
                                break;
                            }
                        }
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if(!isClosed) {
                    System.out.println("Connection failed!");
                    readThread.interrupt();
                    try {
                        close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Stopped SSH connection monitor thread.");
                }

            }
        }, "Connection monitor").start();
        readThread.start();

    }

    public String readLine() throws IOException {
        try {
            while (paused && !isClosed) {
                System.out.println("I'm asleep..");
                Thread.sleep(10);
            }
            if(isClosed) {
                throw new IOException("Connection lost.");
            }
            if (buffer.size() > readedLines) {
                if(writeLineNumbers) {
                    return String.format("%6d: %s", readedLines + 1, buffer.get(readedLines++));
                } else {
                    return buffer.get(readedLines++);
                }
            } else {
                Thread.sleep(10);
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
        if(isClosed) {
            System.out.println("SSH source already closed!");
            return;
        }
        System.out.println("Closing SSH log source..");
        isClosed = true;
        readThread.interrupt();
        if (reader != null)
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        if (channel != null && channel.isConnected()) {
            System.out.println("try to disconnect SSH channel");
            channel.disconnect();
            System.out.println("SSH channel disconnected");
        }
        if (session != null && session.isConnected()) {
            System.out.println("try to disconnect SSH session");
            session.disconnect();
            System.out.println("SSH session disconnected");
        }
        buffer.clear();
    }

    public void setPaused(boolean paused) {
        System.out.println("set paused: " + paused);
        this.paused = paused;
    }

    public boolean isWriteLineNumbers() {
        return writeLineNumbers;
    }

    public void setWriteLineNumbers(boolean writeLineNumbers) {
        this.writeLineNumbers = writeLineNumbers;
    }

    public boolean isPaused() {
        return paused;
    }

    @Override
    public String getName() {
        return host.getHost()+logFile.getPath()+logFile.getName();
    }

    @Override
    public BlockingQueue<String> getDebugOutput() {
        return debugOutput;
    }

}
