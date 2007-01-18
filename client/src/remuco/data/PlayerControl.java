package remuco.data;

import remuco.proto.Remuco;

public class PlayerControl {

    public static final short CODE_JUMP = Remuco.REM_PC_CMD_JUMP;

    public static final short CODE_LOGOFF = Remuco.REM_PC_CMD_LOGOFF;

    public static final short CODE_NEXT = Remuco.REM_PC_CMD_NEXT;

    public static final short CODE_NOOP = Remuco.REM_PC_CMD_NOOP;

    public static final short CODE_PLAY_PAUSE = Remuco.REM_PC_CMD_PLAY_PAUSE;

    public static final short CODE_PREV = Remuco.REM_PC_CMD_PREV;

    public static final short CODE_RATE = Remuco.REM_PC_CMD_RATE;

    public static final short CODE_RESTART = Remuco.REM_PC_CMD_RESTART;

    public static final short CODE_STOP = Remuco.REM_PC_CMD_STOP;

    public static final byte CODE_VOL = Remuco.REM_PC_CMD_VOLUME;

    public static final PlayerControl PC_LOGOFF = new PlayerControl(
            CODE_LOGOFF, (short) 0);

    public static final PlayerControl PC_NEXT = new PlayerControl(CODE_NEXT,
            (short) 0);

    public static final PlayerControl PC_NOOP = new PlayerControl(CODE_NOOP,
            (short) 0);

    public static final PlayerControl PC_PLAY_PAUSE = new PlayerControl(
            CODE_PLAY_PAUSE, (short) 0);

    public static final PlayerControl PC_PREV = new PlayerControl(CODE_PREV,
            (short) 0);

    public static final PlayerControl PC_RESTART = new PlayerControl(
            CODE_RESTART, (short) 0);

    public static final PlayerControl PC_STOP = new PlayerControl(CODE_STOP,
            (short) 0);

    private short cmd;

    private short param;

    public PlayerControl() {
        cmd = PlayerControl.CODE_NOOP;
        param = 0;
    }

    /**
     * @param cmd
     * @param param
     */
    public PlayerControl(short cmd, short param) {
        this.cmd = cmd;
        this.param = param;
    }

    public synchronized boolean equals(Object o) {
        if (o == null)
            return false;
        if (this == o)
            return true;
        if (!(o instanceof PlayerControl))
            return false;
        PlayerControl pc = (PlayerControl) o;
        synchronized (pc) {
            return cmd == pc.cmd && param == pc.param;
        }
    }

    public synchronized short getCmd() {
        return cmd;
    }

    public synchronized short getParam() {
        return param;
    }

    /**
     * Set control command cmd and param. If param does not fit to cmd or cmd is
     * unknown, the player control gets set with null param and noop cmd.
     * 
     * @param cmd
     * @param param
     */
    public synchronized void set(short code, short param) {
        this.cmd = code;
        this.param = param;
    }

    public synchronized String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("CODE: ").append(cmd).append(" - Param: ").append(param);
        return sb.toString();
    }

}
