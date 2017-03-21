package jp.tbox.timecountb.mcbukkit.plugin.ve;

/**
 * ターゲットセレクタ例外
 * @author TimerB
 */
public class SelectorException extends Exception {
	// private static final long serialVersionUID = 1L;
	private final int code;
	
	public SelectorException(int code, String message) {
		super(message);
		this.code = code;
	}
	
	public int getCode() {
		return code;
	}
}
