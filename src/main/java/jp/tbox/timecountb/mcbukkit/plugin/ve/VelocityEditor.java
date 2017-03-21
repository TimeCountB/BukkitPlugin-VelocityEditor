package jp.tbox.timecountb.mcbukkit.plugin.ve;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * VelocityEditorプラグイン本体
 * @author TimerB
 */
public class VelocityEditor extends JavaPlugin implements Listener {
	
	private VelocityCommands cexe;
	
	@Override
	public void onEnable() {
		cexe = new VelocityCommands();
		getCommand("velocity").setExecutor(cexe);
		System.out.println("[VelocityEditer] Enabled");
	}
	
}