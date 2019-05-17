package com.expedia.www.haystack.agent.demo;

import com.expedia.www.haystack.agent.core.Agent;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

import java.io.PrintStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class LetterPrinterAgent implements Agent {

    private volatile Thread printerThread;
    private volatile char letterToPrint;


    @Override
    public String getName() {
        return "LetterPrinterAgent";
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

    private void applyConfig(Config config) throws ConfigException {
        String letterString = config.getString("letter");
        if (letterString.length() != 1 || ! Character.isLetter(letterString.charAt(0))) {
            throw new ConfigException.BadValue("letter", "Invalid letter.");
        }
        letterToPrint = letterString.charAt(0);
    }

    private void createAndStarPrinterThread() {
        printerThread = new Thread(() -> {
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            while (! Thread.interrupted()) {
                LocalTime now = LocalTime.now();
                PrintStream systemOut = System.out;
                systemOut.println(now.format(timeFormatter) + " NumberPrinterAgent: " + letterToPrint);
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
