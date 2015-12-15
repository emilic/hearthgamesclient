package com.hearthgames.client.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class SystemTraySupportedJFrame extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(SystemTraySupportedJFrame.class);

    private TrayIcon trayIcon;
    private SystemTray tray;

    public SystemTraySupportedJFrame(String title) {
        super(title);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.warn("Unable to set LookAndFeel");
        }
        if (SystemTray.isSupported()) {
            tray = SystemTray.getSystemTray();
            Image image = Toolkit.getDefaultToolkit().getImage(getClass().getClassLoader().getResource("logo.png"));
            trayIcon = new TrayIcon(image, title);
            trayIcon.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    if (evt.getClickCount() == 2) {
                        setVisible(true);
                        setExtendedState(JFrame.NORMAL);
                    }
                }
            });

            trayIcon.setImageAutoSize(true);
        }
        addWindowStateListener(e -> {
            if (e.getNewState() == ICONIFIED) {
                try {
                    tray.add(trayIcon);
                    setVisible(false);
                } catch (AWTException ex) {
                    logger.warn("unable to add to tray");
                }
            }
            if (e.getNewState() == 7) {
                try {
                    tray.add(trayIcon);
                    setVisible(false);
                } catch (AWTException ex) {
                    logger.warn("unable to add to system tray");
                }
            }
            if (e.getNewState() == MAXIMIZED_BOTH) {
                tray.remove(trayIcon);
                setVisible(true);
            }
            if (e.getNewState() == NORMAL) {
                tray.remove(trayIcon);
                setVisible(true);
            }
        });
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getClassLoader().getResource("logo.png")));

        setVisible(true);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }
}