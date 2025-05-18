import club.xiaojiawei.DeckPlugin

/**
 * @author ethan ye
 * @date 2025/5/18 00:01
 */
class DarkPriest30Plugin: DeckPlugin {
    override fun description(): String {
//        插件的描述
        return "30快暗"
    }

    override fun author(): String {
//        插件的作者
        return "Ethan Ye"
    }

    override fun version(): String {
//        插件的版本号
        return "1.0.0-template"
    }

    override fun id(): String {
        return "ethanyedarkpriestplugin"
    }

    override fun name(): String {
//        插件的名字
        return "狂野上分快暗"
    }

    override fun homeUrl(): String {
        return "https://github.com/xjw580/Deck-Plugin-Market/tree/master/Deck-Plugin-Template"
    }
}