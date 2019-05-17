package com.expedia.www.haystack.agent.demo;

import com.expedia.www.haystack.agent.core.Agent;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

import java.io.PrintStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class NumberPrinterAgent implements Agent {

    private volatile Thread printerThread;
    private volatile int numberToPrint;


    @Override
    public String getName() {
        return "NumberPrinterAgent";
    }

    @Override
    public void initialize(Config config) throws ConfigException {
        applyConfig(config);
        createAndStarPrinterThread();
    }

    @Override
    public void reconfigure(Config newConfig) throws ConfigException {
        applyConfig(newConfig);
    }

    @Override
    public void close() {
        printerThread.interrupt();
    }

    private void applyConfig(Config config) throws ConfigException  {
        numberToPrint = config.getInt("number");
    }

    private void createAndStarPrinterThread() {
        printerThread = new Thread(() -> {
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            while (! Thread.interrupted()) {
                LocalTime now = LocalTime.now();
                PrintStream systemOut = System.out;
                systemOut.println(now.format(timeFormatter) + " NumberPrinterAgent: " + numberToPrint);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException exception) {
                    break;
                }
            }
        });
        printerThread.start();
    }

}
