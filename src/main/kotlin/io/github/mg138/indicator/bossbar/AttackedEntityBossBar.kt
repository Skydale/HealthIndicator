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
import kotlin.jvm.internal.Ref
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

object AttackedEntityBossBar {
    private val map: MutableMap<ServerPlayerEntity, Triple<LivingEntity, ServerBossBar, Ref.IntRef>> = mutableMapOf()

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
                val age = triple.third.apply { element++ }

                if (age.element >= 60) {
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

    private fun damagesToString(damages: Map<StatType, Double>): Text {
        val text = LiteralText(" (")
        var sum = 0.0
        damages.entries.forEach {
            sum += it.value

            text.append(it.key.icon)
            if (it == damages.entries.last()) {
                val sumText = sum.roundToLong().toString()
                text.append(" $sumText)")
            }
        }
        return text
    }

    private fun show(player: ServerPlayerEntity, damagee: LivingEntity, damages: Map<StatType, Double>) {
        val old = map[player]
        val percentage = damagee.health / damagee.maxHealth
        val name = damagee.displayName.copy().append(damagesToString(damages))

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
        map[player] = Triple(damagee, bossBar, Ref.IntRef().apply { element = 0 })
    }
}