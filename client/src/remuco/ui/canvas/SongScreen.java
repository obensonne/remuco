package remuco.ui.canvas;

import java.util.Vector;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import remuco.data.PlayerState;
import remuco.data.Song;
import remuco.proto.Remuco;
import remuco.util.Log;

public class SongScreen extends Canvas {

    private int currentRating;

    private Song currentSong;

    private byte currentVolume;

    private Image imgScreenBG, imgSongAreaBG;

    private KeyListener kl;

    private ScreenElement seState, seRepeat, seShuffle, seVolume, seSong;

    private Song songDefault;

    private Theme t;

    /**
     * Create a new SongScreen.
     * 
     * @param pcl
     *            the parent {@link CommandListener} to delegate commands to
     */
    public SongScreen(KeyListener kl, Theme t) {

        super();

        this.kl = kl;
        this.t = t;

        songDefault = new Song();
        songDefault.setTag(Remuco.REM_TAG_NAME_TITLE, "Remuco");
        currentSong = songDefault;
        currentVolume = 55;

        applyTheme();
    }

    public void update(PlayerState ps) {

        switch (ps.getState()) {
            case Remuco.REM_PS_STATE_PLAY:
                seState.setImage(t.img[Theme.IMGID_STATE_PLAY]);
                break;
            case Remuco.REM_PS_STATE_PAUSE:
                seState.setImage(t.img[Theme.IMGID_STATE_PAUSE]);
                break;
            case Remuco.REM_PS_STATE_STOP:
                seState.setImage(t.img[Theme.IMGID_STATE_STOP]);
                break;
            case Remuco.REM_PS_STATE_OFF:
                seState.setImage(t.img[Theme.IMGID_STATE_OFF]);
                break;
            case Remuco.REM_PS_STATE_SRVOFF:
                seState.setImage(t.img[Theme.IMGID_STATE_SRVOFF]);
                break;
            case Remuco.REM_PS_STATE_PROBLEM:
                seState.setImage(t.img[Theme.IMGID_STATE_PROBLEM]);
                break;
            case Remuco.REM_PS_STATE_ERROR:
                seState.setImage(t.img[Theme.IMGID_STATE_ERROR]);
                break;
            default:
                seState.setImage(t.img[Theme.IMGID_STATE_PROBLEM]);
                Log.ln(this, "unknown state");
                break;
        }

        if (ps.playlistIsRepeat()) {
            seRepeat.setImage(t.img[Theme.IMGID_REPEAT_ON]);
        } else {
            seRepeat.setImage(t.img[Theme.IMGID_REPEAT_OFF]);
        }

        if (ps.playlistIsShuffle()) {
            seShuffle.setImage(t.img[Theme.IMGID_SHUFFLE_ON]);
        } else {
            seShuffle.setImage(t.img[Theme.IMGID_SHUFFLE_OFF]);
        }

        if (currentVolume != ps.getVolume()) {
            currentVolume = ps.getVolume();
            updateVolumeBar();
        }

        Song s = ps.getCurrentSong();
        if (s == null)
            s = songDefault;
        if (currentSong != s || currentRating != s.getRating()) {
            currentSong = s;
            currentRating = s.getRating();
            updateSongArea();
        }

    }

    protected void keyPressed(int key) {
        kl.keyPressed(translateKey(key));
    }

    protected void keyReleased(int key) {
        kl.keyReleased(translateKey(key));
    }

    protected void paint(Graphics g) {

        g.drawImage(imgScreenBG, 0, 0, Graphics.TOP | Graphics.LEFT);

        seState.draw(g);
        seRepeat.draw(g);
        seShuffle.draw(g);
        seVolume.draw(g);
        seSong.draw(g);

    }

    protected void updateTheme(Theme t) {
        this.t = t;
        applyTheme();
    }

    private void applyTheme() {

        initScreenElements();

        createBackgroundImage();

        updateVolumeBar();

        updateSongArea();

    }

    private void createBackgroundImage() {

        int n, m;
        int width = getWidth();
        int height = getHeight();

        imgScreenBG = Image.createImage(width, height);

        Graphics g = imgScreenBG.getGraphics();

        Point p = new Point();

        g.setColor(t.colors[Theme.COLOR_BG]);

        g.fillRect(0, 0, width, height);

        // //// icon bar at the top //////

        drawImageX(g, t.img[Theme.IMGID_TOP_LEFT], p);
        drawImageX(g, t.img[Theme.IMGID_STATE_PLAY], p);
        drawImageX(g, t.img[Theme.IMGID_SHUFFLE_OFF], p);
        drawImageX(g, t.img[Theme.IMGID_REPEAT_ON], p);
        drawImageX(g, t.img[Theme.IMGID_VOLUME_SYMBOL], p);
        drawImageX(g, t.img[Theme.IMGID_VOLUME_LEFT], p);

        p.x = width - t.img[Theme.IMGID_TOP_RIGHT].getWidth()
                - t.img[Theme.IMGID_VOLUME_RIGHT].getWidth();

        drawImageX(g, t.img[Theme.IMGID_VOLUME_RIGHT], p);
        drawImageX(g, t.img[Theme.IMGID_TOP_RIGHT], p);

        // //// song area background //////

        imgSongAreaBG = Image
                .createImage(seSong.getWidth(), seSong.getHeigth());
        Graphics gTmp = imgSongAreaBG.getGraphics();
        p.y = 0;
        while (p.y < height) {
            p.x = 0;
            while (p.x < width) {
                drawImageX(gTmp, t.img[Theme.IMGID_SONG_AREA], p);
            }
            p.y += t.img[Theme.IMGID_SONG_AREA].getHeight();
        }

        // p.x = t.img[Theme.IMGID_BORDER_LEFT].getWidth();
        // p.y = t.img[Theme.IMGID_TOP_RIGHT].getHeight()
        // + t.img[Theme.IMGID_BORDER_TOP].getHeight();
        // drawImageX(g, imgSongAreaBG, p);

        // //// song area borders //////

        n = t.img[Theme.IMGID_CORNER_TOP_LEFT].getWidth(); // x
        m = width - t.img[Theme.IMGID_CORNER_TOP_LEFT].getWidth()
                - t.img[Theme.IMGID_CORNER_TOP_RIGHT].getWidth(); // width
        g.setClip(n, 0, m, height);
        p.x = 0;
        p.y = t.img[Theme.IMGID_TOP_LEFT].getHeight();
        while (p.x < width) {
            drawImageX(g, t.img[Theme.IMGID_BORDER_TOP], p);
        }
        p.x = 0;
        p.y = height - t.img[Theme.IMGID_BORDER_BOTTOM].getHeight();
        while (p.x < width) {
            drawImageX(g, t.img[Theme.IMGID_BORDER_BOTTOM], p);
        }

        n = t.img[Theme.IMGID_TOP_LEFT].getHeight()
                + t.img[Theme.IMGID_CORNER_TOP_LEFT].getHeight(); // y
        m = height - t.img[Theme.IMGID_TOP_LEFT].getHeight()
                - t.img[Theme.IMGID_CORNER_TOP_LEFT].getHeight()
                - t.img[Theme.IMGID_CORNER_BOTTOM_LEFT].getHeight(); // height
        g.setClip(0, n, width, m);
        p.x = 0;
        p.y = t.img[Theme.IMGID_TOP_RIGHT].getHeight();
        while (p.y < height) {
            drawImageY(g, t.img[Theme.IMGID_BORDER_LEFT], p);
        }
        p.x = width - t.img[Theme.IMGID_BORDER_RIGHT].getWidth();
        p.y = t.img[Theme.IMGID_TOP_RIGHT].getHeight();
        while (p.y < height) {
            drawImageY(g, t.img[Theme.IMGID_BORDER_RIGHT], p);
        }

        g.setClip(0, 0, width, height);

        // //// song area corners //////

        p.x = 0;
        p.y = t.img[Theme.IMGID_TOP_RIGHT].getHeight();
        drawImageX(g, t.img[Theme.IMGID_CORNER_TOP_LEFT], p);

        p.x = width - t.img[Theme.IMGID_CORNER_TOP_RIGHT].getWidth();
        p.y = t.img[Theme.IMGID_TOP_RIGHT].getHeight();
        drawImageX(g, t.img[Theme.IMGID_CORNER_TOP_RIGHT], p);

        p.x = width - t.img[Theme.IMGID_CORNER_BOTTOM_RIGHT].getWidth();
        p.y += height - t.img[Theme.IMGID_CORNER_BOTTOM_RIGHT].getHeight();
        drawImageX(g, t.img[Theme.IMGID_CORNER_BOTTOM_LEFT], p);

        p.x = 0;
        p.y += height - t.img[Theme.IMGID_CORNER_BOTTOM_LEFT].getHeight();
        drawImageX(g, t.img[Theme.IMGID_CORNER_BOTTOM_LEFT], p);

        n = width - t.img[Theme.IMGID_CORNER_TOP_RIGHT].getWidth();
        while (p.x < n) {
            drawImageX(g, t.img[Theme.IMGID_BORDER_TOP], p);
        }
        p.x = n;
        drawImageX(g, t.img[Theme.IMGID_CORNER_TOP_RIGHT], p);

        n = height - t.img[Theme.IMGID_BORDER_BOTTOM].getHeight();
        m = width - t.img[Theme.IMGID_BORDER_RIGHT].getWidth();
        while (p.y < n) {
            p.x = 0;
            drawImageX(g, t.img[Theme.IMGID_BORDER_LEFT], p);
            while (p.x < m) {
                drawImageX(g, t.img[Theme.IMGID_SONG_AREA], p);
            }
            p.x = m;
            drawImageX(g, t.img[Theme.IMGID_BORDER_RIGHT], p);
            p.y += t.img[Theme.IMGID_SONG_AREA].getHeight();
        }

        p.x = 0;
        p.y = n;
        drawImageX(g, t.img[Theme.IMGID_CORNER_BOTTOM_LEFT], p);
        n = width - t.img[Theme.IMGID_CORNER_BOTTOM_RIGHT].getWidth();
        while (p.x < n) {
            drawImageX(g, t.img[Theme.IMGID_BORDER_BOTTOM], p);
        }
        p.x = n;
        drawImageX(g, t.img[Theme.IMGID_CORNER_BOTTOM_RIGHT], p);

        g.drawString("Hey ho up da für", width / 2, height / 2,
                Graphics.BASELINE | Graphics.HCENTER);

    }

    private void drawImageX(Graphics g, Image img, Point p) {
        g.drawImage(img, p.x, p.y, Graphics.TOP | Graphics.LEFT);
        p.x += img.getWidth();
    }

    private void drawImageY(Graphics g, Image img, Point p) {
        g.drawImage(img, p.x, p.y, Graphics.TOP | Graphics.LEFT);
        p.y += img.getHeight();
    }

    private int drawStrings(String[] sa, int width, int vOffset, Graphics g) {

        int i, saLen, fontHeight;
        saLen = sa.length;
        fontHeight = g.getFont().getHeight();

        for (i = 0; i < saLen; i++) {
            g.drawString(sa[i], width / 2, vOffset, Graphics.TOP
                    | Graphics.HCENTER);
            vOffset += fontHeight;
        }

        return vOffset;
    }

    private void initScreenElements() {

        int x, y, w, h;
        Image i;
        Graphics g;

        x = t.img[Theme.IMGID_TOP_LEFT].getWidth();
        y = 0;
        seState = new ScreenElement(x, y, t.img[Theme.IMGID_STATE_PROBLEM]);

        x = seState.getNextX();
        y = 0;
        seRepeat = new ScreenElement(x, y, t.img[Theme.IMGID_REPEAT_OFF]);

        x = seRepeat.getNextX();
        y = 0;
        seShuffle = new ScreenElement(x, y, t.img[Theme.IMGID_SHUFFLE_OFF]);

        x = seShuffle.getNextX() + t.img[Theme.IMGID_VOLUME_SYMBOL].getWidth()
                + t.img[Theme.IMGID_VOLUME_RIGHT].getWidth();
        y = 0;
        w = getWidth() - t.img[Theme.IMGID_TOP_RIGHT].getWidth()
                - t.img[Theme.IMGID_VOLUME_RIGHT].getWidth() - x;
        h = t.img[Theme.IMGID_VOLUME_OFF].getHeight();
        i = Image.createImage(w, h);
        g = i.getGraphics();
        g.setColor(t.colors[Theme.COLOR_BG]);
        g.fillRect(0, 0, w, h);
        seVolume = new ScreenElement(x, y, i);

        x = t.img[Theme.IMGID_BORDER_LEFT].getWidth();
        y = seState.getNextY() + t.img[Theme.IMGID_BORDER_TOP].getHeight();
        w = getWidth() - t.img[Theme.IMGID_BORDER_RIGHT].getWidth() - x;
        h = getHeight() - t.img[Theme.IMGID_BORDER_BOTTOM].getHeight() - y;
        i = Image.createImage(w, h);
        g = i.getGraphics();
        g.setColor(t.colors[Theme.COLOR_BG]);
        g.fillRect(0, 0, w, h);
        seSong = new ScreenElement(x, y, i);

    }

    private int translateKey(int key) {
        switch (key) {
            case Canvas.KEY_STAR:
                Log.ln("*");
                return KeyListener.KEY_RATE_DOWN;
            case Canvas.KEY_POUND:
                Log.ln("#");
                return KeyListener.KEY_RATE_UP;
            case Canvas.KEY_NUM0:
                Log.ln("0");
                return KeyListener.KEY_SHOW_PLAYLIST;
        }
        int ga = getGameAction(key);
        if (ga != 0) {
            switch (ga) {
                case Canvas.FIRE:
                    Log.ln("GA fire");
                    return KeyListener.KEY_PLAY_PAUSE;
                case Canvas.RIGHT:
                    Log.ln("GA right");
                    return KeyListener.KEY_VOLUME_UP;
                case Canvas.LEFT:
                    Log.ln("GA left");
                    return KeyListener.KEY_VOLUME_DOWN;
                case Canvas.UP:
                    Log.ln("GA up");
                    return KeyListener.KEY_PREV;
                case Canvas.DOWN:
                    Log.ln("GA down");
                    return KeyListener.KEY_NEXT;
            }
        }
        return KeyListener.KEY_NOOP;
    }

    private void updateSongArea() {

        Image si = seSong.getImage();
        Graphics g = si.getGraphics();
        int width = si.getWidth();
        int height = si.getHeight();
        int y = 0;
        String[] sa;

        g.drawImage(imgSongAreaBG, 0, 0, Graphics.LEFT | Graphics.TOP);

        // g.drawString(System.currentTimeMillis() + "", si.getWidth() / 2, si
        // .getHeight() / 2, Graphics.BASELINE | Graphics.HCENTER);
        //
        g.setColor(t.colors[Theme.COLOR_ARTIST]);
        g.setFont(Theme.FONT_ARTIST);
        sa = Theme.splitString(currentSong.getTag(Song.TAG_ARTIST), width - 4,
                g.getFont());
        y = drawStrings(sa, width, y, g);

        g.setColor(t.colors[Theme.COLOR_TITLE]);
        g.setFont(Theme.FONT_TITLE);
        sa = Theme.splitString(currentSong.getTag(Song.TAG_TITLE), width - 4, g
                .getFont());
        y = drawStrings(sa, width, y, g);

        g.setColor(t.colors[Theme.COLOR_ALBUM]);
        g.setFont(Theme.FONT_ALBUM);
        sa = Theme.splitString(currentSong.getTag(Song.TAG_ALBUM), width - 4, g
                .getFont());
        y = drawStrings(sa, width, y, g);

        int rating = currentSong.getRating();

        Log.ln("display rating of " + rating);

        if (rating < 0)
            return;

        y = height - t.img[Theme.IMGID_BORDER_BOTTOM].getHeight();

        int rsw = t.img[Theme.IMGID_RATE_OFF].getWidth();
        int x, i;
        x = (width - rsw * currentSong.getRatingMax()) / 2;
        for (i = 1; i <= rating; i++) {
            g.drawImage(t.img[Theme.IMGID_RATE_ON], x + rsw * (i - 1), y,
                    Graphics.LEFT | Graphics.BOTTOM);
        }
        for (i = rating + 1; i <= currentSong.getRatingMax(); i++) {
            g.drawImage(t.img[Theme.IMGID_RATE_OFF], x + rsw * (i - 1), y,
                    Graphics.LEFT | Graphics.BOTTOM);
        }

    }

    private void updateVolumeBar() {

        Graphics g = seVolume.getImage().getGraphics();
        Point p = new Point(0, 0);
        int i;
        int width = seVolume.getImage().getWidth();
        int on = width * currentVolume / 100;

        for (i = 0; i <= on; i++) {
            drawImageX(g, t.img[Theme.IMGID_VOLUME_ON], p);
        }
        for (i = on; i < width; i++) {
            drawImageX(g, t.img[Theme.IMGID_VOLUME_OFF], p);
        }

    }

}
