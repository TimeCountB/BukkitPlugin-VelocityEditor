package jp.tbox.timecountb.mcbukkit.plugin.ve;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

/**
 * ターゲットセレクタクラス
 * @author TimerB
 */
public class MCSelectorArguments {
	
	static final String numRegex = "^(\\-?[0-9]+\\.?[0-9]*)$";
	static final String intRegex = "^(\\-?[0-9]+)(?!\\.[0-9]*)$";
	// static final String dblRegex = "^(\\-?[0-9]*\\.[0-9]+)$";
	
		private enum SenderType {
		
		ENTITY {
				@Override
				Location getLocation(CommandSender s) {
					return ((Entity) s).getLocation();
				}
			},
		BLOCK {
				@Override
				Location getLocation(CommandSender s) {
					return ((BlockCommandSender) s).getBlock().getLocation();
				}
			},
		SERVER {
				@Override
				Location getLocation(CommandSender s) {
					return null;
				}
			};
		
		abstract Location getLocation(CommandSender s);
		
		static SenderType getSenderType(CommandSender sender) {
			if(sender instanceof Entity)
				return SenderType.ENTITY;
			else if(sender instanceof BlockCommandSender)
				return SenderType.BLOCK;
			else
				return SenderType.SERVER;
		}
		
	}
	

	HashSet<String> enabledOptions;
	
	SenderType senderType;
	CommandSender sender;
	Location senderLocation;
	
	char selType;

	Vector postion, volume;
	Double round, round_min;
	boolean checkCubic, checkRound, strictHit;
	
	Double pitch, pitch_min;
	Double yaw, yaw_min;
	
	String name;
	EntityType type;
	
	HashMap<String,Integer> score_max;
	HashMap<String,Integer> score_min;
	String team;
	String tag;
	
	int level, level_min;
	GameMode mode;
	
	int count;
	
	
	
	private static char checkSelectorType(String selector) throws SelectorException {
		selector = selector.trim();

		Matcher mt = Pattern.compile("@(p|a|r|e)(\\[.*])?$").matcher(selector);
		if (mt.find()) return mt.group(1).charAt(0);

		mt = Pattern.compile("@x(p|a|r|e)(\\[.*])?$").matcher(selector);
		if (mt.find()) return mt.group(1).charAt(0);

		throw new SelectorException(1, "セレクタ抽出例外");
	}
	
	private static boolean checkRange(double value, double num1, double num2) {
		return ( (num1 <= value && value <= num2) || 
				 (num1 >= value && value >= num2) );
	}
	
	private static boolean checkRange(int value, int num1, int num2) {
		return ( (num1 <= value && value <= num2) || 
				 (num1 >= value && value >= num2) );
	}
	
	private static ArrayList<Entity> listSortRandom(ArrayList<Entity> list) {
		ArrayList<Entity> temp = new ArrayList<>();
		
		for(ListIterator it=list.listIterator(); it.hasNext();) {
			Entity e = (Entity)it.next();
			temp.add((int)(temp.size()*Math.random()),e);
		}
		
		return temp;
	}
	
	private static ArrayList<Entity> listSortLocationDist(Location sender, ArrayList<Entity> list) {
		ArrayList<Entity> temp = new ArrayList<>();
		
		for(ListIterator it=list.listIterator(); it.hasNext();) {
			Entity e1 = (Entity)it.next();
			
			if(temp.isEmpty()) {
				temp.add(e1);
			} else {
				for(ListIterator tempIt=temp.listIterator(); tempIt.hasNext();) {
					Entity e2 = (Entity)tempIt.next();
					if(sender.distance(e1.getLocation()) < sender.distance(e2.getLocation())) {
						tempIt.previous();
						tempIt.add(e1);
						break;
					}
				}
			}
		}
			
		return temp;
	}
	
	private static ArrayList<Entity> listSplice(int index, ArrayList<Entity> list) {
		ArrayList<Entity> spliced = new ArrayList<>();
		int entnum = Math.abs(index);
		
		if(index > 0) {
			for (ListIterator it = list.listIterator(); it.hasNext() && (entnum > spliced.size());) {
				spliced.add((Entity)it.next());
				it.remove();
			}
		} else if(index < 0) {
			for (ListIterator it = list.listIterator(list.size()); it.hasPrevious() && (entnum > spliced.size());) {
				spliced.add((Entity)it.previous());
				it.remove();
			}
		} else {
			
		}
		
		return spliced;
	}
	
	
	
	public MCSelectorArguments(String selector, CommandSender sender) {
		Matcher mt;
		
		// 初期設定
		volume = new Vector();
		checkCubic = false;
		checkRound = false;
		strictHit = false;
		score_max = new HashMap<>();
		score_min = new HashMap<>();
		
		// コマンド実行者の種類を特定
		this.sender = sender;
		senderType = SenderType.getSenderType(sender);
		senderLocation = senderType.getLocation(sender);
		
		// コマンド実行者に応じたLocationおよび基準点を取得
		senderLocation = senderType.getLocation(sender);
		if(!senderType.equals(SenderType.SERVER)) {
			postion = new Vector(senderLocation.getX(),
								 senderLocation.getY(),
								 senderLocation.getZ());
		} else {
			postion = new Vector();
		}
		
		// セレクタの種類を特定
		try {
			selType = checkSelectorType(selector);
		} catch (SelectorException exc) {
		}
		
		// 有効な絞り込み引数を保存するHashMap
		enabledOptions = new HashSet<>();
		
		mt = Pattern.compile("\\[(.+)\\]").matcher(selector);
		if(mt.find()) {
			
			// キー:値の分割
			String opts[] = mt.group(1).split(",");
			
			// 値を取得しつつ格納
			for (String opt : opts) {
				int eql;
				if ((eql=opt.indexOf("=")) > -1) {
					
					String key = opt.substring(0, eql).trim();
					String value = opt.substring(eql+1).trim();
					
					// keyとvalueを元にセレクタオプションを設定
					// オプションが有効な場合keyを保存
					if(this.setArguments(key, value)) {
						enabledOptions.add(key);
					}
				}
			}
			
		}
		
		// 基準座標のみ設定したとき矩形指定を有効にする
		if( !checkCubic && !checkRound && 
			(enabledOptions.contains("x") || 
			 enabledOptions.contains("y") || 
			 enabledOptions.contains("z")) ) {
			
			checkCubic = true;
		}
		
		// セレクタごとの固有設定
		
		if(selType != 'e') {
			enabledOptions.add("type");
			type = EntityType.PLAYER;
		}
		
		if(enabledOptions.contains("c") && selType == 'a') {
			enabledOptions.remove("c");
		} else if(!enabledOptions.contains("c") && (selType == 'p' || selType == 'r')) {
			enabledOptions.add("c");
			count = 1;
		}
	}
	
	private boolean setArguments(String key, String value) {
		// 有効な引数名/値かのフラグ
		boolean allowed = false;
		
		/* 座標を用いた指定方法の引数 --------------------------- */
		
		// 基準点 [x,y,z]
		allowed = allowed || this.setPostion(key, value);
		
		// 矩形範囲指定点 [z,y,z]
		allowed = allowed || this.setVolume(key, value);
		
		// 球形範囲指定半径 [r,rm]
		allowed = allowed || this.setRound(key, value);
		
		// 座標指定の方法設定 [pos] (引数記憶必要なし)
		this.setHitType(key, value);
		
		/* ------------------------------------------------------ */
		
		
		/* 視線の角度を用いた指定方法の引数 --------------------- */
		
		// 縦の首振り角度 [rx,rxm]
		allowed = allowed || this.setPitch(key, value);
		
		// 横の首振り角度 [rx,rxm]
		allowed = allowed || this.setYaw(key, value);
		
		/* ------------------------------------------------------ */
		
		
		/* エンティティ情報を用いた指定方法の引数 --------------- */
		
		// エンティティ名 [name]
		allowed = allowed || this.setName(key, value);
		
		// エンティティタイプ [type]
		allowed = allowed || this.setType(key, value);
		
		/* ------------------------------------------------------ */
		
		
		/* スコアボードを用いた指定方法の引数 ------------------- */
		
		// スコア上限 [score_****]
		allowed = allowed || this.setScore(key, value);
		
		// スコア下限 [score_****_min]
		allowed = allowed || this.setScoreMin(key, value);
		
		// 参加チーム名 [team]
		allowed = allowed || this.setTeam(key, value);
		
		// タグ [tag]
		allowed = allowed || this.setTag(key, value);
		
		/* ------------------------------------------------------ */
		
		
		/* プレイヤー情報を用いた指定方法の引数 ----------------- */
		
		// 経験値 [l,lm]
		allowed = allowed || this.setLevel(key, value);
		
		// ゲームモード [m]
		allowed = allowed || this.setMode(key, value);
		
		/* ------------------------------------------------------ */
		
		
		/* セレクタ設定引数 ------------------------------------- */
		
		// 何人を選択するか [c]
		allowed = allowed || this.setCount(key, value);
		
		/* ------------------------------------------------------ */
		
		return allowed;
	}
	
	
	
	public ArrayList<Entity> getEntities() {
		
		// サーバ内の全エンティティ取得
		ArrayList<Entity> ents = new ArrayList<>();
		for(World w:Bukkit.getServer().getWorlds()) {
			ents.addAll(w.getEntities());
		}
		
		return getEntities(ents);
		
	}
	
	public ArrayList<Player> getPlayers() {
		ArrayList<Player> plys = new ArrayList<>();
		
		// 全オンラインプレイヤー取得
		ArrayList<Entity> ents = new ArrayList<>();
		ents.addAll(Bukkit.getServer().getOnlinePlayers());
		
		for(Entity e:getEntities(ents)) {
			if(e instanceof Player) plys.add((Player)e);
		}
		
		return plys;
	}
	
	public ArrayList<Entity> getEntities(ArrayList<Entity> base) {
		
		// senderLocationの更新
		switch(senderType) {
			case ENTITY:
				if(!((Entity)sender).isValid())
					senderLocation = ((Entity)sender).getLocation();
				break;
		}
		
		// オプションによる絞込み
		for(ListIterator it=base.listIterator(); it.hasNext();) {
			Entity e = (Entity)it.next();
			
			if(!checkEntitie(e)) {
				it.remove();
			}
		}
		
		// 返却用リストの初期化
		ArrayList<Entity> result;
		
		// 検索状況に応じた並べ替え
		if( selType == 'r' || selType == 'a' || 
			senderType.equals(SenderType.SERVER)) {
			
			result = listSortRandom(base);
		} else {
			result = listSortLocationDist(senderLocation,base);
		}
		
		if (enabledOptions.contains("c")) {
			result = listSplice(count, result);
		}
		
		base.clear();
		return result;
	}
	
	
	private boolean setPostion(String key, String value) {
		
		boolean adding = false;
		
		if(value.startsWith("~")) {
			value = value.substring(1);
			adding = true;
			
			if(value.isEmpty()) value = "0";
		}
		
		if(key.matches("^(x|y|z)$") && (value.matches(numRegex))) {
			
			if(postion == null)
				postion = new Vector();
			
			double val = Double.parseDouble(value);
			
			switch (key) {
				case "x":
					if(adding)
						postion.setX(postion.getX()+val);
					else
						postion.setX(val);
					break;
				case "y":
					if(adding)
						postion.setY(postion.getY()+val);
					else
						postion.setY(val);
					break;
				case "z":
					if(adding)
						postion.setZ(postion.getZ()+val);
					else
						postion.setZ(val);
					break;
			}
			
			return true;
		}
		
		return false;
	}
	
	private boolean setVolume(String key, String value) {
		
		if(key.matches("^d(x|y|z)$") && value.matches(numRegex)) {
			
			if(volume == null)
				volume = new Vector();
			
			double val = Double.parseDouble(value);
			
			switch (key) {
				case "dx":
					volume.setX(val);
					break;
				case "dy":
					volume.setY(val);
					break;
				case "dz":
					volume.setZ(val);
					break;
			}
			
			checkCubic = true;
			return true;
		}
		
		return false;
	}
	
	private boolean setRound(String key, String value) {	
		if(value.matches(numRegex)) {
			switch(key) {
				case "r":
					round = Double.parseDouble(value);
					checkRound = true;
					return true;
				case "rm":
					round_min = Double.parseDouble(value);
					checkRound = true;
					return true;
			}
		}
		
		return false;
	}
	
	private void setHitType(String key, String value) {
		if(key.equals("pos") && value.matches("^(true|t|1)$")) {
			strictHit = true;
		}
	}
	
	private boolean setPitch(String key, String value) {	
		if(value.matches(numRegex)) {
			switch(key) {
				case "rx":
					pitch = Double.parseDouble(value);
					return true;
				case "rxm":
					pitch_min = Double.parseDouble(value);
					return true;
			}
		}
		
		return false;
	}
	
	private boolean setYaw(String key, String value) {	
		if(value.matches(numRegex)) {
			switch(key) {
				case "ry":
					yaw = Double.parseDouble(value);
					return true;
				case "rym":
					yaw_min = Double.parseDouble(value);
					return true;
			}
		}
		
		return false;
	}
	
	private boolean setName(String key, String value) {
		if(key.equals("name")) {
			name = value;
			return true;
		}
		
		return false;
	}
	
	private boolean setType(String key, String value) {
		if(key.equals("type")) {
			if(value.equals("player")) {
				type = EntityType.PLAYER;
			} else {
				try {
					type = EntityType.fromName(value);
				} catch(NullPointerException e) {
					type = null;
				}
			}
			
			return true;
		}
		
		return false;
	}
	
	private boolean setScore(String key, String value) {
		Matcher mt = Pattern.compile("^(?!.*_min)score_(.+)$").matcher(key);
		if(mt.find() && value.matches(intRegex)) {
			score_max.put(mt.group(1),Integer.parseInt(value));
			return true;
		}
		
		return false;
	}
	
	private boolean setScoreMin(String key, String value) {
		Matcher mt = Pattern.compile("^score_(.+)_min$").matcher(key);
		if(mt.find() && value.matches(intRegex)) {
			score_min.put(mt.group(1),Integer.parseInt(value));
			return true;
		}
		
		return false;
	}
	
	private boolean setTeam(String key, String value) {
		if(key.equals("team")) {
			team = value;
			return true;
		}
		
		return false;
	}
	
	private boolean setTag(String key, String value) {
		if(key.equals("tag")) {
			tag = value;
			return true;
		}
		
		return false;
	}
	
	private boolean setLevel(String key, String value) {	
		if(value.matches(intRegex)) {
			switch(key) {
				case "l":
					level = Integer.parseInt(value);
					return true;
				case "lm":
					level_min = Integer.parseInt(value);
					return true;
			}
		}
		
		return false;
	}
	
	private boolean setMode(String key, String value) {
		if (key.equals("m")) {
			if (value.matches("^(0|s|survival)$")) {
				mode = GameMode.SURVIVAL;
				return true;
			} else if (value.matches("^(1|c|creative)$")) {
				mode = GameMode.CREATIVE;
				return true;
			} else if (value.matches("^(2|a|adventure)$")) {
				mode = GameMode.ADVENTURE;
				return true;
			} else if (value.matches("^(3|sp|spectator)$")) {
				mode = GameMode.SPECTATOR;
				return true;
			}
		}
		return false;
	}
	
	private boolean setCount(String key, String value) {	
		if(key.equals("c") && value.matches(intRegex)) {
			count = Integer.parseInt(value);
			return true;
		}
		
		return false;
	}
	
	
	private boolean checkEntitie(Entity e) {
		
		if(!checkRegionCubic(e)) return false;
		if(!checkRegionRound(e)) return false;
		
		if(!checkPitch(e)) return false;
		if(!checkYaw(e)) return false;
		
		if(!checkName(e)) return false;
		if(!checkType(e)) return false;
		
		if(!checkScoreValue(e)) return false;
		if(!checkContainTeam(e)) return false;
		if(!checkHasTag(e)) return false;
		
		if(!checkContainTeam(e)) return false;
		
		if(!checkPlayerLevel(e)) return false;
		if(!checkPlayerGamemode(e)) return false;
		
		return true;
	}
	
	private boolean checkRegionCubic(Entity e) {
		if(!checkCubic) return true;
		
		Location loc = e.getLocation();
		Vector pos1 = postion;
		Vector pos2 = postion.clone().add(volume);
		
		if(!senderLocation.getWorld().equals(loc.getWorld()))
			return false;
		
		if(strictHit) {
			if(!checkRange(loc.getX(),pos1.getX(),pos2.getX()))
				return false;
			if(!checkRange(loc.getY(),pos1.getY(),pos2.getY()))
				return false;
			if(!checkRange(loc.getZ(),pos1.getZ(),pos2.getZ()))
				return false;
		} else {
			if(!checkRange(loc.getBlockX(),pos1.getBlockX(),pos2.getBlockX()))
				return false;
			if(!checkRange(loc.getBlockY(),pos1.getBlockY(),pos2.getBlockY()))
				return false;
			if(!checkRange(loc.getBlockZ(),pos1.getBlockZ(),pos2.getBlockZ()))
				return false;
		}
		
		return true;
	}
	
	private boolean checkRegionRound(Entity e) {
		if(!checkRound) return true;
		
		Double d = senderLocation.distance(e.getLocation());
		
		if(enabledOptions.contains("r") && d > round)
			return false;
		
		if(enabledOptions.contains("rm") && d < round_min)
			return false;
		
		return true;
	}
	
	private boolean checkPitch(Entity e) {
		float p = e.getLocation().getPitch();
		
		if(enabledOptions.contains("rx") && p > pitch)
			return false;
		
		if(enabledOptions.contains("rxm") && p < pitch_min)
			return false;
		
		return true;
	}
	
	private boolean checkYaw(Entity e) {
		float y = e.getLocation().getPitch();
		
		if(enabledOptions.contains("ry") && y > yaw)
			return false;
		
		if(enabledOptions.contains("rym") && y < yaw_min)
			return false;
		
		return true;
	}
	
	private boolean checkName(Entity e) {
		return !(enabledOptions.contains("name") && !name.equalsIgnoreCase(e.getName()));
	}
	
	private boolean checkType(Entity e) {
		return !(enabledOptions.contains("type") && !type.equals(e.getType()));
	}
	
	private boolean checkScoreValue(Entity e) {
		Scoreboard sb = Bukkit.getServer().getScoreboardManager().getMainScoreboard();
		Objective obj;
		
		for(String key:score_max.keySet()) {
			try {
				obj = sb.getObjective(key);
				if(obj != null && obj.getScore(e.getName()).getScore() > score_max.get(key))
					return false;
			} catch(IllegalArgumentException | IllegalStateException exc) {
			}
		}
		
		for(String key:score_min.keySet()) {
			try {
				obj = sb.getObjective(key);
				if(obj != null && obj.getScore(e.getName()).getScore() < score_min.get(key))
					return false;
			} catch(IllegalArgumentException | IllegalStateException exc) {
			}
		}
		
		return true;
	}
	
	private boolean checkContainTeam(Entity e) {
		
		if(!enabledOptions.contains("team"))
			return true;
		
		Scoreboard sb = Bukkit.getServer().getScoreboardManager().getMainScoreboard();
		String team_name;
		boolean isnot, result;
		
		if(team.startsWith("!")) {
			isnot = true;
			team_name = team.substring(1);
		} else {
			isnot = false;
			team_name = team;
		}
		
		result = true;
		
		if(team_name.isEmpty()) {
			for(Team t:sb.getTeams()) {
				if(t.hasEntry(e.getName())) {
					result = false;
					break;
				}
			}
		} else {
			try {
				Team t = sb.getTeam(team_name);
				if(t == null || !t.hasEntry(e.getName()))
					result = false;
			} catch(IllegalArgumentException | IllegalStateException exc) {
				return true;
			}
		}
		
		return result^isnot;
	}
	
	private boolean checkHasTag(Entity e) {
		
		if(!enabledOptions.contains("tag"))
			return true;
		
		Set<String> e_tags = e.getScoreboardTags();
		String tag_name;
		boolean isnot, result;
		
		if(tag.startsWith("!")) {
			isnot = true;
			tag_name = tag.substring(1);
		} else {
			isnot = false;
			tag_name = tag;
		}
		
		result = true;
		
		if(tag_name.isEmpty()) {
			if(!e_tags.isEmpty()) result = false;
		} else if(!e_tags.contains(tag_name)) {
			result = false;
		}
		
		return result^isnot;
	}
	
	private boolean checkPlayerLevel(Entity e) {
		if(enabledOptions.contains("l") || enabledOptions.contains("lm")) {
			if(!(e instanceof Player))
				return false;
			
			Player p = (Player) e;
			
			if(enabledOptions.contains("l") && ((Player)e).getLevel() > level)
				return false;
			
			if(enabledOptions.contains("lm") && ((Player)e).getLevel() > level)
				return false;
		}
		return true;
	}
	
	private boolean checkPlayerGamemode(Entity e) {
		if(!enabledOptions.contains("m"))
			return true;
		
		if(!(e instanceof Player))
			return false;
		
		return mode.equals(((Player)e).getGameMode());
	}
	
}
