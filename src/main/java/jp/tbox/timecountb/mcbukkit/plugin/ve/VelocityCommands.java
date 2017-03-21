package jp.tbox.timecountb.mcbukkit.plugin.ve;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * @author TimerB
 */
class VelocityCommands implements CommandExecutor {
	
	static final String numRegex = "^(\\-?[0-9]+\\.?[0-9]*)$";
	
	public VelocityCommands() {
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		List<Entity> ents;
		Vector vels, temp;
		String mode;
		
		if (command.getName().equalsIgnoreCase("velocity") && args.length >= 4) {
			
			// コマンド権限のチェック
			if(!sender.hasPermission("velocity")){
				sender.sendMessage(ChatColor.RED + String.format("コマンドを実行する権限 velocity がありません"));
				return true;
			}
			
			// 対象プレイヤーの取得
			ents = new ArrayList<>();
			if(args[0].startsWith("@")){
				ents.addAll(new MCSelectorArguments(args[0],sender).getPlayers());
				//tempPlys.add((Player)sender);
			} else {
				Entity ply = getOnlinePlayer(args[0]);
				if(ply != null) ents.add(ply);
				else sender.sendMessage(ChatColor.RED + String.format("名前 %s を発見できません", args[0]));
			}
			
			// 操作タイプの設定
			if(args.length >= 5) {
				mode = args[4];
			} else {
				mode = "add";
			}
			
			for(int i=1; i<=3; i++) {
				// Double型に変換できない文字列を検出したら処理を中止する
				if(!args[i].matches(numRegex)) {
					sender.sendMessage(ChatColor.RED + String.format("%s <player> <x> <y> <z> [add/multi/set]", label));
					return true;
				}
			}
			
			vels = new Vector(Double.parseDouble(args[1]), Double.parseDouble(args[2]), Double.parseDouble(args[3]));
			
			for(Entity ent : ents) {
				switch(mode) {
					case "set":
						ent.setVelocity(vels);
						sender.sendMessage(ChatColor.GRAY + String.format("velocity setted %s to %s,%s,%s", ent.getName(), vels.getX(), vels.getY(), vels.getZ()));
						break;
					case "multi":
						temp = ent.getVelocity();
						temp.multiply(vels);
						ent.setVelocity(temp);
						sender.sendMessage(ChatColor.GRAY + String.format("velocity changed %s to %s,%s,%s", ent.getName(), temp.getX(), temp.getY(), temp.getZ()));
						break;
					case "add":
					default:
						temp = ent.getVelocity();
						temp.add(vels);
						ent.setVelocity(temp);
						sender.sendMessage(ChatColor.GRAY + String.format("velocity added %s to %s,%s,%s", ent.getName(), temp.getX(), temp.getY(), temp.getZ()));
						break;
				}
			}
			
			return true;
		}
		
		return false;
	}
	
	static Entity getOnlinePlayer(String name) {
		for(Player p:Bukkit.getServer().getOnlinePlayers()) {
			if(p.getName().equalsIgnoreCase(name))
				return (Entity)p;
		}
		
		return null;
	}
}
