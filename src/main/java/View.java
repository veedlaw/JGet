import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class View
{
    private JPanel panel;
    private JTextField input;
    private JLabel label;
    private JLabel webpageLabel;
    private JLabel numDownloadedLabel;
    private JLabel toBeDownloadedLabel;

    private JButton startButton;
    private SwingWorker<String, Object> sw;
    private Timer timer;

    private Component createComponents() {
        panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        Color textFieldColor = new Color(166, 138, 138);

        input = new JTextField("Enter URL here:");
        input.setForeground(textFieldColor);
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


        startButton = new JButton("Download");
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = 4;
        gbc.gridy = 1;
        gbc.weighty = 1;
        gbc.weightx = 1;
        panel.add(startButton, gbc);
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startDownload();
            }
        });

        /*bar = new JProgressBar(0, 100);
        panel.add(bar);*/

        label = new JLabel("No download in progress.", SwingConstants.LEFT);
        gbc = new GridBagConstraints();
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5,0,10,0);
        panel.add(label, gbc);

        webpageLabel = new JLabel("", SwingConstants.LEFT);
        gbc = new GridBagConstraints();
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        panel.add(webpageLabel, gbc);

        JLabel text1 = new JLabel("Number of files downloaded: ");
        gbc = new GridBagConstraints();
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(text1, gbc);
        numDownloadedLabel = new JLabel("0");
        gbc = new GridBagConstraints();
        gbc.gridy = 3;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        panel.add(numDownloadedLabel, gbc);

        JLabel text2 = new JLabel("Number of files to be downloaded: ");
        gbc = new GridBagConstraints();
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(text2, gbc);
        toBeDownloadedLabel = new JLabel("0");
        gbc = new GridBagConstraints();
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        panel.add(toBeDownloadedLabel, gbc);

        return panel;
    }

    private static void makeLabel()
    {
        GridBagConstraints gbc = new GridBagConstraints();

    }

    private static JMenuBar createMenu() {
        JMenuBar mb = new JMenuBar();
        JMenu menu = new JMenu("Settings");
        JMenuItem item = new JMenuItem("Preferences");
        menu.add(item);
        item = new JMenuItem("Quit");

        item.addActionListener((ActionEvent e) -> System.exit(0));
        menu.add(item);
        mb.add(menu);

        menu = new JMenu("Help");
        item = new JMenuItem("Content");
        menu.add(item);
        menu.add(new JSeparator());
        item = new JMenuItem("About");
        menu.add(item);
        mb.add(menu);

        return mb;
    }



    private void startDownload()
    {
        label.setText("Downloading: ");

        startButton.setEnabled(false);
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        sw = new SwingWorker<>()
        {
            @Override
            public String doInBackground() {
                Downloader.runDownload(input.getText(), "/Users/kasutaja/Desktop/jsoup");
                return null;
            }
        };

        GUITimer timer = new GUITimer(e1 -> {
            webpageLabel.setText(Downloader.getCurrentDownload());
            numDownloadedLabel.setText(String.valueOf(Downloader.getNumFilesDownloaded()));
            toBeDownloadedLabel.setText(String.valueOf(Downloader.getNumFilesToBeDownloaded()));

        });

        /*Timer timer = new Timer(100, e1 -> {
            webpageLabel.setText(Downloader.getCurrentDownload());
            numDownloadedLabel.setText(String.valueOf(Downloader.getNumFilesDownloaded()));
            toBeDownloadedLabel.setText(String.valueOf(Downloader.getNumFilesToBeDownloaded()));
        });*/
        timer.start();

        /*if (Downloader.getNumFilesDownloaded() > 0 && Downloader.getNumFilesToBeDownloaded() == 0)
        {
            timer.stop();
            startButton.setEnabled(true);
            panel.setCursor(Cursor.getDefaultCursor());
        }*/

        sw.execute();
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
        View view = new View();
        Component panel = view.createComponents();

        frame.getContentPane().add(panel);

        frame.pack();
        frame.setJMenuBar(createMenu());
        frame.setVisible(true);
        frame.requestFocusInWindow();
    }

}
