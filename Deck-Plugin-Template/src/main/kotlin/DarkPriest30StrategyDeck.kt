import club.xiaojiawei.DeckStrategy
import club.xiaojiawei.bean.Card
import club.xiaojiawei.bean.area.DeckArea
import club.xiaojiawei.bean.area.HandArea
import club.xiaojiawei.bean.area.PlayArea
import club.xiaojiawei.bean.isValid
import club.xiaojiawei.config.log
import club.xiaojiawei.enums.CardTypeEnum
import club.xiaojiawei.enums.RunModeEnum
import club.xiaojiawei.status.WAR

/**
 * @author ethan ye
 * @date 2025/5/18 00:01
 */
class DarkPriest30StrategyDeck : DeckStrategy() {

    override fun name(): String {
//        套牌策略名
        return "30快暗"
    }

    override fun getRunMode(): Array<RunModeEnum> {
//        策略允许运行的模式
        return arrayOf(RunModeEnum.WILD)
    }

    override fun deckCode(): String {
        return "AAEBAZ/HAgKRvAK79wMOoQSRD7q2A6P3A633A4aDBd2kBYWOBsacBtCeBsSoBte6BtXBBtzzBgAA"
    }

    override fun id(): String {
        return "ethanyedarkpriestplugin"
    }

    override fun executeChangeCard(cards: HashSet<Card>) {
//        TODO("执行换牌策略")
        val toList = cards.toList()
        for (card in toList) {
            if (card.cost > 2
                || card.cardId in listOf(
                    "CFM_637",  // 帕奇斯
                    "SW_448",   // 大主教
                    "CORE_SW_448",  // 大主教
                )) {
//                不要哪张牌就直接移除
                cards.remove(card)
            }
        }
    }

    /**
     * 深度复制卡牌集合
     */
    fun deepCloneCards(sourceCards: List<Card>): MutableList<Card> {
        val copyCards = mutableListOf<Card>()
        sourceCards.forEach {
            copyCards.add(it.clone())
        }
        return copyCards
    }

    /**
     * 我的回合开始时将会自动调用此方法
     */
    override fun executeOutCard() {
//        TODO("执行出牌策略")
//        需要投降时将needSurrender设为true
//        needSurrender = true
//        获取全局war
        val war = WAR
        //        我方玩家
        val me = war.me
        if (!me.isValid()) return
//        敌方玩家
        val rival = war.rival
        if (!rival.isValid()) return
//            获取战场信息

//            获取我方所有手牌
        val handCards = me.handArea.cards
//            获取我方所有场上的卡牌
        val playCards = me.playArea.cards
//            获取我方英雄
        val hero = me.playArea.hero
//            获取我方武器
        val weapon = me.playArea.weapon
//            获取我方技能
        val power = me.playArea.power
//            获取我方所有牌库中的卡牌
        val deckCards = me.deckArea.cards
//            我方当前可用水晶数
        val usableResource = me.usableResource

//            cardId是游戏写死的，每张牌的cardId都是唯一不变的，如同身份证号码，
        val heroCardId = hero?.cardId
//            entityId在每局游戏中是唯一的
        val heroEntityId = hero?.entityId

//            执行操作
        /*
            使用地标，默认给第一个随从使用
         */
        var firstPlayCard : Card? = null
        if (playCards.isNotEmpty()) {
            for (playCard in playCards) {
                if (playCard.cardType == CardTypeEnum.MINION) {
                    firstPlayCard = playCard
                    break
                }
            }
        }
        if (firstPlayCard != null && playCards.isNotEmpty()) {
            for (playCard in playCards) {
                if (playCard.cardType == CardTypeEnum.LOCATION) {
                    playCard.action.pointTo(firstPlayCard)
                }
            }
        }
        /*
            根据双方场面和自己手牌决策过墙最优解，不考虑536弃牌或435全场睡觉的情况，对一个随从至多只使用一张精神灼烧
         */
        if (rival.playArea.cards.isNotEmpty() && playCards.isNotEmpty() && handCards.isNotEmpty()) {
            for(rivalPlayCard in rival.playArea.cards){
                if (rivalPlayCard.isTaunt) {
                    for (playCard in getBestPlayCardTarget(rivalPlayCard, playCards, handCards, usableResource, power)) {
                        // 我方随从攻击敌方随从
                        playCard.action.attack(rivalPlayCard)
                        playCard.clone()
                    }
                }
            }
        }
        /*
            优先拍随从
         */
        for (handCard in handCards) {
            if (handCard.cardType == CardTypeEnum.MINION && handCard.cost <= me.usableResource) {
                handCard.action.power()
            }
        }
        /*
            到此对面场面不存在墙，若存在则己方随从已攻击完毕
         */
        for (playCard in playCards) {
            if (playCard.canAttack()) {
                playCard.action.attackHero()
            }
        }
        /*
            如果技能没有用来解场，优先放技能
         */
        power?.let{
            if(power.canPower() && power.cost <= me.usableResource){
                power.action.attackHero()
            }
        }
        /*
            最后拍打脸法术和地标
         */
        for (handCard in handCards) {
            if (handCard.cost <= me.usableResource) {
                if (handCard.cardType == CardTypeEnum.LOCATION) {
                    handCard.action.power()?.pointTo(firstPlayCard)
                }
                else {
                    handCard.action.power()
                }
            }
        }
    }

    /*
        解场方案：
        1. 优先保证过墙
        2. 随从保活
        3. 过不了墙全部 all in

        Tips: 不考虑536、435和地标
     */
    private fun getBestPlayCardTarget(
        rivalPlayCard: Card,
        myPlayCards: MutableList<Card>,
        myHandCards: MutableList<Card>,
        usableResource: Int,
        power: Card?
    ): MutableList<Card> {
        var res = mutableListOf<Card>()
        var currentResource = usableResource
        var useSpell: Card? = null
        var rivalPlayCardHealth = rivalPlayCard.health
        // 使用技能
        power?.let{
            if (currentResource >= 2 && power.canPower() && rivalPlayCard.canBeTargetedByMyHeroPowers()) {
                res.add(power)
                currentResource -= 2
                rivalPlayCardHealth -= 2
            }
        }
        if(rivalPlayCardHealth <= 0) return res
        // 使用精神灼烧 只使用一张
        for(handCard in myHandCards) {
            if(handCard.cardId == "NX2_019" && usableResource >= 2 && rivalPlayCard.canBeTargetedByMySpells()) {
                currentResource -= 1
                useSpell = handCard
                rivalPlayCardHealth -= 2
                break
            }
        }
        if(rivalPlayCardHealth <= 0 && useSpell != null) {
            res.add(useSpell)
            return res
        }
        /*
            第一行 使用卡牌
            第二行 不使用卡牌
         */
        val dp = MutableList(2) {
            MutableList(myPlayCards.size) {
                DPEntity(mutableListOf(), 0)
            }
        }
        dp[0][0] = DPEntity(mutableListOf(myPlayCards[0]), myPlayCards[0].atc)
        for (i in 1 until myPlayCards.size) {
            if (myPlayCards[i].canAttack()) {
                for (j in 0 until i) {
                    dp[0][i] = nearestOf(
                        DPEntity(dp[0][j].cards.plusElement(myPlayCards[i]).toMutableList(), dp[0][j].damage + myPlayCards[i].atc),
                        DPEntity(dp[1][j].cards.plusElement(myPlayCards[i]).toMutableList(), dp[1][j].damage + myPlayCards[i].atc),
                        rivalPlayCardHealth
                    )
                }
            }
            dp[1][i] = nearestOf(dp[0][i-1], dp[1][i-1], rivalPlayCardHealth)
        }
        res += nearestOf(dp[0].last(), dp[1].last(), rivalPlayCardHealth).cards
        if (useSpell != null) res.add(useSpell)
        return res
    }

    override fun executeDiscoverChooseCard(vararg cards: Card): Int {
//        TODO("执行选择发现牌策略")
//        返回选择卡牌的下标，这里选择的第一张牌
        return 0
    }

    private fun nearestOf(a: DPEntity, b: DPEntity, target: Int): DPEntity {
        if (a.damage >= target && b.damage >= target) {
            if (a.damage < b.damage) return a
            else return b
        }
        else if (a.damage >= target) return a
        else if (b.damage >= target) return b
        else return maxOf(a, b)
    }
    inner class DPEntity(
        cards: MutableList<Card> = mutableListOf(),
        val damage: Int
    ) : Comparable<DPEntity> {
        val cards: MutableList<Card> = cards.toMutableList()

        override fun compareTo(other: DPEntity): Int = this.damage.compareTo(other.damage)
    }
}