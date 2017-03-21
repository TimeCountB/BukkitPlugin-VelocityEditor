# [BukkitPlugin]VelocityEditor
プレイヤーの移動速度を設定する簡単なBukkitプラグイン

# 使い方
`/velocity <player> <x> <y> <z> [add/multi/set]`

`<player>`で指定したプレイヤーに速度<x><y><z>を設定します

最後の引数[add/multi/set]は省略可能です(省略した場合addとして処理)

* add（指定がない場合）
	* 指定した`<x><y><z>`の値をプレイヤーに加算します
* multi
	* 指定した`<x><y><z>`の値とプレイヤーが持つ現在のx,y,z軸基準速度を掛け算します
* set
	* 指定した`<x><y><z>`の値をそのままプレイヤーに設定します

例えば`/velocity @p 0 1 0`と入力すると、入力したプレイヤーはy方向速度に1が加算され上方向に吹っ飛びます
