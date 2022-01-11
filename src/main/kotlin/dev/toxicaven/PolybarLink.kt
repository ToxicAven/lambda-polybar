package dev.toxicaven

import com.lambda.client.event.events.ShutdownEvent
import com.lambda.client.module.Category
import com.lambda.client.plugin.api.PluginModule
import com.lambda.client.util.InfoCalculator
import com.lambda.client.util.InfoCalculator.speed
import com.lambda.client.util.TickTimer
import com.lambda.client.util.TimeUnit
import com.lambda.client.util.TpsCalculator
import com.lambda.client.util.math.CoordinateConverter.asString
import com.lambda.client.util.math.VectorUtils.toBlockPos
import com.lambda.client.util.threads.runSafeR
import com.lambda.client.util.threads.safeListener
import com.lambda.commons.utils.MathUtils
import com.lambda.event.listener.listener
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.io.DataOutputStream
import java.io.OutputStream
import java.net.Socket

internal object PolybarLink: PluginModule(
    name = "PolybarLink",
    category = Category.CLIENT,
    description = "Sends game details to a polybar",
    showOnArray = false,
    pluginMain = PolybarPlugin
) {
    private val cycleOne by setting("Cycle 1", LineInfo.SERVER_IP)
    private val cycleTwo by setting("Cycle 2", LineInfo.USERNAME)
    private val cycleThree by setting("Cycle 3", LineInfo.DIMENSION, { cycleTwo != LineInfo.NONE })
    private val cycleFour by setting("Cycle 4", LineInfo.HEALTH, { (cycleThree != LineInfo.NONE) || (cycleTwo != LineInfo.NONE && cycleThree != LineInfo.NONE) })
    private val time by setting("Update Time", 30, 15..120, 5)
    private val timer = TickTimer(TimeUnit.SECONDS)
    private var testString : String = ""
    private var currentCycle = cycleOne
    private var forceEnable = false

    init {
        onEnable {
            currentCycle = cycleOne
            forceEnable = true
        }

        onDisable {
            testString = ""
            triggerSocket()
        }

        listener<ShutdownEvent> {
            testString = ""
            triggerSocket()
        }

        safeListener<TickEvent.ClientTickEvent> {
            if (timer.tick(time) || forceEnable) {
                forceEnable = false
                when (currentCycle) {
                    cycleOne -> {
                        currentCycle = if (cycleTwo == LineInfo.NONE
                            && cycleThree == LineInfo.NONE
                            && cycleFour == LineInfo.NONE) cycleOne else cycleTwo
                        testString = getLine(cycleOne)
                    }
                    cycleTwo -> {
                        currentCycle = if (cycleThree == LineInfo.NONE
                            && cycleFour == LineInfo.NONE) cycleOne else cycleThree
                        testString = getLine(cycleTwo)
                    }
                    cycleThree -> {
                        currentCycle = if (cycleFour == LineInfo.NONE) cycleOne else cycleFour
                        testString = getLine(cycleThree)
                    }
                    cycleFour -> {
                        currentCycle = cycleOne
                        testString = getLine(cycleFour)
                    }
                    else -> getLine(cycleOne)
                }
                if (testString.isNotBlank()) {
                    triggerSocket()
                }
            } else return@safeListener
        }
    }

    private fun getLine(line: LineInfo): String {
        return when (line) {
            LineInfo.WORLD -> {
                when {
                    mc.isIntegratedServerRunning -> "Singleplayer"
                    mc.currentServerData != null -> "Multiplayer"
                    else -> "Main Menu"
                }
            }
            LineInfo.DIMENSION -> {
                InfoCalculator.dimension()
            }
            LineInfo.USERNAME -> {
                mc.session.username
            }
            LineInfo.HEALTH -> {
                if (mc.player != null) "${mc.player.health.toInt()} HP"
                else "No HP"
            }
            LineInfo.HUNGER -> {
                if (mc.player != null) "${mc.player.foodStats.foodLevel} hunger"
                else "No Hunger"
            }
            LineInfo.SERVER_IP -> {
                InfoCalculator.getServerType()
            }
            LineInfo.COORDS -> {
                "(${mc.player.positionVector.toBlockPos().asString()})"
            }
            LineInfo.SPEED -> {
                runSafeR {
                    "${"%.1f".format(speed())} m/s"
                } ?: "No Speed"
            }
            LineInfo.HELD_ITEM -> {
                "Holding ${mc.player?.heldItemMainhand?.displayName ?: "Air"}" // Holding air meme
            }
            LineInfo.FPS -> {
                "${Minecraft.getDebugFPS()} FPS"
            }
            LineInfo.TPS -> {
                if (mc.player != null) "${MathUtils.round(TpsCalculator.tickRate, 1)} tps"
                else "No Tps"
            }
            else -> ""
        }
    }

    private fun triggerSocket() {
        try {
        val socket = Socket("localhost", 46721)
        val outputStream: OutputStream = socket.getOutputStream()
        val dataOutputStream = DataOutputStream(outputStream)

        // write the message we want to send
        dataOutputStream.writeUTF(testString)
        dataOutputStream.flush() // send the message

        dataOutputStream.close() // close the output stream when we're done
        socket.close()
        } catch (ignored: Exception) {}
    }
    private enum class LineInfo {
        WORLD, DIMENSION, USERNAME, HEALTH, HUNGER, SERVER_IP, COORDS, SPEED, HELD_ITEM, FPS, TPS, NONE
    }
}