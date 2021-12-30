package io.github.mg138.indicator.bossbar

import io.github.mg138.bookshelf.damage.DamageEvent
import io.github.mg138.bookshelf.stat.stat.Stat
import io.github.mg138.bookshelf.stat.stat.StatSingle
import io.github.mg138.bookshelf.stat.type.StatType
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.boss.BossBar
import net.minecraft.entity.boss.ServerBossBar
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import java.util.*
import kotlin.jvm.internal.Ref
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

object AttackedEntityBossBar {
    private val map: MutableMap<UUID, Triple<LivingEntity, ServerBossBar, Ref.IntRef>> = mutableMapOf()

    fun register() {
        DamageEvent.AFTER_BOOK_DAMAGE.register { event ->
            val damager = event.damager
            if (damager is ServerPlayerEntity) {
                show(damager, event.damagee, event.damages)
            }
            ActionResult.PASS
        }
        ServerTickEvents.END_SERVER_TICK.register {
            val toRemove: MutableList<UUID> = mutableListOf()

            map.forEach { (uuid, triple) ->
                val age = triple.third.apply { element++ }

                if (age.element >= 60) {
                    triple.second.clearPlayers()
                    toRemove += uuid
                }
            }

            toRemove.forEach {
                map.remove(it)
            }
        }
    }

    private fun color(percentage: Float): BossBar.Color {
        if (percentage > 0.7) return BossBar.Color.GREEN
        if (percentage > 0.4) return BossBar.Color.YELLOW
        return BossBar.Color.RED
    }

    private fun damagesToString(damages: Map<StatType, Double>): Text {
        if (damages.isEmpty()) return LiteralText.EMPTY

        val text = LiteralText(" ")

        val sorted = damages.entries.sortedByDescending { it.value }

        var sum = 0.0
        val icons = LiteralText(" ")
        sorted.forEach { (type, damage) ->
            sum += damage
            icons.append(type.icon).append(" ")
        }

        val sumText = sorted.first().key.indicator(StatSingle(sum))
        text.append(sumText).append(icons)

        return text
    }

    private fun show(player: ServerPlayerEntity, damagee: LivingEntity, damages: Map<StatType, Double>) {
        val old = map[player.uuid]
        val percentage = damagee.health / damagee.maxHealth
        val name = damagee.displayName.copy().append(damagesToString(damages))

        val bossBar: ServerBossBar
        if (old == null) {
            bossBar = ServerBossBar(name, color(percentage), BossBar.Style.PROGRESS).apply {
                addPlayer(player)
            }
        } else {
            bossBar = old.second
            bossBar.color = color(percentage)
            bossBar.name = name
        }
        bossBar.percent = percentage
        map[player.uuid] = Triple(damagee, bossBar, Ref.IntRef().apply { element = 0 })
    }
}