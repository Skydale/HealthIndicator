package io.github.mg138.indicator.bossbar

import io.github.mg138.bookshelf.damage.DamageEvent
import io.github.mg138.bookshelf.stat.stat.Stat
import io.github.mg138.bookshelf.stat.type.StatType
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.boss.BossBar
import net.minecraft.entity.boss.ServerBossBar
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import kotlin.math.abs

object AttackedEntityBossBar {
    private val map: MutableMap<ServerPlayerEntity, Triple<LivingEntity, ServerBossBar, Int>> = mutableMapOf()

    fun register() {
        DamageEvent.AFTER_BOOK_DAMAGE.register { event ->
            val damager = event.damager
            if (damager is ServerPlayerEntity) {
                show(damager, event.damagee, event.damages)
            }
            ActionResult.PASS
        }
        ServerTickEvents.END_SERVER_TICK.register {
            map.forEach { (player, triple) ->
                if (abs(triple.first.age - triple.third) > 60) {
                    triple.second.clearPlayers()
                    map.remove(player)
                }
            }
        }
    }

    private fun color(percentage: Float): BossBar.Color {
        if (percentage > 0.7) return BossBar.Color.GREEN
        if (percentage > 0.4) return BossBar.Color.YELLOW
        return BossBar.Color.RED
    }

    private fun damagesToString(damages: Map<StatType, Stat>): Text {
        val text = LiteralText("(")
        damages.entries.forEach {
            text.append(it.key.indicator(it.value))

            if (it != damages.entries.last()) {
                text.append(" ")
            } else {
                text.append(")")
            }
        }
        return text
    }

    private fun show(player: ServerPlayerEntity, damagee: LivingEntity, damages: Map<StatType, Stat>) {
        val old = map[player]
        val percentage = damagee.health / damagee.maxHealth
        val name = damagee.displayName.copy().append(damagesToString(damages))
        val time = damagee.age

        val bossBar: ServerBossBar
        if (old == null) {
            bossBar = ServerBossBar(name, color(percentage), BossBar.Style.NOTCHED_10).apply {
                addPlayer(player)
            }
        } else {
            bossBar = old.second
            bossBar.color = color(percentage)
            bossBar.name = name
        }
        bossBar.percent = percentage
        map[player] = Triple(damagee, bossBar, time)
    }
}