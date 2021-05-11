import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GUITimer implements ActionListener {
    private final Timer timer;
    private boolean stopped = true;

    public GUITimer(ActionListener l)
    {
        timer = new Timer(100, l);
    }

    public void start()
    {
        stopped = false;
        timer.start();
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if(Downloader.getNumFilesDownloaded() > 0 && Downloader.getNumFilesToBeDownloaded() == 0)
        {
            timer.stop();
            stopped = true;
        }
    }

    public boolean isStopped()
    {
        return stopped;
    }
}