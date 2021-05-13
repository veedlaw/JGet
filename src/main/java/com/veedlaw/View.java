package com.veedlaw;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

public class View
{
    private JPanel panel;
    private JTextField input;
    private JLabel downloadLabel;
    private JLabel webpageLabel;
    private JLabel numDownloadedLabel;
    private JLabel toBeDownloadedLabel;

    private JButton startButton;
    private Timer timer;

    private static final String TEXTFIELD_DEFAULT_MESSAGE = "Enter URL here:";
    private static final String DOWNLOADLABEL_DEFAULT_MESSAGE = "No download in progress";
    private static final String DOWNLOADLABEL_RUNNING_MESSAGE = "Downloading: ";
    private static final String BUTTON_DEFAULT_MESSAGE = "Download";
    private static final String BUTTON_RUNNING_MESSAGE = "Downloading ... ";
    private static final String WEBPAGELABEL_DEFAULT_MESSAGE = "";

    /**
     * Creates and adds all components of the GUI to panel.
     * @return The panel with the components.
     */
    private Component createComponents() {
        panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // Creating the address bar
        input = new JTextField(TEXTFIELD_DEFAULT_MESSAGE); // Address bar where the user enters the URL
        input.setForeground(new Color(105, 110, 106)); // Sets text color to gray

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 4;
        gbc.gridy = 0;
        input.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                JTextField source = (JTextField) e.getComponent();
                source.setText("");
                source.removeFocusListener(this);
                source.setForeground(new Color(0, 0, 0));
            }
        });
        panel.add(input, gbc);
        // Address bar created above.


        // Button creation.
        startButton = new JButton(BUTTON_DEFAULT_MESSAGE);
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = 4;
        gbc.gridy = 1;
        gbc.weighty = 1;
        gbc.weightx = 1;
        panel.add(startButton, gbc);
        startButton.addActionListener(this::buttonActionPerformed);
        // Button has been created above.


        // Creating labels.
        downloadLabel = new JLabel(DOWNLOADLABEL_DEFAULT_MESSAGE, SwingConstants.LEFT);
        gbc = makeLabelConstraint(2, 1);
        gbc.insets = new Insets(5,0,10,0);
        panel.add(downloadLabel, gbc);

        webpageLabel = new JLabel("", SwingConstants.LEFT);
        panel.add(webpageLabel, makeLabelConstraint(2, GridBagConstraints.REMAINDER));

        panel.add(new JLabel("Number of files downloaded: "), makeLabelConstraint(3, 1));
        numDownloadedLabel = new JLabel("0");
        panel.add(numDownloadedLabel, makeLabelConstraint(3, GridBagConstraints.RELATIVE));

        panel.add(new JLabel("Number of files to be downloaded: "), makeLabelConstraint(4, 1));
        toBeDownloadedLabel = new JLabel("0");
        panel.add(toBeDownloadedLabel, makeLabelConstraint(4, GridBagConstraints.REMAINDER));
        // Labels created above.

        return panel;
    }

    /**
     * Creates GridBagConstraints, is intended for use with aligning JLabels in the panel.
     * @param gridY     Corresponds to GridBagConstraints gridy.
     * @param gridWidth Corresponds to GridBagConstraints gridwidth.
     * @return          GridBagConstraints with applied gridy and gridwith attributes.
     */
    private static GridBagConstraints makeLabelConstraint(int gridY, int gridWidth)
    {
        // Reduces some repetitive code ...
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = gridY;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = gridWidth;
        return gbc;
    }

    /**
     * Creates the menu bar for the GUI.
     * @return The set up menu bar.
     */
    private static JMenuBar createMenu(JPanel panel) {
        JMenuBar mb = new JMenuBar();
        JMenu menu = new JMenu("File");
        JMenuItem item = new JMenuItem("Quit");
        item.addActionListener((ActionEvent e) -> System.exit(0));
        menu.add(item);
        mb.add(menu);

        String about = "JGet v1.0.0 \n NPRG013 Course project \n Author: Walter Herold Veedla";

        menu = new JMenu("Help");
        item = new JMenuItem("About");
        item.addActionListener((ActionEvent) -> JOptionPane.showMessageDialog(panel, about, "About", JOptionPane.INFORMATION_MESSAGE));
        menu.add(item);
        mb.add(menu);

        return mb;
    }

    /**
     * Displays a file chooser dialog.
     * @return User selected directory in which JSoup will operate in.
     */
    private File chooseSaveDirectory()
    {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION)
        {
            return fc.getSelectedFile();
        }
        return null;
    }

    /**
     * Starts the downloading process via a SwingWorker.
     * @param saveDirectory Root of the directory in which JGet operates; Directory in which the downloaded files will be placed.
     */
    private void startDownload(File saveDirectory)
    {
        SwingWorker<String, Object> sw = new SwingWorker<>() {
            @Override
            public String doInBackground()
            {
                setLabelRunning();
                startButton.setEnabled(false);
                panel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                Downloader.runDownload(input.getText(), saveDirectory.getAbsolutePath());
                return null;
            }

            @Override
            protected void done()
            {
                startButton.setEnabled(true);
                panel.setCursor(Cursor.getDefaultCursor());
                setLabelDefaults();

                timer.stop();
            }
        };

        // Updates the labels
        timer = new Timer(100, e1 -> {
            webpageLabel.setText(Downloader.getCurrentDownload());
            numDownloadedLabel.setText(String.valueOf(Downloader.getNumFilesDownloaded()));
            toBeDownloadedLabel.setText(String.valueOf(Downloader.getNumFilesToBeDownloaded()));
        });
        timer.start();
        sw.execute();
    }

    /**
     * Sets label texts to default values.
     */
    private void setLabelDefaults()
    {
        downloadLabel.setText(DOWNLOADLABEL_DEFAULT_MESSAGE);
        startButton.setText(BUTTON_DEFAULT_MESSAGE);
        webpageLabel.setText(WEBPAGELABEL_DEFAULT_MESSAGE);
        toBeDownloadedLabel.setText("0");
    }

    /**
     * Sets label texts to "running messages". Running messages are defined as constants.
     */
    private void setLabelRunning()
    {
        downloadLabel.setText(DOWNLOADLABEL_RUNNING_MESSAGE);
        startButton.setText(BUTTON_RUNNING_MESSAGE);
    }

    /**
     * Main GUI creation.
     * Handles the highest level GUI creation details.
     */
    public static void createAndShowGUI()
    {
        JFrame frame = new JFrame("JGet");

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(600, 190));
        Component panel = (new View()).createComponents();

        frame.getContentPane().add(panel);

        frame.pack();
        frame.setJMenuBar(createMenu((JPanel) panel));
        frame.setVisible(true);
        frame.requestFocusInWindow();
    }

    /**
     * Displays an error message in a separate window.
     * @param message   Description of the error.
     */
    private void showErrorPane(String message)
    {
        JOptionPane.showMessageDialog(panel, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Decides whether the downloading process will be started or alternatively an error pane will be shown describing the error.
     * Executed when the button is pressed.
     * @param e event
     */
    private void buttonActionPerformed(ActionEvent e)
    {
        if ("".equals(input.getText()) || TEXTFIELD_DEFAULT_MESSAGE.equals(input.getText()))
        {
            showErrorPane("Please enter a website address");
            return;
        }
        if (!DownloaderUtilities.canInitiateDownload(input.getText()))
        {
            showErrorPane("JGet is unable to connect to this address. Please double check the validity of this address.");
            return;
        }

        File selectedDir = chooseSaveDirectory();
        if (selectedDir != null)
        {
            startDownload(selectedDir);
        }
    }
}
